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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DapPlayerState

@Composable
fun DapMiniPlayer(
  playerState: DapPlayerState,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit = {},
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val track = playerState.currentTrack ?: return
  val colors = MaterialTheme.colorScheme

  val progress = if (playerState.durationMs > 0) {
    (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
  } else 0f

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color(0xFF0C0E16))
      .border(1.dp, Color(0xFF2A3144), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .clickable { onClick() }
  ) {
    // Top Precision Progress Hairline with Glowing Progress
    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth()
        .height(3.5.dp),
      color = colors.primary,
      trackColor = Color(0xFF161B26)
    )

    // Expanded, Roomy Main Content Row (~98dp total height)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Prominent 58dp Album Art / Audio Format Emblem
      Box(
        modifier = Modifier
          .size(58.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF151926))
          .border(1.2.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = track.codec.uppercase(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Center: Track Title, Fidelity Badges, Timings, and Mini Peak Meters
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        // Track Title (Larger, more prominent)
        Text(
          text = track.title,
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Fidelity Badge + Timing Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(colors.primary.copy(alpha = 0.22f))
              .border(0.8.dp, colors.primary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
              .padding(horizontal = 5.dp, vertical = 1.5.dp)
          ) {
            Text(
              text = "${track.sampleRate / 1000f}k/${track.bitDepth}b",
              color = colors.primary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Text(
            text = "${playerState.formattedElapsed} / ${playerState.formattedRemaining}",
            color = Color(0xFFB0B6CC),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
          )

          // Mini Live Peak dB Level Indicator
          if (playerState.isPlaying) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              modifier = Modifier.padding(start = 4.dp)
            ) {
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height((playerState.peakMeterLeft * 14f).dp.coerceAtLeast(3.dp))
                  .clip(RoundedCornerShape(1.dp))
                  .background(if (playerState.peakMeterLeft > 0.85f) Color(0xFFFF5252) else Color(0xFF00E676))
              )
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height((playerState.peakMeterRight * 14f).dp.coerceAtLeast(3.dp))
                  .clip(RoundedCornerShape(1.dp))
                  .background(if (playerState.peakMeterRight > 0.85f) Color(0xFFFF5252) else Color(0xFF00E676))
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Disk & Folder Context Subtitle
        Text(
          text = "Disk ${playerState.currentDiskNumber} • ${track.fileName}",
          color = Color(0xFF7A8298),
          fontSize = 9.5.sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Right: Tactile Transport Controls (Prev, Large 48dp Play/Pause, Next)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        IconButton(
          onClick = onSkipPrevious,
          modifier = Modifier.size(42.dp)
        ) {
          Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "Previous Track",
            tint = Color(0xFFC8CDDC),
            modifier = Modifier.size(24.dp)
          )
        }

        // Circular Highlighted Play/Pause Button (48dp Touch Target)
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable { onTogglePlayPause() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
            tint = colors.onPrimary,
            modifier = Modifier.size(28.dp)
          )
        }

        IconButton(
          onClick = onSkipNext,
          modifier = Modifier.size(42.dp)
        ) {
          Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next Track",
            tint = Color(0xFFC8CDDC),
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}
