package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelMode

@Composable
fun DapWaveformScrubber(
  waveformPoints: List<Float>,
  waveformLeft: List<Float> = emptyList(),
  waveformRight: List<Float> = emptyList(),
  channelMode: ChannelMode = ChannelMode.STEREO,
  positionMs: Long,
  durationMs: Long,
  onSeekTo: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val leftColor = Color(0xFF00E5FF) // Cyan for Left Channel
  val rightColor = Color(0xFFFFB300) // Amber/Gold for Right Channel
  val inactiveColor = Color(0xFF262A36)
  val cursorColor = Color(0xFFFFFFFF)

  val progress = if (durationMs > 0L) {
    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
  } else 0f

  var isDragging by remember { mutableStateOf(false) }
  var dragProgress by remember { mutableFloatStateOf(0f) }

  val effectiveProgress = if (isDragging) dragProgress else progress

  val defaultBars = remember { List(60) { 0.35f } }
  val muteBars = remember { List(60) { 0.04f } }

  val barsL = when {
    channelMode == ChannelMode.RIGHT_ONLY -> muteBars
    channelMode == ChannelMode.MONO -> {
      if (waveformLeft.isNotEmpty() && waveformRight.isNotEmpty()) {
        waveformLeft.zip(waveformRight) { l, r -> ((l + r) / 2f).coerceIn(0.08f, 1.0f) }
      } else waveformPoints.ifEmpty { defaultBars }
    }
    waveformLeft.isNotEmpty() -> waveformLeft
    waveformPoints.isNotEmpty() -> waveformPoints
    else -> defaultBars
  }

  val barsR = when {
    channelMode == ChannelMode.LEFT_ONLY -> muteBars
    channelMode == ChannelMode.MONO -> barsL
    waveformRight.isNotEmpty() -> waveformRight
    waveformPoints.isNotEmpty() -> waveformPoints
    else -> defaultBars
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF0B0D13))
      .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Column {
      // Header with Channel Indicators & Scrubber status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "STEREO WAVEFORM [L / R]",
            color = Color(0xFFA0A3B0),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "• L: CH 1 (CYAN)",
            color = if (channelMode == ChannelMode.RIGHT_ONLY) Color(0xFF555555) else leftColor,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "• R: CH 2 (GOLD)",
            color = if (channelMode == ChannelMode.LEFT_ONLY) Color(0xFF555555) else rightColor,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Text(
          text = if (isDragging) "SCRUBBING..." else "SEEKABLE",
          color = if (isDragging) colors.primary else colors.onSurfaceVariant,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Dual-Rail Waveform Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(58.dp)
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
          val count = barsL.size.coerceAtLeast(1)
          val barSpacing = 1.8.dp.toPx()
          val totalSpacing = barSpacing * (count - 1)
          val barWidth = ((w - totalSpacing) / count).coerceAtLeast(1.5f)

          val centerY = h / 2f
          val railH = (h / 2f) - 2.dp.toPx()
          val currentPlayX = effectiveProgress * w

          // Draw center zero-crossing divider
          drawLine(
            color = Color(0xFF202534),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 1.dp.toPx()
          )

          for (i in 0 until count) {
            val barX = i * (barWidth + barSpacing)
            val isPlayed = (barX + barWidth / 2f) <= currentPlayX

            // LEFT CHANNEL (Upper Rail - grows upwards from center)
            val isLeftMuted = channelMode == ChannelMode.RIGHT_ONLY
            val ampL = if (isLeftMuted) 0.03f else barsL[i].coerceIn(0.06f, 1.0f)
            val barHL = (railH * ampL).coerceAtLeast(1.5.dp.toPx())
            val topYL = centerY - barHL
            val colorL = when {
              isLeftMuted -> Color(0xFF141720)
              isPlayed -> leftColor
              else -> inactiveColor
            }

            drawRoundRect(
              color = colorL,
              topLeft = Offset(barX, topYL),
              size = Size(barWidth, barHL),
              cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )

            // RIGHT CHANNEL (Lower Rail - grows downwards from center)
            val isRightMuted = channelMode == ChannelMode.LEFT_ONLY
            val ampR = if (isRightMuted) 0.03f else barsR[i].coerceIn(0.06f, 1.0f)
            val barHR = (railH * ampR).coerceAtLeast(1.5.dp.toPx())
            val topYR = centerY + 1.dp.toPx()
            val colorR = when {
              isRightMuted -> Color(0xFF141720)
              isPlayed -> rightColor
              else -> inactiveColor
            }

            drawRoundRect(
              color = colorR,
              topLeft = Offset(barX, topYR),
              size = Size(barWidth, barHR),
              cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
          }

          // Audiophile Playhead Needle across both rails
          drawLine(
            color = cursorColor,
            start = Offset(currentPlayX, 0f),
            end = Offset(currentPlayX, h),
            strokeWidth = 2.dp.toPx()
          )

          // Playhead Center Indicator
          drawCircle(
            color = cursorColor,
            radius = 3.5.dp.toPx(),
            center = Offset(currentPlayX, centerY)
          )
        }
      }
    }
  }
}
