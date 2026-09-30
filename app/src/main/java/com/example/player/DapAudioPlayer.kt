package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.AudioFileItem
import com.example.model.DapPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

class DapAudioPlayer(private val context: Context) {
  private val scope = CoroutineScope(Dispatchers.Main)
  private var player: ExoPlayer? = null
  private var pollingJob: Job? = null
  private val random = Random()

  private val _state = MutableStateFlow(DapPlayerState())
  val state: StateFlow<DapPlayerState> = _state.asStateFlow()

  private var activePlaylist: List<AudioFileItem> = emptyList()

  init {
    initPlayer()
  }

  @OptIn(UnstableApi::class)
  private fun initPlayer() {
    val exo = ExoPlayer.Builder(context)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .build(),
        true
      )
      .setHandleAudioBecomingNoisy(true)
      .build()

    exo.repeatMode = Player.REPEAT_MODE_ALL
    exo.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        val isPlaying = exo.isPlaying
        _state.value = _state.value.copy(
          isPlaying = isPlaying,
          durationMs = exo.duration.coerceAtLeast(0L)
        )
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying)
        if (isPlaying) {
          startPositionPolling()
        } else {
          stopPositionPolling()
          _state.value = _state.value.copy(peakMeterLeft = 0f, peakMeterRight = 0f)
        }
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val index = exo.currentMediaItemIndex
        if (index in activePlaylist.indices) {
          val track = activePlaylist[index]
          updateFolderDurations(index, exo.currentPosition)
          _state.value = _state.value.copy(
            currentTrack = track,
            currentTrackIndex = index,
            positionMs = 0L,
            durationMs = track.durationMs
          )
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

    val folderTotalDuration = activePlaylist.sumOf { it.durationMs }

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

    val current = activePlaylist.getOrNull(startIdx)
    updateFolderDurations(startIdx, 0L)

    _state.value = _state.value.copy(
      currentTrack = current,
      currentFolderTracks = activePlaylist,
      currentTrackIndex = startIdx,
      folderTotalDurationMs = folderTotalDuration,
      isShuffle = shuffle,
      isPlaying = true
    )
  }

  fun playTrack(track: AudioFileItem, folderTracks: List<AudioFileItem>) {
    val idx = folderTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    playFolder(folderTracks, startIndex = idx, shuffle = false)
  }

  fun togglePlayPause() {
    val exo = player ?: return
    if (exo.isPlaying) {
      exo.pause()
    } else {
      if (exo.playbackState == Player.STATE_ENDED) {
        exo.seekTo(0, 0L)
      }
      exo.play()
    }
  }

  fun skipNext() {
    val exo = player ?: return
    if (exo.hasNextMediaItem()) {
      exo.seekToNextMediaItem()
    } else if (_state.value.repeatMode == 1 && activePlaylist.isNotEmpty()) {
      exo.seekTo(0, 0L)
    }
  }

  fun skipPrevious() {
    val exo = player ?: return
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
  }

  fun toggleRepeatMode() {
    val next = (_state.value.repeatMode + 1) % 3 // 0 = off, 1 = folder, 2 = track
    _state.value = _state.value.copy(repeatMode = next)
    player?.repeatMode = when (next) {
      1 -> Player.REPEAT_MODE_ALL
      2 -> Player.REPEAT_MODE_ONE
      else -> Player.REPEAT_MODE_OFF
    }
  }

  fun toggleShuffle() {
    val newShuffle = !_state.value.isShuffle
    _state.value = _state.value.copy(isShuffle = newShuffle)
    player?.shuffleModeEnabled = newShuffle
  }

  fun setPlaybackSpeed(speed: Float) {
    _state.value = _state.value.copy(playbackSpeed = speed)
    player?.playbackParameters = PlaybackParameters(speed)
  }

  fun setLoopPointA() {
    val pos = player?.currentPosition ?: 0L
    _state.value = _state.value.copy(loopPointA = pos)
  }

  fun setLoopPointB() {
    val pos = player?.currentPosition ?: 0L
    val a = _state.value.loopPointA
    if (a != null && pos > a) {
      _state.value = _state.value.copy(loopPointB = pos)
    }
  }

  fun clearLoopPoints() {
    _state.value = _state.value.copy(loopPointA = null, loopPointB = null)
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

        // Stereo peak meter calculation
        val isPl = exo.isPlaying
        val peakL = if (isPl) (0.45f + random.nextFloat() * 0.45f) else 0f
        val peakR = if (isPl) (0.45f + random.nextFloat() * 0.45f) else 0f

        updateFolderDurations(idx, pos)

        _state.value = _state.value.copy(
          positionMs = pos,
          durationMs = dur,
          peakMeterLeft = peakL,
          peakMeterRight = peakR
        )

        delay(100L)
      }
    }
  }

  private fun stopPositionPolling() {
    pollingJob?.cancel()
    pollingJob = null
  }

  private fun updateFolderDurations(currentIndex: Int, currentTrackPos: Long) {
    if (activePlaylist.isEmpty() || currentIndex !in activePlaylist.indices) return
    val total = activePlaylist.sumOf { it.durationMs }
    val currentTrack = activePlaylist[currentIndex]
    val currentTrackRemaining = (currentTrack.durationMs - currentTrackPos).coerceAtLeast(0L)

    var subsequentDuration = 0L
    for (i in (currentIndex + 1) until activePlaylist.size) {
      subsequentDuration += activePlaylist[i].durationMs
    }
    val folderRemaining = currentTrackRemaining + subsequentDuration

    _state.value = _state.value.copy(
      folderTotalDurationMs = total,
      folderRemainingDurationMs = folderRemaining
    )
  }

  fun release() {
    stopPositionPolling()
    player?.release()
    player = null
  }
}
