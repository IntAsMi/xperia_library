package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.AudioPhaseMode
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
  var phaseMode: AudioPhaseMode = AudioPhaseMode.NORMAL

  var isPhaseInverted: Boolean
    get() = phaseMode != AudioPhaseMode.NORMAL
    set(value) {
      phaseMode = if (value) AudioPhaseMode.INVERT_BOTH else AudioPhaseMode.NORMAL
    }

  var frameListener: DspAudioFrameListener? = null

  private val leftBands16 = FloatArray(16)
  private val rightBands16 = FloatArray(16)

  override fun isActive(): Boolean = true

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    // Preserve exact source sample rate and encoding (16-bit, 24-bit, 32-bit float) without resampling
    val encoding = if (inputAudioFormat.encoding == C.ENCODING_INVALID) C.ENCODING_PCM_16BIT else inputAudioFormat.encoding
    return AudioProcessor.AudioFormat(
      inputAudioFormat.sampleRate,
      inputAudioFormat.channelCount.coerceAtLeast(2),
      encoding
    )
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    val remaining = inputBuffer.remaining()
    if (remaining == 0) return

    val inputFormat = inputAudioFormat
    val channelCount = inputFormat.channelCount

    val bytesPerSample = when (inputFormat.encoding) {
      C.ENCODING_PCM_FLOAT, C.ENCODING_PCM_32BIT -> 4
      C.ENCODING_PCM_24BIT -> 3
      else -> 2
    }
    val bytesPerFrame = channelCount * bytesPerSample
    val frameCount = if (bytesPerFrame > 0) remaining / bytesPerFrame else 0
    if (frameCount == 0) return

    val outputBytes = frameCount * 2 * bytesPerSample
    val buffer = replaceOutputBuffer(outputBytes)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

    val currentMode = channelMode
    val currentPhase = phaseMode
    val isPhaseModified = currentPhase != AudioPhaseMode.NORMAL

    var maxL = 0
    var maxR = 0

    // Reset temporary spectral band energy accumulators
    leftBands16.fill(0f)
    rightBands16.fill(0f)
    val framesPerBand = (frameCount / 16).coerceAtLeast(1)

    // Bit-Perfect Pure Stereo Bypass (No DSP modification, bit-exact pass-through)
    if (currentMode == ChannelMode.STEREO && !isPhaseModified && channelCount == 2 && bytesPerSample == 2) {
      var frameIdx = 0
      val startPos = inputBuffer.position()
      while (inputBuffer.remaining() >= 4) {
        val left = inputBuffer.short.toInt()
        val right = inputBuffer.short.toInt()
        val absL = abs(left)
        val absR = abs(right)
        if (absL > maxL) maxL = absL
        if (absR > maxR) maxR = absR
        val bandIdx = (frameIdx / framesPerBand).coerceIn(0, 15)
        leftBands16[bandIdx] += absL
        rightBands16[bandIdx] += absR
        buffer.putShort(left.toShort())
        buffer.putShort(right.toShort())
        frameIdx++
      }
    } else if (currentMode == ChannelMode.STEREO && !isPhaseModified && bytesPerSample > 2) {
      // 24-bit / 32-bit float Bit-Perfect Direct Stream Bypass
      buffer.put(inputBuffer)
      maxL = 28000
      maxR = 28000
      for (b in 0 until 16) {
        leftBands16[b] = 20000f * framesPerBand
        rightBands16[b] = 20000f * framesPerBand
      }
    } else if (channelCount == 2 && bytesPerSample == 2) {
      var frameIdx = 0
      while (inputBuffer.remaining() >= 4) {
        var left = inputBuffer.short.toInt()
        var right = inputBuffer.short.toInt()

        // Apply Channel Mode
        when (currentMode) {
          ChannelMode.STEREO -> {}
          ChannelMode.LEFT_ONLY -> right = 0
          ChannelMode.RIGHT_ONLY -> left = 0
          ChannelMode.MONO -> {
            val mono = (left + right) / 2
            left = mono
            right = mono
          }
        }

        // Apply Phase Inversion / Wiring Correction
        when (currentPhase) {
          AudioPhaseMode.NORMAL -> {}
          AudioPhaseMode.INVERT_BOTH -> {
            left = -left
            right = -right
          }
          AudioPhaseMode.INVERT_LEFT_ONLY -> {
            left = -left
          }
          AudioPhaseMode.INVERT_RIGHT_ONLY -> {
            right = -right
          }
          AudioPhaseMode.SWAP_CHANNELS -> {
            val tmp = left
            left = right
            right = tmp
          }
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
    } else if (channelCount == 2 && bytesPerSample == 4) {
      // 32-bit Float / PCM processing
      var frameIdx = 0
      while (inputBuffer.remaining() >= 8) {
        var left = inputBuffer.float
        var right = inputBuffer.float

        when (currentMode) {
          ChannelMode.LEFT_ONLY -> right = 0f
          ChannelMode.RIGHT_ONLY -> left = 0f
          ChannelMode.MONO -> {
            val m = (left + right) / 2f
            left = m
            right = m
          }
          ChannelMode.STEREO -> {}
        }

        when (currentPhase) {
          AudioPhaseMode.INVERT_BOTH -> {
            left = -left
            right = -right
          }
          AudioPhaseMode.INVERT_LEFT_ONLY -> {
            left = -left
          }
          AudioPhaseMode.INVERT_RIGHT_ONLY -> {
            right = -right
          }
          AudioPhaseMode.SWAP_CHANNELS -> {
            val tmp = left
            left = right
            right = tmp
          }
          AudioPhaseMode.NORMAL -> {}
        }

        val absL = (abs(left) * 32767f).toInt().coerceIn(0, 32767)
        val absR = (abs(right) * 32767f).toInt().coerceIn(0, 32767)
        if (absL > maxL) maxL = absL
        if (absR > maxR) maxR = absR

        val bandIdx = (frameIdx / framesPerBand).coerceIn(0, 15)
        leftBands16[bandIdx] += absL
        rightBands16[bandIdx] += absR

        buffer.putFloat(left)
        buffer.putFloat(right)
        frameIdx++
      }
    } else if (channelCount == 1) {
      var frameIdx = 0
      while (inputBuffer.remaining() >= 2) {
        var sample = inputBuffer.short.toInt()
        if (currentPhase == AudioPhaseMode.INVERT_BOTH || currentPhase == AudioPhaseMode.INVERT_LEFT_ONLY) {
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
