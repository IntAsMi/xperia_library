package com.example.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.R
import com.example.model.AudioFileItem
import com.example.model.ChannelMode
import com.example.model.DapPlayerState
import com.example.model.SleepTimerOption
import com.example.model.VisualizerChannelMode
import com.example.widget.ACTION_WIDGET_NEXT
import com.example.widget.ACTION_WIDGET_PLAY_PAUSE
import com.example.widget.ACTION_WIDGET_PREV
import com.example.widget.DapWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class DapAudioPlayer(private val context: Context) {
  private val scope = CoroutineScope(Dispatchers.Main)
  private var player: ExoPlayer? = null
  private var mediaSession: MediaSession? = null
  private var pollingJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var bufferJob: Job? = null
  private var waveformJob: Job? = null
  private var specResolutionJob: Job? = null

  val dspAudioProcessor = AudiophileDspAudioProcessor()
  val visualizerEngine = AudioVisualizerEngine()
  val outputManager = AudioOutputManager(context)
  private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

  private val _state = MutableStateFlow(DapPlayerState())
  val state: StateFlow<DapPlayerState> = _state.asStateFlow()

  private var activePlaylist: List<AudioFileItem> = emptyList()
  private var currentSleepOption: SleepTimerOption = SleepTimerOption.OFF
  var isHapticEnabled: Boolean = true
  var isPreBufferEnabled: Boolean = true

  private val widgetReceiver = object : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
      when (intent?.action) {
        ACTION_WIDGET_PLAY_PAUSE -> togglePlayPause()
        ACTION_WIDGET_NEXT -> skipNext()
        ACTION_WIDGET_PREV -> skipPrevious()
      }
    }
  }

  init {
    initPlayer()
    registerWidgetReceiver()
    observeOutputAndVisualizer()
  }

  private fun registerWidgetReceiver() {
    val filter = IntentFilter().apply {
      addAction(ACTION_WIDGET_PLAY_PAUSE)
      addAction(ACTION_WIDGET_NEXT)
      addAction(ACTION_WIDGET_PREV)
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(widgetReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
      } else {
        context.registerReceiver(widgetReceiver, filter)
      }
    } catch (_: Exception) {}
  }

  private fun observeOutputAndVisualizer() {
    scope.launch {
      outputManager.currentOutputSpec.collect { spec ->
        _state.value = _state.value.copy(audioOutputSpec = spec)
      }
    }
    scope.launch {
      outputManager.availableDevices.collect { devs ->
        _state.value = _state.value.copy(availableOutputs = devs)
      }
    }
    scope.launch {
      visualizerEngine.spectrumBands.collect { bands ->
        _state.value = _state.value.copy(spectrumBands = bands)
      }
    }
    scope.launch {
      visualizerEngine.spectrumBandsLeft.collect { lBands ->
        _state.value = _state.value.copy(spectrumBandsLeft = lBands)
      }
    }
    scope.launch {
      visualizerEngine.spectrumBandsRight.collect { rBands ->
        _state.value = _state.value.copy(spectrumBandsRight = rBands)
      }
    }
    scope.launch {
      visualizerEngine.peakLeft.collect { l ->
        _state.value = _state.value.copy(peakMeterLeft = l)
      }
    }
    scope.launch {
      visualizerEngine.peakRight.collect { r ->
        _state.value = _state.value.copy(peakMeterRight = r)
      }
    }
    scope.launch {
      outputManager.shizukuStatus.collect { sStatus ->
        _state.value = _state.value.copy(
          shizukuReport = sStatus.hardwareSinkReport,
          isShizukuPrivileged = sStatus.isPermissionGranted
        )
      }
    }
  }

  private fun initPlayer() {
    dspAudioProcessor.frameListener = visualizerEngine

    val renderersFactory = object : DefaultRenderersFactory(context) {
      override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
      ): AudioSink? {
        return DefaultAudioSink.Builder(context)
          .setEnableFloatOutput(true) // Enforces 24-bit and 32-bit float output without truncation
          .setAudioProcessors(arrayOf(dspAudioProcessor))
          .build()
      }
    }

    val exo = ExoPlayer.Builder(context, renderersFactory)
      .setAudioAttributes(
        Media3AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .build(),
        true
      )
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .setHandleAudioBecomingNoisy(true)
      .setSeekBackIncrementMs(10000L)
      .setSeekForwardIncrementMs(10000L)
      .build()

    exo.repeatMode = Player.REPEAT_MODE_ALL

    // Setup MediaSession for system media controls (the Android Quick Settings media player card)
    val activityIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val sessionActivity = PendingIntent.getActivity(
      context, 0, activityIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      mediaSession = MediaSession.Builder(context, exo)
        .setSessionActivity(sessionActivity)
        .build()

      DapPlaybackService.activeSession = mediaSession

      // Safely start the MediaSessionService
      val serviceIntent = Intent(context, DapPlaybackService::class.java)
      context.startService(serviceIntent)
    } catch (e: Exception) {
      Log.w("DapAudioPlayer", "MediaSession setup error: ${e.message}")
    }

    exo.addListener(object : Player.Listener {
      override fun onAudioSessionIdChanged(audioSessionId: Int) {
        visualizerEngine.attachToSession(audioSessionId)
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        val isPlaying = exo.isPlaying
        _state.value = _state.value.copy(
          isPlaying = isPlaying,
          durationMs = exo.duration.coerceAtLeast(0L)
        )
        DapWidgetUpdater.updateAll(context, _state.value)
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying)
        if (isPlaying) {
          startPositionPolling()
          visualizerEngine.start(
            isPlayingProvider = { player?.isPlaying == true },
            positionProvider = { player?.currentPosition ?: 0L },
            durationProvider = { player?.duration ?: 0L },
            waveformLeftProvider = { _state.value.currentTrack?.waveformLeft ?: emptyList() },
            waveformRightProvider = { _state.value.currentTrack?.waveformRight ?: emptyList() }
          )
        } else {
          stopPositionPolling()
          visualizerEngine.stop()
        }
        DapWidgetUpdater.updateAll(context, _state.value)
      }

      override fun onPlayerError(error: PlaybackException) {
        Log.e("DapAudioPlayer", "ExoPlayer playback error: ${error.message}", error)
        _state.value = _state.value.copy(isPlaying = false)
      }

      override fun onTracksChanged(tracks: Tracks) {
        for (group in tracks.groups) {
          if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
            for (i in 0 until group.length) {
              if (group.isTrackSelected(i)) {
                val format = group.getTrackFormat(i)
                val current = _state.value.currentTrack
                if (current != null) {
                  val realSr = if (format.sampleRate > 0) format.sampleRate else current.sampleRate
                  val realCh = if (format.channelCount > 0) format.channelCount else current.channels
                  val realBd = when (format.pcmEncoding) {
                    C.ENCODING_PCM_FLOAT -> 32
                    C.ENCODING_PCM_32BIT -> 32
                    C.ENCODING_PCM_24BIT -> 24
                    C.ENCODING_PCM_16BIT -> 16
                    else -> current.bitDepth
                  }
                  val realBr = if (format.bitrate > 0) (format.bitrate / 1000) else current.bitrateKbps
                  val updated = current.copy(
                    sampleRate = realSr,
                    channels = realCh,
                    bitDepth = realBd,
                    bitrateKbps = realBr
                  )
                  _state.value = _state.value.copy(currentTrack = updated)
                  outputManager.updateTrackFidelity(updated)
                }
              }
            }
          }
        }
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val index = exo.currentMediaItemIndex
        if (index in activePlaylist.indices) {
          val track = activePlaylist[index]
          updateTimings(index, exo.currentPosition)
          _state.value = _state.value.copy(
            currentTrack = track,
            currentTrackIndex = index,
            currentDiskNumber = track.diskNumber,
            positionMs = 0L,
            durationMs = track.durationMs
          )
          DapWidgetUpdater.updateAll(context, _state.value)
          extractWaveformForTrack(track)
          resolveAndApplyGenuineSpecs(track)

          // Sleep timer check: End of Track
          if (currentSleepOption == SleepTimerOption.END_OF_TRACK && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            pause()
            setSleepTimer(SleepTimerOption.OFF)
          }
        }
      }
    })

    player = exo
  }

  fun resolveAndApplyGenuineSpecs(track: AudioFileItem) {
    specResolutionJob?.cancel()
    specResolutionJob = scope.launch(Dispatchers.IO) {
      val genuine = ActualAudioSpecExtractor.extractGenuineSpecs(
        context = context,
        uriString = track.uriString,
        fileName = track.fileName,
        fileSize = track.sizeBytes,
        fallbackItem = track
      )
      val updated = track.copy(
        codec = genuine.codec,
        sampleRate = genuine.sampleRateHz,
        bitDepth = genuine.bitDepth,
        bitrateKbps = genuine.bitrateKbps,
        channels = genuine.channelCount,
        durationMs = if (genuine.durationMs > 0L) genuine.durationMs else track.durationMs
      )
      withContext(Dispatchers.Main) {
        if (_state.value.currentTrack?.id == track.id) {
          _state.value = _state.value.copy(currentTrack = updated)
        }
        outputManager.updateTrackFidelity(updated)
      }
    }
  }

  fun requestShizukuPermission() {
    outputManager.requestShizukuPermission()
  }

  fun playFolder(tracks: List<AudioFileItem>, startIndex: Int = 0, shuffle: Boolean = false) {
    if (tracks.isEmpty()) return
    val exo = player ?: return

    activePlaylist = if (shuffle) tracks.shuffled() else tracks
    val startIdx = if (shuffle) 0 else startIndex.coerceIn(0, activePlaylist.size - 1)
    val current = activePlaylist.getOrNull(startIdx) ?: return

    performTactileFeedback()

    // Pre-buffering indication for large lossless files
    if (isPreBufferEnabled && current.sizeBytes > 40_000_000L) {
      _state.value = _state.value.copy(isBufferingLargeFile = true, bufferProgress = 0.2f)
      bufferJob?.cancel()
      bufferJob = scope.launch {
        delay(50L)
        _state.value = _state.value.copy(bufferProgress = 0.7f)
        delay(40L)
        _state.value = _state.value.copy(bufferProgress = 1.0f, isBufferingLargeFile = false)
      }
    } else {
      _state.value = _state.value.copy(isBufferingLargeFile = false, bufferProgress = 1.0f)
    }

    try {
      exo.clearMediaItems()
      val mediaItems = activePlaylist.map { track ->
        val uri = if (track.uriString.contains("demo_synth") || track.uriString.contains("raw/demo_synth")) {
          RawResourceDataSource.buildRawResourceUri(R.raw.demo_synth)
        } else {
          Uri.parse(track.uriString)
        }

        // Set rich technical metadata so Android Quick Settings Media Player card displays file name and file specs
        val metadata = MediaMetadata.Builder()
          .setTitle(track.fileName)
          .setArtist("${track.codec} • ${track.sampleRate / 1000}kHz/${track.bitDepth}bit • ${track.bitrateKbps}kbps")
          .setAlbumTitle(track.title.ifBlank { track.fileName })
          .setDisplayTitle(track.fileName)
          .setIsPlayable(true)
          .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
          .build()

        MediaItem.Builder()
          .setUri(uri)
          .setMediaId(track.id)
          .setMediaMetadata(metadata)
          .build()
      }

      exo.setMediaItems(mediaItems, startIdx, 0L)
      exo.prepare()
      exo.play()

      updateTimings(startIdx, 0L)

      _state.value = _state.value.copy(
        currentTrack = current,
        currentFolderTracks = activePlaylist,
        currentTrackIndex = startIdx,
        currentDiskNumber = current.diskNumber,
        isShuffle = shuffle,
        isPlaying = true
      )

      DapWidgetUpdater.updateAll(context, _state.value)
      extractWaveformForTrack(current)
      resolveAndApplyGenuineSpecs(current)

      // Ensure MediaSessionService is aware of active session to show Quick Settings media player card
      try {
        DapPlaybackService.activeSession = mediaSession
        val serviceIntent = Intent(context, DapPlaybackService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
      } catch (e: Exception) {
        try {
          val serviceIntent = Intent(context, DapPlaybackService::class.java)
          context.startService(serviceIntent)
        } catch (_: Exception) {}
      }
    } catch (e: Exception) {
      Log.e("DapAudioPlayer", "Error starting playback: ${e.message}", e)
    }
  }

  private fun extractWaveformForTrack(track: AudioFileItem) {
    waveformJob?.cancel()
    waveformJob = scope.launch(Dispatchers.IO) {
      val stereo = AudioWaveformExtractor.extractStereoWaveform(context, track.uriString, track.durationMs)
      kotlinx.coroutines.withContext(Dispatchers.Main) {
        if (_state.value.currentTrack?.id == track.id) {
          val updated = _state.value.currentTrack?.copy(
            waveformLeft = stereo.left,
            waveformRight = stereo.right,
            waveform = stereo.combined
          )
          if (updated != null) {
            _state.value = _state.value.copy(currentTrack = updated)
          }
        }
      }
    }
  }

  fun playTrack(track: AudioFileItem, folderTracks: List<AudioFileItem>) {
    val idx = folderTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    playFolder(folderTracks, startIndex = idx, shuffle = false)
  }

  fun togglePlayPause() {
    val exo = player ?: return
    performTactileFeedback()
    if (exo.isPlaying) {
      exo.pause()
    } else {
      if (exo.playbackState == Player.STATE_ENDED) {
        exo.seekTo(0, 0L)
      }
      exo.play()
    }
  }

  fun pause() {
    player?.pause()
  }

  fun skipNext() {
    val exo = player ?: return
    performTactileFeedback()
    try {
      if (exo.hasNextMediaItem()) {
        exo.seekToNextMediaItem()
      } else if (exo.mediaItemCount > 0 && (_state.value.repeatMode == 1 || _state.value.repeatMode == 0)) {
        exo.seekTo(0, 0L)
      }
    } catch (e: Exception) {
      Log.e("DapAudioPlayer", "Error during skipNext: ${e.message}", e)
    }
  }

  fun skipPrevious() {
    val exo = player ?: return
    performTactileFeedback()
    try {
      if (exo.currentPosition > 3000L) {
        exo.seekTo(0L)
      } else if (exo.hasPreviousMediaItem()) {
        exo.seekToPreviousMediaItem()
      } else if (exo.mediaItemCount > 0) {
        val targetIdx = (exo.mediaItemCount - 1).coerceAtLeast(0)
        exo.seekTo(targetIdx, 0L)
      }
    } catch (e: Exception) {
      Log.e("DapAudioPlayer", "Error during skipPrevious: ${e.message}", e)
    }
  }

  fun seekTo(positionMs: Long) {
    player?.seekTo(positionMs)
    _state.value = _state.value.copy(positionMs = positionMs)
    updateTimings(_state.value.currentTrackIndex, positionMs)
    DapWidgetUpdater.updateAll(context, _state.value)
  }

  fun fastForwardStep(stepMs: Long = 10000L) {
    val pos = (player?.currentPosition ?: 0L) + stepMs
    val dur = player?.duration ?: 0L
    seekTo(pos.coerceAtMost(dur))
    performTactileFeedback()
  }

  fun fastRewindStep(stepMs: Long = 10000L) {
    val pos = (player?.currentPosition ?: 0L) - stepMs
    seekTo(pos.coerceAtLeast(0L))
    performTactileFeedback()
  }

  fun toggleRepeatMode() {
    performTactileFeedback()
    val next = (_state.value.repeatMode + 1) % 3
    _state.value = _state.value.copy(repeatMode = next)
    player?.repeatMode = when (next) {
      1 -> Player.REPEAT_MODE_ALL
      2 -> Player.REPEAT_MODE_ONE
      else -> Player.REPEAT_MODE_OFF
    }
  }

  fun toggleShuffle() {
    performTactileFeedback()
    val newShuffle = !_state.value.isShuffle
    _state.value = _state.value.copy(isShuffle = newShuffle)
    player?.shuffleModeEnabled = newShuffle
  }

  fun setPlaybackSpeed(speed: Float) {
    performTactileFeedback()
    _state.value = _state.value.copy(playbackSpeed = speed)
    player?.playbackParameters = PlaybackParameters(speed)
  }

  fun setLoopPointA() {
    performTactileFeedback()
    val pos = player?.currentPosition ?: 0L
    _state.value = _state.value.copy(loopPointA = pos)
  }

  fun setLoopPointB() {
    performTactileFeedback()
    val pos = player?.currentPosition ?: 0L
    val a = _state.value.loopPointA
    if (a != null && pos > a) {
      _state.value = _state.value.copy(loopPointB = pos)
    }
  }

  fun clearLoopPoints() {
    performTactileFeedback()
    _state.value = _state.value.copy(loopPointA = null, loopPointB = null)
  }

  /**
   * Toggles audio phase between 0° (normal) and 180° (inverted).
   * This is actively processed in real-time by AudiophileDspAudioProcessor on the actual audio output!
   */
  fun toggleAudioPhase() {
    performTactileFeedback()
    val newMode = if (_state.value.audioPhaseMode == com.example.model.AudioPhaseMode.NORMAL) {
      com.example.model.AudioPhaseMode.INVERT_BOTH
    } else {
      com.example.model.AudioPhaseMode.NORMAL
    }
    setAudioPhaseMode(newMode)
  }

  fun setAudioPhaseMode(mode: com.example.model.AudioPhaseMode) {
    performTactileFeedback()
    dspAudioProcessor.phaseMode = mode
    dspAudioProcessor.isPhaseInverted = mode != com.example.model.AudioPhaseMode.NORMAL
    _state.value = _state.value.copy(
      audioPhaseMode = mode,
      audioPhaseInverted = mode != com.example.model.AudioPhaseMode.NORMAL
    )
  }

  /**
   * Switches channel mode between STEREO, LEFT_ONLY, RIGHT_ONLY, and MONO.
   * This is actively processed in real-time by AudiophileDspAudioProcessor on the actual audio output!
   */
  fun setChannelMode(mode: ChannelMode) {
    performTactileFeedback()
    dspAudioProcessor.channelMode = mode
    _state.value = _state.value.copy(channelMode = mode)
    visualizerEngine.setChannelMode(
      when (mode) {
        ChannelMode.LEFT_ONLY -> VisualizerChannelMode.LEFT_ONLY
        ChannelMode.RIGHT_ONLY -> VisualizerChannelMode.RIGHT_ONLY
        ChannelMode.MONO -> VisualizerChannelMode.MONO
        else -> VisualizerChannelMode.STEREO
      }
    )
  }

  fun setVisualizerChannelMode(mode: VisualizerChannelMode) {
    performTactileFeedback()
    _state.value = _state.value.copy(visualizerChannelMode = mode)
    visualizerEngine.setChannelMode(mode)
  }

  fun selectOutputDevice(deviceId: Int) {
    performTactileFeedback()
    outputManager.selectDevice(deviceId)
    _state.value = _state.value.copy(selectedOutputDeviceId = deviceId)
  }

  fun setSleepTimer(option: SleepTimerOption) {
    currentSleepOption = option
    sleepTimerJob?.cancel()

    if (option == SleepTimerOption.OFF) {
      _state.value = _state.value.copy(sleepTimerRemainingSeconds = null)
      return
    }

    if (option.minutes > 0) {
      var remaining = option.minutes * 60
      _state.value = _state.value.copy(sleepTimerRemainingSeconds = remaining)

      sleepTimerJob = scope.launch {
        while (isActive && remaining > 0) {
          delay(1000L)
          remaining--
          _state.value = _state.value.copy(sleepTimerRemainingSeconds = remaining)
        }
        pause()
        _state.value = _state.value.copy(sleepTimerRemainingSeconds = null)
        currentSleepOption = SleepTimerOption.OFF
      }
    }
  }

  private fun performTactileFeedback() {
    if (!isHapticEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(20)
      }
    } catch (_: Exception) {}
  }

  private fun startPositionPolling() {
    pollingJob?.cancel()
    pollingJob = scope.launch {
      while (isActive) {
        val exo = player ?: break
        val pos = exo.currentPosition.coerceAtLeast(0L)
        val dur = exo.duration.coerceAtLeast(0L)
        val idx = exo.currentMediaItemIndex

        // A-B Looping check
        val a = _state.value.loopPointA
        val b = _state.value.loopPointB
        if (a != null && b != null && pos >= b) {
          exo.seekTo(a)
        }

        updateTimings(idx, pos)

        _state.value = _state.value.copy(
          positionMs = pos,
          durationMs = dur
        )

        DapWidgetUpdater.updateAll(context, _state.value)
        delay(200L)
      }
    }
  }

  private fun stopPositionPolling() {
    pollingJob?.cancel()
    pollingJob = null
  }

  private fun updateTimings(currentIndex: Int, currentTrackPos: Long) {
    if (activePlaylist.isEmpty() || currentIndex !in activePlaylist.indices) return

    val currentTrack = activePlaylist[currentIndex]
    val currentTrackRemaining = (currentTrack.durationMs - currentTrackPos).coerceAtLeast(0L)

    val folderTotal = activePlaylist.sumOf { it.durationMs }
    var folderSubsequent = 0L
    for (i in (currentIndex + 1) until activePlaylist.size) {
      folderSubsequent += activePlaylist[i].durationMs
    }
    val folderRemaining = currentTrackRemaining + folderSubsequent

    val currentDisk = currentTrack.diskNumber
    val diskTracks = activePlaylist.filter { it.diskNumber == currentDisk }
    val diskTotal = diskTracks.sumOf { it.durationMs }

    var diskSubsequent = 0L
    for (i in (currentIndex + 1) until activePlaylist.size) {
      val t = activePlaylist[i]
      if (t.diskNumber == currentDisk) {
        diskSubsequent += t.durationMs
      }
    }
    val diskRemaining = currentTrackRemaining + diskSubsequent

    _state.value = _state.value.copy(
      currentDiskNumber = currentDisk,
      diskTotalDurationMs = diskTotal,
      diskRemainingDurationMs = diskRemaining,
      folderTotalDurationMs = folderTotal,
      folderRemainingDurationMs = folderRemaining
    )
  }

  fun release() {
    stopPositionPolling()
    sleepTimerJob?.cancel()
    bufferJob?.cancel()
    visualizerEngine.release()
    try {
      context.stopService(Intent(context, DapPlaybackService::class.java))
    } catch (_: Exception) {}
    try {
      context.unregisterReceiver(widgetReceiver)
    } catch (_: Exception) {}
    try {
      mediaSession?.release()
      mediaSession = null
      DapPlaybackService.activeSession = null
    } catch (_: Exception) {}
    player?.release()
    player = null
  }
}
