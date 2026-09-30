package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.sensor.LevelState
import com.example.ui.theme.SonyAlphaGreen
import com.example.ui.theme.SonyOrange
import kotlin.math.abs

@Composable
fun AlphaLevelGauge(
  levelState: LevelState,
  modifier: Modifier = Modifier
) {
  val isLevel = levelState.isLevel
  val targetColor = when {
    isLevel -> SonyAlphaGreen
    abs(levelState.roll) < 5f && abs(levelState.pitch) < 5f -> Color.White.copy(alpha = 0.85f)
    else -> SonyOrange.copy(alpha = 0.85f)
  }

  val gaugeColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = tween(durationMillis = 200),
    label = "levelGaugeColor"
  )

  Box(modifier = modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val centerX = size.width / 2f
      val centerY = size.height / 2f

      // 1. Center Reference Reticle (Fixed Center Cross)
      val reticleSize = 14f
      val refColor = Color.White.copy(alpha = 0.35f)
      drawLine(
        color = refColor,
        start = Offset(centerX - reticleSize, centerY),
        end = Offset(centerX + reticleSize, centerY),
        strokeWidth = 2f
      )
      drawLine(
        color = refColor,
        start = Offset(centerX, centerY - reticleSize),
        end = Offset(centerX, centerY + reticleSize),
        strokeWidth = 2f
      )

      // 2. Roll Horizon Bar (Rotates based on roll angle)
      rotate(degrees = -levelState.roll, pivot = Offset(centerX, centerY)) {
        val barHalfLen = 90f
        val gap = 20f

        // Left wing
        drawLine(
          color = gaugeColor,
          start = Offset(centerX - barHalfLen, centerY),
          end = Offset(centerX - gap, centerY),
          strokeWidth = 3f,
          cap = StrokeCap.Round
        )

        // Right wing
        drawLine(
          color = gaugeColor,
          start = Offset(centerX + gap, centerY),
          end = Offset(centerX + barHalfLen, centerY),
          strokeWidth = 3f,
          cap = StrokeCap.Round
        )

        // Center dot
        drawCircle(
          color = gaugeColor,
          radius = 3.5f,
          center = Offset(centerX, centerY)
        )
      }

      // 3. Pitch Indicator Bar (Vertical displacement)
      val maxPitchOffset = 50f
      val pitchPx = (levelState.pitch / 25f).coerceIn(-1f, 1f) * maxPitchOffset

      // Pitch ticks on right side of reticle
      val tickX = centerX + 110f
      val tickColor = Color.White.copy(alpha = 0.4f)
      drawLine(
        color = tickColor,
        start = Offset(tickX - 6f, centerY - maxPitchOffset),
        end = Offset(tickX + 6f, centerY - maxPitchOffset),
        strokeWidth = 1.5f
      )
      drawLine(
        color = tickColor,
        start = Offset(tickX - 10f, centerY),
        end = Offset(tickX + 10f, centerY),
        strokeWidth = 2f
      )
      drawLine(
        color = tickColor,
        start = Offset(tickX - 6f, centerY + maxPitchOffset),
        end = Offset(tickX + 6f, centerY + maxPitchOffset),
        strokeWidth = 1.5f
      )

      // Pitch marker needle
      val pitchNeedleY = centerY + pitchPx
      val pitchNeedleColor = if (levelState.isPitchLevel) SonyAlphaGreen else SonyOrange
      drawLine(
        color = pitchNeedleColor,
        start = Offset(tickX - 14f, pitchNeedleY),
        end = Offset(tickX + 4f, pitchNeedleY),
        strokeWidth = 3f,
        cap = StrokeCap.Square
      )
    }
  }
}
