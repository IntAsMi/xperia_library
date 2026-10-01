package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R

class DapPlaybackService : MediaSessionService() {

  companion object {
    const val CHANNEL_ID = "dap_playback_channel"
    const val NOTIFICATION_ID = 1001

    @Volatile
    var activeMediaSession: MediaSession? = null

    fun startService(context: Context) {
      try {
        val intent = Intent(context, DapPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return activeMediaSession
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "DAP Audio Playback",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows media controls for DAP Console audio playback"
        setShowBadge(false)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    activeMediaSession = null
    super.onDestroy()
  }
}
