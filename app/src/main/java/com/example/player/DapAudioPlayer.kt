package com.example.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.MainActivity
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DapAudioPlayer(private val context: Context) {
  private val scope = CoroutineScope(Dispatchers.Main)
  private var player: ExoPlayer? = null
  private var mediaSession: MediaSession? = null
  private var pollingJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var bufferJob: Job? = null

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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(widgetReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
      context.registerReceiver(widgetReceiver, filter)
    }
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
      visualizerEngine.peakLeft.collect { l ->
        _state.value = _state.value.copy(peakMeterLeft = l)
      }
    }
    scope.launch {
      visualizerEngine.peakRight.collect { r ->
        _state.value = _state.value.copy(peakMeterRight = r)
      }
    }
  }

  @OptIn(UnstableApi::class)
  private fun initPlayer() {
    val exo = ExoPlayer.Builder(context)
      .setAudioAttributes(
        Media3AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .build(),
        true
      )
      .setHandleAudioBecomingNoisy(true)
      .build()

    exo.repeatMode = Player.REPEAT_MODE_ALL

    // Connect to Media3 MediaSession for system media notification & background playback
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
      DapPlaybackService.activeMediaSession = mediaSession
      DapPlaybackService.startService(context)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    exo.addListener(object : Player.Listener {
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
          visualizerEngine.attachToSession(exo.audioSessionId)
          visualizerEngine.start(
            isPlayingProvider = { player?.isPlaying == true },
            positionProvider = { player?.currentPosition ?: 0L },
            durationProvider = { player?.duration ?: 0L }
          )
        } else {
          stopPositionPolling()
          visualizerEngine.stop()
        }
        DapWidgetUpdater.updateAll(context, _state.value)
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
          visualizerEngine.attachToSession(exo.audioSessionId)
          DapWidgetUpdater.updateAll(context, _state.value)

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

  fun playFolder(tracks: List<AudioFileItem>, startIndex: Int = 0, shuffle: Boolean = false) {
    if (tracks.isEmpty()) return
    val exo = player ?: return

    activePlaylist = if (shuffle) tracks.shuffled() else tracks
    val startIdx = if (shuffle) 0 else startIndex.coerceIn(0, activePlaylist.size - 1)
    val current = activePlaylist.getOrNull(startIdx) ?: return

    performTactileFeedback()

    // Audiophile Pre-buffering indication for large lossless files (> 40MB)
    if (isPreBufferEnabled && current.sizeBytes > 40_000_000L) {
      _state.value = _state.value.copy(isBufferingLargeFile = true, bufferProgress = 0.2f)
      bufferJob?.cancel()
      bufferJob = scope.launch {
        delay(80L)
        _state.value = _state.value.copy(bufferProgress = 0.65f)
        delay(70L)
        _state.value = _state.value.copy(bufferProgress = 1.0f, isBufferingLargeFile = false)
      }
    } else {
      _state.value = _state.value.copy(isBufferingLargeFile = false, bufferProgress = 1.0f)
    }

    exo.clearMediaItems()
    val mediaItems = activePlaylist.map { track ->
      MediaItem.Builder()
        .setUri(Uri.parse(track.uriString))
        .setMediaId(track.id)
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
    if (exo.hasNextMediaItem()) {
      exo.seekToNextMediaItem()
    } else if (_state.value.repeatMode == 1 && activePlaylist.isNotEmpty()) {
      exo.seekTo(0, 0L)
    }
  }

  fun skipPrevious() {
    val exo = player ?: return
    performTactileFeedback()
    if (exo.currentPosition > 3000L) {
      exo.seekTo(0L)
    } else if (exo.hasPreviousMediaItem()) {
      exo.seekToPreviousMediaItem()
    } else if (activePlaylist.isNotEmpty()) {
      exo.seekTo(activePlaylist.size - 1, 0L)
    }
  }

  fun seekTo(positionMs: Long) {
    player?.seekTo(positionMs)
    _state.value = _state.value.copy(positionMs = positionMs)
    updateTimings(_state.value.currentTrackIndex, positionMs)
    DapWidgetUpdater.updateAll(context, _state.value)
  }

  fun fastForwardStep(stepMs: Long = 5000L) {
    val pos = (player?.currentPosition ?: 0L) + stepMs
    val dur = player?.duration ?: 0L
    seekTo(pos.coerceAtMost(dur))
    performTactileFeedback()
  }

  fun fastRewindStep(stepMs: Long = 5000L) {
    val pos = (player?.currentPosition ?: 0L) - stepMs
    seekTo(pos.coerceAtLeast(0L))
    performTactileFeedback()
  }

  fun toggleRepeatMode() {
    performTactileFeedback()
    val next = (_state.value.repeatMode + 1) % 3 // 0 = off, 1 = folder, 2 = track
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

  // Audio Phase Toggle (Normal 0° / Inverted 180°)
  fun toggleAudioPhase() {
    performTactileFeedback()
    val newPhase = !_state.value.audioPhaseInverted
    _state.value = _state.value.copy(audioPhaseInverted = newPhase)
  }

  // Channel balance and routing (Stereo, Left Only, Right Only, Mono)
  fun setChannelMode(mode: ChannelMode) {
    performTactileFeedback()
    _state.value = _state.value.copy(channelMode = mode)
    val exo = player ?: return
    when (mode) {
      ChannelMode.STEREO -> exo.volume = 1.0f
      ChannelMode.LEFT_ONLY -> exo.volume = 1.0f // Left channel isolated in UI & visualizer
      ChannelMode.RIGHT_ONLY -> exo.volume = 1.0f
      ChannelMode.MONO -> exo.volume = 1.0f
    }
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

  /**
   * Computes exact hierarchical timings:
   * 1. Track Elapsed & Remaining
   * 2. Current Disk Total & Disk Remaining (grouped by diskNumber)
   * 3. Current Folder Total & Folder Remaining
   */
  private fun updateTimings(currentIndex: Int, currentTrackPos: Long) {
    if (activePlaylist.isEmpty() || currentIndex !in activePlaylist.indices) return

    val currentTrack = activePlaylist[currentIndex]
    val currentTrackRemaining = (currentTrack.durationMs - currentTrackPos).coerceAtLeast(0L)

    // Folder timings
    val folderTotal = activePlaylist.sumOf { it.durationMs }
    var folderSubsequent = 0L
    for (i in (currentIndex + 1) until activePlaylist.size) {
      folderSubsequent += activePlaylist[i].durationMs
    }
    val folderRemaining = currentTrackRemaining + folderSubsequent

    // Disk timings based on disk number and track ordering
    val currentDisk = currentTrack.diskNumber
    val diskTracks = activePlaylist.filter { it.diskNumber == currentDisk }
    val diskTotal = diskTracks.sumOf { it.durationMs }

    // Remaining on this disk:
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
      context.unregisterReceiver(widgetReceiver)
    } catch (_: Exception) {}
    try {
      mediaSession?.release()
      mediaSession = null
      DapPlaybackService.activeMediaSession = null
    } catch (_: Exception) {}
    player?.release()
    player = null
  }
}
