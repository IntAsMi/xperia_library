package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_photos")
data class PhotoEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val filePath: String,
  val timestamp: Long = System.currentTimeMillis(),
  val lensFocalLength: String = "24mm",
  val aperture: String = "F1.9",
  val shutterSpeed: String = "1/250",
  val iso: String = "100",
  val ev: String = "0.0",
  val creativeLook: String = "ST",
  val format: String = "JPEG",
  val isSample: Boolean = false,
  val sampleDrawableRes: Int = 0
)

@Entity(tableName = "shooting_presets")
data class PresetEntity(
  @PrimaryKey
  val slotKey: String, // "M1", "M2", "M3"
  val name: String,
  val mode: String,
  val lens: String,
  val shutterSpeed: String,
  val iso: String,
  val ev: String,
  val whiteBalance: String,
  val focusMode: String,
  val creativeLook: String,
  val driveMode: String
)
