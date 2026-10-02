package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DapPreferences
import com.example.data.FolderRepository
import com.example.model.AudioFileItem
import com.example.model.ChannelMode
import com.example.model.DapFontSize
import com.example.model.DapPlayerState
import com.example.model.DapThemeSetting
import com.example.model.FolderItem
import com.example.model.ScanningMode
import com.example.model.SleepTimerOption
import com.example.model.VisualizerChannelMode
import com.example.player.DapAudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DapUiState(
  val rootFolderUri: String? = null,
  val currentFolderUri: String? = null,
  val currentFolderPath: String = "/Music",
  val currentFolderName: String = "All Music",
  val subfolders: List<FolderItem> = emptyList(),
  val audioFiles: List<AudioFileItem> = emptyList(),
  val isLoading: Boolean = false,
  val isNowPlayingExpanded: Boolean = false,
  val isSettingsOpen: Boolean = false,
  val isAudioTuningDrawerOpen: Boolean = false,
  val folderHistory: List<Pair<String?, String>> = emptyList(),
  val isScanningLibrary: Boolean = false,
  val scanProgress: Pair<Int, Int>? = null,
  val isPortraitLocked: Boolean = false,
  val fontSize: DapFontSize = DapFontSize.MEDIUM,
  val scanningMode: ScanningMode = ScanningMode.AUTOMATIC,
  val isHapticEnabled: Boolean = true,
  val isPreBufferEnabled: Boolean = true,
  val isMultiOutputEnabled: Boolean = false
)

class DapViewModel(application: Application) : AndroidViewModel(application) {
  private val context = application.applicationContext
  private val prefs = DapPreferences(context)
  private val repository = FolderRepository(context)
  val player = DapAudioPlayer(context)

  private val _uiState = MutableStateFlow(
    DapUiState(
      isPortraitLocked = prefs.isPortraitLocked(),
      fontSize = prefs.getFontSize(),
      scanningMode = prefs.getScanningMode(),
      isHapticEnabled = prefs.isHapticEnabled(),
      isPreBufferEnabled = prefs.isPreBufferEnabled(),
      isMultiOutputEnabled = prefs.isMultiOutputEnabled()
    )
  )
  val uiState: StateFlow<DapUiState> = _uiState.asStateFlow()

  val playerState: StateFlow<DapPlayerState> = player.state
  val themeSetting: StateFlow<DapThemeSetting> = prefs.themeFlow
  val lockPortraitFlow: StateFlow<Boolean> = prefs.lockPortraitFlow
  val fontSizeFlow: StateFlow<DapFontSize> = prefs.fontSizeFlow

  init {
    player.isHapticEnabled = prefs.isHapticEnabled()
    player.isPreBufferEnabled = prefs.isPreBufferEnabled()
    loadInitialFolder()
  }

  private fun loadInitialFolder() {
    viewModelScope.launch {
      val savedRoot = prefs.getRootFolderUri()
      val savedLast = prefs.getLastVisitedFolderUri() ?: savedRoot

      _uiState.value = _uiState.value.copy(
        rootFolderUri = savedRoot,
        currentFolderUri = savedLast,
        isLoading = true
      )

      val (folders, files) = repository.loadFolderContents(
        folderUriString = savedLast,
        rootUriString = savedRoot
      )

      val friendlyPath = if (savedLast != null) extractFriendlyPath(savedLast) else "/Music"
      val folderName = if (savedLast != null) friendlyPath.substringAfterLast("/") else "Music Root"

      _uiState.value = _uiState.value.copy(
        subfolders = folders,
        audioFiles = files,
        currentFolderPath = friendlyPath,
        currentFolderName = if (folderName.isBlank()) "Music Root" else folderName,
        isLoading = false
      )
    }
  }

