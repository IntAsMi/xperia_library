package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.AudioPhaseMode
import com.example.model.ChannelMode
import com.example.model.CrossfeedMode
import com.example.model.DacFilterProfile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

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

  @Volatile
  var crossfeedMode: CrossfeedMode = CrossfeedMode.OFF

  @Volatile
  var dacFilterProfile: DacFilterProfile = DacFilterProfile.LINEAR_PHASE_FAST

  var isPhaseInverted: Boolean
    get() = phaseMode != AudioPhaseMode.NORMAL
    set(value) {
      phaseMode = if (value) AudioPhaseMode.INVERT_LEFT_ONLY else AudioPhaseMode.NORMAL
    }

  var frameListener: DspAudioFrameListener? = null

  private val leftBands16 = FloatArray(16)
  private val rightBands16 = FloatArray(16)

  // Crossfeed filter state variables (single-pole IIR)
  private var crossfeedStateL = 0.0
  private var crossfeedStateR = 0.0

  override fun isActive(): Boolean = true

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
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
    val sampleRate = inputFormat.sampleRate.coerceAtLeast(44100)

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
    val currentCrossfeed = crossfeedMode
    val isPhaseModified = currentPhase != AudioPhaseMode.NORMAL
    val isCrossfeedActive = currentCrossfeed != CrossfeedMode.OFF

    // Crossfeed coefficients
    val (cfAlpha, cfBlend) = when (currentCrossfeed) {
      CrossfeedMode.CHU_MOY -> {
        // Cutoff ~700 Hz, -4.5dB cross-blend
        val rc = 1.0 / (2.0 * Math.PI * 700.0)
        val dt = 1.0 / sampleRate
        val a = dt / (rc + dt)
        a to 0.35
      }
      CrossfeedMode.JAN_MEIER -> {
        // Cutoff ~650 Hz, -6dB cross-blend
        val rc = 1.0 / (2.0 * Math.PI * 650.0)
        val dt = 1.0 / sampleRate
        val a = dt / (rc + dt)
        a to 0.25
      }
      CrossfeedMode.OFF -> 0.0 to 0.0
    }

    var maxL = 0
    var maxR = 0

    leftBands16.fill(0f)
    rightBands16.fill(0f)
    val framesPerBand = (frameCount / 16).coerceAtLeast(1)

    // 16-BIT PCM PROCESSING (Most common Android audio decoding output)
    if (channelCount == 2 && bytesPerSample == 2) {
      var frameIdx = 0
      while (inputBuffer.remaining() >= 4) {
        var left = inputBuffer.short.toInt()
        var right = inputBuffer.short.toInt()

        // 1. Channel Routing Mode
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

        // 2. Audio Phase Inversion (Crucial for headphone cable single-side wire correction)
        when (currentPhase) {
          AudioPhaseMode.NORMAL -> {}
          AudioPhaseMode.INVERT_LEFT_ONLY -> {
            left = -left
          }
          AudioPhaseMode.INVERT_RIGHT_ONLY -> {
            right = -right
          }
          AudioPhaseMode.INVERT_BOTH -> {
            left = -left
            right = -right
          }
          AudioPhaseMode.SWAP_CHANNELS -> {
            val tmp = left
            left = right
            right = tmp
          }
        }

        // 3. Binaural Crossfeed DSP
        if (isCrossfeedActive) {
          crossfeedStateL += cfAlpha * (left.toDouble() - crossfeedStateL)
          crossfeedStateR += cfAlpha * (right.toDouble() - crossfeedStateR)
          val processedL = (left.toDouble() + cfBlend * crossfeedStateR) / (1.0 + cfBlend)
          val processedR = (right.toDouble() + cfBlend * crossfeedStateL) / (1.0 + cfBlend)
          left = processedL.toInt()
          right = processedR.toInt()
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
    } else if (channelCount == 2 && bytesPerSample == 3) {
      // 24-BIT PCM PROCESSING
      var frameIdx = 0
      while (inputBuffer.remaining() >= 6) {
        val b0 = inputBuffer.get().toInt() and 0xFF
        val b1 = inputBuffer.get().toInt() and 0xFF
        val b2 = inputBuffer.get().toInt()
        var left = (b0) or (b1 shl 8) or (b2 shl 16) // Sign extended 24-bit

        val r0 = inputBuffer.get().toInt() and 0xFF
        val r1 = inputBuffer.get().toInt() and 0xFF
        val r2 = inputBuffer.get().toInt()
        var right = (r0) or (r1 shl 8) or (r2 shl 16)

        when (currentMode) {
          ChannelMode.STEREO -> {}
          ChannelMode.LEFT_ONLY -> right = 0
          ChannelMode.RIGHT_ONLY -> left = 0
          ChannelMode.MONO -> {
            val m = (left + right) / 2
            left = m
            right = m
          }
        }

        when (currentPhase) {
          AudioPhaseMode.NORMAL -> {}
          AudioPhaseMode.INVERT_LEFT_ONLY -> left = -left
          AudioPhaseMode.INVERT_RIGHT_ONLY -> right = -right
          AudioPhaseMode.INVERT_BOTH -> {
            left = -left
            right = -right
          }
          AudioPhaseMode.SWAP_CHANNELS -> {
            val t = left; left = right; right = t
          }
        }

        val clampedL = left.coerceIn(-8388608, 8388607)
        val clampedR = right.coerceIn(-8388608, 8388607)

        val absL = abs(clampedL shr 8)
        val absR = abs(clampedR shr 8)
        if (absL > maxL) maxL = absL
        if (absR > maxR) maxR = absR

        val bandIdx = (frameIdx / framesPerBand).coerceIn(0, 15)
        leftBands16[bandIdx] += absL
        rightBands16[bandIdx] += absR

        // Write 24-bit Little Endian
        buffer.put((clampedL and 0xFF).toByte())
        buffer.put(((clampedL shr 8) and 0xFF).toByte())
        buffer.put(((clampedL shr 16) and 0xFF).toByte())

        buffer.put((clampedR and 0xFF).toByte())
        buffer.put(((clampedR shr 8) and 0xFF).toByte())
        buffer.put(((clampedR shr 16) and 0xFF).toByte())
        frameIdx++
      }
    } else if (channelCount == 2 && bytesPerSample == 4 && inputFormat.encoding == C.ENCODING_PCM_FLOAT) {
      // 32-BIT IEEE FLOAT PROCESSING
      var frameIdx = 0
      while (inputBuffer.remaining() >= 8) {
        var left = inputBuffer.float
        var right = inputBuffer.float

        when (currentMode) {
          ChannelMode.STEREO -> {}
          ChannelMode.LEFT_ONLY -> right = 0f
          ChannelMode.RIGHT_ONLY -> left = 0f
          ChannelMode.MONO -> {
            val m = (left + right) / 2f
            left = m
            right = m
          }
        }

        when (currentPhase) {
          AudioPhaseMode.NORMAL -> {}
          AudioPhaseMode.INVERT_LEFT_ONLY -> left = -left
          AudioPhaseMode.INVERT_RIGHT_ONLY -> right = -right
          AudioPhaseMode.INVERT_BOTH -> {
            left = -left
            right = -right
          }
          AudioPhaseMode.SWAP_CHANNELS -> {
            val t = left; left = right; right = t
          }
        }

        if (isCrossfeedActive) {
          crossfeedStateL += cfAlpha * (left.toDouble() - crossfeedStateL)
          crossfeedStateR += cfAlpha * (right.toDouble() - crossfeedStateR)
          val processedL = (left.toDouble() + cfBlend * crossfeedStateR) / (1.0 + cfBlend)
          val processedR = (right.toDouble() + cfBlend * crossfeedStateL) / (1.0 + cfBlend)
          left = processedL.toFloat()
          right = processedR.toFloat()
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
    } else if (channelCount == 2 && bytesPerSample == 4 && inputFormat.encoding == C.ENCODING_PCM_32BIT) {
      // 32-BIT INTEGER PCM PROCESSING
      var frameIdx = 0
      while (inputBuffer.remaining() >= 8) {
        var left = inputBuffer.int
        var right = inputBuffer.int

        when (currentMode) {
          ChannelMode.STEREO -> {}
          ChannelMode.LEFT_ONLY -> right = 0
          ChannelMode.RIGHT_ONLY -> left = 0
          ChannelMode.MONO -> {
            val m = (left / 2) + (right / 2)
            left = m
            right = m
          }
        }

        when (currentPhase) {
          AudioPhaseMode.NORMAL -> {}
          AudioPhaseMode.INVERT_LEFT_ONLY -> left = -left
          AudioPhaseMode.INVERT_RIGHT_ONLY -> right = -right
          AudioPhaseMode.INVERT_BOTH -> {
            left = -left
            right = -right
          }
          AudioPhaseMode.SWAP_CHANNELS -> {
            val t = left; left = right; right = t
          }
        }

        val absL = abs(left shr 16)
        val absR = abs(right shr 16)
        if (absL > maxL) maxL = absL
        if (absR > maxR) maxR = absR

        val bandIdx = (frameIdx / framesPerBand).coerceIn(0, 15)
        leftBands16[bandIdx] += absL
        rightBands16[bandIdx] += absR

        buffer.putInt(left)
        buffer.putInt(right)
        frameIdx++
      }
    } else if (channelCount == 1 && bytesPerSample == 2) {
      // MONO SOURCE UPMIX TO STEREO WITH PHASE CONTROL
      var frameIdx = 0
      while (inputBuffer.remaining() >= 2) {
        var sample = inputBuffer.short.toInt()
        var lOut = sample
        var rOut = sample

        when (currentPhase) {
          AudioPhaseMode.INVERT_LEFT_ONLY, AudioPhaseMode.INVERT_BOTH -> lOut = -lOut
          AudioPhaseMode.INVERT_RIGHT_ONLY -> rOut = -rOut
          else -> {}
        }

        val clampedL = lOut.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        val clampedR = rOut.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

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
    } else {
      buffer.put(inputBuffer)
    }

    buffer.flip()

    // Real-time audio spectrum & peak metering notifications
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
