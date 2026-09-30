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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.AudioFileItem
import com.example.model.FolderItem
import com.example.model.formatDuration
import com.example.model.formatFileSize
import com.example.viewmodel.DapUiState

@Composable
fun DapFolderBrowser(
  uiState: DapUiState,
  currentPlayingId: String?,
  isPlaying: Boolean,
  onOpenFolder: (FolderItem) -> Unit,
  onNavigateUp: () -> Unit,
  onPlayTrack: (AudioFileItem) -> Unit,
  onPlayFolder: (Boolean) -> Unit,
  onSelectFolderClick: () -> Unit,
  onRefreshClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // 1. Technical Top Bar: Folder Path & Root Controls
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(colors.surface)
        .border(1.dp, colors.outline.copy(alpha = 0.4f))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Up Button [..]
          IconButton(
            onClick = onNavigateUp,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ArrowUpward,
              contentDescription = "Navigate Up",
              tint = colors.primary,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Current Folder Title
          Column {
            Text(
              text = uiState.currentFolderName.uppercase(),
              color = colors.onSurface,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = uiState.currentFolderPath,
              color = colors.onSurfaceVariant,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Action Icons: Pick Folder, Refresh, Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onSelectFolderClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = "Select SD Card Folder",
              tint = colors.primary,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = onRefreshClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh",
              tint = colors.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = colors.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Folder Playback Bar if audio files exist
      if (uiState.audioFiles.isNotEmpty()) {
        val totalMs = uiState.audioFiles.sumOf { it.durationMs }
        val totalSize = uiState.audioFiles.sumOf { it.sizeBytes }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${uiState.audioFiles.size} tracks | ${formatDuration(totalMs)} | ${formatFileSize(totalSize)}",
            color = colors.onSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Play Folder
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.primary)
                .clickable { onPlayFolder(false) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = colors.onPrimary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "PLAY",
                  color = colors.onPrimary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            // Shuffle Folder
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable { onPlayFolder(true) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Shuffle,
                  contentDescription = null,
                  tint = colors.onSurfaceVariant,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "SHUFFLE",
                  color = colors.onSurfaceVariant,
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

    // 2. High-Density File & Folder List
    if (uiState.isLoading) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = colors.primary)
      }
    } else if (uiState.subfolders.isEmpty() && uiState.audioFiles.isEmpty()) {
      // Empty Folder State
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.FolderOpen,
          contentDescription = null,
          tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "No Audio Files In This Folder",
          color = colors.onSurface,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Navigate up or tap folder icon above to pick your SD Card music root.",
          color = colors.onSurfaceVariant,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace,
          lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.primary)
            .clickable { onSelectFolderClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Text(
            text = "SELECT MUSIC ROOT FOLDER",
            color = colors.onPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(bottom = 90.dp)
      ) {
        // Subfolders
        items(uiState.subfolders) { folder ->
          FolderRowItem(
            folder = folder,
            onClick = { onOpenFolder(folder) }
          )
          HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
        }

        // Audio Files
        items(uiState.audioFiles) { file ->
          val isCurrent = file.id == currentPlayingId
          AudioFileRowItem(
            file = file,
            isPlaying = isCurrent && isPlaying,
            isCurrent = isCurrent,
            onClick = { onPlayTrack(file) }
          )
          HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
        }
      }
    }
  }
}

@Composable
fun FolderRowItem(
  folder: FolderItem,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.colorScheme

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.Folder,
      contentDescription = "Folder",
      tint = colors.primary,
      modifier = Modifier.size(24.dp)
    )

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = folder.name,
        color = colors.onSurface,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      val countText = when {
        folder.fileCount > 0 && folder.subfolderCount > 0 -> "${folder.subfolderCount} folders, ${folder.fileCount} tracks"
        folder.fileCount > 0 -> "${folder.fileCount} tracks"
        folder.subfolderCount > 0 -> "${folder.subfolderCount} subfolders"
        else -> "empty"
      }

      Text(
        text = countText,
        color = colors.onSurfaceVariant,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace
      )
    }

    if (folder.totalDurationMs > 0) {
      Text(
        text = folder.formattedDuration,
        color = colors.onSurfaceVariant,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

@Composable
fun AudioFileRowItem(
  file: AudioFileItem,
  isPlaying: Boolean,
  isCurrent: Boolean,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.colorScheme

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(if (isCurrent) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Play indicator or track number
    if (isPlaying) {
      Icon(
        imageVector = Icons.Default.GraphicEq,
        contentDescription = "Playing",
        tint = colors.primary,
        modifier = Modifier.size(18.dp)
      )
    } else {
      Text(
        text = if (file.trackNumber > 0) file.trackNumber.toString().padStart(2, '0') else "•",
        color = if (isCurrent) colors.primary else colors.onSurfaceVariant,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.width(22.dp)
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Track Title and technical metadata line
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = file.title,
        color = if (isCurrent) colors.primary else colors.onSurface,
        fontSize = 13.sp,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Codec badge (.flac, .wav, etc.)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (isCurrent) colors.primary else colors.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
          Text(
            text = file.codec,
            color = if (isCurrent) colors.onPrimary else colors.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
          text = "${file.sampleRate / 1000f}kHz | ${file.bitDepth}-bit | ${file.bitrateKbps}kbps | ${file.formattedSize}",
          color = colors.onSurfaceVariant,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Track duration
    Text(
      text = file.formattedDuration,
      color = if (isCurrent) colors.primary else colors.onSurfaceVariant,
      fontSize = 12.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium
    )
  }
}