  fun setRootFolder(uri: Uri) {
    val uriString = uri.toString()
    prefs.setRootFolderUri(uriString)
    prefs.setLastVisitedFolderUri(uriString)

    val friendly = extractFriendlyPath(uriString)
    val folderName = friendly.substringAfterLast("/").ifEmpty { "SD Card Music" }

    _uiState.value = _uiState.value.copy(
      rootFolderUri = uriString,
      currentFolderUri = uriString,
      currentFolderPath = friendly,
      currentFolderName = folderName,
      folderHistory = emptyList(),
      isLoading = true
    )

    viewModelScope.launch {
      val (folders, files) = repository.loadFolderContents(uriString, uriString)
      _uiState.value = _uiState.value.copy(
        subfolders = folders,
        audioFiles = files,
        isLoading = false
      )

      if (_uiState.value.scanningMode == ScanningMode.AUTOMATIC && uriString.startsWith("content://")) {
        triggerLibraryScan(uriString)
      }
    }
  }

  fun openFolder(folder: FolderItem) {
    val prevHistory = _uiState.value.folderHistory + (_uiState.value.currentFolderUri to _uiState.value.currentFolderPath)
    prefs.setLastVisitedFolderUri(folder.uriString)

    _uiState.value = _uiState.value.copy(
      currentFolderUri = folder.uriString,
      currentFolderPath = folder.path,
      currentFolderName = folder.name,
      folderHistory = prevHistory,
      isLoading = true
    )

    viewModelScope.launch {
      val (sub, files) = repository.loadFolderContents(folder.uriString, _uiState.value.rootFolderUri)
      _uiState.value = _uiState.value.copy(
        subfolders = sub,
        audioFiles = files,
        isLoading = false
      )
    }
  }

  fun navigateUp() {
    val history = _uiState.value.folderHistory
    if (history.isEmpty()) {
      val root = _uiState.value.rootFolderUri
      if (_uiState.value.currentFolderUri != root) {
        prefs.setLastVisitedFolderUri(root)
        _uiState.value = _uiState.value.copy(
          currentFolderUri = root,
          currentFolderPath = if (root != null) extractFriendlyPath(root) else "/Music",
          currentFolderName = "Music Root",
          isLoading = true
        )
        viewModelScope.launch {
          val (sub, files) = repository.loadFolderContents(root, root)
          _uiState.value = _uiState.value.copy(
            subfolders = sub,
            audioFiles = files,
            isLoading = false
          )
        }
      }
      return
    }

    val last = history.last()
    val newHistory = history.dropLast(1)
    prefs.setLastVisitedFolderUri(last.first)

    val folderName = last.second.substringAfterLast("/").ifEmpty { "Music" }

    _uiState.value = _uiState.value.copy(
      currentFolderUri = last.first,
      currentFolderPath = last.second,
      currentFolderName = folderName,
      folderHistory = newHistory,
      isLoading = true
    )

    viewModelScope.launch {
      val (sub, files) = repository.loadFolderContents(last.first, _uiState.value.rootFolderUri)
      _uiState.value = _uiState.value.copy(
        subfolders = sub,
        audioFiles = files,
        isLoading = false
      )
    }
  }

  fun refreshFolder() {
    val current = _uiState.value.currentFolderUri
    _uiState.value = _uiState.value.copy(isLoading = true)
    viewModelScope.launch {
      val (sub, files) = repository.loadFolderContents(current, _uiState.value.rootFolderUri)
      _uiState.value = _uiState.value.copy(
        subfolders = sub,
        audioFiles = files,
        isLoading = false
      )
    }
  }

