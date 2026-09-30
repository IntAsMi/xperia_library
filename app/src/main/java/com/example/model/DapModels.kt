package com.example.model

enum class DapThemeSetting(val displayName: String, val description: String) {
  DARK("Dark", "Technical studio dark slate aesthetic"),
  LIGHT("Light", "Clean high-contrast studio day theme"),
  SYSTEM("System Default", "Follows Android OS system theme"),
  MATERIAL_U("Material You Expressive", "Dynamic color extraction from system wallpaper"),
  AMOLED_BLACK("AMOLED Black + Material You", "Pure #000000 true pitch black with Material You accents")
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
  val trackNumber: Int = 0
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
  val folderTotalDurationMs: Long = 0L,
  val folderRemainingDurationMs: Long = 0L,
  val repeatMode: Int = 1, // 0 = off, 1 = repeat folder, 2 = repeat track
  val isShuffle: Boolean = false,
  val playbackSpeed: Float = 1.0f,
  val isGaplessActive: Boolean = true,
  val peakMeterLeft: Float = 0f,
  val peakMeterRight: Float = 0f,
  val loopPointA: Long? = null,
  val loopPointB: Long? = null
) {
  val trackRemainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)
  val formattedElapsed: String get() = formatDuration(positionMs)
  val formattedRemaining: String get() = "-${formatDuration(trackRemainingMs)}"
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
