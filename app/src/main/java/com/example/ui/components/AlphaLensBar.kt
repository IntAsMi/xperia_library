package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.LensOption
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyTextPrimary
import com.example.ui.theme.SonyTextSecondary

@Composable
fun AlphaLensBar(
  selectedLens: LensOption,
  currentZoomRatio: Float,
  onSelectLens: (LensOption) -> Unit,
  onZoomChange: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  val lenses = listOf(
    LensOption.ULTRA_WIDE_16MM,
    LensOption.WIDE_24MM,
    LensOption.WIDE_48MM,
    LensOption.TELE_85_125MM
  )

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .background(SonyPanelDark.copy(alpha = 0.85f))
      .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Lens Buttons Row
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      lenses.forEach { lens ->
        val isSelected = selectedLens == lens
        val label = when (lens) {
          LensOption.ULTRA_WIDE_16MM -> "16"
          LensOption.WIDE_24MM -> "24"
          LensOption.WIDE_48MM -> "48"
          LensOption.TELE_85_125MM -> "85-125"
        }

        Box(
          modifier = Modifier
            .size(if (lens == LensOption.TELE_85_125MM) 54.dp else 42.dp, 42.dp)
            .clip(CircleShape)
            .background(if (isSelected) SonyOrange else Color.Transparent)
            .border(
              width = if (isSelected) 0.dp else 1.dp,
              color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.25f),
              shape = CircleShape
            )
            .clickable { onSelectLens(lens) },
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = label,
              color = if (isSelected) Color.White else SonyTextPrimary,
              fontSize = if (lens == LensOption.TELE_85_125MM) 11.sp else 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            if (isSelected && lens == LensOption.TELE_85_125MM) {
              val mm = (currentZoomRatio * 24f).toInt()
              Text(
                text = "${mm}mm",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    }

    // 2. Optical Continuous Zoom Slider for Telephoto 85-125mm
    AnimatedVisibility(
      visible = selectedLens == LensOption.TELE_85_125MM,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Column(
        modifier = Modifier
          .width(260.dp)
          .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "85mm (3.5x)",
            color = if (currentZoomRatio <= 3.6f) SonyOrange else SonyTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onZoomChange(3.5f) }
          )
          Text(
            text = "105mm (4.4x)",
            color = if (currentZoomRatio in 4.2f..4.6f) SonyOrange else SonyTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onZoomChange(4.4f) }
          )
          Text(
            text = "125mm (5.2x)",
            color = if (currentZoomRatio >= 5.1f) SonyOrange else SonyTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onZoomChange(5.2f) }
          )
        }

        Slider(
          value = currentZoomRatio,
          onValueChange = onZoomChange,
          valueRange = 3.5f..5.2f,
          colors = SliderDefaults.colors(
            thumbColor = SonyOrange,
            activeTrackColor = SonyOrange,
            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
          ),
          modifier = Modifier.height(28.dp)
        )
      }
    }
  }
}
