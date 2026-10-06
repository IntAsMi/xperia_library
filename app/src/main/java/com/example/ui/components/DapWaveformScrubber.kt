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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioPhaseMode
import com.example.model.ChannelMode
import com.example.model.formatDuration

@Composable
fun DapWaveformScrubber(
  waveformPoints: List<Float>,
  waveformLeft: List<Float> = emptyList(),
  waveformRight: List<Float> = emptyList(),
  channelMode: ChannelMode = ChannelMode.STEREO,
  audioPhaseMode: AudioPhaseMode = AudioPhaseMode.NORMAL,
  positionMs: Long,
  durationMs: Long,
  onSeekTo: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme

  // Strictly Monochromatic Technical Audio Aesthetic
  val activeColor = Color(0xFFFFFFFF) // Pure bright white for elapsed waveform
  val inactiveColor = Color(0xFF333846) // Dark slate for remaining unplayed waveform
  val centerLineColor = Color(0xFF202534)
  val cursorColor = Color(0xFFFFFFFF)

  val progress = if (durationMs > 0L) {
    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
  } else 0f

  var isDragging by remember { mutableStateOf(false) }
  var dragProgress by remember { mutableFloatStateOf(0f) }

  val effectiveProgress = if (isDragging) dragProgress else progress
  val previewTimeMs = (effectiveProgress * durationMs).toLong()

  // Generate ultra-dense fallback bars if track waveform hasn't loaded yet
  val denseBarsCount = 240
  val defaultDenseBars = remember {
    List(denseBarsCount) { i ->
      val t = i.toFloat() / denseBarsCount.toFloat()
      (0.35f + 0.25f * kotlin.math.sin(t * 12f) + 0.15f * kotlin.math.cos(t * 28f)).coerceIn(0.12f, 0.95f)
    }
  }
  val muteBars = remember { List(denseBarsCount) { 0.04f } }

  // Channel routing filtering
  val barsL = when {
    channelMode == ChannelMode.RIGHT_ONLY -> muteBars
    channelMode == ChannelMode.MONO -> {
      if (waveformLeft.isNotEmpty() && waveformRight.isNotEmpty()) {
        waveformLeft.zip(waveformRight) { l, r -> ((l + r) / 2f).coerceIn(0.08f, 1.0f) }
      } else waveformPoints.ifEmpty { defaultDenseBars }
    }
    waveformLeft.isNotEmpty() -> waveformLeft
    waveformPoints.isNotEmpty() -> waveformPoints
    else -> defaultDenseBars
  }

  val barsR = when {
    channelMode == ChannelMode.LEFT_ONLY -> muteBars
    channelMode == ChannelMode.MONO -> barsL
    waveformRight.isNotEmpty() -> waveformRight
    waveformPoints.isNotEmpty() -> waveformPoints
    else -> defaultDenseBars
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF090B10))
      .border(1.dp, Color(0xFF202636), RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 7.dp)
  ) {
    Column {
      // Header: Monochromatic Stereo Indicators & Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "STEREO WAVEFORM [L/R HIGH-DENSITY]",
            color = Color(0xFFA0A5B8),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          // Left channel status indicator
          val leftInv = audioPhaseMode == AudioPhaseMode.INVERT_LEFT_ONLY || audioPhaseMode == AudioPhaseMode.INVERT_BOTH
          Text(
            text = if (leftInv) "L [-180° INV]" else "L [0°]",
            color = if (leftInv) Color(0xFFFF5252) else Color(0xFFD0D4E4),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          // Right channel status indicator
          val rightInv = audioPhaseMode == AudioPhaseMode.INVERT_RIGHT_ONLY || audioPhaseMode == AudioPhaseMode.INVERT_BOTH
          Text(
            text = if (rightInv) "R [-180° INV]" else "R [0°]",
            color = if (rightInv) Color(0xFFFF5252) else Color(0xFFD0D4E4),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        Text(
          text = if (isDragging) "SCRUB: ${formatDuration(previewTimeMs)}" else "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
          color = if (isDragging) Color.White else Color(0xFFA0A5B8),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // High-Density Monochromatic Stereo Waveform Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
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
          val countL = barsL.size.coerceAtLeast(1)
          val countR = barsR.size.coerceAtLeast(1)
          val count = maxOf(countL, countR).coerceAtLeast(denseBarsCount)

          val barWidth = (w / count.toFloat()).coerceAtLeast(1.0f)
          val centerY = h / 2f
          val halfH = centerY - 1.5.dp.toPx()
          val currentPlayX = effectiveProgress * w

          // Draw center zero-crossing divider
          drawLine(
            color = centerLineColor,
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 1.dp.toPx()
          )

          // Top Track: LEFT CHANNEL (Extends upward from center line)
          for (i in 0 until count) {
            val amp = if (i < countL) barsL[i] else 0.25f
            val barH = (amp * halfH).coerceAtLeast(1.dp.toPx())
            val x = i * barWidth
            val isPlayed = (x + barWidth) <= currentPlayX

            val barColor = if (isPlayed) activeColor else inactiveColor

            drawRect(
              color = barColor,
              topLeft = Offset(x, centerY - barH),
              size = Size((barWidth - 0.5f).coerceAtLeast(0.8f), barH)
            )
          }

          // Bottom Track: RIGHT CHANNEL (Extends downward from center line)
          for (i in 0 until count) {
            val amp = if (i < countR) barsR[i] else 0.25f
            val barH = (amp * halfH).coerceAtLeast(1.dp.toPx())
            val x = i * barWidth
            val isPlayed = (x + barWidth) <= currentPlayX

            val barColor = if (isPlayed) activeColor else inactiveColor

            drawRect(
              color = barColor,
              topLeft = Offset(x, centerY),
              size = Size((barWidth - 0.5f).coerceAtLeast(0.8f), barH)
            )
          }

          // Vertical Scrubber Cursor Hairline
          drawLine(
            color = cursorColor,
            start = Offset(currentPlayX, 0f),
            end = Offset(currentPlayX, h),
            strokeWidth = 2.dp.toPx()
          )

          // Playhead Diamond Indicator at Center
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
