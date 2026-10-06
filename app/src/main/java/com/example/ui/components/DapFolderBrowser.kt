package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
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
  onToggleSearch: (Boolean) -> Unit = {},
  onSearchQueryChange: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val config = LocalConfiguration.current
  val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
  val scale = uiState.fontSize.scaleFactor

  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(uiState.isSearchActive) {
    if (uiState.isSearchActive) {
      kotlinx.coroutines.delay(120L)
      try {
        focusRequester.requestFocus()
        keyboardController?.show()
      } catch (_: Exception) {}
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0C0E14))
  ) {
    // 1. Technical Top Bar: Folder Path & Root Controls (Or Active Search Bar)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF131620))
        .border(1.dp, colors.outline.copy(alpha = 0.35f))
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      if (uiState.isSearchActive) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { onToggleSearch(false) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Close Search",
              tint = colors.primary,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          Box(
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF090B10))
              .border(1.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
              .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            if (uiState.searchQuery.isEmpty()) {
              Text(
                text = "Search tracks, albums, folders...",
                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = (11 * scale).sp,
                fontFamily = FontFamily.Monospace
              )
            }
            BasicTextField(
              value = uiState.searchQuery,
              onValueChange = onSearchQueryChange,
              textStyle = TextStyle(
                color = colors.onSurface,
                fontSize = (12 * scale).sp,
                fontFamily = FontFamily.Monospace
              ),
              cursorBrush = SolidColor(colors.primary),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
            )
          }

          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear Search",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      } else {
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

          // Action Icons: Search, Scan, Pick Folder, Refresh, Settings
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Instant Search Button
            IconButton(
              onClick = { onToggleSearch(true) },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Library",
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
              )
            }

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
      }

      // Scanning Progress Bar
      AnimatedVisibility(visible = uiState.isScanningLibrary) {
        val (current, totalRaw) = uiState.scanProgress ?: (0 to 1)
        val total = totalRaw.coerceAtLeast(1)

        Column(modifier = Modifier.padding(top = 4.dp)) {
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

      // Folder Playback Bar if audio files exist and not searching
      if (!uiState.isSearchActive && uiState.audioFiles.isNotEmpty()) {
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

    // 2. High-Density File & Folder List OR Search Results View
    if (uiState.isSearchActive) {
      SearchResultsView(
        uiState = uiState,
        currentPlayingId = currentPlayingId,
        isPlaying = isPlaying,
        scale = scale,
        colors = colors,
        onOpenFolder = { folder ->
          onToggleSearch(false)
          onOpenFolder(folder)
        },
        onPlayTrack = onPlayTrack,
        onPlayAllTracks = { shuffle ->
          if (uiState.searchTracks.isNotEmpty()) {
            onPlayTrack(uiState.searchTracks.first())
          }
        },
        onScanLibrary = onScanLibraryClick
      )
    } else if (uiState.isLoading) {
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
      // LANDSCAPE: macOS Finder Multi-Column Layout (Left: Parent Directory, Right: Current Directory)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        // LEFT COLUMN: Parent Directory (Lists sibling folders with active selection and expansion arrow)
        Column(
          modifier = Modifier
            .weight(1.05f)
            .fillMaxHeight()
            .border(width = 0.5.dp, color = colors.outline.copy(alpha = 0.25f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF131722))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "PARENT: ${uiState.parentFolderName.uppercase()}",
                color = colors.primary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (uiState.folderHistory.isNotEmpty()) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.primary.copy(alpha = 0.2f))
                    .clickable { onNavigateUp() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "▲ UP",
                    color = colors.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
          ) {
            if (uiState.parentFolders.isEmpty()) {
              item {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Top-level Root Directory",
                    color = colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            } else {
              items(uiState.parentFolders, key = { "parent_${it.uriString}" }) { folder ->
                val isActive = folder.uriString == uiState.currentFolderUri || folder.name.equals(uiState.currentFolderName, ignoreCase = true)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isActive) colors.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onOpenFolder(folder) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = if (isActive) Icons.Default.FolderOpen else Icons.Default.Folder,
                      contentDescription = null,
                      tint = if (isActive) colors.primary else colors.onSurfaceVariant,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = folder.name,
                      color = if (isActive) colors.primary else colors.onSurface,
                      fontSize = (11 * scale).sp,
                      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                      fontFamily = FontFamily.Monospace,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  if (isActive) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                      contentDescription = "Active Folder",
                      tint = colors.primary,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
                HorizontalDivider(color = colors.outline.copy(alpha = 0.12f), thickness = 0.5.dp)
              }
            }
          }
        }

        // Vertical Divider Line between Columns
        Box(
          modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Color(0xFF222838))
        )

        // RIGHT COLUMN: Current Directory (Lists Subfolders + Audio Tracks)
        Column(
          modifier = Modifier
            .weight(1.35f)
            .fillMaxHeight()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF131722))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "CURRENT: ${uiState.currentFolderName.uppercase()} (${uiState.subfolders.size} DIRS, ${uiState.audioFiles.size} TRACKS)",
                color = Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 50.dp)
          ) {
            // Section 1: Subfolders in current directory
            if (uiState.subfolders.isNotEmpty()) {
              item(key = "hdr_subfolders") {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F121A))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "SUBFOLDERS (${uiState.subfolders.size})",
                    color = colors.primary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
              items(uiState.subfolders, key = { "sub_${it.uriString}" }) { folder ->
                FolderRowItem(
                  folder = folder,
                  scale = scale,
                  onClick = { onOpenFolder(folder) }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.12f), thickness = 0.5.dp)
              }
            }

            // Section 2: Audio Tracks in current directory
            if (uiState.audioFiles.isNotEmpty()) {
              item(key = "hdr_tracks") {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F121A))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "AUDIO TRACKS (${uiState.audioFiles.size})",
                    color = colors.primary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
              items(uiState.audioFiles, key = { "track_${it.id}" }) { file ->
                val isCurrent = file.id == currentPlayingId
                AudioFileRowItem(
                  file = file,
                  isPlaying = isCurrent && isPlaying,
                  isCurrent = isCurrent,
                  scale = scale,
                  onClick = { onPlayTrack(file) }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.12f), thickness = 0.5.dp)
              }
            }
          }
        }
      }
    } else {
      // PORTRAIT: Samsung One UI Ease-of-Reachability Pattern
      // Large header space in upper half ensures the first item starts comfortably in the lower half of the screen
      // As the user scrolls, the list glides up to utilize the entire screen.
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        val reachabilityHeight = (maxHeight * 0.40f).coerceIn(160.dp, 340.dp)

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 120.dp)
        ) {
          // Samsung One UI Reachability Header Area
          item(key = "one_ui_reachability_header") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(reachabilityHeight)
                .background(Color(0xFF0C0E14))
                .padding(horizontal = 16.dp, vertical = 14.dp),
              contentAlignment = Alignment.BottomStart
            ) {
              Column {
                Text(
                  text = uiState.currentFolderName.uppercase(),
                  color = colors.onSurface,
                  fontSize = (22 * scale).sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "${uiState.subfolders.size} FOLDERS",
                    color = colors.primary,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = " • ",
                    color = colors.onSurfaceVariant,
                    fontSize = (11 * scale).sp,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = "${uiState.audioFiles.size} AUDIO TRACKS",
                    color = colors.primary,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
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
          }

          // Subfolders
          items(uiState.subfolders, key = { "sub_${it.uriString}" }) { folder ->
            FolderRowItem(
              folder = folder,
              scale = scale,
              onClick = { onOpenFolder(folder) }
            )
            HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
          }

          // Audio Files
          items(uiState.audioFiles, key = { "track_${it.id}" }) { file ->
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

@Composable
fun SearchResultsView(
  uiState: DapUiState,
  currentPlayingId: String?,
  isPlaying: Boolean,
  scale: Float,
  colors: androidx.compose.material3.ColorScheme,
  onOpenFolder: (FolderItem) -> Unit,
  onPlayTrack: (AudioFileItem) -> Unit,
  onPlayAllTracks: (Boolean) -> Unit,
  onScanLibrary: () -> Unit
) {
  val query = uiState.searchQuery
  val tracks = uiState.searchTracks
  val folders = uiState.searchFolders

  if (query.isBlank()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        tint = colors.primary.copy(alpha = 0.5f),
        modifier = Modifier.size(44.dp)
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "INSTANT INDEXED LIBRARY SEARCH",
        color = colors.primary,
        fontSize = (12 * scale).sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Type any track title, artist, album, or folder name.\nResults are retrieved instantaneously from the indexed database.",
        color = colors.onSurfaceVariant,
        fontSize = (10 * scale).sp,
        fontFamily = FontFamily.Monospace,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        lineHeight = 15.sp
      )
    }
    return
  }

  if (tracks.isEmpty() && folders.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(44.dp)
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "No Indexed Matches for \"$query\"",
        color = colors.onSurface,
        fontSize = (12 * scale).sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "If new audio files or folders were recently copied to your device, trigger a library scan to index their metadata.",
        color = colors.onSurfaceVariant,
        fontSize = (10 * scale).sp,
        fontFamily = FontFamily.Monospace,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        lineHeight = 14.sp
      )
      Spacer(modifier = Modifier.height(14.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(colors.primary)
          .clickable { onScanLibrary() }
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "SCAN & INDEX LIBRARY",
            color = colors.onPrimary,
            fontSize = (10 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 120.dp)
  ) {
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF10131B))
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "FOUND ${tracks.size} TRACKS • ${folders.size} FOLDERS",
          color = colors.primary,
          fontSize = (10 * scale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )

        if (tracks.isNotEmpty()) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(colors.primary)
              .clickable { onPlayAllTracks(false) }
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "PLAY ALL",
                color = colors.onPrimary,
                fontSize = (9 * scale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    }

    if (folders.isNotEmpty()) {
      item {
        Text(
          text = "MATCHING FOLDERS:",
          color = colors.onSurfaceVariant,
          fontSize = (9 * scale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
        )
      }
      items(folders, key = { "f_${it.uriString}" }) { folder ->
        FolderRowItem(
          folder = folder,
          scale = scale,
          onClick = { onOpenFolder(folder) }
        )
        HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))
      }
    }

    if (tracks.isNotEmpty()) {
      item {
        Text(
          text = "MATCHING TRACKS:",
          color = colors.onSurfaceVariant,
          fontSize = (9 * scale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
        )
      }
      items(tracks, key = { "t_${it.id}" }) { track ->
        val isCurrent = track.id == currentPlayingId
        AudioFileRowItem(
          file = track,
          isPlaying = isCurrent && isPlaying,
          isCurrent = isCurrent,
          scale = scale,
          onClick = { onPlayTrack(track) }
        )
        HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))
      }
    }
  }
}
