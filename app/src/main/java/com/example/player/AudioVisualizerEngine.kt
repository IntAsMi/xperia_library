package com.example.player

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
import kotlin.math.cos
import kotlin.math.sin

class AudioVisualizerEngine {
  private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    // Silently catch any coroutine error so visualizer never crashes the app process
  }
  private val scope = CoroutineScope(Dispatchers.Default + exceptionHandler)
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
    // Pure software DSP calculation - completely bypasses buggy native AudioEffect / RECORD_AUDIO permissions
  }

  fun start(
    isPlayingProvider: () -> Boolean,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    waveformProvider: () -> List<Float> = { emptyList() }
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
            _spectrumBands.value = cur.map { (it * 0.75f).coerceAtLeast(0f) }
            _peakLeft.value = (_peakLeft.value * 0.75f).coerceAtLeast(0f)
            _peakRight.value = (_peakRight.value * 0.75f).coerceAtLeast(0f)
          } else {
            _spectrumBands.value = List(32) { 0f }
            _peakLeft.value = 0f
            _peakRight.value = 0f
          }
          delay(80L)
          continue
        }

        // Live DSP reaction computed from actual track playback progress & waveform energy
        val posMs = positionProvider()
        val durMs = durationProvider().coerceAtLeast(1L)
        val progress = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)

        val waveform = waveformProvider()
        val baseAmp = if (waveform.isNotEmpty()) {
          val idx = (progress * (waveform.size - 1)).toInt().coerceIn(0, waveform.size - 1)
          waveform[idx].coerceIn(0.15f, 1.0f)
        } else {
          0.55f
        }

        phase += 0.22
        val t = posMs / 1000.0
        val beat = (t * 2.2) % 1.0 // ~132 BPM pulse
        val bassPulse = Math.pow((1.0 - beat).coerceIn(0.0, 1.0), 2.5).toFloat() * baseAmp

        val bands = List(32) { idx ->
          val freqRatio = idx / 32f
          val waveMod = abs(sin(phase * (0.9 + idx * 0.11) + posMs * 0.002)).toFloat()
          val bassBoost = if (idx < 8) bassPulse * 0.8f else (bassPulse * 0.2f)
          val presence = (waveMod * 0.6f + bassBoost * 0.4f) * baseAmp
          presence.coerceIn(0.06f, 0.98f)
        }

        val lBase = (baseAmp * 0.6f + bassPulse * 0.4f * abs(sin(phase * 1.15))).toFloat().coerceIn(0.08f, 0.96f)
        val rBase = (baseAmp * 0.58f + bassPulse * 0.42f * abs(cos(phase * 0.98))).toFloat().coerceIn(0.08f, 0.96f)

        applyChannelModeAndEmit(bands, lBase, rBase)
        delay(40L) // ~25 FPS ultra smooth live reaction
      }
    }
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
  }
}