  fun triggerLibraryScan(targetFolderUri: String? = _uiState.value.currentFolderUri) {
    val uriStr = targetFolderUri ?: _uiState.value.rootFolderUri ?: "virtual_demo://root"

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isScanningLibrary = true, scanProgress = 0 to 1)
      val enriched = repository.scanFolderDeep(uriStr) { current, total ->
        _uiState.value = _uiState.value.copy(scanProgress = current to total)
      }
      val (sub, files) = repository.loadFolderContents(_uiState.value.currentFolderUri, _uiState.value.rootFolderUri)
      _uiState.value = _uiState.value.copy(
        subfolders = sub,
        audioFiles = if (files.isNotEmpty()) files else enriched,
        isScanningLibrary = false,
        scanProgress = null
      )
    }
  }

  fun playTrack(track: AudioFileItem) {
    player.playTrack(track, _uiState.value.audioFiles)
  }

  fun playCurrentFolder(shuffle: Boolean = false) {
    player.playFolder(_uiState.value.audioFiles, startIndex = 0, shuffle = shuffle)
  }

  fun togglePlayPause() = player.togglePlayPause()
  fun skipNext() = player.skipNext()
  fun skipPrevious() = player.skipPrevious()
  fun seekTo(positionMs: Long) = player.seekTo(positionMs)
  fun fastForwardStep() = player.fastForwardStep()
  fun fastRewindStep() = player.fastRewindStep()
  fun toggleRepeat() = player.toggleRepeatMode()
  fun toggleShuffle() = player.toggleShuffle()
  fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed)
  fun setLoopPointA() = player.setLoopPointA()
  fun setLoopPointB() = player.setLoopPointB()
  fun clearLoopPoints() = player.clearLoopPoints()

  // Audio Tuning Drawer Controls
  fun toggleAudioPhase() = player.toggleAudioPhase()
  fun setChannelMode(mode: ChannelMode) = player.setChannelMode(mode)
  fun setVisualizerChannelMode(mode: VisualizerChannelMode) = player.setVisualizerChannelMode(mode)
  fun selectOutputDevice(deviceId: Int) = player.selectOutputDevice(deviceId)
  fun setSleepTimer(option: SleepTimerOption) = player.setSleepTimer(option)

  fun setNowPlayingExpanded(expanded: Boolean) {
    _uiState.value = _uiState.value.copy(isNowPlayingExpanded = expanded)
  }

  fun setSettingsOpen(open: Boolean) {
    _uiState.value = _uiState.value.copy(isSettingsOpen = open)
  }

  fun setAudioTuningDrawerOpen(open: Boolean) {
    _uiState.value = _uiState.value.copy(isAudioTuningDrawerOpen = open)
  }

  fun setTheme(theme: DapThemeSetting) {
    prefs.setTheme(theme)
  }

  fun setPortraitLocked(locked: Boolean) {
    prefs.setPortraitLocked(locked)
    _uiState.value = _uiState.value.copy(isPortraitLocked = locked)
  }

  fun setFontSize(size: DapFontSize) {
    prefs.setFontSize(size)
    _uiState.value = _uiState.value.copy(fontSize = size)
  }

  fun setScanningMode(mode: ScanningMode) {
    prefs.setScanningMode(mode)
    _uiState.value = _uiState.value.copy(scanningMode = mode)
  }

  fun setHapticEnabled(enabled: Boolean) {
    prefs.setHapticEnabled(enabled)
    player.isHapticEnabled = enabled
    _uiState.value = _uiState.value.copy(isHapticEnabled = enabled)
  }

  fun setPreBufferEnabled(enabled: Boolean) {
    prefs.setPreBufferEnabled(enabled)
    player.isPreBufferEnabled = enabled
    _uiState.value = _uiState.value.copy(isPreBufferEnabled = enabled)
  }

  fun setMultiOutputEnabled(enabled: Boolean) {
    prefs.setMultiOutputEnabled(enabled)
    player.outputManager.setSimultaneousSpeakerPlayback(enabled)
    _uiState.value = _uiState.value.copy(isMultiOutputEnabled = enabled)
  }

  private fun extractFriendlyPath(uriStr: String): String {
    return try {
      val decoded = Uri.decode(uriStr)
      when {
        decoded.contains(":") -> {
          val segment = decoded.substringAfterLast(":")
          if (segment.startsWith("/")) segment else "/$segment"
        }
        decoded.startsWith("virtual_demo://") -> {
          "/SD_CARD/Music/" + decoded.substringAfterLast("/")
        }
        else -> decoded
      }
    } catch (_: Exception) {
      "/Music"
    }
  }

  override fun onCleared() {
    super.onCleared()
    player.release()
  }
}
