package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.ChannelMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

interface DspAudioFrameListener {
  fun onAudioFrame(
    peakLeft: Float,
    peakRight: Float,
    bandsLeft: FloatArray,
    bandsRight: FloatArray
  )
}

@OptIn(UnstableApi::class)
class AudiophileDspAudioProcessor : BaseAudioProcessor() {

  @Volatile
  var channelMode: ChannelMode = ChannelMode.STEREO

  @Volatile
  var isPhaseInverted: Boolean = false

  var frameListener: DspAudioFrameListener? = null

  private val leftBands16 = FloatArray(16)
  private val rightBands16 = FloatArray(16)

  override fun isActive(): Boolean = true

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    return AudioProcessor.AudioFormat(
      inputAudioFormat.sampleRate,
      2, // stereo out
      C.ENCODING_PCM_16BIT
    )
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    val remaining = inputBuffer.remaining()
    if (remaining == 0) return

    val inputFormat = inputAudioFormat
    val channelCount = inputFormat.channelCount

    // Allocate output buffer
    val bytesPerFrame = channelCount * 2
    val frameCount = remaining / bytesPerFrame
    val outputBytes = frameCount * 4 // 2 channels * 2 bytes = 4 bytes per stereo frame
    val buffer = replaceOutputBuffer(outputBytes)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

    val currentMode = channelMode
    val invertPhase = isPhaseInverted

    var maxL = 0
    var maxR = 0

    // Reset temporary spectral band energy accumulators
    leftBands16.fill(0f)
    rightBands16.fill(0f)
    val framesPerBand = (frameCount / 16).coerceAtLeast(1)

    if (channelCount == 2) {
      var frameIdx = 0
      while (inputBuffer.remaining() >= 4) {
        var left = inputBuffer.short.toInt()
        var right = inputBuffer.short.toInt()

        // Apply Channel Mode
        when (currentMode) {
          ChannelMode.STEREO -> {
            // Keep left & right distinct
          }
          ChannelMode.LEFT_ONLY -> {
            // Solo Left channel, mute Right
            right = 0
          }
          ChannelMode.RIGHT_ONLY -> {
            // Solo Right channel, mute Left
            left = 0
          }
          ChannelMode.MONO -> {
            val mono = (left + right) / 2
            left = mono
            right = mono
          }
        }

        // Apply Phase Inversion (180 degrees)
        if (invertPhase) {
          left = -left
          right = -right
        }

        val clampedL = left.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        val clampedR = right.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

        val absL = abs(clampedL)
        val absR = abs(clampedR)
        if (absL > maxL) maxL = absL
        if (absR > maxR) maxR = absR

        val bandIdx = (frameIdx / framesPerBand).coerceIn(0, 15)
        leftBands16[bandIdx] += absL
        rightBands16[bandIdx] += absR

        buffer.putShort(clampedL.toShort())
        buffer.putShort(clampedR.toShort())
        frameIdx++
      }
    } else if (channelCount == 1) {
      var frameIdx = 0
      while (inputBuffer.remaining() >= 2) {
        var sample = inputBuffer.short.toInt()
        if (invertPhase) {
          sample = -sample
        }
        val clamped = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        val absS = abs(clamped)

        var lOut = clamped
        var rOut = clamped
        when (currentMode) {
          ChannelMode.LEFT_ONLY -> rOut = 0
          ChannelMode.RIGHT_ONLY -> lOut = 0
          else -> {}
        }

        val absL = abs(lOut)
        val absR = abs(rOut)
        if (absL > maxL) maxL = absL
        if (absR > maxR) maxR = absR

        val bandIdx = (frameIdx / framesPerBand).coerceIn(0, 15)
        leftBands16[bandIdx] += absL
        rightBands16[bandIdx] += absR

        buffer.putShort(lOut.toShort())
        buffer.putShort(rOut.toShort())
        frameIdx++
      }
    } else {
      buffer.put(inputBuffer)
    }

    buffer.flip()

    // Normalize and notify listener for live real-time visualizer
    val listener = frameListener
    if (listener != null && frameCount > 0) {
      val peakL = (maxL.toFloat() / 32768f).coerceIn(0f, 1f)
      val peakR = (maxR.toFloat() / 32768f).coerceIn(0f, 1f)

      val normBandsL = FloatArray(16)
      val normBandsR = FloatArray(16)
      val divisor = (framesPerBand * 32768f).coerceAtLeast(1f)

      for (b in 0 until 16) {
        normBandsL[b] = ((leftBands16[b] / divisor) * 2.5f).coerceIn(0.04f, 1.0f)
        normBandsR[b] = ((rightBands16[b] / divisor) * 2.5f).coerceIn(0.04f, 1.0f)
      }

      listener.onAudioFrame(peakL, peakR, normBandsL, normBandsR)
    }
  }
}
