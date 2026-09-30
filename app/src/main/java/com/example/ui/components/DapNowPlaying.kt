package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DapPlayerState
import com.example.model.formatDuration

@Composable
fun DapNowPlaying(
  playerState: DapPlayerState,
  onClose: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onToggleRepeat: () -> Unit,
  onToggleShuffle: () -> Unit,
  onSetSpeed: (Float) -> Unit,
  onSetLoopA: () -> Unit,
  onSetLoopB: () -> Unit,
  onClearLoop: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onClose)
  val track = playerState.currentTrack ?: return
  val colors = MaterialTheme.colorScheme

  val progress = if (playerState.durationMs > 0) {
    (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
  } else 0f

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // 1. Top Header Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp, bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onClose) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = "Collapse Now Playing",
          tint = colors.onBackground,
          modifier = Modifier.size(28.dp)
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "NOW PLAYING",
          color = colors.primary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 2.sp
        )
        if (playerState.currentFolderTracks.isNotEmpty()) {
          Text(
            text = "Track ${playerState.currentTrackIndex + 1} of ${playerState.currentFolderTracks.size}",
            color = colors.onSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Row {
        // Repeat mode button
        IconButton(onClick = onToggleRepeat) {
          Icon(
            imageVector = when (playerState.repeatMode) {
              2 -> Icons.Default.RepeatOne
              1 -> Icons.Default.Repeat
              else -> Icons.Default.Repeat
            },
            contentDescription = "Repeat Mode",
            tint = if (playerState.repeatMode > 0) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f)
          )
        }

        // Shuffle button
        IconButton(onClick = onToggleShuffle) {
          Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle",
            tint = if (playerState.isShuffle) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Track Title & Filename (Nostalgic Foobar Technical Style)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(colors.surface)
        .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(14.dp)
    ) {
      Column {
        Text(
          text = track.title,
          color = colors.onSurface,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = track.fileName,
          color = colors.primary,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = track.filePath,
          color = colors.onSurfaceVariant,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          lineHeight = 14.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3. Complete Technical Specs Grid (Codec, Sample Rate, Bit depth, Bitrate, Channels, Size, Gapless)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(colors.surface)
        .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(12.dp)
    ) {
      Column {
        Text(
          text = "AUDIO SPECIFICATIONS & METADATA",
          color = colors.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
          SpecItem(label = "CODEC", value = track.codec, modifier = Modifier.weight(1f))
          SpecItem(label = "SAMPLE RATE", value = "${track.sampleRate} Hz (${track.sampleRate / 1000f} kHz)", modifier = Modifier.weight(1.5f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
          SpecItem(label = "BIT DEPTH", value = "${track.bitDepth}-bit PCM", modifier = Modifier.weight(1f))
          SpecItem(label = "BITRATE", value = "${track.bitrateKbps} kbps", modifier = Modifier.weight(1.5f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
          SpecItem(label = "CHANNELS", value = if (track.channels == 2) "Stereo (2.0)" else "${track.channels} Channel", modifier = Modifier.weight(1f))
          SpecItem(label = "FILE SIZE", value = track.formattedSize, modifier = Modifier.weight(1.5f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "GAPLESS ENGINE:",
            color = colors.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "ACTIVE (0 ms latency)",
            color = colors.tertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. Stereo Peak dB VU Meter (Real-time bouncing audio meter)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(Color.Black.copy(alpha = 0.85f))
        .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "STEREO VU PEAK", color = Color(0xFFA0A3B0), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          Text(text = "-40    -20    -10    -6    -3    0dB", color = Color(0xFFA0A3B0), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Left Channel
        VuMeterBar(channel = "L", level = playerState.peakMeterLeft)
        Spacer(modifier = Modifier.height(3.dp))
        // Right Channel
        VuMeterBar(channel = "R", level = playerState.peakMeterRight)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 5. Exact Timings Breakdown: Song Duration/Remaining + Folder Total/Remaining!
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(colors.surfaceVariant.copy(alpha = 0.5f))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "TRACK ELAPSED", color = colors.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(text = playerState.formattedElapsed, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "TRACK REMAINING", color = colors.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(text = playerState.formattedRemaining, color = colors.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(text = "TOTAL LENGTH", color = colors.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(text = formatDuration(playerState.durationMs), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "FOLDER REMAINING: ${playerState.formattedFolderRemaining}",
            color = colors.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "FOLDER TOTAL: ${playerState.formattedFolderTotal}",
            color = colors.onSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // 6. Precise Scrub Slider
    Slider(
      value = progress,
      onValueChange = { frac ->
        val targetMs = (frac * playerState.durationMs).toLong()
        onSeekTo(targetMs)
      },
      colors = SliderDefaults.colors(
        thumbColor = colors.primary,
        activeTrackColor = colors.primary,
        inactiveTrackColor = colors.surfaceVariant
      ),
      modifier = Modifier.fillMaxWidth()
    )

    // 7. Large Tactile Playback Controls
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Jump -10s
      IconButton(onClick = { onSeekTo((playerState.positionMs - 10000L).coerceAtLeast(0L)) }) {
        Icon(imageVector = Icons.Default.FastRewind, contentDescription = "-10s", tint = colors.onSurface)
      }

      // Previous Track
      IconButton(onClick = onSkipPrevious) {
        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Previous Track", tint = colors.onSurface, modifier = Modifier.size(32.dp))
      }

      // Play / Pause (Large Circular Button)
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(colors.primary)
          .clickable { onTogglePlayPause() },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = "Play/Pause",
          tint = colors.onPrimary,
          modifier = Modifier.size(34.dp)
        )
      }

      // Next Track
      IconButton(onClick = onSkipNext) {
        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next Track", tint = colors.onSurface, modifier = Modifier.size(32.dp))
      }

      // Jump +10s
      IconButton(onClick = { onSeekTo((playerState.positionMs + 10000L).coerceAtMost(playerState.durationMs)) }) {
        Icon(imageVector = Icons.Default.FastForward, contentDescription = "+10s", tint = colors.onSurface)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 8. Audiophile Extras: Speed & A-B Looper
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(colors.surface)
        .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(12.dp)
    ) {
      Column {
        // Speed Selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "PLAYBACK SPEED:", color = colors.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
              val isSel = playerState.playbackSpeed == spd
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isSel) colors.primary else colors.surfaceVariant)
                  .clickable { onSetSpeed(spd) }
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text(
                  text = "${spd}x",
                  color = if (isSel) colors.onPrimary else colors.onSurface,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // A-B Looping
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "A-B REPEAT LOOP:", color = colors.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (playerState.loopPointA != null) colors.primary else colors.surfaceVariant)
                .clickable { onSetLoopA() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = if (playerState.loopPointA != null) "A: ${com.example.model.formatDuration(playerState.loopPointA)}" else "[SET A]",
                color = if (playerState.loopPointA != null) colors.onPrimary else colors.onSurface,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (playerState.loopPointB != null) colors.primary else colors.surfaceVariant)
                .clickable { onSetLoopB() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = if (playerState.loopPointB != null) "B: ${com.example.model.formatDuration(playerState.loopPointB)}" else "[SET B]",
                color = if (playerState.loopPointB != null) colors.onPrimary else colors.onSurface,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
              )
            }

            if (playerState.loopPointA != null || playerState.loopPointB != null) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(colors.surfaceVariant)
                  .clickable { onClearLoop() }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "CLR",
                  color = colors.error,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
fun SpecItem(label: String, value: String, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(modifier = modifier) {
    Text(
      text = label,
      color = colors.onSurfaceVariant,
      fontSize = 9.sp,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = value,
      color = colors.onSurface,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
fun VuMeterBar(channel: String, level: Float) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = channel,
      color = Color(0xFFA0A3B0),
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(16.dp)
    )

    Canvas(
      modifier = Modifier
        .weight(1f)
        .height(8.dp)
    ) {
      val w = size.width
      val h = size.height
      val clamped = level.coerceIn(0f, 1f)

      // Background trough
      drawRect(Color(0xFF1E2028), Offset(0f, 0f), Size(w, h))

      // Multi-segment LED bar: Green (0..0.7), Yellow (0.7..0.9), Red (0.9..1.0)
      val greenW = (w * 0.7f * (clamped / 0.7f).coerceIn(0f, 1f))
      if (greenW > 0f) {
        drawRect(Color(0xFF00E676), Offset(0f, 0f), Size(greenW, h))
      }

      if (clamped > 0.7f) {
        val yellowProgress = ((clamped - 0.7f) / 0.2f).coerceIn(0f, 1f)
        val yellowW = w * 0.2f * yellowProgress
        drawRect(Color(0xFFFFD600), Offset(w * 0.7f, 0f), Size(yellowW, h))
      }

      if (clamped > 0.9f) {
        val redProgress = ((clamped - 0.9f) / 0.1f).coerceIn(0f, 1f)
        val redW = w * 0.1f * redProgress
        drawRect(Color(0xFFFF3D00), Offset(w * 0.9f, 0f), Size(redW, h))
      }
    }
  }
}
