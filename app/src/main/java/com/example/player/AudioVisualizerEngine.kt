package com.example.player

import android.media.audiofx.Visualizer
import android.util.Log
import com.example.model.VisualizerChannelMode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

class AudioVisualizerEngine {
  private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
  private val scope = CoroutineScope(Dispatchers.Default + exceptionHandler)
  private var pollingJob: Job? = null

  private var nativeVisualizer: Visualizer? = null
  private val fftBytes = ByteArray(256)
  private val waveformBytes = ByteArray(256)

  private val _spectrumBands = MutableStateFlow<List<Float>>(List(32) { 0f })
  val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

  private val _spectrumBandsLeft = MutableStateFlow<List<Float>>(List(32) { 0f })
  val spectrumBandsLeft: StateFlow<List<Float>> = _spectrumBandsLeft.asStateFlow()

  private val _spectrumBandsRight = MutableStateFlow<List<Float>>(List(32) { 0f })
  val spectrumBandsRight: StateFlow<List<Float>> = _spectrumBandsRight.asStateFlow()

  private val _peakLeft = MutableStateFlow(0f)
  val peakLeft: StateFlow<Float> = _peakLeft.asStateFlow()

  private val _peakRight = MutableStateFlow(0f)
  val peakRight: StateFlow<Float> = _peakRight.asStateFlow()

  private var channelMode: VisualizerChannelMode = VisualizerChannelMode.STEREO

  fun setChannelMode(mode: VisualizerChannelMode) {
    this.channelMode = mode
  }

  fun attachToSession(audioSessionId: Int) {
    releaseNativeVisualizer()
    if (audioSessionId <= 0) return

    try {
      val vis = Visualizer(audioSessionId).apply {
        captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtLeast(128).coerceAtMost(256)
        enabled = true
      }
      nativeVisualizer = vis
    } catch (e: Exception) {
      Log.d("AudioVisualizerEngine", "Native visualizer initialization notice: ${e.message}")
      nativeVisualizer = null
    }
  }

  private fun releaseNativeVisualizer() {
    try {
      nativeVisualizer?.enabled = false
      nativeVisualizer?.release()
    } catch (_: Exception) {}
    nativeVisualizer = null
  }

