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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
  BackHandler(enabled = uiState.isNowPlayingExpanded || uiState.folderHistory.isNotEmpty()) {
    if (uiState.isNowPlayingExpanded) {
      viewModel.setNowPlayingExpanded(false)
    } else if (uiState.folderHistory.isNotEmpty()) {
      viewModel.navigateUp()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = colors.background,
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
        onPlayTrack = { track ->
          viewModel.playTrack(track)
        },
        onPlayFolder = { shuffle -> viewModel.playCurrentFolder(shuffle) },
        onSelectFolderClick = { folderPickerLauncher.launch(null) },
        onRefreshClick = { viewModel.refreshFolder() },
        onSettingsClick = { viewModel.setSettingsOpen(true) },
        modifier = Modifier.fillMaxSize()
      )

      // 2. Docked MiniPlayer when audio is loaded/playing
      if (playerState.currentTrack != null && !uiState.isNowPlayingExpanded) {
        val navInsets = WindowInsets.navigationBars.asPaddingValues()
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = navInsets.calculateBottomPadding())
        ) {
          DapMiniPlayer(
            playerState = playerState,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onClick = { viewModel.setNowPlayingExpanded(true) }
          )
        }
      }

      // 3. Full-Screen Now Playing Overlay with Foobar Specs
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
          onToggleRepeat = { viewModel.toggleRepeat() },
          onToggleShuffle = { viewModel.toggleShuffle() },
          onSetSpeed = { spd -> viewModel.setSpeed(spd) },
          onSetLoopA = { viewModel.setLoopPointA() },
          onSetLoopB = { viewModel.setLoopPointB() },
          onClearLoop = { viewModel.clearLoopPoints() },
          modifier = Modifier.fillMaxSize()
        )
      }

      // 4. Settings Sheet for Themes and SD Root
      if (uiState.isSettingsOpen) {
        DapSettingsSheet(
          currentTheme = currentTheme,
          onThemeSelect = { theme -> viewModel.setTheme(theme) },
          onSelectRootFolder = { folderPickerLauncher.launch(null) },
          onDismiss = { viewModel.setSettingsOpen(false) }
        )
      }
    }
  }
}
