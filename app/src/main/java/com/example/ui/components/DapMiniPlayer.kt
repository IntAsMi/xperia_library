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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
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
      .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
      .background(Color(0xFF000000)) // Strictly opaque black without transparency or blur
      .border(1.dp, Color(0xFF2A2E3A), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
      .clickable { onClick() }
  ) {
    // Mini progress bar on top
    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth()
        .height(2.5.dp),
      color = colors.primary,
      trackColor = Color(0xFF1E212B)
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Track Title, Codec, Multi-level Timings
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(2.dp))
              .background(colors.primary)
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text(
              text = track.codec,
              color = colors.onPrimary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = track.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "${playerState.formattedElapsed}/${playerState.formattedRemaining} • Disk ${playerState.currentDiskNumber}: ${playerState.formattedDiskRemaining} • Folder: ${playerState.formattedFolderRemaining}",
          color = Color(0xFFA0A3B0),
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Play/Pause & Next Buttons
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = "Play/Pause",
            tint = colors.primary,
            modifier = Modifier.size(24.dp)
          )
        }

        IconButton(
          onClick = onSkipNext,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }
  }
}
