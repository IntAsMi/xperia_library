package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.CreativeLook
import kotlin.math.exp
import kotlin.math.pow

@Composable
fun AlphaHistogram(
  exposureComp: String,
  creativeLook: CreativeLook,
  modifier: Modifier = Modifier
) {
  val evFloat = remember(exposureComp) {
    exposureComp.replace("+", "").toFloatOrNull() ?: 0f
  }

  // Pre-generate histogram curve points based on exposure and look
  val curve = remember(evFloat, creativeLook) {
    val bins = 40
    val mean = (bins / 2f) + (evFloat * 4f)
    val sigma = 7f + when (creativeLook) {
      CreativeLook.BW, CreativeLook.VV -> -1.5f
      CreativeLook.FL, CreativeLook.NT -> 2.5f
      else -> 0f
    }
    FloatArray(bins) { i ->
      val diff = i - mean
      val exponent = -(diff * diff) / (2f * sigma * sigma)
      val height = exp(exponent.toDouble()).toFloat()
      // Add slight secondary peak for high dynamic range look
      val secondary = 0.35f * exp(-((i - (mean + 9f)).pow(2)) / (2f * 16f))
      (height + secondary.toFloat()).coerceIn(0.05f, 1.0f)
    }
  }

  Box(
    modifier = modifier
      .width(110.dp)
      .height(55.dp)
      .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(2.dp))
      .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
      .padding(3.dp)
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val stepX = w / (curve.size - 1)

      // 1. Luminance curve (filled translucent white)
      val lumPath = Path()
      lumPath.moveTo(0f, h)
      for (i in curve.indices) {
        val x = i * stepX
        val y = h - (curve[i] * (h - 2f))
        lumPath.lineTo(x, y)
      }
      lumPath.lineTo(w, h)
      lumPath.close()

      drawPath(lumPath, color = Color.White.copy(alpha = 0.3f))
      drawPath(lumPath, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.5f))

      // 2. Red channel curve (shifted slightly warm)
      val rPath = Path()
      val rShift = if (creativeLook == CreativeLook.SE || creativeLook == CreativeLook.VV) 2 else 0
      rPath.moveTo(0f, h)
      for (i in curve.indices) {
        val shiftedIndex = (i - rShift).coerceIn(0, curve.size - 1)
        val x = i * stepX
        val y = h - (curve[shiftedIndex] * 0.95f * (h - 2f))
        if (i == 0) rPath.moveTo(x, y) else rPath.lineTo(x, y)
      }
      drawPath(rPath, color = Color(0xFFFF5252).copy(alpha = 0.75f), style = Stroke(width = 1.2f))

      // 3. Green channel curve
      val gPath = Path()
      for (i in curve.indices) {
        val x = i * stepX
        val y = h - (curve[i] * 0.88f * (h - 2f))
        if (i == 0) gPath.moveTo(x, y) else gPath.lineTo(x, y)
      }
      drawPath(gPath, color = Color(0xFF69F0AE).copy(alpha = 0.75f), style = Stroke(width = 1.2f))

      // 4. Blue channel curve
      val bPath = Path()
      val bShift = if (creativeLook == CreativeLook.FL) 3 else -1
      for (i in curve.indices) {
        val shiftedIndex = (i - bShift).coerceIn(0, curve.size - 1)
        val x = i * stepX
        val y = h - (curve[shiftedIndex] * 0.82f * (h - 2f))
        if (i == 0) bPath.moveTo(x, y) else bPath.lineTo(x, y)
      }
      drawPath(bPath, color = Color(0xFF448AFF).copy(alpha = 0.75f), style = Stroke(width = 1.2f))
    }
  }
}
