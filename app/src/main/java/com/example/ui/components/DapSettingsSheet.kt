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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DapThemeSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DapSettingsSheet(
  currentTheme: DapThemeSetting,
  onThemeSelect: (DapThemeSetting) -> Unit,
  onSelectRootFolder: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val colors = MaterialTheme.colorScheme

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surface,
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
            text = "Audiophile folder playback & engine configuration",
            color = colors.onSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.onSurface)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(16.dp))

      // Section 1: Themes
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "THEME SELECTION",
          color = colors.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DapThemeSetting.values().forEach { theme ->
          val isSelected = theme == currentTheme
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.5f))
              .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
              )
              .clickable { onThemeSelect(theme) }
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = theme.displayName,
                color = if (isSelected) colors.primary else colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = theme.description,
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
              )
            }

            if (isSelected) {
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(colors.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = colors.onPrimary,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: Music Root Folder Picker
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "SD CARD / STORAGE ROOT",
          color = colors.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Select your main music folder from your SD Card or device. DAP Console will always remember and directly open inside this folder every time the app launches.",
        color = colors.onSurfaceVariant,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 15.sp
      )

      Spacer(modifier = Modifier.height(10.dp))
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
            text = "CHOOSE SD CARD MUSIC FOLDER",
            color = colors.onPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(16.dp))

      // Section 3: Engine Architecture Specifications
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "AUDIO ENGINE SPECIFICATIONS",
          color = colors.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(colors.surfaceVariant.copy(alpha = 0.4f))
          .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
          .padding(10.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = "• Decoder: Media3 ExoPlayer Engine (Low-latency)", color = colors.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          Text(text = "• Gapless: Native buffer pre-caching (0 ms overlap)", color = colors.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          Text(text = "• Formats: FLAC, WAV, ALAC, DSD/DSF, MP3, AAC, OGG, OPUS", color = colors.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          Text(text = "• Timings: Dual remaining calculation (Track & Folder)", color = colors.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          Text(text = "• Memory footprint: < 28 MB RAM (Lightning fast)", color = colors.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}
