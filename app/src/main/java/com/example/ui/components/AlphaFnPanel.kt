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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriveMode
import com.example.model.FileFormat
import com.example.model.FlashMode
import com.example.model.FocusArea
import com.example.model.FocusMode
import com.example.model.MeteringMode
import com.example.model.ShootingMode
import com.example.ui.theme.SonyAlphaGreen
import com.example.ui.theme.SonyBorder
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyPanelLight
import com.example.ui.theme.SonyTextMuted
import com.example.ui.theme.SonyTextPrimary
import com.example.ui.theme.SonyTextSecondary
import com.example.viewmodel.ActiveWheel
import com.example.viewmodel.CameraUiState

@Composable
fun AlphaFnPanel(
  uiState: CameraUiState,
  onOpenWheel: (ActiveWheel) -> Unit,
  onToggleDriveMode: () -> Unit,
  onToggleFocusMode: () -> Unit,
  onToggleFocusArea: () -> Unit,
  onToggleMetering: () -> Unit,
  onToggleFlash: () -> Unit,
  onToggleFormat: () -> Unit,
  onToggleDro: () -> Unit,
  onTogglePeaking: () -> Unit,
  onOpenCreativeLook: () -> Unit,
  onOpenSettings: () -> Unit,
  onSavePreset: (String) -> Unit,
  onRecallPreset: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(SonyPanelDark)
      .border(1.dp, SonyBorder, RoundedCornerShape(8.dp))
      .padding(8.dp)
  ) {
    // 1. Exposure Bar Meter (-2 ... 1 ... 0 ... 1 ... +2)
    ExposureBarMeter(
      ev = uiState.exposureComp,
      shutterSpeed = uiState.shutterSpeed,
      iso = uiState.calculatedIso,
      aperture = uiState.selectedLens.aperture,
      onTapShutter = { onOpenWheel(ActiveWheel.SHUTTER) },
      onTapIso = { onOpenWheel(ActiveWheel.ISO) },
      onTapEv = { onOpenWheel(ActiveWheel.EV) },
      shootingMode = uiState.shootingMode
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 2. Memory Recall Shortcuts if in MR mode
    if (uiState.shootingMode == ShootingMode.MR) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf("M1", "M2", "M3").forEach { slot ->
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(4.dp))
              .background(SonyPanelLight)
              .border(1.dp, SonyOrange, RoundedCornerShape(4.dp))
              .clickable { onRecallPreset(slot) }
              .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "RECALL $slot",
              color = SonyOrange,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }

    // 3. Grid of Tactile Function Tiles (Sony Fn Menu)
    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      // Tile 1: DRIVE MODE
      item {
        FnTile(
          label = "DRIVE",
          value = uiState.driveMode.symbol,
          subValue = if (uiState.driveMode == DriveMode.BURST_HI) "30fps" else "",
          highlight = uiState.driveMode != DriveMode.SINGLE,
          onClick = onToggleDriveMode
        )
      }

      // Tile 2: FOCUS MODE
      item {
        FnTile(
          label = "FOCUS",
          value = uiState.focusMode.shortName,
          highlight = uiState.focusMode == FocusMode.MF,
          onClick = {
            if (uiState.focusMode == FocusMode.MF) {
              onOpenWheel(ActiveWheel.MANUAL_FOCUS)
            } else {
              onToggleFocusMode()
            }
          }
        )
      }

      // Tile 3: FOCUS AREA
      item {
        FnTile(
          label = "AREA",
          value = uiState.focusArea.shortName,
          highlight = uiState.focusArea == FocusArea.TRACKING,
          onClick = onToggleFocusArea
        )
      }

      // Tile 4: WHITE BALANCE
      item {
        FnTile(
          label = "WB",
          value = if (uiState.whiteBalance == "CUSTOM_K") "${uiState.customKelvin}K" else uiState.whiteBalance,
          highlight = uiState.whiteBalance != "AWB",
          onClick = { onOpenWheel(ActiveWheel.WHITE_BALANCE) }
        )
      }

      // Tile 5: CREATIVE LOOK
      item {
        FnTile(
          label = "LOOK",
          value = uiState.creativeLook.code,
          subValue = uiState.creativeLook.title,
          highlight = uiState.creativeLook.code != "ST",
          onClick = onOpenCreativeLook
        )
      }

      // Tile 6: METERING
      item {
        FnTile(
          label = "METER",
          value = uiState.meteringMode.shortName,
          onClick = onToggleMetering
        )
      }

      // Tile 7: FLASH
      item {
        FnTile(
          label = "FLASH",
          value = uiState.flashMode.shortName,
          highlight = uiState.flashMode != FlashMode.OFF,
          onClick = onToggleFlash
        )
      }

      // Tile 8: FORMAT
      item {
        FnTile(
          label = "FORMAT",
          value = uiState.fileFormat.label,
          highlight = uiState.fileFormat != FileFormat.JPEG,
          onClick = onToggleFormat
        )
      }

      // Tile 9: D-RANGE (DRO)
      item {
        FnTile(
          label = "DRO",
          value = if (uiState.droEnabled) "AUTO" else "OFF",
          highlight = uiState.droEnabled,
          onClick = onToggleDro
        )
      }

      // Tile 10: PEAKING
      item {
        FnTile(
          label = "PEAKING",
          value = if (uiState.peakingEnabled) "ON" else "OFF",
          highlight = uiState.peakingEnabled,
          onClick = onTogglePeaking
        )
      }

      // Tile 11: SAVE AS M1
      item {
        FnTile(
          label = "MEMORY",
          value = "SAVE M1",
          onClick = { onSavePreset("M1") }
        )
      }

      // Tile 12: SETTINGS / MENU
      item {
        FnTile(
          label = "MENU",
          value = "SETUP",
          icon = true,
          onClick = onOpenSettings
        )
      }
    }
  }
}

