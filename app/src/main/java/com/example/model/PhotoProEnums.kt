package com.example.model

enum class DeviceProfile(val modelName: String, val chipName: String, val tagline: String) {
  XPERIA_1_VIII("Xperia 1 VIII", "Snapdragon 8 Gen 5 + Sony AI Processing Unit", "Next-Gen Optical & AI Camera Engine"),
  XPERIA_1_V("Xperia 1 V", "Snapdragon 8 Gen 2", "Classic 2023 Photography Pro Heritage")
}

enum class ShootingMode(val label: String, val description: String) {
  BASIC("BASIC", "Streamlined mobile shooting with quick bokeh and color controls"),
  AUTO("AUTO", "Alpha intelligent auto with scene detection and Auto HDR"),
  P("P", "Program Auto: Camera sets shutter & aperture, you control ISO, EV, WB"),
  S("S", "Shutter Priority: Manual shutter speed with auto/manual exposure"),
  M("M", "Manual Exposure: Full manual shutter speed, ISO, and manual focus"),
  MR("MR", "Memory Recall: Quickly recall custom saved shooting profiles")
}

enum class LensOption(
  val focalLength: String,
  val aperture: String,
  val sensorName: String,
  val baseZoom: Float,
  val minZoom: Float,
  val maxZoom: Float,
  val isOpticalZoomModule: Boolean = false,
  val isTeleMacroCapable: Boolean = false
) {
  // Xperia 1 VIII Optics
  ULTRA_WIDE_16MM("16mm", "F2.0", "1/2.0\" Exmor T for mobile", 0.6f, 0.6f, 0.9f),
  WIDE_24MM("24mm", "F1.8", "1/1.28\" Exmor T 48MP dual-layer", 1.0f, 1.0f, 1.9f),
  WIDE_48MM("48mm", "F1.8", "2x Lossless Sensor Crop", 2.0f, 2.0f, 3.4f),
  TELE_85_170MM("85-170mm", "F2.3-F3.5", "Continuous Optical Periscope Zoom", 3.5f, 3.5f, 21.3f, true, true),

  // Xperia 1 V Heritage Optics (Legacy Profile)
  CLASSIC_16MM("16mm", "F2.2", "1/2.5\" Exmor RS", 0.6f, 0.6f, 0.9f),
  CLASSIC_24MM("24mm", "F1.9", "1/1.35\" Exmor T for mobile", 1.0f, 1.0f, 1.9f),
  CLASSIC_48MM("48mm", "F1.9", "2x Lossless Sensor Crop", 2.0f, 2.0f, 3.4f),
  CLASSIC_85_125MM("85-125mm", "F2.3-F2.8", "True Continuous Optical Zoom", 3.5f, 3.5f, 15.6f, true, false)
}

enum class AiSubjectTracking(val label: String, val shortName: String) {
  OFF("Standard AF Array", "AF-STD"),
  HUMAN("AI Human (Eye/Body Pose)", "AI-HUMAN"),
  ANIMAL_BIRD("AI Animal / Bird Eye AF", "AI-ANIMAL"),
  VEHICLE("AI Vehicle / Aircraft", "AI-VEHICLE")
}

enum class DriveMode(val label: String, val symbol: String, val fps: Int = 1) {
  SINGLE("Single Shooting", "1S", 1),
  BURST_ULTRA("Continuous Shooting: Ultra (60fps)", "60", 60),
  BURST_HI("Continuous Shooting: Hi (30fps)", "Hi", 30),
  BURST_LO("Continuous Shooting: Lo (10fps)", "Lo", 10),
  TIMER_3S("Self-timer: 3 sec", "3s", 0),
  TIMER_10S("Self-timer: 10 sec", "10s", 0)
}

enum class FocusMode(val label: String, val shortName: String) {
  AF_S("Single-shot AF", "AF-S"),
  AF_C("Continuous AF", "AF-C"),
  MF("Manual Focus", "MF")
}

enum class FocusArea(val label: String, val shortName: String) {
  WIDE("Wide (399-point)", "WIDE"),
  CENTER("Center Fix", "CENTER"),
  TRACKING("Real-time Eye/Object Tracking", "TRACK")
}

enum class MeteringMode(val label: String, val shortName: String) {
  MULTI("Multi-pattern", "MULTI"),
  CENTER("Center-weighted", "CENTER"),
  SPOT("Spot Metering", "SPOT")
}

enum class FlashMode(val label: String, val shortName: String) {
  OFF("Flash Off", "OFF"),
  AUTO("Auto Flash", "AUTO"),
  FILL("Fill-flash", "FILL"),
  RED_EYE("Red-eye Reduction", "RED-EYE"),
  TORCH("Continuous Torch", "TORCH")
}

enum class AspectRatio(val label: String, val ratio: Float) {
  RATIO_4_3("4:3", 4f / 3f),
  RATIO_16_9("16:9", 16f / 9f),
  RATIO_1_1("1:1", 1f),
  RATIO_21_9("21:9", 21f / 9f) // Sony CinemaWide aspect ratio
}

enum class FileFormat(val label: String, val extension: String) {
  JPEG("JPEG", "JPG"),
  RAW("RAW", "DNG"),
  RAW_JPEG("RAW + JPEG", "DNG+JPG")
}

enum class GridType(val label: String) {
  NONE("Off"),
  RULE_OF_THIRDS("Rule of Thirds"),
  SQUARE("Square Grid"),
  GOLDEN_RATIO("Golden Ratio")
}

enum class CreativeLook(
  val code: String,
  val title: String,
  val description: String,
  val contrast: Float = 0f,
  val saturation: Float = 0f,
  val warmth: Float = 0f,
  val fade: Float = 0f
) {
  ST("ST", "Standard", "Standard Sony Alpha look with balanced colors and contrast", 0f, 0f, 0f, 0f),
  NT("NT", "Neutral", "Low saturation and contrast for soft tones, perfect for editing", -0.2f, -0.25f, 0f, 0.1f),
  VV("VV", "Vivid", "High clarity and vivid saturation for vibrant landscapes and flowers", 0.3f, 0.4f, 0.05f, 0f),
  FL("FL", "Film", "Matte film look with deep blue/green cast and lifted blacks", 0.1f, -0.1f, -0.1f, 0.25f),
  IN("IN", "Instant", "Instant camera aesthetic with soft highlights and vintage fade", -0.1f, -0.15f, 0.15f, 0.35f),
  SH("SH", "Soft High-key", "Bright, transparent look with soft contrast for flattering portraits", -0.25f, 0.05f, 0.05f, 0.2f),
  BW("BW", "Black & White", "Classic monochrome with rich microcontrast and tonal gradations", 0.35f, -1.0f, 0f, 0f),
  SE("SE", "Sepia", "Warm nostalgic amber sepia monochrome with vintage warmth", 0.2f, -0.8f, 0.6f, 0.1f)
}
