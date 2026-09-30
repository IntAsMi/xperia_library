package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.ShootingMode
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyPanelLight
import com.example.ui.theme.SonyTextPrimary
import com.example.ui.theme.SonyTextSecondary

@Composable
fun ModeDialSelector(
  selectedMode: ShootingMode,
  onSelectMode: (ShootingMode) -> Unit,
  modifier: Modifier = Modifier
) {
  val modes = ShootingMode.values()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(SonyPanelDark)
      .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    modes.forEach { mode ->
      val isSelected = mode == selectedMode

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(6.dp))
          .background(if (isSelected) SonyOrange else SonyPanelLight)
          .clickable { onSelectMode(mode) }
          .padding(vertical = 8.dp)
          .testTag("mode_${mode.name.lowercase()}"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = mode.label,
          color = if (isSelected) Color.White else SonyTextSecondary,
          fontSize = 12.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}
