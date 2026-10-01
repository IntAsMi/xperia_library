package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.widget.ACTION_WIDGET_NEXT
import com.example.widget.ACTION_WIDGET_PLAY_PAUSE
import com.example.widget.ACTION_WIDGET_PREV

class DapPlaybackService : Service() {

  companion object {
    const val CHANNEL_ID = "dap_playback_channel"
    const val NOTIFICATION_ID = 1001

    const val ACTION_START_OR_UPDATE = "com.example.dap.ACTION_START_OR_UPDATE"
    const val ACTION_STOP_SERVICE = "com.example.dap.ACTION_STOP_SERVICE"

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_ARTIST = "extra_artist"
    const val EXTRA_IS_PLAYING = "extra_is_playing"

    fun updatePlaybackState(
      context: Context,
      title: String,
      folderOrSpecs: String,
      isPlaying: Boolean
    ) {
      try {
        val intent = Intent(context, DapPlaybackService::class.java).apply {
          action = ACTION_START_OR_UPDATE
          putExtra(EXTRA_TITLE, title)
          putExtra(EXTRA_ARTIST, folderOrSpecs)
          putExtra(EXTRA_IS_PLAYING, isPlaying)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    fun stopService(context: Context) {
      try {
        val intent = Intent(context, DapPlaybackService::class.java).apply {
          action = ACTION_STOP_SERVICE
        }
        context.startService(intent)
      } catch (_: Exception) {}
    }
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    // Guarantee startForeground is called immediately on service start
    val initial = buildNotification("DAP Console", "Audiophile Engine Ready", false)
    startForegroundSafely(initial)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_SERVICE) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    val title = intent?.getStringExtra(EXTRA_TITLE) ?: "DAP Console"
    val artist = intent?.getStringExtra(EXTRA_ARTIST) ?: "Audiophile Playback"
    val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, false) ?: false

    val notification = buildNotification(title, artist, isPlaying)
    startForegroundSafely(notification)

    return START_STICKY
  }

  private fun startForegroundSafely(notification: Notification) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun buildNotification(title: String, subtitle: String, isPlaying: Boolean): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPending = PendingIntent.getActivity(
      this, 0, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val prevIntent = Intent(ACTION_WIDGET_PREV).setPackage(packageName)
    val prevPending = PendingIntent.getBroadcast(
      this, 10, prevIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val playPauseIntent = Intent(ACTION_WIDGET_PLAY_PAUSE).setPackage(packageName)
    val playPausePending = PendingIntent.getBroadcast(
      this, 11, playPauseIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val nextIntent = Intent(ACTION_WIDGET_NEXT).setPackage(packageName)
    val nextPending = PendingIntent.getBroadcast(
      this, 12, nextIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(title)
      .setContentText(subtitle)
      .setSmallIcon(R.drawable.ic_widget_dap)
      .setContentIntent(contentPending)
      .setOngoing(isPlaying)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .addAction(R.drawable.ic_widget_prev, "Previous", prevPending)
      .addAction(playIcon, if (isPlaying) "Pause" else "Play", playPausePending)
      .addAction(R.drawable.ic_widget_next, "Next", nextPending)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "DAP Audio Playback",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows playback notification for DAP Console"
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    super.onDestroy()
  }
}
