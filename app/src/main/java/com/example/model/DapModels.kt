package com.example.model

enum class DapThemeSetting(val displayName: String, val description: String) {
  DARK("Dark", "Technical studio dark slate aesthetic"),
  LIGHT("Light", "Clean high-contrast studio day theme"),
  SYSTEM("System Default", "Follows Android OS system theme"),
  MATERIAL_U("Material You Expressive", "Dynamic color extraction from system wallpaper"),
  AMOLED_BLACK("AMOLED Black + Material You", "Pure #000000 true pitch black with Material You accents")
}

enum class ChannelMode(val displayName: String, val description: String) {
  STEREO("Stereo (L+R)", "Normal balanced stereo playback"),
  LEFT_ONLY("Left Only", "Left channel active, right channel muted"),
  RIGHT_ONLY("Right Only", "Right channel active, left channel muted"),
  MONO("Mono Downmix", "Summed (L+R)/2 sent to both channels")
}

enum class VisualizerChannelMode(val displayName: String) {
  STEREO("Stereo (L+R)"),
  LEFT_ONLY("Left Only (Hide Right)"),
  RIGHT_ONLY("Right Only (Hide Left)")
}

enum class DapFontSize(val displayName: String, val scaleFactor: Float) {
  SMALL("Small (DAP Compact)", 0.88f),
  MEDIUM("Standard", 1.0f),
  LARGE("Large", 1.15f),
  EXTRA_LARGE("Extra Large", 1.30f)
}

enum class SleepTimerOption(val displayName: String, val minutes: Int) {
  OFF("Off", 0),
  MIN_15("15 Minutes", 15),
  MIN_30("30 Minutes", 30),
  MIN_45("45 Minutes", 45),
  MIN_60("60 Minutes", 60),
  END_OF_TRACK("End of Current Track", -1),
  END_OF_DISK("End of Disk / Folder", -2)
}

enum class ScanningMode(val displayName: String, val description: String) {
  AUTOMATIC("Automatic", "Auto-index whenever files or folders change"),
  MANUAL("Manual", "User-initiated library indexing and caching")
}

data class AudioOutputDevice(
  val id: Int,
  val name: String,
  val typeName: String,
  val isConnected: Boolean = true,
  val isDefault: Boolean = false,
  val specsSummary: String = "48kHz / 16-bit"
)

data class AudioOutputSpec(
  val deviceName: String = "Built-In Speaker",
  val connectionType: String = "Internal",
  val sampleRateHz: Int = 48000,
  val bitDepth: Int = 16,
  val estimatedBitrateKbps: Int = 1536,
  val isHiRes: Boolean = false,
  val technology: String = "PCM Direct AudioTrack",
  val latencyDescription: String = "Low Latency (Output Buffer 128 frames)"
) {
  val formattedOutputSummary: String
    get() = "$deviceName • $technology • ${sampleRateHz / 1000f}kHz / $bitDepth-bit (${estimatedBitrateKbps}kbps)"

  companion object {
    val DEFAULT = AudioOutputSpec(
      deviceName = "Built-In Speaker",
      connectionType = "Built-in",
      sampleRateHz = 48000,
      bitDepth = 16,
      estimatedBitrateKbps = 1536,
      isHiRes = false,
      technology = "Stereo Loudspeaker (48kHz DAC)"
    )
  }
}

data class AudioFileItem(
  val id: String,
  val uriString: String,
  val title: String,
  val fileName: String,
  val extension: String,
  val filePath: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val sampleRate: Int = 44100,
  val bitDepth: Int = 16,
  val bitrateKbps: Int = 1411,
  val channels: Int = 2,
  val codec: String = "FLAC",
  val trackNumber: Int = 0,
  val diskNumber: Int = 1,
  val waveformLeft: List<Float> = emptyList(),
  val waveformRight: List<Float> = emptyList(),
  val waveform: List<Float> = emptyList()
) {
  val formattedDuration: String get() = formatDuration(durationMs)
  val formattedSize: String get() = formatFileSize(sizeBytes)
  val formattedSpecs: String get() = "$codec | ${sampleRate / 1000f} kHz | ${bitDepth}-bit | ${bitrateKbps} kbps | ${if (channels == 2) "Stereo" else if (channels == 1) "Mono" else "$channels Ch"}"
}

data class FolderItem(
  val uriString: String,
  val name: String,
  val path: String,
  val fileCount: Int,
  val subfolderCount: Int,
  val totalDurationMs: Long = 0L,
  val isVirtual: Boolean = false
) {
  val formattedDuration: String get() = formatDuration(totalDurationMs)
}

data class DapPlayerState(
  val currentTrack: AudioFileItem? = null,
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val currentFolderTracks: List<AudioFileItem> = emptyList(),
  val currentTrackIndex: Int = -1,
  val currentDiskNumber: Int = 1,
  val diskTotalDurationMs: Long = 0L,
  val diskRemainingDurationMs: Long = 0L,
  val folderTotalDurationMs: Long = 0L,
  val folderRemainingDurationMs: Long = 0L,
  val repeatMode: Int = 1, // 0 = off, 1 = repeat folder, 2 = repeat track
  val isShuffle: Boolean = false,
  val playbackSpeed: Float = 1.0f,
  val isGaplessActive: Boolean = true,
  val peakMeterLeft: Float = 0f,
  val peakMeterRight: Float = 0f,
  val spectrumBands: List<Float> = List(32) { 0f },
  val spectrumBandsLeft: List<Float> = List(32) { 0f },
  val spectrumBandsRight: List<Float> = List(32) { 0f },
  val loopPointA: Long? = null,
  val loopPointB: Long? = null,
  val audioPhaseInverted: Boolean = false,
  val channelMode: ChannelMode = ChannelMode.STEREO,
  val visualizerChannelMode: VisualizerChannelMode = VisualizerChannelMode.STEREO,
  val audioOutputSpec: AudioOutputSpec = AudioOutputSpec.DEFAULT,
  val availableOutputs: List<AudioOutputDevice> = emptyList(),
  val selectedOutputDeviceId: Int? = null,
  val isBufferingLargeFile: Boolean = false,
  val bufferProgress: Float = 1.0f,
  val sleepTimerRemainingSeconds: Int? = null
) {
  val trackRemainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)
  val formattedElapsed: String get() = formatDuration(positionMs)
  val formattedRemaining: String get() = "-${formatDuration(trackRemainingMs)}"
  val formattedDiskTotal: String get() = formatDuration(diskTotalDurationMs)
  val formattedDiskRemaining: String get() = "-${formatDuration(diskRemainingDurationMs)}"
  val formattedFolderTotal: String get() = formatDuration(folderTotalDurationMs)
  val formattedFolderRemaining: String get() = "-${formatDuration(folderRemainingDurationMs)}"
}

fun formatDuration(ms: Long): String {
  if (ms <= 0L) return "00:00"
  val totalSeconds = ms / 1000
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    String.format("%02d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format("%02d:%02d", minutes, seconds)
  }
}

fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 MB"
  val mb = bytes.toDouble() / (1024.0 * 1024.0)
  return if (mb >= 1.0) {
    String.format("%.1f MB", mb)
  } else {
    val kb = bytes / 1024
    "$kb KB"
  }
}
