package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DapWaveformScrubber(
  waveformPoints: List<Float>,
  positionMs: Long,
  durationMs: Long,
  onSeekTo: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val activeColor = colors.primary
  val inactiveColor = colors.surfaceVariant.copy(alpha = 0.7f)
  val cursorColor = colors.tertiary

  val progress = if (durationMs > 0L) {
    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
  } else 0f

  var isDragging by remember { mutableStateOf(false) }
  var dragProgress by remember { mutableFloatStateOf(0f) }

  val effectiveProgress = if (isDragging) dragProgress else progress
  val bars = if (waveformPoints.isEmpty()) {
    remember { List(50) { 0.35f + (it % 5) * 0.12f } }
  } else {
    waveformPoints
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .pointerInput(durationMs) {
        detectTapGestures { offset ->
          val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
          onSeekTo((frac * durationMs).toLong())
        }
      }
      .pointerInput(durationMs) {
        detectDragGestures(
          onDragStart = { offset ->
            isDragging = true
            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
          },
          onDragEnd = {
            isDragging = false
            onSeekTo((dragProgress * durationMs).toLong())
          },
          onDragCancel = {
            isDragging = false
          },
          onDrag = { change, _ ->
            change.consume()
            dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
          }
        )
      }
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val barCount = bars.size
      val barSpacing = 2.dp.toPx()
      val totalSpacing = barSpacing * (barCount - 1)
      val barWidth = ((w - totalSpacing) / barCount).coerceAtLeast(1.5f)

      val centerY = h / 2f
      val currentPlayX = effectiveProgress * w

      for (i in 0 until barCount) {
        val barX = i * (barWidth + barSpacing)
        val amplitude = bars[i].coerceIn(0.12f, 1.0f)
        val barHeight = (h * 0.85f * amplitude).coerceAtLeast(4.dp.toPx())
        val topY = centerY - (barHeight / 2f)

        val isPlayed = (barX + barWidth / 2f) <= currentPlayX
        val barColor = if (isPlayed) activeColor else inactiveColor

        drawRoundRect(
          color = barColor,
          topLeft = Offset(barX, topY),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
      }

      // Draw sharp audiophile playback playhead line
      drawLine(
        color = cursorColor,
        start = Offset(currentPlayX, 0f),
        end = Offset(currentPlayX, h),
        strokeWidth = 2.dp.toPx()
      )

      // Playhead top marker
      drawCircle(
        color = cursorColor,
        radius = 4.dp.toPx(),
        center = Offset(currentPlayX, 4.dp.toPx())
      )
    }
  }
}
