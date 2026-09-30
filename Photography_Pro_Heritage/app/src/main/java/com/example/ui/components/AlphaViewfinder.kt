package com.example.ui.components

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AiSubjectTracking
import com.example.model.AspectRatio
import com.example.model.CreativeLook
import com.example.model.DeviceProfile
import com.example.model.FocusMode
import com.example.model.FocusPoint
import com.example.model.GridType
import com.example.model.ShootingMode
import com.example.sensor.LevelState
import com.example.ui.theme.SonyAlphaGreen
import com.example.ui.theme.SonyAlphaRed
import com.example.ui.theme.SonyAlphaYellow
import com.example.ui.theme.SonyOrange
import com.example.viewmodel.CameraViewModel

@Composable
fun AlphaViewfinder(
  viewModel: CameraViewModel,
  levelState: LevelState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val uiState = viewModel.uiState.value

  val flashAlpha = remember { Animatable(0f) }
  LaunchedEffect(uiState.shutterFlashTrigger) {
    if (uiState.shutterFlashTrigger > 0L) {
      flashAlpha.snapTo(0.85f)
      flashAlpha.animateTo(0f, animationSpec = tween(140))
    }
  }

  val aspect = when (uiState.aspectRatio) {
    AspectRatio.RATIO_4_3 -> 4f / 3f
    AspectRatio.RATIO_16_9 -> 16f / 9f
    AspectRatio.RATIO_1_1 -> 1f
    AspectRatio.RATIO_21_9 -> 21f / 9f
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
    // 1. Camera Viewfinder Frame with Aspect Ratio
    Box(
      modifier = Modifier
        .aspectRatio(aspect)
        .fillMaxSize()
        .clip(RoundedCornerShape(2.dp))
        .background(Color(0xFF101216))
        .pointerInput(Unit) {
          detectTapGestures { offset ->
            val normX = (offset.x / size.width).coerceIn(0f, 1f)
            val normY = (offset.y / size.height).coerceIn(0f, 1f)
            viewModel.onUserTapFocus(normX, normY)
          }
        }
    ) {
      // CameraX Live Surface or High-fidelity synthetic preview
      if (uiState.isCameraPermissionGranted) {
        AndroidView(
          factory = { ctx ->
            PreviewView(ctx).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              scaleType = PreviewView.ScaleType.FILL_CENTER
              viewModel.bindCameraX(lifecycleOwner, this.surfaceProvider)
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        SyntheticLiveView(
          creativeLook = uiState.creativeLook,
          exposureComp = uiState.exposureComp,
          iso = uiState.calculatedIso,
          modifier = Modifier.fillMaxSize()
        )
      }

      // 2. Grid Lines Overlay (Rule of Thirds / Square / Golden)
      if (uiState.gridType != GridType.NONE) {
        ViewfinderGrid(gridType = uiState.gridType)
      }

      // 3. Electronic Level Gauge
      if (uiState.showLevelGauge) {
        AlphaLevelGauge(
          levelState = levelState,
          modifier = Modifier.matchParentSize()
        )
      }

      // 4. Focus Peaking Simulation Overlay in MF / Telemacro mode
      if (uiState.peakingEnabled && (uiState.focusMode == FocusMode.MF || uiState.isTeleMacroActive)) {
        PeakingOverlay(
          colorIndex = uiState.peakingColorIndex,
          modifier = Modifier.matchParentSize()
        )
      }

      // 5. Sony Alpha AF Target Brackets & AI Subject Pose Overlay
      AfPointsOverlay(
        focusPoints = uiState.focusPoints,
        isAfLocked = uiState.isAfLocked,
        modifier = Modifier.matchParentSize()
      )

      // 6. Top Status Bar Overlay (Alpha Camera Readouts)
      TopStatusBar(
        deviceProfile = uiState.deviceProfile,
        aiTracking = uiState.aiSubjectTracking,
        isTeleMacro = uiState.isTeleMacroActive,
        battery = "86%",
        shotsLeft = "9999+",
        format = uiState.fileFormat.label,
        creativeLook = uiState.creativeLook.code,
        aspectRatio = uiState.aspectRatio.label,
        shootingMode = uiState.shootingMode,
        dro = uiState.droEnabled,
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
      )

      // 7. Real-Time Live Histogram Widget (Bottom-Left)
      if (uiState.showHistogram) {
        AlphaHistogram(
          exposureComp = uiState.exposureComp,
          creativeLook = uiState.creativeLook,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 12.dp, bottom = 12.dp)
        )
      }

      // 8. Lens Selection Bar Floating at Bottom
      AlphaLensBar(
        deviceProfile = uiState.deviceProfile,
        selectedLens = uiState.selectedLens,
        currentZoomRatio = uiState.currentZoomRatio,
        isTeleMacroActive = uiState.isTeleMacroActive,
        onSelectLens = { viewModel.selectLens(it) },
        onZoomChange = { viewModel.setContinuousZoom(it) },
        onToggleTeleMacro = { viewModel.toggleTeleMacro() },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 12.dp)
      )

      // 9. Camera Shutter Flash Effect
      if (flashAlpha.value > 0f) {
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(Color.White.copy(alpha = flashAlpha.value))
        )
      }
    }
  }
}