@Composable
fun ExposureBarMeter(
  ev: String,
  shutterSpeed: String,
  iso: String,
  aperture: String,
  onTapShutter: () -> Unit,
  onTapIso: () -> Unit,
  onTapEv: () -> Unit,
  shootingMode: ShootingMode
) {
  val evFloat = ev.replace("+", "").toFloatOrNull() ?: 0f

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(SonyPanelLight)
      .border(1.dp, SonyBorder, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    // Top primary readout: Shutter, Aperture, EV, ISO
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Shutter speed button
      Column(
        modifier = Modifier
          .clickable(enabled = shootingMode == ShootingMode.S || shootingMode == ShootingMode.M) {
            onTapShutter()
          }
          .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
      ) {
        Text(
          text = "SS",
          color = SonyTextMuted,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = shutterSpeed,
          color = if (shootingMode == ShootingMode.S || shootingMode == ShootingMode.M) SonyOrange else SonyTextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      // Aperture (Fixed optical physical f-stop of current lens)
      Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "F",
          color = SonyTextMuted,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = aperture,
          color = SonyTextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      // Exposure EV
      Column(
        modifier = Modifier
          .clickable { onTapEv() }
          .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "EV",
          color = SonyTextMuted,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = if (evFloat > 0) "+$ev" else ev,
          color = if (evFloat != 0f) SonyOrange else SonyTextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      // ISO
      Column(
        modifier = Modifier
          .clickable(enabled = shootingMode != ShootingMode.BASIC) {
            onTapIso()
          }
          .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.End
      ) {
        Text(
          text = "ISO",
          color = SonyTextMuted,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = iso,
          color = if (shootingMode == ShootingMode.M || shootingMode == ShootingMode.P) SonyOrange else SonyTextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Scale tick marks: -2 ... -1 ... 0 ... +1 ... +2
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "-2", color = SonyTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
      Text(text = "•", color = SonyTextMuted, fontSize = 8.sp)
      Text(text = "-1", color = SonyTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
      Text(text = "•", color = SonyTextMuted, fontSize = 8.sp)
      Text(
        text = "0",
        color = if (evFloat == 0f) SonyAlphaGreen else SonyTextPrimary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Text(text = "•", color = SonyTextMuted, fontSize = 8.sp)
      Text(text = "+1", color = SonyTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
      Text(text = "•", color = SonyTextMuted, fontSize = 8.sp)
      Text(text = "+2", color = SonyTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
  }
}

@Composable
fun FnTile(
  label: String,
  value: String,
  subValue: String = "",
  highlight: Boolean = false,
  icon: Boolean = false,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(if (highlight) SonyPanelLight else SonyPanelDark)
      .border(
        width = 1.dp,
        color = if (highlight) SonyOrange else SonyBorder,
        shape = RoundedCornerShape(4.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 6.dp, vertical = 6.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.Start
    ) {
      Text(
        text = label,
        color = SonyTextMuted,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(2.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = value,
          color = if (highlight) SonyOrange else SonyTextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1
        )
        if (subValue.isNotEmpty()) {
          Text(
            text = subValue,
            color = SonyTextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
        if (icon) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = SonyTextSecondary,
            modifier = Modifier.size(13.dp)
          )
        }
      }
    }
  }
}
