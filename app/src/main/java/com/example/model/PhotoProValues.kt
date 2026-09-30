package com.example.model

object PhotoProValues {
  val SHUTTER_SPEEDS = listOf(
    "1/8000", "1/4000", "1/2000", "1/1000", "1/500",
    "1/250", "1/125", "1/60", "1/30", "1/15",
    "1/8", "1/4", "1/2", "1\"", "2\"", "4\"", "8\"", "15\"", "30\""
  )

  val ISO_VALUES = listOf(
    "AUTO", "50", "64", "100", "125", "160", "200", "250", "320",
    "400", "500", "640", "800", "1000", "1250", "1600", "2000",
    "2500", "3200", "6400", "12800"
  )

  val EV_COMP_VALUES = listOf(
    "-3.0", "-2.7", "-2.3", "-2.0", "-1.7", "-1.3", "-1.0", "-0.7", "-0.3",
    "0.0",
    "+0.3", "+0.7", "+1.0", "+1.3", "+1.7", "+2.0", "+2.3", "+2.7", "+3.0"
  )

  val WHITE_BALANCE_PRESETS = listOf(
    "AWB" to "Auto White Balance",
    "DAYLIGHT" to "Daylight (5500K)",
    "CLOUDY" to "Cloudy (6500K)",
    "SHADE" to "Shade (7500K)",
    "INCANDESCENT" to "Incandescent (3200K)",
    "FLUORESCENT" to "Fluorescent (4000K)",
    "FLASH" to "Flash (5500K)",
    "CUSTOM_K" to "Custom Color Temp (K)"
  )
}

data class FocusPoint(
  val x: Float, // Normalized 0..1 in viewfinder
  val y: Float, // Normalized 0..1 in viewfinder
  val isLocked: Boolean = false,
  val isEyeAf: Boolean = false
)
