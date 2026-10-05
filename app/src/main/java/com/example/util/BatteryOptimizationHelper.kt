package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

object BatteryOptimizationHelper {

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
      return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }
    return true
  }

  fun requestIgnoreBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (isIgnoringBatteryOptimizations(context)) {
        Toast.makeText(context, "Background playback is already unrestricted!", Toast.LENGTH_SHORT).show()
        return
      }
      try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
          data = Uri.parse("package:${context.packageName}")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
      } catch (_: Exception) {
        openAppInfoSettings(context)
      }
    } else {
      Toast.makeText(context, "Background playback is unrestricted on this Android version.", Toast.LENGTH_SHORT).show()
    }
  }

  fun openAppInfoSettings(context: Context) {
    try {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {
      try {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
      } catch (_: Exception) {}
    }
  }
}
