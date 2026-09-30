package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriveMode
import com.example.ui.theme.SonyAlphaGreen
import com.example.ui.theme.SonyOrange

@Composable
fun TwoStageShutterButton(
  isAfLocked: Boolean,
  driveMode: DriveMode,
  isBurstShooting: Boolean,
  burstCount: Int,
  timerCountdown: Int?,
  onHalfPress: (Boolean) -> Unit,
  onFullPress: () -> Unit,
  onRelease: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isPressedDown by remember { mutableStateOf(false) }

  val scale by animateFloatAsState(
    targetValue = if (isPressedDown) 0.92f else 1.0f,
    animationSpec = tween(100),
    label = "shutterScale"
  )

  Box(
    modifier = modifier
      .size(76.dp)
      .scale(scale)
      .testTag("shutter_button")
      .pointerInput(driveMode) {
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          isPressedDown = true
          onHalfPress(true)

          // Wait to see if user holds or releases
          val up = waitForUpOrCancellation()
          isPressedDown = false
          if (up != null) {
            onFullPress()
          }
          onHalfPress(false)
          onRelease()
        }
      },
    contentAlignment = Alignment.Center
  ) {
    // 1. Textured Outer Knurled Bezel
    Canvas(modifier = Modifier.matchParentSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.width / 2f - 4f

      // Bezel base ring
      drawCircle(
        color = Color(0xFF23252E),
        radius = radius,
        center = center
      )

      // Outer metallic rim
      drawCircle(
        color = if (isAfLocked) SonyAlphaGreen else Color(0xFF4A4E5C),
        radius = radius,
        center = center,
        style = Stroke(width = 2.5f)
      )

      // Diamond knurling tick marks around rim
      val knurlCount = 28
      for (i in 0 until knurlCount) {
        val angleRad = Math.toRadians((i * 360.0 / knurlCount))
        val x1 = center.x + (radius - 5f) * Math.cos(angleRad).toFloat()
        val y1 = center.y + (radius - 5f) * Math.sin(angleRad).toFloat()
        val x2 = center.x + (radius - 1f) * Math.cos(angleRad).toFloat()
        val y2 = center.y + (radius - 1f) * Math.sin(angleRad).toFloat()
        drawLine(
          color = Color.White.copy(alpha = 0.25f),
          start = Offset(x1, y1),
          end = Offset(x2, y2),
          strokeWidth = 1.2f
        )
      }
    }

    // 2. Inner Action Button (Brushed metallic gradient / Orange ring)
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              if (isPressedDown) Color(0xFFE0E0E0) else Color(0xFFFAFAFA),
              if (isPressedDown) Color(0xFF9E9E9E) else Color(0xFFCCCCCC)
            )
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      // Sony Alpha Orange inner ring accent
      Canvas(modifier = Modifier.matchParentSize()) {
        drawCircle(
          color = if (isAfLocked) SonyAlphaGreen else SonyOrange,
          radius = size.width / 2f - 5f,
          style = Stroke(width = 2.5f)
        )
      }

      // Timer countdown or burst counter display
      if (timerCountdown != null) {
        Text(
          text = "$timerCountdown",
          color = SonyOrange,
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace
        )
      } else if (isBurstShooting) {
        Text(
          text = "${burstCount.coerceAtLeast(1)}",
          color = SonyOrange,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      } else if (driveMode == DriveMode.BURST_HI || driveMode == DriveMode.BURST_LO) {
        Text(
          text = driveMode.symbol,
          color = Color(0xFF333333),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // AF-ON lock indicator badge
    if (isAfLocked) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = (-2).dp, y = 2.dp)
          .size(12.dp)
          .clip(CircleShape)
          .background(SonyAlphaGreen)
      )
    }
  }
}
