package com.example.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.R

class DapPlaybackService : MediaSessionService() {

  companion object {
    @Volatile
    var activeSession: MediaSession? = null
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    try {
      val provider = DefaultMediaNotificationProvider.Builder(applicationContext)
        .setNotificationId(1001)
        .setChannelId("dap_media_session_channel")
        .setChannelName(R.string.app_name)
        .build()
      setMediaNotificationProvider(provider)
    } catch (_: Exception) {}
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return activeSession
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = activeSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    activeSession = null
    super.onDestroy()
  }
}
