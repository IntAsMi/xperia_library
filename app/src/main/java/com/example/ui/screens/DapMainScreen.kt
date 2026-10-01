package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

  // Handle hardware / gesture Back
  BackHandler(enabled = uiState.isAudioTuningDrawerOpen || uiState.isNowPlayingExpanded || uiState.folderHistory.isNotEmpty()) {
    if (uiState.isAudioTuningDrawerOpen) {
      viewModel.setAudioTuningDrawerOpen(false)
    } else if (uiState.isNowPlayingExpanded) {
      viewModel.setNowPlayingExpanded(false)
    } else if (uiState.folderHistory.isNotEmpty()) {
      viewModel.navigateUp()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color(0xFF0A0C12),
    contentWindowInsets = WindowInsets.statusBars
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
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
        modifier = Modifier.fillMaxSize()
      )

      // 2. Docked MiniPlayer: From Now Playing bottom tab downwards is completely opaque pitch black (#000000)
      if (playerState.currentTrack != null && !uiState.isNowPlayingExpanded) {
        val navInsets = WindowInsets.navigationBars.asPaddingValues()
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(Color(0xFF000000)) // Pure solid opaque black below bottom tab, zero blur
        ) {
          DapMiniPlayer(
            playerState = playerState,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onClick = { viewModel.setNowPlayingExpanded(true) }
          )

          // Extra opaque black space for navigation bars
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF000000))
              .padding(bottom = navInsets.calculateBottomPadding())
          )
        }
      }

      // 3. Full-Screen Now Playing Overlay with Waveform, Reactive Visualizer & Disk Timings
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
          modifier = Modifier.fillMaxSize()
        )
      }

      // 4. Audiophile Audio Tuning Drawer (Phase, Channels, Output Routing)
      if (uiState.isAudioTuningDrawerOpen) {
        DapAudioTuningSheet(
          playerState = playerState,
          onTogglePhase = { viewModel.toggleAudioPhase() },
          onSetChannelMode = { mode -> viewModel.setChannelMode(mode) },
          onSetVisualizerChannelMode = { vMode -> viewModel.setVisualizerChannelMode(vMode) },
          onSelectOutputDevice = { devId -> viewModel.selectOutputDevice(devId) },
          onDismiss = { viewModel.setAudioTuningDrawerOpen(false) }
        )
      }

      // 5. Settings Sheet for Themes, Portrait Lock, Font Size, and Engine Configuration
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
          onSelectRootFolder = { folderPickerLauncher.launch(null) },
          onDismiss = { viewModel.setSettingsOpen(false) }
        )
      }
    }
  }
}
