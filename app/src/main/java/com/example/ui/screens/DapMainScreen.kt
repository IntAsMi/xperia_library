package com.example.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DapAudioTuningSheet
import com.example.ui.components.DapFolderBrowser
import com.example.ui.components.DapMiniPlayer
import com.example.ui.components.DapNowPlaying
import com.example.ui.components.DapSettingsSheet
import com.example.viewmodel.DapViewModel

@Composable
fun DapMainScreen(
  viewModel: DapViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val playerState by viewModel.playerState.collectAsStateWithLifecycle()
  val currentTheme by viewModel.themeSetting.collectAsStateWithLifecycle()
  val colors = MaterialTheme.colorScheme

  // Storage Access Framework folder tree picker
  val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri: Uri? ->
    uri?.let {
      try {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(it, flags)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      viewModel.setRootFolder(it)
    }
  }

  val activity = context as? android.app.Activity

  // Handle hardware / gesture Back - Navigate backward towards root folder, never exit accidentally
  BackHandler {
    if (uiState.isSearchActive) {
      viewModel.setSearchActive(false)
    } else if (uiState.isSettingsOpen) {
      viewModel.setSettingsOpen(false)
    } else if (uiState.isAudioTuningDrawerOpen) {
      viewModel.setAudioTuningDrawerOpen(false)
    } else if (!isLandscape && uiState.isNowPlayingExpanded) {
      viewModel.setNowPlayingExpanded(false)
    } else if (uiState.folderHistory.isNotEmpty() || (uiState.currentFolderUri != null && uiState.currentFolderUri != uiState.rootFolderUri)) {
      viewModel.navigateUp()
    } else {
      // Keep music playing when exiting the app via back button at root folder
      activity?.moveTaskToBack(true)
    }
  }

  // Adjustable landscape split pane ratio (default ~46% browser, ~54% now playing)
  var landscapeSplitRatio by remember { mutableFloatStateOf(0.46f) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color(0xFF0A0C12),
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      if (isLandscape) {
        // LANDSCAPE MODE: Adjustable Split Panes (Left: Finder Browser, Right: Now Playing)
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val totalWidthPx = constraints.maxWidth.toFloat()

          Row(
            modifier = Modifier.fillMaxSize()
          ) {
            // LEFT PANE: macOS Finder Style Dual-Column Folder Navigator
            Box(
              modifier = Modifier
                .weight(landscapeSplitRatio)
                .fillMaxHeight()
            ) {
              DapFolderBrowser(
                uiState = uiState,
                currentPlayingId = playerState.currentTrack?.id,
                isPlaying = playerState.isPlaying,
                onOpenFolder = { folder -> viewModel.openFolder(folder) },
                onNavigateUp = { viewModel.navigateUp() },
                onPlayTrack = { track -> viewModel.playTrack(track) },
                onPlayFolder = { shuffle -> viewModel.playCurrentFolder(shuffle) },
                onSelectFolderClick = { folderPickerLauncher.launch(null) },
                onRefreshClick = { viewModel.refreshFolder() },
                onScanLibraryClick = { viewModel.triggerLibraryScan() },
                onSettingsClick = { viewModel.setSettingsOpen(true) },
                onToggleSearch = { active -> viewModel.setSearchActive(active) },
                onSearchQueryChange = { q -> viewModel.updateSearchQuery(q) },
                modifier = Modifier.fillMaxSize()
              )
            }

            // Draggable Divider Handle between Panes (Adjustable Split Window)
            Box(
              modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .pointerInput(totalWidthPx) {
                  detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaRatio = dragAmount.x / totalWidthPx
                    landscapeSplitRatio = (landscapeSplitRatio + deltaRatio).coerceIn(0.28f, 0.72f)
                  }
                },
              contentAlignment = Alignment.Center
            ) {
              // Center divider hairline
              Box(
                modifier = Modifier
                  .width(1.dp)
                  .fillMaxHeight()
                  .background(Color(0xFF242A3C))
              )
              // Draggable grip pill indicator
              Box(
                modifier = Modifier
                  .width(5.dp)
                  .height(36.dp)
                  .clip(RoundedCornerShape(3.dp))
                  .background(Color(0xFF3F4964))
              )
            }

            // RIGHT PANE: Now Playing Screen (Waveform, Visualizer, Specs, VU Meters)
            Box(
              modifier = Modifier
                .weight(1f - landscapeSplitRatio)
                .fillMaxHeight()
                .background(Color(0xFF0C0E14))
            ) {
            if (playerState.currentTrack != null) {
              DapNowPlaying(
                playerState = playerState,
                onClose = { /* Remains permanently docked on the right in landscape */ },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSeekTo = { pos -> viewModel.seekTo(pos) },
                onFastForwardStep = { viewModel.fastForwardStep() },
                onFastRewindStep = { viewModel.fastRewindStep() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onSetSpeed = { spd -> viewModel.setSpeed(spd) },
                onSetLoopA = { viewModel.setLoopPointA() },
                onSetLoopB = { viewModel.setLoopPointB() },
                onClearLoop = { viewModel.clearLoopPoints() },
                onOpenAudioTuning = { viewModel.setAudioTuningDrawerOpen(true) },
                onTogglePhase = { viewModel.toggleAudioPhase() },
                onSetChannelMode = { mode -> viewModel.setChannelMode(mode) },
                onSetCrossfeed = { mode -> viewModel.setCrossfeedMode(mode) },
                modifier = Modifier.fillMaxSize()
              )
            } else {
              LandscapeStandbyScreen(
                onPlayFirst = {
                  if (uiState.audioFiles.isNotEmpty()) {
                    viewModel.playTrack(uiState.audioFiles.first())
                  } else {
                    viewModel.playCurrentFolder(false)
                  }
                },
                onOpenAudioTuning = { viewModel.setAudioTuningDrawerOpen(true) }
              )
            }
          }
        }
      }
    } else {
      // PORTRAIT MODE: Standard Single View + Bottom Docked MiniPlayer + Pop-up Now Playing
        // 1. Main Folder Navigation Browser
        DapFolderBrowser(
          uiState = uiState,
          currentPlayingId = playerState.currentTrack?.id,
          isPlaying = playerState.isPlaying,
          onOpenFolder = { folder -> viewModel.openFolder(folder) },
          onNavigateUp = { viewModel.navigateUp() },
          onPlayTrack = { track -> viewModel.playTrack(track) },
          onPlayFolder = { shuffle -> viewModel.playCurrentFolder(shuffle) },
          onSelectFolderClick = { folderPickerLauncher.launch(null) },
          onRefreshClick = { viewModel.refreshFolder() },
          onScanLibraryClick = { viewModel.triggerLibraryScan() },
          onSettingsClick = { viewModel.setSettingsOpen(true) },
          onToggleSearch = { active -> viewModel.setSearchActive(active) },
          onSearchQueryChange = { q -> viewModel.updateSearchQuery(q) },
          modifier = Modifier.fillMaxSize()
        )

        // 2. Docked MiniPlayer
        if (playerState.currentTrack != null && !uiState.isNowPlayingExpanded) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .background(Color(0xFF000000))
          ) {
            DapMiniPlayer(
              playerState = playerState,
              onTogglePlayPause = { viewModel.togglePlayPause() },
              onSkipNext = { viewModel.skipNext() },
              onClick = { viewModel.setNowPlayingExpanded(true) }
            )
          }
        }

        // 3. Full-Screen Now Playing Overlay
        AnimatedVisibility(
          visible = uiState.isNowPlayingExpanded && playerState.currentTrack != null,
          enter = slideInVertically(initialOffsetY = { it }),
          exit = slideOutVertically(targetOffsetY = { it })
        ) {
          DapNowPlaying(
            playerState = playerState,
            onClose = { viewModel.setNowPlayingExpanded(false) },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeekTo = { pos -> viewModel.seekTo(pos) },
            onFastForwardStep = { viewModel.fastForwardStep() },
            onFastRewindStep = { viewModel.fastRewindStep() },
            onToggleRepeat = { viewModel.toggleRepeat() },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onSetSpeed = { spd -> viewModel.setSpeed(spd) },
            onSetLoopA = { viewModel.setLoopPointA() },
            onSetLoopB = { viewModel.setLoopPointB() },
            onClearLoop = { viewModel.clearLoopPoints() },
            onOpenAudioTuning = { viewModel.setAudioTuningDrawerOpen(true) },
            onTogglePhase = { viewModel.toggleAudioPhase() },
            onSetChannelMode = { mode -> viewModel.setChannelMode(mode) },
            onSetCrossfeed = { mode -> viewModel.setCrossfeedMode(mode) },
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      // 4. Audiophile Audio Tuning Drawer (Phase, Channels, Output Routing, Shizuku, Bit-Perfect)
      if (uiState.isAudioTuningDrawerOpen) {
        DapAudioTuningSheet(
          playerState = playerState,
          isBitPerfectForced = uiState.isBitPerfectForced,
          onToggleBitPerfect = { forced -> viewModel.setBitPerfectForced(forced) },
          onTogglePhase = { viewModel.toggleAudioPhase() },
          onSetAudioPhaseMode = { mode -> viewModel.setAudioPhaseMode(mode) },
          onSetChannelMode = { mode -> viewModel.setChannelMode(mode) },
          onSetVisualizerChannelMode = { vMode -> viewModel.setVisualizerChannelMode(vMode) },
          onSelectOutputDevice = { devId -> viewModel.selectOutputDevice(devId) },
          onTestShizukuConnection = { viewModel.testShizukuConnection() },
          onRequestShizuku = { viewModel.requestShizukuPermission() },
          onSetCrossfeedMode = { mode -> viewModel.setCrossfeedMode(mode) },
          onSetDacFilterProfile = { profile -> viewModel.setDacFilterProfile(profile) },
          onDismiss = { viewModel.setAudioTuningDrawerOpen(false) }
        )
      }

      // 5. Settings Sheet for Themes, Portrait Lock, Font Size, Bit-Perfect, and Engine Configuration
      if (uiState.isSettingsOpen) {
        DapSettingsSheet(
          currentTheme = currentTheme,
          onThemeSelect = { theme -> viewModel.setTheme(theme) },
          isPortraitLocked = uiState.isPortraitLocked,
          onTogglePortraitLocked = { locked -> viewModel.setPortraitLocked(locked) },
          fontSize = uiState.fontSize,
          onFontSizeSelect = { size -> viewModel.setFontSize(size) },
          sleepTimerRemaining = playerState.sleepTimerRemainingSeconds,
          onSleepTimerSelect = { opt -> viewModel.setSleepTimer(opt) },
          isHapticEnabled = uiState.isHapticEnabled,
          onToggleHaptic = { en -> viewModel.setHapticEnabled(en) },
          isPreBufferEnabled = uiState.isPreBufferEnabled,
          onTogglePreBuffer = { en -> viewModel.setPreBufferEnabled(en) },
          isMultiOutputEnabled = uiState.isMultiOutputEnabled,
          onToggleMultiOutput = { en -> viewModel.setMultiOutputEnabled(en) },
          scanningMode = uiState.scanningMode,
          onScanningModeSelect = { mode -> viewModel.setScanningMode(mode) },
          isBitPerfectForced = uiState.isBitPerfectForced,
          onToggleBitPerfect = { forced -> viewModel.setBitPerfectForced(forced) },
          onRequestShizuku = { viewModel.requestShizukuPermission() },
          isShizukuPrivileged = playerState.isShizukuPrivileged,
          onSelectRootFolder = { folderPickerLauncher.launch(null) },
          onDismiss = { viewModel.setSettingsOpen(false) }
        )
      }
    }
  }
}

@Composable
fun LandscapeStandbyScreen(
  onPlayFirst: () -> Unit,
  onOpenAudioTuning: () -> Unit
) {
  val colors = MaterialTheme.colorScheme

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      imageVector = Icons.Default.GraphicEq,
      contentDescription = null,
      tint = colors.primary.copy(alpha = 0.7f),
      modifier = Modifier.size(56.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "AUDIOPHILE DAP READY",
      color = colors.primary,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Select any audio track from the folder browser on the left.\nBit-perfect direct audio playback is armed.",
      color = colors.onSurfaceVariant,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      lineHeight = 16.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(colors.primary)
          .clickable { onPlayFirst() }
          .padding(horizontal = 14.dp, vertical = 9.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "PLAY FIRST TRACK",
            color = colors.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF1B1E29))
          .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
          .clickable { onOpenAudioTuning() }
          .padding(horizontal = 14.dp, vertical = 9.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "BIT-PERFECT TUNING",
            color = colors.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}
