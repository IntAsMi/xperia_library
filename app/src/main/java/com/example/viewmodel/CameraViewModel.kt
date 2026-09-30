package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.SoundFeedback
import com.example.data.PhotoDatabase
import com.example.data.PhotoEntity
import com.example.data.PresetEntity
import com.example.model.AiSubjectTracking
import com.example.model.AspectRatio
import com.example.model.CreativeLook
import com.example.model.DeviceProfile
import com.example.model.DriveMode
import com.example.model.FileFormat
import com.example.model.FlashMode
import com.example.model.FocusArea
import com.example.model.FocusMode
import com.example.model.FocusPoint
import com.example.model.GridType
import com.example.model.LensOption
import com.example.model.MeteringMode
import com.example.model.PhotoProValues
import com.example.model.ShootingMode
import com.example.sensor.LevelSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

enum class ActiveWheel {
  NONE,
  SHUTTER,
  ISO,
  EV,
  WHITE_BALANCE,
  MANUAL_FOCUS,
  CREATIVE_LOOK,
  ASPECT_RATIO,
  DRIVE_MODE
}

data class CameraUiState(
  val deviceProfile: DeviceProfile = DeviceProfile.XPERIA_1_VIII,
  val shootingMode: ShootingMode = ShootingMode.AUTO,
  val selectedLens: LensOption = LensOption.WIDE_24MM,
  val currentZoomRatio: Float = 1.0f,
  val isTeleMacroActive: Boolean = false,
  val aiSubjectTracking: AiSubjectTracking = AiSubjectTracking.HUMAN,
  val driveMode: DriveMode = DriveMode.SINGLE,
  val focusMode: FocusMode = FocusMode.AF_C,
  val focusArea: FocusArea = FocusArea.WIDE,
  val manualFocusDistance: Float = 0.5f,
  val shutterSpeed: String = "1/250",
  val iso: String = "AUTO",
  val calculatedIso: String = "100",
  val exposureComp: String = "0.0",
  val whiteBalance: String = "AWB",
  val customKelvin: Int = 5500,
  val customTint: Int = 0,
  val meteringMode: MeteringMode = MeteringMode.MULTI,
  val flashMode: FlashMode = FlashMode.OFF,
  val creativeLook: CreativeLook = CreativeLook.ST,
  val aspectRatio: AspectRatio = AspectRatio.RATIO_4_3,
  val fileFormat: FileFormat = FileFormat.JPEG,
  val gridType: GridType = GridType.RULE_OF_THIRDS,
  val droEnabled: Boolean = true,
  val peakingEnabled: Boolean = false,
  val peakingColorIndex: Int = 1, // 0=White, 1=Yellow, 2=Red
  val showHistogram: Boolean = true,
  val showLevelGauge: Boolean = true,
  val isAfLocked: Boolean = false,
  val focusPoints: List<FocusPoint> = emptyList(),
  val activeWheel: ActiveWheel = ActiveWheel.NONE,
  val isCameraPermissionGranted: Boolean = false,
  val isTorchActive: Boolean = false,
  val isBurstShooting: Boolean = false,
  val burstCount: Int = 0,
  val timerCountdown: Int? = null,
  val shutterFlashTrigger: Long = 0L,
  val isGalleryOpen: Boolean = false,
  val selectedPhotoForReview: PhotoEntity? = null,
  val isMenuOpen: Boolean = false,
  val memorySlotSavedAlert: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
  private val context = application.applicationContext
  private val database = PhotoDatabase.getDatabase(context)
  private val photoDao = database.photoDao()
  private val presetDao = database.presetDao()

  val soundFeedback = SoundFeedback(context)
  val levelSensorManager = LevelSensorManager(context)

  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

  val capturedPhotos = photoDao.getAllPhotos().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  private var cameraProvider: ProcessCameraProvider? = null
  private var camera: Camera? = null
  private var imageCapture: ImageCapture? = null
  private var preview: Preview? = null

  private var burstJob: Job? = null
  private var timerJob: Job? = null
  private val random = Random()

  init {
    levelSensorManager.start()
    seedSamplePhotosIfNeeded()
    generateSimulatedAfPoints()
  }

  override fun onCleared() {
    super.onCleared()
    levelSensorManager.stop()
  }

  private fun seedSamplePhotosIfNeeded() {
    viewModelScope.launch {
      val count = photoDao.getPhotoCount()
      if (count == 0) {
        photoDao.insertPhoto(
          PhotoEntity(
            filePath = "sample_cinematic",
            timestamp = System.currentTimeMillis() - 3600_000 * 2,
            lensFocalLength = "24mm",
            aperture = "F1.8",
            shutterSpeed = "1/125",
            iso = "400",
            ev = "+0.3",
            creativeLook = "FL",
            format = "RAW+JPEG",
            isSample = true,
            sampleDrawableRes = R.drawable.sample_cinematic
          )
        )
        photoDao.insertPhoto(
          PhotoEntity(
            filePath = "sample_portrait",
            timestamp = System.currentTimeMillis() - 3600_000 * 24,
            lensFocalLength = "85mm",
            aperture = "F2.3",
            shutterSpeed = "1/500",
            iso = "100",
            ev = "0.0",
            creativeLook = "ST",
            format = "JPEG",
            isSample = true,
            sampleDrawableRes = R.drawable.sample_portrait
          )
        )
      }
    }
  }

  fun setDeviceProfile(profile: DeviceProfile) {
    soundFeedback.vibrateDialTick()
    val defaultLens = if (profile == DeviceProfile.XPERIA_1_VIII) LensOption.WIDE_24MM else LensOption.CLASSIC_24MM
    _uiState.value = _uiState.value.copy(
      deviceProfile = profile,
      selectedLens = defaultLens,
      currentZoomRatio = 1.0f,
      isTeleMacroActive = false
    )
    generateSimulatedAfPoints()
  }

  fun toggleAiSubjectTracking() {
    soundFeedback.vibrateDialTick()
    val next = when (_uiState.value.aiSubjectTracking) {
      AiSubjectTracking.OFF -> AiSubjectTracking.HUMAN
      AiSubjectTracking.HUMAN -> AiSubjectTracking.ANIMAL_BIRD
      AiSubjectTracking.ANIMAL_BIRD -> AiSubjectTracking.VEHICLE
      AiSubjectTracking.VEHICLE -> AiSubjectTracking.OFF
    }
    _uiState.value = _uiState.value.copy(aiSubjectTracking = next)
    generateSimulatedAfPoints()
  }

  fun toggleTeleMacro() {
    soundFeedback.vibrateDialTick()
    val newMacroState = !_uiState.value.isTeleMacroActive
    _uiState.value = _uiState.value.copy(
      isTeleMacroActive = newMacroState,
      peakingEnabled = newMacroState || _uiState.value.peakingEnabled,
      focusMode = if (newMacroState) FocusMode.MF else _uiState.value.focusMode
    )
  }

  fun setCameraPermissionGranted(granted: Boolean) {
    _uiState.value = _uiState.value.copy(isCameraPermissionGranted = granted)
  }

  fun bindCameraX(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    surfaceProvider: Preview.SurfaceProvider
  ) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
      try {
        cameraProvider = cameraProviderFuture.get()
        preview = Preview.Builder().build().also {
          it.surfaceProvider = surfaceProvider
        }
        imageCapture = ImageCapture.Builder()
          .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
          .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider?.unbindAll()
        camera = cameraProvider?.bindToLifecycle(
          lifecycleOwner,
          cameraSelector,
          preview,
          imageCapture
        )

        applyZoomRatio(_uiState.value.currentZoomRatio)
        applyExposureCompensation(_uiState.value.exposureComp)
      } catch (e: Exception) {
        Log.e("CameraViewModel", "Failed to bind CameraX lifecycle", e)
      }
    }, ContextCompat.getMainExecutor(context))
  }

  // Shooting Mode Dial (BASIC, AUTO, P, S, M, MR)
  fun setShootingMode(mode: ShootingMode) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(
      shootingMode = mode,
      activeWheel = ActiveWheel.NONE
    )
    if (mode == ShootingMode.AUTO || mode == ShootingMode.BASIC) {
      _uiState.value = _uiState.value.copy(
        iso = "AUTO",
        shutterSpeed = "1/250"
      )
    }
  }

  // Lens Selection
  fun selectLens(lens: LensOption) {
    soundFeedback.vibrateDialTick()
    val newZoom = lens.baseZoom
    _uiState.value = _uiState.value.copy(
      selectedLens = lens,
      currentZoomRatio = newZoom,
      isTeleMacroActive = false
    )
    applyZoomRatio(newZoom)
    generateSimulatedAfPoints()
  }

  fun setContinuousZoom(zoom: Float) {
    val clamped = zoom.coerceIn(0.6f, 21.3f)
    _uiState.value = _uiState.value.copy(currentZoomRatio = clamped)
    applyZoomRatio(clamped)
  }

  private fun applyZoomRatio(ratio: Float) {
    camera?.cameraControl?.setZoomRatio(ratio.coerceAtLeast(1.0f))
  }

  // Shutter Speed
  fun setShutterSpeed(speed: String) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(shutterSpeed = speed)
  }

  // ISO
  fun setIso(isoVal: String) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(
      iso = isoVal,
      calculatedIso = if (isoVal == "AUTO") "100" else isoVal
    )
  }

  // Exposure Comp
  fun setExposureComp(ev: String) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(exposureComp = ev)
    applyExposureCompensation(ev)
  }

  private fun applyExposureCompensation(evString: String) {
    try {
      val evFloat = evString.replace("+", "").toFloatOrNull() ?: 0f
      val index = (evFloat * 3).toInt()
      camera?.cameraControl?.setExposureCompensationIndex(index)
    } catch (_: Exception) {}
  }

  // White Balance
  fun setWhiteBalance(wb: String) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(whiteBalance = wb)
  }

  fun setCustomKelvin(kelvin: Int) {
    _uiState.value = _uiState.value.copy(customKelvin = kelvin)
  }

  fun setCustomTint(tint: Int) {
    _uiState.value = _uiState.value.copy(customTint = tint)
  }

  // Focus Mode
  fun setFocusMode(mode: FocusMode) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(
      focusMode = mode,
      peakingEnabled = (mode == FocusMode.MF) || _uiState.value.isTeleMacroActive
    )
  }

  fun setManualFocusDistance(distance: Float) {
    _uiState.value = _uiState.value.copy(manualFocusDistance = distance.coerceIn(0f, 1f))
  }

  // Focus Area
  fun setFocusArea(area: FocusArea) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(focusArea = area)
    generateSimulatedAfPoints()
  }

  // Drive Mode
  fun setDriveMode(mode: DriveMode) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(driveMode = mode)
  }

  // Creative Look
  fun setCreativeLook(look: CreativeLook) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(creativeLook = look)
  }

  // Aspect Ratio
  fun setAspectRatio(ratio: AspectRatio) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(aspectRatio = ratio)
  }

  // Flash Mode
  fun setFlashMode(flash: FlashMode) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(
      flashMode = flash,
      isTorchActive = (flash == FlashMode.TORCH)
    )
    camera?.cameraControl?.enableTorch(flash == FlashMode.TORCH)
  }

  // Metering Mode
  fun setMeteringMode(mode: MeteringMode) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(meteringMode = mode)
  }

  // File Format
  fun setFileFormat(format: FileFormat) {
    soundFeedback.vibrateDialTick()
    _uiState.value = _uiState.value.copy(fileFormat = format)
  }

  // Grid
  fun setGridType(grid: GridType) {
    _uiState.value = _uiState.value.copy(gridType = grid)
  }

  // Peaking
  fun togglePeaking() {
    _uiState.value = _uiState.value.copy(peakingEnabled = !_uiState.value.peakingEnabled)
  }

  fun setPeakingColorIndex(index: Int) {
    _uiState.value = _uiState.value.copy(peakingColorIndex = index)
  }

  // DRO
  fun toggleDro() {
    _uiState.value = _uiState.value.copy(droEnabled = !_uiState.value.droEnabled)
  }

  // Histogram
  fun toggleHistogram() {
    _uiState.value = _uiState.value.copy(showHistogram = !_uiState.value.showHistogram)
  }

  // Level Gauge
  fun toggleLevelGauge() {
    _uiState.value = _uiState.value.copy(showLevelGauge = !_uiState.value.showLevelGauge)
  }

  // Active adjustment wheel
  fun setActiveWheel(wheel: ActiveWheel) {
    if (_uiState.value.activeWheel == wheel) {
      _uiState.value = _uiState.value.copy(activeWheel = ActiveWheel.NONE)
    } else {
      soundFeedback.vibrateDialTick()
      _uiState.value = _uiState.value.copy(activeWheel = wheel)
    }
  }

  fun closeActiveWheel() {
    _uiState.value = _uiState.value.copy(activeWheel = ActiveWheel.NONE)
  }

  // Tap to Focus in Viewfinder
  fun onUserTapFocus(normX: Float, normY: Float) {
    val point = FocusPoint(normX, normY, isLocked = true, isEyeAf = false, label = "LOCK")
    _uiState.value = _uiState.value.copy(
      focusPoints = listOf(point),
      isAfLocked = true
    )
    soundFeedback.playAfLockSound()

    try {
      val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
      val meteringPoint = factory.createPoint(normX, normY)
      val action = FocusMeteringAction.Builder(meteringPoint).build()
      camera?.cameraControl?.startFocusAndMetering(action)
    } catch (_: Exception) {}

    viewModelScope.launch {
      delay(2200)
      if (_uiState.value.focusMode != FocusMode.AF_S) {
        generateSimulatedAfPoints()
      }
    }
  }

  /**
   * Two-Stage Physical Shutter Button Emulation:
   * Half-press triggers AF-ON lock and meters exposure.
   */
  fun onShutterHalfPress(isPressed: Boolean) {
    if (isPressed) {
      val currentPoints = _uiState.value.focusPoints.map { it.copy(isLocked = true) }
      _uiState.value = _uiState.value.copy(
        isAfLocked = true,
        focusPoints = currentPoints
      )
      soundFeedback.playAfLockSound()
    } else {
      if (!_uiState.value.isBurstShooting) {
        _uiState.value = _uiState.value.copy(isAfLocked = false)
        generateSimulatedAfPoints()
      }
    }
  }

  /**
   * Full Press Shutter Action (or tap on shutter key).
   */
  fun onShutterFullPress() {
    val drive = _uiState.value.driveMode
    when (drive) {
      DriveMode.SINGLE -> executeShutterCapture()
      DriveMode.TIMER_3S -> startTimerCountdown(3)
      DriveMode.TIMER_10S -> startTimerCountdown(10)
      DriveMode.BURST_ULTRA -> startBurstShooting(DriveMode.BURST_ULTRA)
      DriveMode.BURST_HI -> startBurstShooting(DriveMode.BURST_HI)
      DriveMode.BURST_LO -> startBurstShooting(DriveMode.BURST_LO)
    }
  }

  fun onShutterRelease() {
    if (_uiState.value.isBurstShooting) {
      burstJob?.cancel()
      _uiState.value = _uiState.value.copy(isBurstShooting = false, burstCount = 0)
    }
  }

  private fun startTimerCountdown(seconds: Int) {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      for (i in seconds downTo 1) {
        _uiState.value = _uiState.value.copy(timerCountdown = i)
        soundFeedback.playTimerBeep(isFinal = (i == 1))
        delay(1000)
      }
      _uiState.value = _uiState.value.copy(timerCountdown = null)
      executeShutterCapture()
    }
  }

  private fun startBurstShooting(mode: DriveMode) {
    burstJob?.cancel()
    _uiState.value = _uiState.value.copy(isBurstShooting = true, burstCount = 0)
    val intervalMs = when (mode) {
      DriveMode.BURST_ULTRA -> 16L // 60fps Xperia 1 VIII ultra burst!
      DriveMode.BURST_HI -> 33L    // 30fps
      else -> 100L                 // 10fps
    }
    val maxFrames = if (mode == DriveMode.BURST_ULTRA) 60 else 30

    burstJob = viewModelScope.launch {
      var count = 0
      while (count < maxFrames) {
        count++
        _uiState.value = _uiState.value.copy(burstCount = count)
        executeShutterCapture(isSilent = count > 1)
        delay(intervalMs)
      }
      _uiState.value = _uiState.value.copy(isBurstShooting = false, burstCount = 0)
    }
  }

  private fun executeShutterCapture(isSilent: Boolean = false) {
    if (!isSilent) {
      soundFeedback.playShutterSound()
    }
    _uiState.value = _uiState.value.copy(shutterFlashTrigger = System.currentTimeMillis())

    val capture = imageCapture
    if (capture != null && _uiState.value.isCameraPermissionGranted) {
      saveCameraXShot(capture)
    } else {
      saveSimulatedShot()
    }
  }

  private fun saveCameraXShot(capture: ImageCapture) {
    val photoDir = File(context.filesDir, "xperia_photos").apply { mkdirs() }
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    val photoFile = File(photoDir, "PHOTO_PRO_${timeStamp}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          savePhotoToDatabase(photoFile.absolutePath)
        }

        override fun onError(exception: ImageCaptureException) {
          Log.w("CameraViewModel", "CameraX capture error: ${exception.message}", exception)
          saveSimulatedShot()
        }
      }
    )
  }

  private fun saveSimulatedShot() {
    viewModelScope.launch {
      try {
        val photoDir = File(context.filesDir, "xperia_photos").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val photoFile = File(photoDir, "PHOTO_PRO_${timeStamp}.jpg")

        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val look = _uiState.value.creativeLook
        val topColor = when (look) {
          CreativeLook.BW -> AndroidColor.rgb(30, 30, 30)
          CreativeLook.SE -> AndroidColor.rgb(60, 45, 30)
          CreativeLook.FL -> AndroidColor.rgb(20, 38, 48)
          CreativeLook.VV -> AndroidColor.rgb(20, 60, 100)
          else -> AndroidColor.rgb(25, 35, 55)
        }
        val bottomColor = when (look) {
          CreativeLook.BW -> AndroidColor.rgb(180, 180, 180)
          CreativeLook.SE -> AndroidColor.rgb(210, 170, 120)
          CreativeLook.FL -> AndroidColor.rgb(190, 140, 100)
          CreativeLook.VV -> AndroidColor.rgb(240, 130, 40)
          else -> AndroidColor.rgb(220, 150, 80)
        }

        val shader = android.graphics.LinearGradient(
          0f, 0f, width.toFloat(), height.toFloat(),
          topColor, bottomColor, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.shader = null
        paint.color = AndroidColor.argb(160, 0, 0, 0)
        canvas.drawRect(0f, height - 120f, width.toFloat(), height.toFloat(), paint)

        paint.color = AndroidColor.rgb(255, 96, 0)
        paint.textSize = 34f
        paint.isFakeBoldText = true
        val modelBrand = if (_uiState.value.deviceProfile == DeviceProfile.XPERIA_1_VIII) "SONY α | Xperia 1 VIII Photography Pro" else "SONY α | Xperia 1 V Photography Pro"
        canvas.drawText(modelBrand, 60f, height - 60f, paint)

        paint.color = AndroidColor.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = false
        val exifText = "${_uiState.value.selectedLens.focalLength} ${_uiState.value.selectedLens.aperture}  |  ${_uiState.value.shutterSpeed}s  |  ISO ${_uiState.value.calculatedIso}  |  ${look.code}  |  ${_uiState.value.fileFormat.label}"
        canvas.drawText(exifText, 60f, height - 25f, paint)

        val fos = FileOutputStream(photoFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        fos.flush()
        fos.close()

        savePhotoToDatabase(photoFile.absolutePath)
      } catch (e: Exception) {
        Log.e("CameraViewModel", "Failed to save simulated photo", e)
      }
    }
  }

  private fun savePhotoToDatabase(filePath: String) {
    viewModelScope.launch {
      val state = _uiState.value
      val photo = PhotoEntity(
        filePath = filePath,
        timestamp = System.currentTimeMillis(),
        lensFocalLength = state.selectedLens.focalLength,
        aperture = state.selectedLens.aperture,
        shutterSpeed = state.shutterSpeed,
        iso = state.calculatedIso,
        ev = state.exposureComp,
        creativeLook = state.creativeLook.code,
        format = state.fileFormat.label,
        isSample = false
      )
      photoDao.insertPhoto(photo)
    }
  }

  fun deletePhoto(photoId: Long) {
    viewModelScope.launch {
      photoDao.deletePhoto(photoId)
      if (_uiState.value.selectedPhotoForReview?.id == photoId) {
        _uiState.value = _uiState.value.copy(selectedPhotoForReview = null)
      }
    }
  }

  // Memory Recall (MR) Presets
  fun saveMemoryPreset(slotKey: String) {
    viewModelScope.launch {
      val state = _uiState.value
      val preset = PresetEntity(
        slotKey = slotKey,
        name = "Memory $slotKey",
        mode = state.shootingMode.name,
        lens = state.selectedLens.name,
        shutterSpeed = state.shutterSpeed,
        iso = state.iso,
        ev = state.exposureComp,
        whiteBalance = state.whiteBalance,
        focusMode = state.focusMode.name,
        creativeLook = state.creativeLook.name,
        driveMode = state.driveMode.name
      )
      presetDao.savePreset(preset)
      _uiState.value = _uiState.value.copy(memorySlotSavedAlert = "Saved current settings to $slotKey")
      delay(2000)
      _uiState.value = _uiState.value.copy(memorySlotSavedAlert = null)
    }
  }

  fun recallMemoryPreset(slotKey: String) {
    viewModelScope.launch {
      val preset = presetDao.getPreset(slotKey) ?: return@launch
      try {
        val lens = LensOption.valueOf(preset.lens)
        val creative = CreativeLook.valueOf(preset.creativeLook)
        val focus = FocusMode.valueOf(preset.focusMode)
        val drive = DriveMode.valueOf(preset.driveMode)
        _uiState.value = _uiState.value.copy(
          selectedLens = lens,
          currentZoomRatio = lens.baseZoom,
          shutterSpeed = preset.shutterSpeed,
          iso = preset.iso,
          exposureComp = preset.ev,
          whiteBalance = preset.whiteBalance,
          focusMode = focus,
          creativeLook = creative,
          driveMode = drive
        )
        soundFeedback.playAfLockSound()
      } catch (_: Exception) {}
    }
  }

  fun openGallery(photo: PhotoEntity? = null) {
    _uiState.value = _uiState.value.copy(
      isGalleryOpen = true,
      selectedPhotoForReview = photo
    )
  }

  fun closeGallery() {
    _uiState.value = _uiState.value.copy(
      isGalleryOpen = false,
      selectedPhotoForReview = null
    )
  }

  fun selectPhotoForReview(photo: PhotoEntity) {
    _uiState.value = _uiState.value.copy(selectedPhotoForReview = photo)
  }

  fun openMenu() {
    _uiState.value = _uiState.value.copy(isMenuOpen = true)
  }

  fun closeMenu() {
    _uiState.value = _uiState.value.copy(isMenuOpen = false)
  }

  private fun generateSimulatedAfPoints() {
    val points = mutableListOf<FocusPoint>()
    val aiMode = _uiState.value.aiSubjectTracking

    if (aiMode != AiSubjectTracking.OFF) {
      when (aiMode) {
        AiSubjectTracking.HUMAN -> {
          // AI Pose Estimation & Eye AF tracking reticle
          points.add(FocusPoint(0.51f, 0.38f, isLocked = false, isEyeAf = true, label = "EYE [R]"))
          points.add(FocusPoint(0.50f, 0.48f, isLocked = false, isAiBodyPose = true, label = "POSE"))
        }
        AiSubjectTracking.ANIMAL_BIRD -> {
          points.add(FocusPoint(0.48f, 0.45f, isLocked = false, isEyeAf = true, label = "ANIMAL EYE"))
        }
        AiSubjectTracking.VEHICLE -> {
          points.add(FocusPoint(0.50f, 0.55f, isLocked = false, isAiBodyPose = true, label = "VEHICLE"))
        }
        else -> {}
      }
    } else {
      val area = _uiState.value.focusArea
      when (area) {
        FocusArea.WIDE -> {
          val xs = listOf(0.35f, 0.5f, 0.65f)
          val ys = listOf(0.4f, 0.5f, 0.6f)
          for (x in xs) {
            for (y in ys) {
              points.add(FocusPoint(x, y, isLocked = false, isEyeAf = false))
            }
          }
        }
        FocusArea.CENTER -> {
          points.add(FocusPoint(0.5f, 0.5f, isLocked = false, isEyeAf = false))
        }
        FocusArea.TRACKING -> {
          points.add(FocusPoint(0.52f, 0.42f, isLocked = false, isEyeAf = true, label = "TRACK"))
        }
      }
    }
    _uiState.value = _uiState.value.copy(focusPoints = points, isAfLocked = false)
  }
}
