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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.CreativeLook
import com.example.ui.theme.SonyBorder
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyPanelLight
import com.example.ui.theme.SonyTextMuted
import com.example.ui.theme.SonyTextPrimary
import com.example.ui.theme.SonyTextSecondary

@Composable
fun AlphaCreativeLookSheet(
  selectedLook: CreativeLook,
  onSelectLook: (CreativeLook) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val looks = CreativeLook.values()

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(SonyPanelDark.copy(alpha = 0.96f))
      .border(1.dp, SonyBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .padding(16.dp)
  ) {
    Column {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "CREATIVE LOOK",
            color = SonyOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )
          Text(
            text = "${selectedLook.code} - ${selectedLook.title}",
            color = SonyTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(
          onClick = onClose,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = SonyTextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Description
      Text(
        text = selectedLook.description,
        color = SonyTextSecondary,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(bottom = 12.dp)
      )

      // Look cards row
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(looks) { look ->
          val isSelected = look == selectedLook
          Box(
            modifier = Modifier
              .width(72.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) SonyOrange else SonyPanelLight)
              .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else SonyBorder,
                shape = RoundedCornerShape(8.dp)
              )
              .clickable { onSelectLook(look) }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = look.code,
                color = if (isSelected) Color.White else SonyTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = look.title,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else SonyTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
              )
            }
          }
        }
      }
    }
  }
}
