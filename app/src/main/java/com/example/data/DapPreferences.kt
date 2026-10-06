package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DapFontSize
import com.example.model.DapThemeSetting
import com.example.model.ScanningMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DapPreferences(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("dap_console_prefs", Context.MODE_PRIVATE)

  private val _themeFlow = MutableStateFlow(getTheme())
  val themeFlow: StateFlow<DapThemeSetting> = _themeFlow.asStateFlow()

  private val _lockPortraitFlow = MutableStateFlow(isPortraitLocked())
  val lockPortraitFlow: StateFlow<Boolean> = _lockPortraitFlow.asStateFlow()

  private val _fontSizeFlow = MutableStateFlow(getFontSize())
  val fontSizeFlow: StateFlow<DapFontSize> = _fontSizeFlow.asStateFlow()

  fun getRootFolderUri(): String? = prefs.getString("root_folder_uri", null)
  fun setRootFolderUri(uri: String?) = prefs.edit().putString("root_folder_uri", uri).apply()

  fun getLastVisitedFolderUri(): String? = prefs.getString("last_folder_uri", null)
  fun setLastVisitedFolderUri(uri: String?) = prefs.edit().putString("last_folder_uri", uri).apply()

  fun getLastPlayedTrackUri(): String? = prefs.getString("last_track_uri", null)
  fun getLastPlayedTrackTitle(): String? = prefs.getString("last_track_title", null)
  fun getLastPlayedTrackCodec(): String = prefs.getString("last_track_codec", "FLAC") ?: "FLAC"
  fun getLastPlayedTrackDuration(): Long = prefs.getLong("last_track_duration", 0L)
  fun getLastPlayedTrackPosition(): Long = prefs.getLong("last_track_pos", 0L)
  fun getLastPlayedTrackSampleRate(): Int = prefs.getInt("last_track_sr", 44100)
  fun getLastPlayedTrackBitDepth(): Int = prefs.getInt("last_track_bd", 16)

  fun saveLastPlayedTrack(uri: String, title: String, codec: String, duration: Long, position: Long, sr: Int, bd: Int) {
    prefs.edit()
      .putString("last_track_uri", uri)
      .putString("last_track_title", title)
      .putString("last_track_codec", codec)
      .putLong("last_track_duration", duration)
      .putLong("last_track_pos", position)
      .putInt("last_track_sr", sr)
      .putInt("last_track_bd", bd)
      .apply()
  }

  fun getTheme(): DapThemeSetting {
    val name = prefs.getString("dap_theme", DapThemeSetting.DARK.name) ?: DapThemeSetting.DARK.name
    return runCatching { DapThemeSetting.valueOf(name) }.getOrDefault(DapThemeSetting.DARK)
  }

  fun setTheme(theme: DapThemeSetting) {
    prefs.edit().putString("dap_theme", theme.name).apply()
    _themeFlow.value = theme
  }

  fun isPortraitLocked(): Boolean = prefs.getBoolean("lock_portrait", false)
  fun setPortraitLocked(locked: Boolean) {
    prefs.edit().putBoolean("lock_portrait", locked).apply()
    _lockPortraitFlow.value = locked
  }

  fun getFontSize(): DapFontSize {
    val name = prefs.getString("font_size", DapFontSize.MEDIUM.name) ?: DapFontSize.MEDIUM.name
    return runCatching { DapFontSize.valueOf(name) }.getOrDefault(DapFontSize.MEDIUM)
  }

  fun setFontSize(size: DapFontSize) {
    prefs.edit().putString("font_size", size.name).apply()
    _fontSizeFlow.value = size
  }

  fun getScanningMode(): ScanningMode {
    val name = prefs.getString("scan_mode", ScanningMode.AUTOMATIC.name) ?: ScanningMode.AUTOMATIC.name
    return runCatching { ScanningMode.valueOf(name) }.getOrDefault(ScanningMode.AUTOMATIC)
  }

  fun setScanningMode(mode: ScanningMode) {
    prefs.edit().putString("scan_mode", mode.name).apply()
  }

  fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_feedback", true)
  fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_feedback", enabled).apply()

  fun isPreBufferEnabled(): Boolean = prefs.getBoolean("pre_buffer", true)
  fun setPreBufferEnabled(enabled: Boolean) = prefs.edit().putBoolean("pre_buffer", enabled).apply()

  fun isMultiOutputEnabled(): Boolean = prefs.getBoolean("multi_output", false)
  fun setMultiOutputEnabled(enabled: Boolean) = prefs.edit().putBoolean("multi_output", enabled).apply()

  fun isGapless(): Boolean = prefs.getBoolean("gapless_enabled", true)
  fun setGapless(enabled: Boolean) = prefs.edit().putBoolean("gapless_enabled", enabled).apply()
}
