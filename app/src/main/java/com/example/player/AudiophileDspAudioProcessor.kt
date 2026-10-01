package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.ChannelMode
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class AudiophileDspAudioProcessor : BaseAudioProcessor() {

  @Volatile
  var channelMode: ChannelMode = ChannelMode.STEREO

  @Volatile
  var isPhaseInverted: Boolean = false

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
      throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
    }
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

    if (channelCount == 2) {
      while (inputBuffer.remaining() >= 4) {
        var left = inputBuffer.short.toInt()
        var right = inputBuffer.short.toInt()

        // Apply Channel Mode
        when (currentMode) {
          ChannelMode.STEREO -> {
            // Keep left & right distinct
          }
          ChannelMode.LEFT_ONLY -> {
            // Solo Left channel
            right = 0
          }
          ChannelMode.RIGHT_ONLY -> {
            // Solo Right channel
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

        buffer.putShort(left.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        buffer.putShort(right.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
      }
    } else if (channelCount == 1) {
      while (inputBuffer.remaining() >= 2) {
        var sample = inputBuffer.short.toInt()
        if (invertPhase) {
          sample = -sample
        }
        val clamped = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        when (currentMode) {
          ChannelMode.LEFT_ONLY -> {
            buffer.putShort(clamped)
            buffer.putShort(0)
          }
          ChannelMode.RIGHT_ONLY -> {
            buffer.putShort(0)
            buffer.putShort(clamped)
          }
          else -> {
            buffer.putShort(clamped)
            buffer.putShort(clamped)
          }
        }
      }
    } else {
      // Passthrough any unsupported channel count
      buffer.put(inputBuffer)
    }

    buffer.flip()
  }
}