@Composable
fun TopStatusBar(
  deviceProfile: DeviceProfile,
  aiTracking: AiSubjectTracking,
  isTeleMacro: Boolean,
  battery: String,
  shotsLeft: String,
  format: String,
  creativeLook: String,
  aspectRatio: String,
  shootingMode: ShootingMode,
  dro: Boolean,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.55f))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Model Profile Badge e.g. [1 VIII | α] or [1 V | α]
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(3.dp))
        .background(Color(0xFF222530))
        .border(1.dp, SonyOrange, RoundedCornerShape(3.dp))
        .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
      Text(
        text = if (deviceProfile == DeviceProfile.XPERIA_1_VIII) "1 VIII | α" else "1 V | α",
        color = SonyOrange,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Shooting Mode Badge
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(3.dp))
        .background(SonyOrange)
        .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
      Text(
        text = shootingMode.label,
        color = Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    // AI Tracking Badge if active
    if (aiTracking != AiSubjectTracking.OFF) {
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "[${aiTracking.shortName}]",
        color = SonyAlphaGreen,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    // Tele-macro indicator badge
    if (isTeleMacro) {
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "[MACRO]",
        color = SonyAlphaYellow,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Format & capacity: [RAW+J] 9999+
    Text(
      text = "[$format] $shotsLeft",
      color = Color.White.copy(alpha = 0.9f),
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.weight(1f))

    // Creative look badge
    Text(
      text = creativeLook,
      color = if (creativeLook != "ST") SonyOrange else Color.White.copy(alpha = 0.8f),
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Aspect ratio
    Text(
      text = aspectRatio,
      color = Color.White.copy(alpha = 0.85f),
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Battery percentage
    Text(
      text = battery,
      color = Color.White,
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun ViewfinderGrid(
  gridType: GridType,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val gridColor = Color.White.copy(alpha = 0.28f)

    when (gridType) {
      GridType.RULE_OF_THIRDS -> {
        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), 1f)
        drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), 1f)
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), 1f)
        drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), 1f)
      }
      GridType.SQUARE -> {
        val squareDim = minOf(w, h)
        val left = (w - squareDim) / 2f
        val top = (h - squareDim) / 2f
        drawRect(gridColor, Offset(left, top), Size(squareDim, squareDim), style = Stroke(1f))
      }
      GridType.GOLDEN_RATIO -> {
        val phi = 0.618f
        drawLine(gridColor, Offset(w * (1f - phi), 0f), Offset(w * (1f - phi), h), 1f)
        drawLine(gridColor, Offset(w * phi, 0f), Offset(w * phi, h), 1f)
        drawLine(gridColor, Offset(0f, h * (1f - phi)), Offset(w, h * (1f - phi)), 1f)
        drawLine(gridColor, Offset(0f, h * phi), Offset(w, h * phi), 1f)
      }
      GridType.NONE -> {}
    }
  }
}

@Composable
fun AfPointsOverlay(
  focusPoints: List<FocusPoint>,
  isAfLocked: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val boxColor = if (isAfLocked) SonyAlphaGreen else Color.White.copy(alpha = 0.65f)
    val strokeWidth = if (isAfLocked) 2.5f else 1.2f

    for (pt in focusPoints) {
      val cx = pt.x * size.width
      val cy = pt.y * size.height

      if (pt.isEyeAf) {
        // Sony Alpha Eye AF target reticle (Real-time Eye AF)
        val eyeSize = 34f
        drawRect(
          color = boxColor,
          topLeft = Offset(cx - eyeSize / 2f, cy - eyeSize / 2f),
          size = Size(eyeSize, eyeSize),
          style = Stroke(width = strokeWidth)
        )
        drawCircle(
          color = boxColor,
          radius = 3f,
          center = Offset(cx, cy)
        )
      } else if (pt.isAiBodyPose) {
        // Sony Alpha AI Body Pose estimation tracking reticle
        val poseW = 70f
        val poseH = 110f
        drawRect(
          color = boxColor.copy(alpha = 0.75f),
          topLeft = Offset(cx - poseW / 2f, cy - poseH / 2f),
          size = Size(poseW, poseH),
          style = Stroke(width = 1.2f)
        )
        // Joint markers
        drawCircle(color = boxColor, radius = 2.5f, center = Offset(cx - 20f, cy - 30f))
        drawCircle(color = boxColor, radius = 2.5f, center = Offset(cx + 20f, cy - 30f))
        drawCircle(color = boxColor, radius = 2.5f, center = Offset(cx, cy + 20f))
      } else {
        // Bracket Style AF Box `[ ]`
        val s = 22f
        val arm = 6f
        val left = cx - s / 2f
        val right = cx + s / 2f
        val top = cy - s / 2f
        val bottom = cy + s / 2f

        drawLine(boxColor, Offset(left, top), Offset(left + arm, top), strokeWidth)
        drawLine(boxColor, Offset(left, top), Offset(left, top + arm), strokeWidth)
        drawLine(boxColor, Offset(right, top), Offset(right - arm, top), strokeWidth)
        drawLine(boxColor, Offset(right, top), Offset(right, top + arm), strokeWidth)
        drawLine(boxColor, Offset(left, bottom), Offset(left + arm, bottom), strokeWidth)
        drawLine(boxColor, Offset(left, bottom), Offset(left, bottom - arm), strokeWidth)
        drawLine(boxColor, Offset(right, bottom), Offset(right - arm, bottom), strokeWidth)
        drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - arm), strokeWidth)
      }
    }
  }
}

