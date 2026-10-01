package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
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
  onScanLibraryClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val config = LocalConfiguration.current
  val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
  val scale = uiState.fontSize.scaleFactor

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0C0E14))
  ) {
    // 1. Technical Top Bar: Folder Path & Root Controls
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF131620))
        .border(1.dp, colors.outline.copy(alpha = 0.35f))
        .padding(horizontal = 12.dp, vertical = 6.dp)
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
              fontSize = (13 * scale).sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = uiState.currentFolderPath,
              color = colors.onSurfaceVariant,
              fontSize = (9 * scale).sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Action Icons: Scan, Pick Folder, Refresh, Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Library Indexing Scan Button
          IconButton(
            onClick = onScanLibraryClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Index Library Metadata & Waveforms",
              tint = if (uiState.isScanningLibrary) colors.primary else colors.onSurfaceVariant,
              modifier = Modifier.size(19.dp)
            )
          }

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

      // Scanning Progress Bar
      AnimatedVisibility(visible = uiState.isScanningLibrary) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
          val current = uiState.scanProgress?.first ?: 0
          val total = (uiState.scanProgress?.second ?: 1).coerceAtLeast(1)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "INDEXING METADATA & WAVEFORMS...",
              color = colors.primary,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "$current / $total",
              color = colors.primary,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          LinearProgressIndicator(
            progress = { (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = colors.primary,
            trackColor = colors.surfaceVariant
          )
        }
      }

      // Folder Playback Bar if audio files exist
      if (uiState.audioFiles.isNotEmpty()) {
        val totalMs = uiState.audioFiles.sumOf { it.durationMs }
        val totalSize = uiState.audioFiles.sumOf { it.sizeBytes }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${uiState.audioFiles.size} tracks | ${formatDuration(totalMs)} | ${formatFileSize(totalSize)}",
            color = colors.onSurfaceVariant,
            fontSize = (10 * scale).sp,
            fontFamily = FontFamily.Monospace
          )

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Play Folder
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.primary)
                .clickable { onPlayFolder(false) }
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = colors.onPrimary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "PLAY",
                  color = colors.onPrimary,
                  fontSize = (9 * scale).sp,
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
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Shuffle,
                  contentDescription = null,
                  tint = colors.onSurfaceVariant,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "SHUFFLE",
                  color = colors.onSurfaceVariant,
                  fontSize = (9 * scale).sp,
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
          fontSize = (13 * scale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Tap folder icon above to pick your SD Card music root or open a subfolder.",
          color = colors.onSurfaceVariant,
          fontSize = (11 * scale).sp,
          fontFamily = FontFamily.Monospace,
          lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.primary)
            .clickable { onSelectFolderClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text(
            text = "SELECT MUSIC ROOT FOLDER",
            color = colors.onPrimary,
            fontSize = (11 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    } else if (isLandscape) {
      // LANDSCAPE: Vertically Split Left (Subfolders) and Right (Audio Files)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        // Left Column: Subfolders
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .border(width = 0.5.dp, color = colors.outline.copy(alpha = 0.25f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF141722))
              .padding(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Text(
              text = "SUBFOLDERS (${uiState.subfolders.size})",
              color = colors.primary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 70.dp)
          ) {
            items(uiState.subfolders) { folder ->
              FolderRowItem(
                folder = folder,
                scale = scale,
                onClick = { onOpenFolder(folder) }
              )
              HorizontalDivider(color = colors.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
            }
          }
        }

        // Right Column: Audio Tracks
        Column(
          modifier = Modifier
            .weight(1.3f)
            .fillMaxHeight()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF141722))
              .padding(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Text(
              text = "TRACKS IN CURRENT FOLDER (${uiState.audioFiles.size})",
              color = colors.primary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 70.dp)
          ) {
            items(uiState.audioFiles) { file ->
              val isCurrent = file.id == currentPlayingId
              AudioFileRowItem(
                file = file,
                isPlaying = isCurrent && isPlaying,
                isCurrent = isCurrent,
                scale = scale,
                onClick = { onPlayTrack(file) }
              )
              HorizontalDivider(color = colors.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
            }
          }
        }
      }
    } else {
      // PORTRAIT: Single combined high-performance list
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(bottom = 85.dp)
      ) {
        // Subfolders
        items(uiState.subfolders) { folder ->
          FolderRowItem(
            folder = folder,
            scale = scale,
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
            scale = scale,
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
  scale: Float = 1.0f,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.colorScheme

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.Folder,
      contentDescription = "Folder",
      tint = colors.primary,
      modifier = Modifier.size(22.dp)
    )

    Spacer(modifier = Modifier.width(10.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = folder.name,
        color = colors.onSurface,
        fontSize = (12 * scale).sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      val countText = when {
        folder.fileCount > 0 && folder.subfolderCount > 0 -> "${folder.subfolderCount} folders, ${folder.fileCount} tracks"
        folder.fileCount > 0 -> "${folder.fileCount} tracks"
        folder.subfolderCount > 0 -> "${folder.subfolderCount} subfolders"
        else -> "Folder"
      }

      Text(
        text = countText,
        color = colors.onSurfaceVariant,
        fontSize = (9 * scale).sp,
        fontFamily = FontFamily.Monospace
      )
    }

    if (folder.totalDurationMs > 0) {
      Text(
        text = folder.formattedDuration,
        color = colors.onSurfaceVariant,
        fontSize = (10 * scale).sp,
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
  scale: Float = 1.0f,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.colorScheme

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(if (isCurrent) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 7.dp),
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
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(26.dp)) {
        if (file.diskNumber > 1) {
          Text(
            text = "D${file.diskNumber}",
            color = if (isCurrent) colors.primary else Color(0xFFE0A938),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = if (file.trackNumber > 0) file.trackNumber.toString().padStart(2, '0') else "•",
          color = if (isCurrent) colors.primary else colors.onSurfaceVariant,
          fontSize = (10 * scale).sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Track Title and technical metadata line
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = file.title,
        color = if (isCurrent) colors.primary else colors.onSurface,
        fontSize = (12 * scale).sp,
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
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.width(5.dp))

        Text(
          text = "${file.sampleRate / 1000f}kHz | ${file.bitDepth}b | ${file.bitrateKbps}kbps | ${file.formattedSize}",
          color = colors.onSurfaceVariant,
          fontSize = (9 * scale).sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Track duration
    Text(
      text = file.formattedDuration,
      color = if (isCurrent) colors.primary else colors.onSurfaceVariant,
      fontSize = (11 * scale).sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium
    )
  }
}
