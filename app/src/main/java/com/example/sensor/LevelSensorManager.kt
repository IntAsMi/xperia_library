package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class LevelState(
  val pitch: Float = 0f, // Degrees (-90 to +90)
  val roll: Float = 0f,  // Degrees (-180 to +180)
  val isPitchLevel: Boolean = false,
  val isRollLevel: Boolean = false,
  val isLevel: Boolean = false
)

class LevelSensorManager(context: Context) : SensorEventListener {
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private val _levelState = MutableStateFlow(LevelState())
  val levelState: StateFlow<LevelState> = _levelState.asStateFlow()

  private var isListening = false
  private val rotationMatrix = FloatArray(9)
  private val orientationAngles = FloatArray(3)

  // Low pass filter smoothing factor
  private var smoothedPitch = 0f
  private var smoothedRoll = 0f
  private val alpha = 0.2f

  fun start() {
    if (isListening) return
    isListening = true
    if (rotationSensor != null) {
      sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    } else if (accelSensor != null) {
      sensorManager?.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
    }
  }

  fun stop() {
    if (!isListening) return
    isListening = false
    sensorManager?.unregisterListener(this)
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null) return

    var currentPitch = 0f
    var currentRoll = 0f

    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
      SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
      SensorManager.getOrientation(rotationMatrix, orientationAngles)
      // orientationAngles: [0] = azimuth, [1] = pitch, [2] = roll (all in radians)
      currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
      currentRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
    } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
      val ax = event.values[0]
      val ay = event.values[1]
      val az = event.values[2]

      currentRoll = Math.toDegrees(atan2(ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
      currentPitch = Math.toDegrees(atan2(-ay.toDouble(), az.toDouble())).toFloat()
    }

    // Apply smoothing filter
    smoothedPitch += alpha * (currentPitch - smoothedPitch)
    smoothedRoll += alpha * (currentRoll - smoothedRoll)

    val isPitchLevel = abs(smoothedPitch) < 1.2f
    val isRollLevel = abs(smoothedRoll) < 1.2f
    val isLevel = isPitchLevel && isRollLevel

    _levelState.value = LevelState(
      pitch = smoothedPitch,
      roll = smoothedRoll,
      isPitchLevel = isPitchLevel,
      isRollLevel = isRollLevel,
      isLevel = isLevel
    )
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // No-op
  }
}
