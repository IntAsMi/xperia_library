package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DapThemeSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DapPreferences(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("dap_console_prefs", Context.MODE_PRIVATE)

  private val _themeFlow = MutableStateFlow(getTheme())
  val themeFlow: StateFlow<DapThemeSetting> = _themeFlow.asStateFlow()

  fun getRootFolderUri(): String? {
    return prefs.getString("root_folder_uri", null)
  }

  fun setRootFolderUri(uri: String?) {
    prefs.edit().putString("root_folder_uri", uri).apply()
  }

  fun getLastVisitedFolderUri(): String? {
    return prefs.getString("last_folder_uri", null)
  }

  fun setLastVisitedFolderUri(uri: String?) {
    prefs.edit().putString("last_folder_uri", uri).apply()
  }

  fun getTheme(): DapThemeSetting {
    val name = prefs.getString("dap_theme", DapThemeSetting.DARK.name) ?: DapThemeSetting.DARK.name
    return runCatching { DapThemeSetting.valueOf(name) }.getOrDefault(DapThemeSetting.DARK)
  }

  fun setTheme(theme: DapThemeSetting) {
    prefs.edit().putString("dap_theme", theme.name).apply()
    _themeFlow.value = theme
  }

  fun isGapless(): Boolean {
    return prefs.getBoolean("gapless_enabled", true)
  }

  fun setGapless(enabled: Boolean) {
    prefs.edit().putBoolean("gapless_enabled", enabled).apply()
  }
}
