package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundFeedback(private val context: Context) {
  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  private val scope = CoroutineScope(Dispatchers.Default)

  var soundEnabled: Boolean = true
  var hapticsEnabled: Boolean = true

  /**
   * Plays the signature Sony Alpha high-frequency dual focus confirmation beep (1750Hz).
   */
  fun playAfLockSound() {
    if (!soundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          ToneNote(frequency = 1750, durationMs = 45),
          ToneNote(frequency = 0, durationMs = 25), // silence
          ToneNote(frequency = 1750, durationMs = 55)
        )
      )
    }
    vibrateClick(isHeavy = false)
  }

  /**
   * Synthesizes the mechanical curtain sound of a Sony Alpha camera shutter release.
   */
  fun playShutterSound() {
    if (!soundEnabled) return
    scope.launch {
      // Simulate two mechanical clicks: mirror/curtain opening and closing
      playMechanicalClick()
    }
    vibrateClick(isHeavy = true)
  }

  /**
   * Plays a short beep for self-timer countdown.
   */
  fun playTimerBeep(isFinal: Boolean = false) {
    if (!soundEnabled) return
    scope.launch {
      val freq = if (isFinal) 2000 else 1400
      val dur = if (isFinal) 120 else 50
      playToneSequence(listOf(ToneNote(frequency = freq, durationMs = dur)))
    }
    vibrateClick(isHeavy = isFinal)
  }

  /**
   * Gentle haptic tick when rotating dials or switching values.
   */
  fun vibrateDialTick() {
    if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(10)
      }
    } catch (_: Exception) { }
  }

  fun vibrateClick(isHeavy: Boolean) {
    if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val effect = if (isHeavy) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK
        vibrator.vibrate(VibrationEffect.createPredefined(effect))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(if (isHeavy) 45 else 20)
      }
    } catch (_: Exception) { }
  }

  private data class ToneNote(val frequency: Int, val durationMs: Int)

  private fun playToneSequence(notes: List<ToneNote>) {
    try {
      val sampleRate = 44100
      var totalSamples = 0
      for (n in notes) {
        totalSamples += (sampleRate * n.durationMs) / 1000
      }
      if (totalSamples <= 0) return

      val buffer = ShortArray(totalSamples)
      var offset = 0

      for (note in notes) {
        val count = (sampleRate * note.durationMs) / 1000
        if (note.frequency > 0) {
          val angularFreq = 2.0 * Math.PI * note.frequency / sampleRate
          for (i in 0 until count) {
            // Apply slight envelope smoothing to prevent pops
            val attack = (i.toFloat() / (count * 0.1f)).coerceIn(0f, 1f)
            val decay = ((count - i).toFloat() / (count * 0.2f)).coerceIn(0f, 1f)
            val envelope = attack * decay
            val sample = (sin(i * angularFreq) * Short.MAX_VALUE * 0.6f * envelope).toInt()
            buffer[offset + i] = sample.toShort()
          }
        } else {
          // Silence
          for (i in 0 until count) {
            buffer[offset + i] = 0
          }
        }
        offset += count
      }

      val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(buffer.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      audioTrack.write(buffer, 0, buffer.size)
      audioTrack.play()
      // Release after playing
      scope.launch {
        kotlinx.coroutines.delay((totalSamples * 1000L / sampleRate) + 50)
        audioTrack.release()
      }
    } catch (_: Exception) { }
  }

  private fun playMechanicalClick() {
    try {
      val sampleRate = 44100
      val durationMs = 85
      val totalSamples = (sampleRate * durationMs) / 1000
      val buffer = ShortArray(totalSamples)

      // Generate realistic camera mechanical noise burst with fast decay
      val random = java.util.Random(42)
      for (i in 0 until totalSamples) {
        val t = i.toFloat() / totalSamples
        val env = (1f - t) * (1f - t)
        val noise = (random.nextFloat() * 2f - 1f)
        val lowThump = sin(2.0 * Math.PI * 180.0 * i / sampleRate).toFloat()
        val highClick = sin(2.0 * Math.PI * 2200.0 * i / sampleRate).toFloat()
        val mixed = (noise * 0.5f + lowThump * 0.3f + highClick * 0.2f) * env * Short.MAX_VALUE * 0.8f
        buffer[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }

      val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(buffer.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      audioTrack.write(buffer, 0, buffer.size)
      audioTrack.play()
      scope.launch {
        kotlinx.coroutines.delay(durationMs.toLong() + 50)
        audioTrack.release()
      }
    } catch (_: Exception) { }
  }
}
