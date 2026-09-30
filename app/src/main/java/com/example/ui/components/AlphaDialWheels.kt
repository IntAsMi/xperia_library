package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PhotoProValues
import com.example.ui.theme.SonyBorder
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyPanelLight
import com.example.ui.theme.SonyTextPrimary
import com.example.ui.theme.SonyTextSecondary
import com.example.viewmodel.ActiveWheel

@Composable
fun AlphaDialWheels(
  activeWheel: ActiveWheel,
  shutterSpeed: String,
  iso: String,
  exposureComp: String,
  whiteBalance: String,
  customKelvin: Int,
  manualFocusDistance: Float,
  onSelectShutter: (String) -> Unit,
  onSelectIso: (String) -> Unit,
  onSelectEv: (String) -> Unit,
  onSelectWb: (String) -> Unit,
  onChangeKelvin: (Int) -> Unit,
  onChangeManualFocus: (Float) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (activeWheel == ActiveWheel.NONE) return

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(SonyPanelDark.copy(alpha = 0.95f))
      .border(1.dp, SonyBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Column {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val title = when (activeWheel) {
          ActiveWheel.SHUTTER -> "SHUTTER SPEED (SS)"
          ActiveWheel.ISO -> "ISO SENSITIVITY"
          ActiveWheel.EV -> "EXPOSURE COMPENSATION (EV)"
          ActiveWheel.WHITE_BALANCE -> "WHITE BALANCE (WB)"
          ActiveWheel.MANUAL_FOCUS -> "MANUAL FOCUS (MF)"
          else -> ""
        }

        Text(
          text = title,
          color = SonyOrange,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )

        IconButton(
          onClick = onClose,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Dial",
            tint = SonyTextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Content based on active wheel
      when (activeWheel) {
        ActiveWheel.SHUTTER -> {
          WheelListSelector(
            items = PhotoProValues.SHUTTER_SPEEDS,
            selected = shutterSpeed,
            onSelect = onSelectShutter
          )
        }
        ActiveWheel.ISO -> {
          WheelListSelector(
            items = PhotoProValues.ISO_VALUES,
            selected = iso,
            onSelect = onSelectIso
          )
        }
        ActiveWheel.EV -> {
          WheelListSelector(
            items = PhotoProValues.EV_COMP_VALUES,
            selected = exposureComp,
            onSelect = onSelectEv
          )
        }
        ActiveWheel.WHITE_BALANCE -> {
          Column {
            WheelListSelector(
              items = PhotoProValues.WHITE_BALANCE_PRESETS.map { it.first },
              selected = whiteBalance,
              onSelect = onSelectWb
            )

            if (whiteBalance == "CUSTOM_K") {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Color Temp: ${customKelvin}K",
                  color = SonyTextPrimary,
                  fontSize = 12.sp,
                  fontFamily = FontFamily.Monospace
                )
                Slider(
                  value = customKelvin.toFloat(),
                  onValueChange = { onChangeKelvin(it.toInt()) },
                  valueRange = 2500f..9900f,
                  colors = SliderDefaults.colors(
                    thumbColor = SonyOrange,
                    activeTrackColor = SonyOrange
                  ),
                  modifier = Modifier
                    .width(200.dp)
                    .height(28.dp)
                )
              }
            }
          }
        }
        ActiveWheel.MANUAL_FOCUS -> {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Macro (Near)",
                color = SonyTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "Focus Distance: ${(manualFocusDistance * 100).toInt()}%",
                color = SonyOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "Infinity (∞)",
                color = SonyTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
            Slider(
              value = manualFocusDistance,
              onValueChange = onChangeManualFocus,
              valueRange = 0f..1f,
              colors = SliderDefaults.colors(
                thumbColor = SonyOrange,
                activeTrackColor = SonyOrange
              ),
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
        else -> {}
      }
    }
  }
}

@Composable
fun WheelListSelector(
  items: List<String>,
  selected: String,
  onSelect: (String) -> Unit
) {
  val listState = rememberLazyListState()

  LaunchedEffect(selected) {
    val index = items.indexOf(selected)
    if (index >= 0) {
      listState.animateScrollToItem((index - 2).coerceAtLeast(0))
    }
  }

  LazyRow(
    state = listState,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    items(items) { item ->
      val isSelected = item == selected
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (isSelected) SonyOrange else SonyPanelLight)
          .border(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else SonyBorder,
            shape = RoundedCornerShape(6.dp)
          )
          .clickable { onSelect(item) }
          .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = item,
          color = if (isSelected) Color.White else SonyTextPrimary,
          fontSize = 13.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}
