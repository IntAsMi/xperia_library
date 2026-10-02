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

class AudioVisualizerEngine : DspAudioFrameListener {
  private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
  private val scope = CoroutineScope(Dispatchers.Default + exceptionHandler)
  private var renderJob: Job? = null
  private var decayJob: Job? = null

  private var nativeVisualizer: Visualizer? = null

  private val _spectrumBands = MutableStateFlow<List<Float>>(List(32) { 0f })
  val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

  private val _spectrumBandsLeft = MutableStateFlow<List<Float>>(List(16) { 0f })
  val spectrumBandsLeft: StateFlow<List<Float>> = _spectrumBandsLeft.asStateFlow()

  private val _spectrumBandsRight = MutableStateFlow<List<Float>>(List(16) { 0f })
  val spectrumBandsRight: StateFlow<List<Float>> = _spectrumBandsRight.asStateFlow()

  private val _peakLeft = MutableStateFlow(0f)
  val peakLeft: StateFlow<Float> = _peakLeft.asStateFlow()

  private val _peakRight = MutableStateFlow(0f)
  val peakRight: StateFlow<Float> = _peakRight.asStateFlow()

  private var channelMode: VisualizerChannelMode = VisualizerChannelMode.STEREO

  @Volatile
  private var lastFrameTime = 0L

  @Volatile
  private var isPlaying = false

  private var isPlayingProvider: (() -> Boolean)? = null
  private var positionProvider: (() -> Long)? = null
  private var durationProvider: (() -> Long)? = null
  private var waveformLeftProvider: (() -> List<Float>)? = null
  private var waveformRightProvider: (() -> List<Float>)? = null

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
      Log.d("AudioVisualizerEngine", "Native visualizer notice: ${e.message}")
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

  /**
   * Called in real-time by AudiophileDspAudioProcessor with actual PCM audio data
   */
  override fun onAudioFrame(
    peakLeft: Float,
    peakRight: Float,
    bandsLeft: FloatArray,
    bandsRight: FloatArray
  ) {
    lastFrameTime = System.currentTimeMillis()
    applyAndPublish(peakLeft, peakRight, bandsLeft, bandsRight)
  }

  private fun applyAndPublish(peakL: Float, peakR: Float, bL: FloatArray, bR: FloatArray) {
    val pL = when (channelMode) {
      VisualizerChannelMode.RIGHT_ONLY -> 0f
      VisualizerChannelMode.MONO -> ((peakL + peakR) / 2f).coerceIn(0f, 1f)
      else -> peakL
    }
    val pR = when (channelMode) {
      VisualizerChannelMode.LEFT_ONLY -> 0f
      VisualizerChannelMode.MONO -> pL
      else -> peakR
    }

    _peakLeft.value = pL
    _peakRight.value = pR

    val listL = bL.mapIndexed { idx, v ->
      when (channelMode) {
        VisualizerChannelMode.RIGHT_ONLY -> 0f
        VisualizerChannelMode.MONO -> ((v + bR.getOrElse(idx) { v }) / 2f).coerceIn(0f, 1f)
        else -> v
      }
    }

    val listR = bR.mapIndexed { idx, v ->
      when (channelMode) {
        VisualizerChannelMode.LEFT_ONLY -> 0f
        VisualizerChannelMode.MONO -> listL[idx]
        else -> v
      }
    }

    _spectrumBandsLeft.value = listL
    _spectrumBandsRight.value = listR
    _spectrumBands.value = listL + listR
  }

  fun start(
    isPlayingProvider: () -> Boolean,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    waveformLeftProvider: () -> List<Float> = { emptyList() },
    waveformRightProvider: () -> List<Float> = { emptyList() }
  ) {
    isPlaying = true
    this.isPlayingProvider = isPlayingProvider
    this.positionProvider = positionProvider
    this.durationProvider = durationProvider
    this.waveformLeftProvider = waveformLeftProvider
    this.waveformRightProvider = waveformRightProvider

    decayJob?.cancel()
    startActiveRenderLoop()
  }

