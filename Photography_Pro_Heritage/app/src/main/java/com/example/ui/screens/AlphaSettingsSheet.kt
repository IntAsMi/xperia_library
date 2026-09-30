package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiSubjectTracking
import com.example.model.AspectRatio
import com.example.model.DeviceProfile
import com.example.model.GridType
import com.example.ui.theme.SonyAlphaGreen
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
  val context = LocalContext.current
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
            text = "Photography Pro (Xperia Edition)",
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

      // Section: Device Profile (1 VIII vs 1 V)
      SettingHeader("DEVICE HARDWARE PROFILE")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DeviceProfile.values().forEach { profile ->
          val isSelected = uiState.deviceProfile == profile
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) SonyOrange else SonyPanelLight)
              .border(1.dp, if (isSelected) SonyOrange else SonyBorder, RoundedCornerShape(6.dp))
              .clickable { viewModel.setDeviceProfile(profile) }
              .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = profile.modelName,
                color = if (isSelected) Color.White else SonyTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = if (profile == DeviceProfile.XPERIA_1_VIII) "16-170mm + AI" else "16-125mm Classic",
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else SonyTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Section: AI Subject Recognition Priority
      SettingHeader("AI SUBJECT RECOGNITION AF")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        AiSubjectTracking.values().forEach { tracking ->
          val isSelected = uiState.aiSubjectTracking == tracking
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSelected) SonyOrange else SonyPanelLight)
              .clickable {
                if (uiState.aiSubjectTracking != tracking) {
                  viewModel.toggleAiSubjectTracking()
                }
              }
              .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = tracking.shortName,
              color = if (isSelected) Color.White else SonyTextPrimary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Section: Viewfinder Display
      SettingHeader("VIEWFINDER FRAMING GRID")
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
      SettingHeader("DISPLAY GAUGES & ASSIST")
      SettingSwitchRow(
        title = "Dual-Axis Electronic Level",
        checked = uiState.showLevelGauge,
        onCheckedChange = { viewModel.toggleLevelGauge() }
      )
      SettingSwitchRow(
        title = "Live Multi-Channel Histogram",
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

      // Section: Audio & Feedback
      SettingHeader("AUDIO & TACTILE FEEDBACK")
      SettingSwitchRow(
        title = "Sony Alpha Audio Signals (1750Hz beep & shutter)",
        checked = viewModel.soundFeedback.soundEnabled,
        onCheckedChange = { viewModel.soundFeedback.soundEnabled = it }
      )
      SettingSwitchRow(
        title = "Dial Haptic Ticks & Shutter Vibration",
        checked = viewModel.soundFeedback.hapticsEnabled,
        onCheckedChange = { viewModel.soundFeedback.hapticsEnabled = it }
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Section: RELEASE APK & GITHUB GUIDE
      SettingHeader("APK & GITHUB REPO DOWNLOAD")
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(SonyPanelLight)
          .border(1.dp, SonyBorder, RoundedCornerShape(8.dp))
          .padding(12.dp)
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = null,
              tint = SonyAlphaGreen,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "EASY APK DOWNLOAD INSTRUCTIONS",
              color = SonyAlphaGreen,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "1. AI Studio UI: Open the project settings / gear menu in AI Studio and click 'Download APK' or 'Export ZIP' to push directly to your new GitHub repo.",
            color = SonyTextPrimary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "2. Local Build: In your cloned GitHub repo, generate a standalone installable APK with:",
            color = SonyTextPrimary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(6.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(4.dp))
              .background(Color.Black.copy(alpha = 0.6f))
              .border(1.dp, SonyBorder, RoundedCornerShape(4.dp))
              .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Gradle Build", "gradle assembleDebug"))
                Toast.makeText(context, "Command copied: gradle assembleDebug", Toast.LENGTH_SHORT).show()
              }
              .padding(horizontal = 10.dp, vertical = 8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "gradle assembleDebug",
                color = SonyOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy command",
                tint = SonyTextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Output: app/build/outputs/apk/debug/app-debug.apk (Directly installable on any Android phone).",
            color = SonyTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

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
