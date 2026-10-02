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
    const val CHANNEL_ID = "dap_media_session_channel"
    const val NOTIFICATION_ID = 1001

    @Volatile
    var instance: DapPlaybackService? = null

    @Volatile
    var activeSession: MediaSession? = null
      set(value) {
        field = value
        val s = instance
        if (s != null && value != null) {
          try {
            if (!s.isSessionAdded(value)) {
              s.addSession(value)
            }
          } catch (_: Exception) {}
        }
      }
  }

  private fun createNotificationChannel() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      val channel = android.app.NotificationChannel(
        CHANNEL_ID,
        getString(R.string.app_name),
        android.app.NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Audiophile DAP Console Media Controls"
        setShowBadge(false)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }
      val nm = getSystemService(android.app.NotificationManager::class.java)
      nm?.createNotificationChannel(channel)
    }
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    instance = this

    createNotificationChannel()

    try {
      val defaultProvider = DefaultMediaNotificationProvider.Builder(applicationContext)
        .setNotificationId(NOTIFICATION_ID)
        .setChannelId(CHANNEL_ID)
        .setChannelName(R.string.app_name)
        .build()

      val provider = object : androidx.media3.session.MediaNotification.Provider {
        override fun createNotification(
          mediaSession: MediaSession,
          customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
          actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
          onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
        ): androidx.media3.session.MediaNotification {
          val mediaNotification = defaultProvider.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
          mediaNotification.notification.icon = R.drawable.ic_widget_dap
          try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
              val field = android.app.Notification::class.java.getDeclaredField("mSmallIcon")
              field.isAccessible = true
              field.set(mediaNotification.notification, android.graphics.drawable.Icon.createWithResource(applicationContext, R.drawable.ic_widget_dap))
            }
          } catch (_: Exception) {}
          return mediaNotification
        }

        override fun handleCustomCommand(
          session: MediaSession,
          action: String,
          extras: android.os.Bundle
        ): Boolean = false
      }
      setMediaNotificationProvider(provider)
    } catch (_: Exception) {}

    val s = activeSession
    if (s != null) {
      try {
        if (!isSessionAdded(s)) {
          addSession(s)
        }
      } catch (_: Exception) {}
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val s = activeSession
    if (s != null) {
      try {
        if (!isSessionAdded(s)) {
          addSession(s)
        }
      } catch (_: Exception) {}
    }
    return super.onStartCommand(intent, flags, startId)
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
    val s = activeSession
    if (s != null) {
      try {
        removeSession(s)
      } catch (_: Exception) {}
    }
    instance = null
    super.onDestroy()
  }
}
