package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.DapThemeSetting

private val DapDarkColorScheme = darkColorScheme(
  primary = DapAmber,
  onPrimary = Color.Black,
  primaryContainer = DapAmber.copy(alpha = 0.25f),
  onPrimaryContainer = DapAmber,
  secondary = DapCyan,
  onSecondary = Color.Black,
  secondaryContainer = DarkSurfaceElevated,
  onSecondaryContainer = DapCyan,
  tertiary = DapGreen,
  background = DarkBg,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorder
)

private val DapLightColorScheme = lightColorScheme(
  primary = Color(0xFFD84315),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFCCBC),
  onPrimaryContainer = Color(0xFFBF360C),
  secondary = Color(0xFF00838F),
  onSecondary = Color.White,
  secondaryContainer = LightSurfaceElevated,
  onSecondaryContainer = Color(0xFF006064),
  tertiary = Color(0xFF2E7D32),
  background = LightBg,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceElevated,
  onSurfaceVariant = LightTextSecondary,
  outline = LightBorder
)

private val DapAmoledColorScheme = darkColorScheme(
  primary = DapAmber,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF1E1408),
  onPrimaryContainer = DapAmber,
  secondary = DapCyan,
  onSecondary = Color.Black,
  secondaryContainer = AmoledSurfaceElevated,
  onSecondaryContainer = DapCyan,
  tertiary = DapGreen,
  background = AmoledBg,
  onBackground = Color(0xFFF5F5F7),
  surface = AmoledSurface,
  onSurface = Color(0xFFF5F5F7),
  surfaceVariant = AmoledSurfaceElevated,
  onSurfaceVariant = Color(0xFFA0A3B0),
  outline = AmoledBorder
)

@Composable
fun MyApplicationTheme(
  themeSetting: DapThemeSetting = DapThemeSetting.DARK,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val systemDark = isSystemInDarkTheme()

  val colorScheme: ColorScheme = when (themeSetting) {
    DapThemeSetting.DARK -> DapDarkColorScheme

    DapThemeSetting.LIGHT -> DapLightColorScheme

    DapThemeSetting.SYSTEM -> {
      if (systemDark) DapDarkColorScheme else DapLightColorScheme
    }

    DapThemeSetting.MATERIAL_U -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else {
        if (systemDark) DapDarkColorScheme else DapLightColorScheme
      }
    }

    DapThemeSetting.AMOLED_BLACK -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynDark = dynamicDarkColorScheme(context)
        DapAmoledColorScheme.copy(
          primary = dynDark.primary,
          secondary = dynDark.secondary,
          tertiary = dynDark.tertiary,
          primaryContainer = dynDark.primary.copy(alpha = 0.2f),
          onPrimaryContainer = dynDark.primary
        )
      } else {
        DapAmoledColorScheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
