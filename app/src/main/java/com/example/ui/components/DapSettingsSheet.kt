package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DapFontSize
import com.example.model.DapThemeSetting
import com.example.model.ScanningMode
import com.example.model.SleepTimerOption
import com.example.util.BatteryOptimizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DapSettingsSheet(
  currentTheme: DapThemeSetting,
  onThemeSelect: (DapThemeSetting) -> Unit,
  isPortraitLocked: Boolean,
  onTogglePortraitLocked: (Boolean) -> Unit,
  fontSize: DapFontSize,
  onFontSizeSelect: (DapFontSize) -> Unit,
  sleepTimerRemaining: Int?,
  onSleepTimerSelect: (SleepTimerOption) -> Unit,
  isHapticEnabled: Boolean,
  onToggleHaptic: (Boolean) -> Unit,
  isPreBufferEnabled: Boolean,
  onTogglePreBuffer: (Boolean) -> Unit,
  isMultiOutputEnabled: Boolean,
  onToggleMultiOutput: (Boolean) -> Unit,
  scanningMode: ScanningMode,
  onScanningModeSelect: (ScanningMode) -> Unit,
  isBitPerfectForced: Boolean = true,
  onToggleBitPerfect: (Boolean) -> Unit = {},
  onRequestShizuku: () -> Unit = {},
  isShizukuPrivileged: Boolean = false,
  onSelectRootFolder: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val colors = MaterialTheme.colorScheme
  val context = LocalContext.current
  val isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF10121A),
    contentColor = colors.onSurface,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "DAP CONSOLE SETTINGS",
            color = colors.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )
          Text(
            text = "Audiophile engine, display, and playback preferences",
            color = colors.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.onSurface)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Portrait Lock & Orientation
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(imageVector = Icons.Default.ScreenLockPortrait, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "LOCK PORTRAIT MODE",
              color = colors.onSurface,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = if (isPortraitLocked) "Locked in Portrait (ignores rotation)" else "Auto-rotate allowed (Landscape split view enabled)",
              color = colors.onSurfaceVariant,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
        Switch(
          checked = isPortraitLocked,
          onCheckedChange = onTogglePortraitLocked,
          colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Font Size Selector
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "FONT SIZE / SCALE",
          color = colors.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        DapFontSize.values().forEach { size ->
          val isSel = size == fontSize
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSel) colors.primary else colors.surfaceVariant.copy(alpha = 0.5f))
              .border(1.dp, if (isSel) colors.primary else colors.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
              .clickable { onFontSizeSelect(size) }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = when (size) {
                DapFontSize.SMALL -> "SMALL"
                DapFontSize.MEDIUM -> "STANDARD"
                DapFontSize.LARGE -> "LARGE"
                DapFontSize.EXTRA_LARGE -> "XL"
              },
              color = if (isSel) colors.onPrimary else colors.onSurface,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Sleep Timer
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "SLEEP TIMER",
            color = colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          if (sleepTimerRemaining != null) {
            Text(
              text = "Active: ${sleepTimerRemaining / 60}m ${sleepTimerRemaining % 60}s remaining",
              color = colors.primary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SleepTimerOption.values().forEach { option ->
          val isSel = (option == SleepTimerOption.OFF && sleepTimerRemaining == null)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSel) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.4f))
              .clickable { onSleepTimerSelect(option) }
              .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = option.displayName,
              color = if (isSel) colors.primary else colors.onSurface,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
            )
            if (isSel) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Haptic Feedback & Button Clicks
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "TACTILE HAPTIC FEEDBACK",
              color = colors.onSurface,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "Vibrate on button press (play, pause, next, rewind)",
              color = colors.onSurfaceVariant,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
        Switch(
          checked = isHapticEnabled,
          onCheckedChange = onToggleHaptic,
          colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Large File Pre-buffering
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "AUDIOPHILE RAM PRE-BUFFERING",
              color = colors.onSurface,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "Shows loading bar when loading large (>40MB) FLAC/DSD masters",
              color = colors.onSurfaceVariant,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
        Switch(
          checked = isPreBufferEnabled,
          onCheckedChange = onTogglePreBuffer,
          colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Multiple Outputs
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(imageVector = Icons.Default.SpeakerGroup, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "PLAY THROUGH MULTIPLE OUTPUTS",
              color = colors.onSurface,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "Force speaker alongside connected headphones if supported",
              color = colors.onSurfaceVariant,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
        Switch(
          checked = isMultiOutputEnabled,
          onCheckedChange = onToggleMultiOutput,
          colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Audiophile Bit-Perfect Output & Shizuku
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFFE0A938), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "FORCE BIT-PERFECT DIRECT AUDIO",
              color = Color(0xFFE0A938),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = if (isBitPerfectForced) "Enforcing 1:1 bit depth & sample rate (0 Hz Delta, no SRC)" else "Standard resampling allowed",
              color = colors.onSurfaceVariant,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
        Switch(
          checked = isBitPerfectForced,
          onCheckedChange = onToggleBitPerfect,
          colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = Color(0xFFE0A938))
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Shizuku HAL Lock button in Settings
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF161B26))
          .border(1.dp, if (isShizukuPrivileged) Color(0xFF00E676) else colors.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
          .clickable { onRequestShizuku() }
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isShizukuPrivileged) "SHIZUKU PRIVILEGED HAL LOCK: ACTIVE" else "SHIZUKU PRIVILEGED HAL LOCK",
              color = if (isShizukuPrivileged) Color(0xFF00E676) else colors.primary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (isShizukuPrivileged) "Kernel audio HAL offload active without Android mixer resampling." else "Tap to grant Shizuku authorization for hardware HAL bypass.",
              color = colors.onSurfaceVariant,
              fontSize = 8.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (isShizukuPrivileged) Color(0xFF1B2A1E) else colors.primary)
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = if (isShizukuPrivileged) "LOCKED" else "ENABLE / TEST",
              color = if (isShizukuPrivileged) Color(0xFF00E676) else colors.onPrimary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Background Audio & Battery Optimization
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isBatteryOptimized) Icons.Default.BatteryAlert else Icons.Default.BatteryChargingFull,
          contentDescription = null,
          tint = if (isBatteryOptimized) Color(0xFFFFA726) else Color(0xFF00E676),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "BACKGROUND PLAYBACK PROTECTION",
            color = colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = if (isBatteryOptimized) "Android Battery Saver is RESTRICTING background playback!" else "Unrestricted background audio playback is ACTIVE!",
            color = if (isBatteryOptimized) Color(0xFFFFA726) else Color(0xFF00E676),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Notice: Android aggressively stops background media players after 1-2 minutes if the app is optimized for battery. To ensure audio never stops playing in the background, set this app to 'Unrestricted' battery usage below.",
        color = colors.onSurfaceVariant,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 13.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isBatteryOptimized) Color(0xFFE0A938) else colors.surfaceVariant)
            .clickable {
              BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
            }
            .padding(vertical = 9.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isBatteryOptimized) "ALLOW UNRESTRICTED" else "ALREADY UNRESTRICTED",
            color = if (isBatteryOptimized) Color.Black else colors.onSurface,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable {
              BatteryOptimizationHelper.openAppInfoSettings(context)
            }
            .padding(vertical = 9.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "APP INFO / SETTINGS",
            color = colors.onSurface,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Library Scanning Mode
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "LIBRARY SCANNING PROCESS",
          color = colors.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ScanningMode.values().forEach { mode ->
          val isSel = mode == scanningMode
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSel) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.4f))
              .clickable { onScanningModeSelect(mode) }
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = mode.displayName,
                color = if (isSel) colors.primary else colors.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = mode.description,
                color = colors.onSurfaceVariant,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }
            if (isSel) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section: Themes
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "THEME SELECTION",
          color = colors.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DapThemeSetting.values().forEach { theme ->
          val isSelected = theme == currentTheme
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.4f))
              .clickable { onThemeSelect(theme) }
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = theme.displayName,
                color = if (isSelected) colors.primary else colors.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = theme.description,
                color = colors.onSurfaceVariant,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }
            if (isSelected) {
              Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = colors.primary, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Music Root Folder Picker
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(colors.primary)
          .clickable {
            onSelectRootFolder()
            onDismiss()
          }
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "CHOOSE SD CARD MUSIC ROOT FOLDER",
            color = colors.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}
