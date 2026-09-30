package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatio
import com.example.model.FileFormat
import com.example.model.GridType
import com.example.ui.theme.SonyBorder
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyPanelLight
import com.example.ui.theme.SonyTextMuted
import com.example.ui.theme.SonyTextPrimary
import com.example.ui.theme.SonyTextSecondary
import com.example.viewmodel.CameraViewModel

@Composable
fun AlphaSettingsSheet(
  viewModel: CameraViewModel,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState = viewModel.uiState.value

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(SonyPanelDark)
      .border(1.dp, SonyBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .padding(16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
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
            text = "α SONY SETUP MENU",
            color = SonyOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )
          Text(
            text = "Photography Pro Settings",
            color = SonyTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(
          onClick = onClose,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Menu",
            tint = SonyTextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Section: Grid Line
      SettingHeader("VIEWFINDER DISPLAY")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        GridType.values().forEach { grid ->
          val isSelected = uiState.gridType == grid
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSelected) SonyOrange else SonyPanelLight)
              .clickable { viewModel.setGridType(grid) }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = grid.label.take(7),
              color = if (isSelected) Color.White else SonyTextPrimary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Aspect Ratio Setting
      SettingHeader("ASPECT RATIO")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        AspectRatio.values().forEach { ratio ->
          val isSelected = uiState.aspectRatio == ratio
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSelected) SonyOrange else SonyPanelLight)
              .clickable { viewModel.setAspectRatio(ratio) }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = ratio.label,
              color = if (isSelected) Color.White else SonyTextPrimary,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Section: Switches
      SettingHeader("DISPLAY ASSIST & GAUGES")
      SettingSwitchRow(
        title = "Electronic Level Gauge",
        checked = uiState.showLevelGauge,
        onCheckedChange = { viewModel.toggleLevelGauge() }
      )
      SettingSwitchRow(
        title = "Real-Time Histogram",
        checked = uiState.showHistogram,
        onCheckedChange = { viewModel.toggleHistogram() }
      )
      SettingSwitchRow(
        title = "Focus Peaking in MF",
        checked = uiState.peakingEnabled,
        onCheckedChange = { viewModel.togglePeaking() }
      )

      if (uiState.peakingEnabled) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Peaking Color:", color = SonyTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
          listOf("White", "Yellow", "Red").forEachIndexed { idx, name ->
            val isSel = uiState.peakingColorIndex == idx
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSel) SonyOrange else SonyPanelLight)
                .clickable { viewModel.setPeakingColorIndex(idx) }
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = name,
                color = if (isSel) Color.White else SonyTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Section: Sounds & Haptics
      SettingHeader("AUDIO & FEEDBACK")
      SettingSwitchRow(
        title = "Sony Alpha Audio Signals (Beep & Shutter)",
        checked = viewModel.soundFeedback.soundEnabled,
        onCheckedChange = { viewModel.soundFeedback.soundEnabled = it }
      )
      SettingSwitchRow(
        title = "Haptic Vibration Feedback",
        checked = viewModel.soundFeedback.hapticsEnabled,
        onCheckedChange = { viewModel.soundFeedback.hapticsEnabled = it }
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun SettingHeader(text: String) {
  Text(
    text = text,
    color = SonyTextMuted,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.Monospace,
    letterSpacing = 1.sp,
    modifier = Modifier.padding(bottom = 6.dp)
  )
}

@Composable
fun SettingSwitchRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      color = SonyTextPrimary,
      fontSize = 13.sp
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = SonyOrange,
        checkedTrackColor = SonyOrange.copy(alpha = 0.5f),
        uncheckedThumbColor = SonyTextSecondary,
        uncheckedTrackColor = SonyPanelLight
      )
    )
  }
}