@Composable
fun PeakingOverlay(
  colorIndex: Int,
  modifier: Modifier = Modifier
) {
  val peakingColor = when (colorIndex) {
    0 -> Color.White.copy(alpha = 0.75f)
    1 -> SonyAlphaYellow.copy(alpha = 0.75f)
    else -> SonyAlphaRed.copy(alpha = 0.75f)
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val random = java.util.Random(101)
    val pointsCount = 50
    for (i in 0 until pointsCount) {
      val x = w * (0.3f + random.nextFloat() * 0.4f)
      val y = h * (0.35f + random.nextFloat() * 0.35f)
      val len = 10f + random.nextFloat() * 18f
      drawLine(
        color = peakingColor,
        start = Offset(x, y),
        end = Offset(x + len, y + random.nextFloat() * 4f - 2f),
        strokeWidth = 2f
      )
    }
  }
}

@Composable
fun SyntheticLiveView(
  creativeLook: CreativeLook,
  exposureComp: String,
  iso: String,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val evFloat = exposureComp.replace("+", "").toFloatOrNull() ?: 0f
    val brightnessMultiplier = (1.0f + (evFloat * 0.15f)).coerceIn(0.4f, 1.8f)

    val (cTop, cBottom) = when (creativeLook) {
      CreativeLook.BW -> Color(0xFF1E1E1E) to Color(0xFF9E9E9E)
      CreativeLook.SE -> Color(0xFF382918) to Color(0xFFC49A6C)
      CreativeLook.FL -> Color(0xFF142028) to Color(0xFFC08E64)
      CreativeLook.VV -> Color(0xFF10345E) to Color(0xFFFF7A18)
      CreativeLook.IN -> Color(0xFF282836) to Color(0xFFD4A88C)
      CreativeLook.SH -> Color(0xFF323640) to Color(0xFFE8DACB)
      CreativeLook.NT -> Color(0xFF24272D) to Color(0xFFA69A8E)
      CreativeLook.ST -> Color(0xFF162338) to Color(0xFFE29B52)
    }

    val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
      colors = listOf(
        cTop.copy(
          red = (cTop.red * brightnessMultiplier).coerceIn(0f, 1f),
          green = (cTop.green * brightnessMultiplier).coerceIn(0f, 1f),
          blue = (cTop.blue * brightnessMultiplier).coerceIn(0f, 1f)
        ),
        cBottom.copy(
          red = (cBottom.red * brightnessMultiplier).coerceIn(0f, 1f),
          green = (cBottom.green * brightnessMultiplier).coerceIn(0f, 1f),
          blue = (cBottom.blue * brightnessMultiplier).coerceIn(0f, 1f)
        )
      )
    )
    drawRect(brush = brush)

    val silColor = Color.Black.copy(alpha = 0.55f)
    drawRect(silColor, Offset(w * 0.1f, h * 0.6f), Size(w * 0.15f, h * 0.4f))
    drawRect(silColor, Offset(w * 0.28f, h * 0.52f), Size(w * 0.2f, h * 0.48f))
    drawRect(silColor, Offset(w * 0.52f, h * 0.58f), Size(w * 0.18f, h * 0.42f))
    drawRect(silColor, Offset(w * 0.73f, h * 0.48f), Size(w * 0.22f, h * 0.52f))

    val isoNum = iso.toIntOrNull() ?: 100
    if (isoNum >= 1600) {
      val grainCount = (isoNum / 80).coerceIn(30, 180)
      val random = java.util.Random(isoNum.toLong())
      for (i in 0 until grainCount) {
        val gx = random.nextFloat() * w
        val gy = random.nextFloat() * h
        drawCircle(Color.White.copy(alpha = 0.2f), radius = 1.2f, center = Offset(gx, gy))
      }
    }
  }
}