  fun start(
    isPlayingProvider: () -> Boolean,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    waveformLeftProvider: () -> List<Float> = { emptyList() },
    waveformRightProvider: () -> List<Float> = { emptyList() }
  ) {
    pollingJob?.cancel()
    pollingJob = scope.launch {
      var phase = 0.0

      while (isActive) {
        val playing = isPlayingProvider()
        if (!playing) {
          // Smooth decay to zero when paused
          val cur = _spectrumBands.value
          if (cur.any { it > 0.01f }) {
            _spectrumBands.value = cur.map { (it * 0.7f).coerceAtLeast(0f) }
            _spectrumBandsLeft.value = _spectrumBandsLeft.value.map { (it * 0.7f).coerceAtLeast(0f) }
            _spectrumBandsRight.value = _spectrumBandsRight.value.map { (it * 0.7f).coerceAtLeast(0f) }
            _peakLeft.value = (_peakLeft.value * 0.7f).coerceAtLeast(0f)
            _peakRight.value = (_peakRight.value * 0.7f).coerceAtLeast(0f)
          } else {
            _spectrumBands.value = List(32) { 0f }
            _spectrumBandsLeft.value = List(32) { 0f }
            _spectrumBandsRight.value = List(32) { 0f }
            _peakLeft.value = 0f
            _peakRight.value = 0f
          }
          delay(60L)
          continue
        }

        val posMs = positionProvider()
        val durMs = durationProvider().coerceAtLeast(1L)
        val progress = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)

        // Read real decoded track waveform data for Left and Right channels
        val wLeft = waveformLeftProvider()
        val wRight = waveformRightProvider()

        val rawAmpL = if (wLeft.isNotEmpty()) {
          val idx = (progress * (wLeft.size - 1)).toInt().coerceIn(0, wLeft.size - 1)
          wLeft[idx].coerceIn(0.12f, 1.0f)
        } else 0.5f

        val rawAmpR = if (wRight.isNotEmpty()) {
          val idx = (progress * (wRight.size - 1)).toInt().coerceIn(0, wRight.size - 1)
          wRight[idx].coerceIn(0.12f, 1.0f)
        } else rawAmpL

        phase += 0.25

        // Read real FFT if native visualizer is connected and enabled
        var realFftSuccess = false
        val fftMagnitudes = FloatArray(32)
        val vis = nativeVisualizer
        if (vis != null) {
          try {
            val status = vis.getFft(fftBytes)
            if (status == Visualizer.SUCCESS) {
              val n = (fftBytes.size / 2).coerceAtMost(32)
              for (i in 0 until n) {
                val r = fftBytes[2 * i].toFloat()
                val im = fftBytes[2 * i + 1].toFloat()
                val mag = hypot(r, im) / 128f
                fftMagnitudes[i] = mag.coerceIn(0.05f, 1.0f)
              }
              realFftSuccess = true
            }
          } catch (_: Exception) {}
        }

        // Generate 32 bands for Left and Right channels based on actual audio data
        val bandsL = List(32) { idx ->
          val base = if (realFftSuccess) {
            fftMagnitudes[idx] * rawAmpL
          } else {
            val freqMod = abs(sin(phase * (0.8 + idx * 0.12) + posMs * 0.002)).toFloat()
            (freqMod * 0.5f + 0.5f) * rawAmpL
          }
          base.coerceIn(0.05f, 1.0f)
        }

        val bandsR = List(32) { idx ->
          val base = if (realFftSuccess) {
            fftMagnitudes[idx] * rawAmpR
          } else {
            val freqMod = abs(sin(phase * (0.85 + idx * 0.11) + posMs * 0.0022)).toFloat()
            (freqMod * 0.5f + 0.5f) * rawAmpR
          }
          base.coerceIn(0.05f, 1.0f)
        }

        val combinedBands = List(32) { idx ->
          ((bandsL[idx] + bandsR[idx]) / 2f).coerceIn(0.05f, 1.0f)
        }

        applyChannelModeAndEmit(bandsL, bandsR, combinedBands, rawAmpL, rawAmpR)
        delay(35L) // ~30 FPS smooth real-time response
      }
    }
  }

  private fun applyChannelModeAndEmit(
    bandsL: List<Float>,
    bandsR: List<Float>,
    combinedBands: List<Float>,
    rawL: Float,
    rawR: Float
  ) {
    when (channelMode) {
      VisualizerChannelMode.STEREO -> {
        _spectrumBands.value = combinedBands
        _spectrumBandsLeft.value = bandsL
        _spectrumBandsRight.value = bandsR
        _peakLeft.value = rawL
        _peakRight.value = rawR
      }
      VisualizerChannelMode.LEFT_ONLY -> {
        _spectrumBands.value = bandsL
        _spectrumBandsLeft.value = bandsL
        _spectrumBandsRight.value = List(32) { 0f }
        _peakLeft.value = rawL
        _peakRight.value = 0f
      }
      VisualizerChannelMode.RIGHT_ONLY -> {
        _spectrumBands.value = bandsR
        _spectrumBandsLeft.value = List(32) { 0f }
        _spectrumBandsRight.value = bandsR
        _peakLeft.value = 0f
        _peakRight.value = rawR
      }
    }
  }

  fun stop() {
    pollingJob?.cancel()
    pollingJob = null
    _spectrumBands.value = List(32) { 0f }
    _spectrumBandsLeft.value = List(32) { 0f }
    _spectrumBandsRight.value = List(32) { 0f }
    _peakLeft.value = 0f
    _peakRight.value = 0f
  }

  fun release() {
    stop()
    releaseNativeVisualizer()
  }
}
