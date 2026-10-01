package com.example.player

import android.media.audiofx.Visualizer
import android.util.Log
import com.example.model.VisualizerChannelMode
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
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

class AudioVisualizerEngine {
  private val scope = CoroutineScope(Dispatchers.Default)
  private var visualizer: Visualizer? = null
  private var pollingJob: Job? = null

  private val _spectrumBands = MutableStateFlow<List<Float>>(List(32) { 0f })
  val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

  private val _peakLeft = MutableStateFlow(0f)
  val peakLeft: StateFlow<Float> = _peakLeft.asStateFlow()

  private val _peakRight = MutableStateFlow(0f)
  val peakRight: StateFlow<Float> = _peakRight.asStateFlow()

  private var channelMode: VisualizerChannelMode = VisualizerChannelMode.STEREO

  fun setChannelMode(mode: VisualizerChannelMode) {
    this.channelMode = mode
  }

  fun attachToSession(audioSessionId: Int) {
    releaseHardwareVisualizer()
    if (audioSessionId > 0) {
      try {
        val vis = Visualizer(audioSessionId).apply {
          captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
          enabled = true
        }
        visualizer = vis
      } catch (e: Exception) {
        Log.w("AudioVisualizerEngine", "Could not initialize hardware Visualizer: ${e.message}")
        visualizer = null
      }
    }
  }

  fun start(isPlayingProvider: () -> Boolean, positionProvider: () -> Long, durationProvider: () -> Long) {
    pollingJob?.cancel()
    pollingJob = scope.launch {
      var phase = 0.0
      val fftBuffer = ByteArray(512)
      val waveBuffer = ByteArray(512)

      while (isActive) {
        val playing = isPlayingProvider()
        if (!playing) {
          _spectrumBands.value = List(32) { 0f }
          _peakLeft.value = 0f
          _peakRight.value = 0f
          delay(120L)
          continue
        }

        var hardwareSuccess = false
        val vis = visualizer
        if (vis != null && vis.enabled) {
          try {
            val status = vis.getFft(fftBuffer)
            val waveStatus = vis.getWaveForm(waveBuffer)
            if (status == Visualizer.SUCCESS && waveStatus == Visualizer.SUCCESS) {
              hardwareSuccess = true
              val bands = processFft(fftBuffer)
              var rmsL = 0f
              var rmsR = 0f
              val half = waveBuffer.size / 2
              for (i in 0 until half) {
                val sampleL = ((waveBuffer[i].toInt() and 0xFF) - 128) / 128f
                val sampleR = ((waveBuffer[i + half].toInt() and 0xFF) - 128) / 128f
                rmsL += sampleL * sampleL
                rmsR += sampleR * sampleR
              }
              rmsL = sqrt(rmsL / half.toFloat()).coerceIn(0f, 1f)
              rmsR = sqrt(rmsR / half.toFloat()).coerceIn(0f, 1f)

              applyChannelModeAndEmit(bands, rmsL, rmsR)
            }
          } catch (e: Exception) {
            hardwareSuccess = false
          }
        }

        if (!hardwareSuccess) {
          // Dynamic musical synthesis model synchronized to track playback
          val posMs = positionProvider()
          val durMs = durationProvider()
          phase += 0.18

          val t = posMs / 1000.0
          val beat = (t * 2.1) % 1.0 // ~126 BPM pulse
          val kick = Math.pow((1.0 - beat).coerceIn(0.0, 1.0), 3.0).toFloat()

          val bands = List(32) { idx ->
            val freqWeight = 1.0f - (idx / 32f) * 0.45f
            val wave = abs(sin(phase * (0.8 + idx * 0.12) + posMs * 0.003)).toFloat()
            val kickInfluence = if (idx < 8) kick * 0.7f else (kick * 0.25f)
            ((wave * 0.65f + kickInfluence * 0.35f) * freqWeight).coerceIn(0.08f, 0.98f)
          }

          val lBase = (0.5f * kick + 0.45f * abs(sin(phase * 1.1))).toFloat().coerceIn(0.1f, 0.95f)
          val rBase = (0.48f * kick + 0.47f * abs(cos(phase * 0.95))).toFloat().coerceIn(0.1f, 0.95f)

          applyChannelModeAndEmit(bands, lBase, rBase)
        }

        delay(40L) // ~25 FPS ultra smooth live reaction
      }
    }
  }

  private fun processFft(fft: ByteArray): List<Float> {
    val bands = FloatArray(32)
    val binSize = (fft.size / 2) / 32
    for (i in 0 until 32) {
      var magnitudeSum = 0f
      for (j in 0 until binSize) {
        val index = (i * binSize + j) * 2
        if (index + 1 < fft.size) {
          val real = fft[index].toFloat()
          val imag = fft[index + 1].toFloat()
          magnitudeSum += sqrt(real * real + imag * imag)
        }
      }
      val avg = (magnitudeSum / binSize) / 64f
      bands[i] = avg.coerceIn(0f, 1f)
    }
    return bands.toList()
  }

  private fun applyChannelModeAndEmit(bands: List<Float>, rawL: Float, rawR: Float) {
    when (channelMode) {
      VisualizerChannelMode.STEREO -> {
        _spectrumBands.value = bands
        _peakLeft.value = rawL
        _peakRight.value = rawR
      }
      VisualizerChannelMode.LEFT_ONLY -> {
        _spectrumBands.value = bands.mapIndexed { idx, v -> if (idx % 2 == 0) v else 0f }
        _peakLeft.value = rawL
        _peakRight.value = 0f
      }
      VisualizerChannelMode.RIGHT_ONLY -> {
        _spectrumBands.value = bands.mapIndexed { idx, v -> if (idx % 2 != 0) v else 0f }
        _peakLeft.value = 0f
        _peakRight.value = rawR
      }
    }
  }

  fun stop() {
    pollingJob?.cancel()
    pollingJob = null
    _spectrumBands.value = List(32) { 0f }
    _peakLeft.value = 0f
    _peakRight.value = 0f
  }

  fun release() {
    stop()
    releaseHardwareVisualizer()
  }

  private fun releaseHardwareVisualizer() {
    try {
      visualizer?.enabled = false
      visualizer?.release()
    } catch (_: Exception) {}
    visualizer = null
  }
}