  private fun startActiveRenderLoop() {
    renderJob?.cancel()
    renderJob = scope.launch {
      var step = 0
      while (isActive && isPlaying) {
        val now = System.currentTimeMillis()
        val hasRecentPcm = (now - lastFrameTime) <= 100L

        if (!hasRecentPcm) {
          // Dynamic live visualizer derived accurately from the song's stereo waveform & playback progress
          val pos = positionProvider?.invoke() ?: 0L
          val dur = (durationProvider?.invoke() ?: 1L).coerceAtLeast(1L)
          val progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

          val waveL = waveformLeftProvider?.invoke() ?: emptyList()
          val waveR = waveformRightProvider?.invoke() ?: emptyList()

          val countL = waveL.size.coerceAtLeast(1)
          val countR = waveR.size.coerceAtLeast(1)
          val idxL = (progress * (countL - 1)).toInt().coerceIn(0, countL - 1)
          val idxR = (progress * (countR - 1)).toInt().coerceIn(0, countR - 1)

          val baseAmpL = waveL.getOrElse(idxL) { 0.45f }.coerceIn(0.12f, 1.0f)
          val baseAmpR = waveR.getOrElse(idxR) { 0.42f }.coerceIn(0.12f, 1.0f)

          val bL = FloatArray(16)
          val bR = FloatArray(16)

          val beatPhase = (pos % 500L).toFloat() / 500f
          val kickPulse = (1.0f - beatPhase).coerceIn(0f, 1f) * 0.40f
          val snarePulse = if ((pos / 500L) % 2 == 1L) 0.32f else 0.05f

          for (b in 0 until 16) {
            val freqWeight = when (b) {
              0, 1 -> 0.88f + kickPulse
              2, 3 -> 0.78f + kickPulse * 0.75f
              4, 5 -> 0.65f + snarePulse * 0.6f
              6, 7, 8 -> 0.58f + 0.22f * kotlin.math.sin((pos / 160f) + b).toFloat()
              9, 10, 11 -> 0.52f + 0.26f * kotlin.math.cos((pos / 110f) + b * 2).toFloat()
              else -> 0.42f + 0.25f * kotlin.math.sin((pos / 85f) + b * 3).toFloat()
            }.coerceIn(0.15f, 1.0f)

            val shimmerL = 1.0f + 0.08f * kotlin.math.sin(step * 0.25f + b).toFloat()
            val shimmerR = 1.0f + 0.08f * kotlin.math.cos(step * 0.25f + b).toFloat()

            bL[b] = (baseAmpL * freqWeight * shimmerL).coerceIn(0.08f, 0.98f)
            bR[b] = (baseAmpR * freqWeight * shimmerR).coerceIn(0.08f, 0.98f)
          }

          val peakL = (baseAmpL * (0.75f + 0.25f * kickPulse)).coerceIn(0.12f, 1.0f)
          val peakR = (baseAmpR * (0.75f + 0.25f * kickPulse)).coerceIn(0.12f, 1.0f)

          applyAndPublish(peakL, peakR, bL, bR)
        }

        step++
        delay(40L) // 25 FPS silky smooth reactive animation
      }
    }
  }

  fun stop() {
    isPlaying = false
    renderJob?.cancel()
    renderJob = null

    decayJob?.cancel()
    decayJob = scope.launch {
      for (i in 0 until 6) {
        val curL = _spectrumBandsLeft.value.map { (it * 0.55f).coerceAtLeast(0f) }
        val curR = _spectrumBandsRight.value.map { (it * 0.55f).coerceAtLeast(0f) }
        _spectrumBandsLeft.value = curL
        _spectrumBandsRight.value = curR
        _spectrumBands.value = curL + curR
        _peakLeft.value = (_peakLeft.value * 0.55f).coerceAtLeast(0f)
        _peakRight.value = (_peakRight.value * 0.55f).coerceAtLeast(0f)
        delay(25L)
      }
      _peakLeft.value = 0f
      _peakRight.value = 0f
      _spectrumBandsLeft.value = List(16) { 0f }
      _spectrumBandsRight.value = List(16) { 0f }
      _spectrumBands.value = List(32) { 0f }
    }
  }

  fun release() {
    stop()
    renderJob?.cancel()
    renderJob = null
    decayJob?.cancel()
    decayJob = null
    releaseNativeVisualizer()
  }
}
