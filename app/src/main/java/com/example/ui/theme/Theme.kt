package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SonyDarkColorScheme = darkColorScheme(
  primary = SonyOrange,
  onPrimary = Color.White,
  primaryContainer = SonyOrangeHover,
  onPrimaryContainer = Color.White,
  secondary = SonyAlphaGreen,
  onSecondary = Color.Black,
  secondaryContainer = SonyPanelLight,
  onSecondaryContainer = SonyTextPrimary,
  tertiary = SonyAlphaYellow,
  onTertiary = Color.Black,
  background = SonyDarkChassis,
  onBackground = SonyTextPrimary,
  surface = SonyPanelDark,
  onSurface = SonyTextPrimary,
  surfaceVariant = SonyPanelLight,
  onSurfaceVariant = SonyTextSecondary,
  outline = SonyBorder
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = SonyDarkColorScheme,
    typography = Typography,
    content = content
  )
}
