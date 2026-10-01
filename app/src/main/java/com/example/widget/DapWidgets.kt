package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.AudioFileItem
import com.example.model.DapPlayerState
import com.example.model.formatDuration

const val ACTION_WIDGET_PLAY_PAUSE = "com.example.dap.ACTION_WIDGET_PLAY_PAUSE"
const val ACTION_WIDGET_NEXT = "com.example.dap.ACTION_WIDGET_NEXT"
const val ACTION_WIDGET_PREV = "com.example.dap.ACTION_WIDGET_PREV"

class DapSimpleWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    for (widgetId in appWidgetIds) {
      updateSimpleWidget(context, appWidgetManager, widgetId, null)
    }
  }

  companion object {
    fun updateSimpleWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      widgetId: Int,
      state: DapPlayerState?
    ) {
      val views = RemoteViews(context.packageName, R.layout.widget_dap_simple_2x1)

      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      val openPending = PendingIntent.getActivity(
        context, 0, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_simple_root, openPending)

      if (state != null && state.currentTrack != null) {
        val track = state.currentTrack
        views.setTextViewText(R.id.widget_simple_title, track.title)
        views.setTextViewText(R.id.widget_simple_subtitle, "${track.codec} • ${track.sampleRate / 1000}kHz/${track.bitDepth}b")
        views.setTextViewText(R.id.widget_simple_time, "${state.formattedElapsed} / ${formatDuration(state.durationMs)}")
        views.setTextViewText(R.id.widget_simple_state, if (state.isPlaying) "PLAYING" else "PAUSED")
      } else {
        views.setTextViewText(R.id.widget_simple_title, "DAP Console")
        views.setTextViewText(R.id.widget_simple_subtitle, "Select music folder to play")
        views.setTextViewText(R.id.widget_simple_time, "--:-- / --:--")
        views.setTextViewText(R.id.widget_simple_state, "STANDBY")
      }

      appWidgetManager.updateAppWidget(widgetId, views)
    }
  }
}

class DapControlsWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    for (widgetId in appWidgetIds) {
      updateControlsWidget(context, appWidgetManager, widgetId, null)
    }
  }

  companion object {
    fun updateControlsWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      widgetId: Int,
      state: DapPlayerState?
    ) {
      val views = RemoteViews(context.packageName, R.layout.widget_dap_controls_4x1)

      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      val openPending = PendingIntent.getActivity(
        context, 1, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_controls_info_container, openPending)
      views.setOnClickPendingIntent(R.id.widget_controls_app_icon, openPending)

      // Prev Button PendingIntent
      val prevIntent = Intent(ACTION_WIDGET_PREV).setPackage(context.packageName)
      val prevPending = PendingIntent.getBroadcast(
        context, 2, prevIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

      // Play/Pause Button PendingIntent
      val playIntent = Intent(ACTION_WIDGET_PLAY_PAUSE).setPackage(context.packageName)
      val playPending = PendingIntent.getBroadcast(
        context, 3, playIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPending)

      // Next Button PendingIntent
      val nextIntent = Intent(ACTION_WIDGET_NEXT).setPackage(context.packageName)
      val nextPending = PendingIntent.getBroadcast(
        context, 4, nextIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

      if (state != null && state.currentTrack != null) {
        val track = state.currentTrack
        views.setTextViewText(R.id.widget_controls_title, track.title)
        views.setTextViewText(R.id.widget_controls_folder, track.fileName)
        views.setTextViewText(
          R.id.widget_controls_specs_time,
          "${track.codec} ${track.sampleRate / 1000}k/${track.bitDepth}b | ${state.formattedElapsed} / ${formatDuration(state.durationMs)}"
        )
        views.setImageViewResource(
          R.id.widget_btn_play_pause,
          if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )
      } else {
        views.setTextViewText(R.id.widget_controls_title, "DAP Console")
        views.setTextViewText(R.id.widget_controls_folder, "Ready for SD Card Playback")
        views.setTextViewText(R.id.widget_controls_specs_time, "FLAC Master | 00:00 / 00:00")
        views.setImageViewResource(R.id.widget_btn_play_pause, R.drawable.ic_widget_play)
      }

      appWidgetManager.updateAppWidget(widgetId, views)
    }
  }
}

object DapWidgetUpdater {
  fun updateAll(context: Context, state: DapPlayerState) {
    try {
      val manager = AppWidgetManager.getInstance(context) ?: return

      // Update 2x1 simple widgets
      val simpleComponent = ComponentName(context, DapSimpleWidgetProvider::class.java)
      val simpleIds = manager.getAppWidgetIds(simpleComponent)
      for (id in simpleIds) {
        DapSimpleWidgetProvider.updateSimpleWidget(context, manager, id, state)
      }

      // Update 4x1 controls widgets
      val controlsComponent = ComponentName(context, DapControlsWidgetProvider::class.java)
      val controlsIds = manager.getAppWidgetIds(controlsComponent)
      for (id in controlsIds) {
        DapControlsWidgetProvider.updateControlsWidget(context, manager, id, state)
      }
    } catch (e: Exception) {
      // Ignore widget update failures silently
    }
  }
}
