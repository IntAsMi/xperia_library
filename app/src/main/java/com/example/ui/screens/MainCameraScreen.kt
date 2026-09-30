package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.DriveMode
import com.example.model.FileFormat
import com.example.model.FlashMode
import com.example.model.FocusArea
import com.example.model.FocusMode
import com.example.model.MeteringMode
import com.example.ui.components.AlphaCreativeLookSheet
import com.example.ui.components.AlphaDialWheels
import com.example.ui.components.AlphaFnPanel
import com.example.ui.components.AlphaViewfinder
import com.example.ui.components.ModeDialSelector
import com.example.ui.components.TwoStageShutterButton
import com.example.ui.theme.SonyBorder
import com.example.ui.theme.SonyDarkChassis
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyTextPrimary
import com.example.viewmodel.ActiveWheel
import com.example.viewmodel.CameraViewModel
import java.io.File

@Composable
fun MainCameraScreen(
  viewModel: CameraViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val levelState by viewModel.levelSensorManager.levelState.collectAsState()
  val photos by viewModel.capturedPhotos.collectAsState()

  var showCreativeLookSheet by remember { mutableStateOf(false) }

  // Check camera permission
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.setCameraPermissionGranted(isGranted)
  }

  LaunchedEffect(Unit) {
    val isGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    viewModel.setCameraPermissionGranted(isGranted)
    if (!isGranted) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  // Gallery screen takes over if open
  if (uiState.isGalleryOpen) {
    AlphaPlaybackScreen(
      photos = photos,
      selectedPhoto = uiState.selectedPhotoForReview,
      onSelectPhoto = { viewModel.selectPhotoForReview(it) },
      onDeletePhoto = { viewModel.deletePhoto(it) },
      onClose = { viewModel.closeGallery() }
    )
    return
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(SonyDarkChassis)
  ) {
    val isLandscape = maxWidth > maxHeight

    if (isLandscape) {
      // Landscape Sony Alpha Camera Layout (similar to Xperia horizontal ergonomics)
      Row(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
      ) {
        // Left: Viewfinder
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          AlphaViewfinder(
            viewModel = viewModel,
            levelState = levelState,
            modifier = Modifier.fillMaxSize()
          )
        }

        // Right: Control Hub & Shutter
        Column(
          modifier = Modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(SonyPanelDark)
            .border(1.dp, SonyBorder)
            .padding(10.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          ModeDialSelector(
            selectedMode = uiState.shootingMode,
            onSelectMode = { viewModel.setShootingMode(it) }
          )

          AlphaFnPanel(
            uiState = uiState,
            onOpenWheel = { viewModel.setActiveWheel(it) },
            onToggleDriveMode = {
              val next = when (uiState.driveMode) {
                DriveMode.SINGLE -> DriveMode.BURST_HI
                DriveMode.BURST_HI -> DriveMode.BURST_LO
                DriveMode.BURST_LO -> DriveMode.TIMER_3S
                DriveMode.TIMER_3S -> DriveMode.TIMER_10S
                DriveMode.TIMER_10S -> DriveMode.SINGLE
              }
              viewModel.setDriveMode(next)
            },
            onToggleFocusMode = {
              val next = when (uiState.focusMode) {
                FocusMode.AF_C -> FocusMode.AF_S
                FocusMode.AF_S -> FocusMode.MF
                FocusMode.MF -> FocusMode.AF_C
              }
              viewModel.setFocusMode(next)
            },
            onToggleFocusArea = {
              val next = when (uiState.focusArea) {
                FocusArea.WIDE -> FocusArea.CENTER
                FocusArea.CENTER -> FocusArea.TRACKING
                FocusArea.TRACKING -> FocusArea.WIDE
              }
              viewModel.setFocusArea(next)
            },
            onToggleMetering = {
              val next = when (uiState.meteringMode) {
                MeteringMode.MULTI -> MeteringMode.CENTER
                MeteringMode.CENTER -> MeteringMode.SPOT
                MeteringMode.SPOT -> MeteringMode.MULTI
              }
              viewModel.setMeteringMode(next)
            },
            onToggleFlash = {
              val next = when (uiState.flashMode) {
                FlashMode.OFF -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.FILL
                FlashMode.FILL -> FlashMode.TORCH
                FlashMode.TORCH -> FlashMode.OFF
                else -> FlashMode.OFF
              }
              viewModel.setFlashMode(next)
            },
            onToggleFormat = {
              val next = when (uiState.fileFormat) {
                FileFormat.JPEG -> FileFormat.RAW_JPEG
                FileFormat.RAW_JPEG -> FileFormat.RAW
                FileFormat.RAW -> FileFormat.JPEG
              }
              viewModel.setFileFormat(next)
            },
            onToggleDro = { viewModel.toggleDro() },
            onTogglePeaking = { viewModel.togglePeaking() },
            onOpenCreativeLook = { showCreativeLookSheet = true },
            onOpenSettings = { viewModel.openMenu() },
            onSavePreset = { viewModel.saveMemoryPreset(it) },
            onRecallPreset = { viewModel.recallMemoryPreset(it) },
            modifier = Modifier.fillMaxWidth()
          )

          // Shutter + Gallery Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            RecentThumbnail(
              photos = photos,
              onClick = { viewModel.openGallery(photos.firstOrNull()) }
            )

            TwoStageShutterButton(
              isAfLocked = uiState.isAfLocked,
              driveMode = uiState.driveMode,
              isBurstShooting = uiState.isBurstShooting,
              burstCount = uiState.burstCount,
              timerCountdown = uiState.timerCountdown,
              onHalfPress = { viewModel.onShutterHalfPress(it) },
              onFullPress = { viewModel.onShutterFullPress() },
              onRelease = { viewModel.onShutterRelease() }
            )
          }
        }
      }
    } else {
      // Portrait Layout
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // 1. Top Mode Dial Bar
        ModeDialSelector(
          selectedMode = uiState.shootingMode,
          onSelectMode = { viewModel.setShootingMode(it) },
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        // 2. Viewfinder Frame
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          AlphaViewfinder(
            viewModel = viewModel,
            levelState = levelState,
            modifier = Modifier.fillMaxSize()
          )
        }

        // 3. Lower Control Panel: Fn Panel + Shutter Row
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(SonyPanelDark)
            .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
          AlphaFnPanel(
            uiState = uiState,
            onOpenWheel = { viewModel.setActiveWheel(it) },
            onToggleDriveMode = {
              val next = when (uiState.driveMode) {
                DriveMode.SINGLE -> DriveMode.BURST_HI
                DriveMode.BURST_HI -> DriveMode.BURST_LO
                DriveMode.BURST_LO -> DriveMode.TIMER_3S
                DriveMode.TIMER_3S -> DriveMode.TIMER_10S
                DriveMode.TIMER_10S -> DriveMode.SINGLE
              }
              viewModel.setDriveMode(next)
            },
            onToggleFocusMode = {
              val next = when (uiState.focusMode) {
                FocusMode.AF_C -> FocusMode.AF_S
                FocusMode.AF_S -> FocusMode.MF
                FocusMode.MF -> FocusMode.AF_C
              }
              viewModel.setFocusMode(next)
            },
            onToggleFocusArea = {
              val next = when (uiState.focusArea) {
                FocusArea.WIDE -> FocusArea.CENTER
                FocusArea.CENTER -> FocusArea.TRACKING
                FocusArea.TRACKING -> FocusArea.WIDE
              }
              viewModel.setFocusArea(next)
            },
            onToggleMetering = {
              val next = when (uiState.meteringMode) {
                MeteringMode.MULTI -> MeteringMode.CENTER
                MeteringMode.CENTER -> MeteringMode.SPOT
                MeteringMode.SPOT -> MeteringMode.MULTI
              }
              viewModel.setMeteringMode(next)
            },
            onToggleFlash = {
              val next = when (uiState.flashMode) {
                FlashMode.OFF -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.FILL
                FlashMode.FILL -> FlashMode.TORCH
                FlashMode.TORCH -> FlashMode.OFF
                else -> FlashMode.OFF
              }
              viewModel.setFlashMode(next)
            },
            onToggleFormat = {
              val next = when (uiState.fileFormat) {
                FileFormat.JPEG -> FileFormat.RAW_JPEG
                FileFormat.RAW_JPEG -> FileFormat.RAW
                FileFormat.RAW -> FileFormat.JPEG
              }
              viewModel.setFileFormat(next)
            },
            onToggleDro = { viewModel.toggleDro() },
            onTogglePeaking = { viewModel.togglePeaking() },
            onOpenCreativeLook = { showCreativeLookSheet = true },
            onOpenSettings = { viewModel.openMenu() },
            onSavePreset = { viewModel.saveMemoryPreset(it) },
            onRecallPreset = { viewModel.recallMemoryPreset(it) },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Shutter Row: Gallery Thumb, Center Spacer/Info, Physical Shutter Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            RecentThumbnail(
              photos = photos,
              onClick = { viewModel.openGallery(photos.firstOrNull()) }
            )

            // Center Xperia Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "XPERIA 1 V",
                color = SonyOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
              )
              Text(
                text = "PHOTOGRAPHY PRO",
                color = SonyTextPrimary.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
              )
            }

            TwoStageShutterButton(
              isAfLocked = uiState.isAfLocked,
              driveMode = uiState.driveMode,
              isBurstShooting = uiState.isBurstShooting,
              burstCount = uiState.burstCount,
              timerCountdown = uiState.timerCountdown,
              onHalfPress = { viewModel.onShutterHalfPress(it) },
              onFullPress = { viewModel.onShutterFullPress() },
              onRelease = { viewModel.onShutterRelease() }
            )
          }
        }
      }
    }

    // Overlays: Dial Wheels (Shutter, ISO, EV, WB, MF)
    AnimatedVisibility(
      visible = uiState.activeWheel != ActiveWheel.NONE,
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      AlphaDialWheels(
        activeWheel = uiState.activeWheel,
        shutterSpeed = uiState.shutterSpeed,
        iso = uiState.iso,
        exposureComp = uiState.exposureComp,
        whiteBalance = uiState.whiteBalance,
        customKelvin = uiState.customKelvin,
        manualFocusDistance = uiState.manualFocusDistance,
        onSelectShutter = { viewModel.setShutterSpeed(it) },
        onSelectIso = { viewModel.setIso(it) },
        onSelectEv = { viewModel.setExposureComp(it) },
        onSelectWb = { viewModel.setWhiteBalance(it) },
        onChangeKelvin = { viewModel.setCustomKelvin(it) },
        onChangeManualFocus = { viewModel.setManualFocusDistance(it) },
        onClose = { viewModel.closeActiveWheel() }
      )
    }

    // Overlays: Creative Look Selector Sheet
    AnimatedVisibility(
      visible = showCreativeLookSheet,
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      AlphaCreativeLookSheet(
        selectedLook = uiState.creativeLook,
        onSelectLook = {
          viewModel.setCreativeLook(it)
          showCreativeLookSheet = false
        },
        onClose = { showCreativeLookSheet = false }
      )
    }

    // Overlays: Settings Menu Sheet
    AnimatedVisibility(
      visible = uiState.isMenuOpen,
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      AlphaSettingsSheet(
        viewModel = viewModel,
        onClose = { viewModel.closeMenu() }
      )
    }

    // Memory Saved Toast Notification
    if (uiState.memorySlotSavedAlert != null) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 60.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(SonyOrange)
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(
          text = uiState.memorySlotSavedAlert ?: "",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

@Composable
fun RecentThumbnail(
  photos: List<com.example.data.PhotoEntity>,
  onClick: () -> Unit
) {
  val context = LocalContext.current
  val recent = photos.firstOrNull()

  Box(
    modifier = Modifier
      .size(54.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(SonyDarkChassis)
      .border(1.dp, SonyBorder, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .testTag("gallery_thumbnail_button"),
    contentAlignment = Alignment.Center
  ) {
    if (recent != null) {
      val model = if (recent.isSample) recent.sampleDrawableRes else File(recent.filePath)
      AsyncImage(
        model = ImageRequest.Builder(context)
          .data(model)
          .crossfade(true)
          .build(),
        contentDescription = "Recent Photo Thumbnail",
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize()
      )
    } else {
      Text(
        text = "PLAY",
        color = SonyTextPrimary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}
