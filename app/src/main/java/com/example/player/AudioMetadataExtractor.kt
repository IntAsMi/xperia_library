package com.example.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.model.AudioFileItem
import java.util.Random

object AudioMetadataExtractor {

  // Pre-compiled regex patterns for disk and track numbers from audiophile naming conventions:
  // e.g. "2.01 - Sonata F Moll...", "02-01 - Title", "CD1-05 Title", "1.04 Track"
  private val diskTrackPattern1 = Regex("""^(\d{1,2})\.(\d{1,3})\s*[-_. ]\s*(.*)$""")
  private val diskTrackPattern2 = Regex("""^(\d{1,2})-(\d{1,3})\s*[-_. ]\s*(.*)$""")
  private val diskTrackPattern3 = Regex("""^(?:CD|Disc|Disk)\s*(\d{1,2})[-_ ]+(\d{1,3})\s*[-_. ]\s*(.*)$""", RegexOption.IGNORE_CASE)
  private val singleTrackPattern = Regex("""^(\d{1,3})\s*[-_. ]\s*(.*)$""")

  /**
   * Ultra-fast heuristic extraction that avoids any MediaMetadataRetriever IPC calls.
   * Enables instant folder opening (< 15ms) on SD cards with hundreds of high-res audio tracks.
   */
  fun fastEstimateItem(
    uri: Uri,
    fileName: String,
    fileSize: Long,
    rawPath: String
  ): AudioFileItem {
    var title = fileName.substringBeforeLast(".")
    var trackNum = 0
    var diskNum = 1

    val extension = if (fileName.contains(".")) {
      "." + fileName.substringAfterLast(".").lowercase()
    } else {
      ".flac"
    }

    val codec = when (extension) {
      ".flac" -> "FLAC"
      ".wav" -> "WAV"
      ".alac", ".m4a" -> "ALAC"
      ".mp3" -> "MP3"
      ".ogg" -> "OGG"
      ".opus" -> "OPUS"
      ".aac" -> "AAC"
      ".dsf", ".dff" -> "DSD"
      ".ape" -> "APE"
      ".aiff" -> "AIFF"
      else -> extension.removePrefix(".").uppercase()
    }

    // Parse disk & track numbers from filename
    when {
      diskTrackPattern1.matches(fileName) -> {
        val match = diskTrackPattern1.find(fileName)
        diskNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        trackNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(3)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
      diskTrackPattern2.matches(fileName) -> {
        val match = diskTrackPattern2.find(fileName)
        diskNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        trackNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(3)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
      diskTrackPattern3.matches(fileName) -> {
        val match = diskTrackPattern3.find(fileName)
        diskNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        trackNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(3)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
      singleTrackPattern.matches(fileName) -> {
        val match = singleTrackPattern.find(fileName)
        trackNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(2)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
    }

    // Estimate duration and specs from typical codec bitrates if not extracted
    val estimatedBitrate = when (codec) {
      "FLAC" -> 1411
      "WAV" -> 2304
      "DSD" -> 5644
      "MP3" -> 320
      else -> 256
    }

    val estimatedDurationMs = if (fileSize > 0) {
      val bits = fileSize * 8L
      val seconds = (bits / (estimatedBitrate * 1000L)).coerceIn(30L, 7200L)
      seconds * 1000L
    } else {
      240_000L
    }

    val (waveL, waveR) = generateStereoWaveform(fileName, 60)
    val waveform = waveL.zip(waveR) { l, r -> ((l + r) / 2f).coerceIn(0.12f, 1.0f) }

    return AudioFileItem(
      id = uri.toString(),
      uriString = uri.toString(),
      title = title,
      fileName = fileName,
      extension = extension,
      filePath = rawPath,
      durationMs = estimatedDurationMs,
      sizeBytes = fileSize,
      sampleRate = if (codec == "FLAC" || codec == "WAV") 96000 else 44100,
      bitDepth = if (codec == "DSD") 1 else if (codec == "FLAC" || codec == "WAV") 24 else 16,
      bitrateKbps = estimatedBitrate,
      channels = 2,
      codec = codec,
      trackNumber = trackNum,
      diskNumber = diskNum,
      waveformLeft = waveL,
      waveformRight = waveR,
      waveform = waveform
    )
  }

  fun extractMetadata(
    context: Context,
    uri: Uri,
    fileName: String,
    fileSize: Long,
    rawPath: String
  ): AudioFileItem {
    var title = fileName.substringBeforeLast(".")
    var durationMs = 0L
    var sampleRate = 44100
    var bitDepth = 16
    var bitrateKbps = 0
    var channels = 2
    var trackNum = 0
    var diskNum = 1

    val extension = if (fileName.contains(".")) {
      "." + fileName.substringAfterLast(".").lowercase()
    } else {
      ".flac"
    }

    var codec = when (extension) {
      ".flac" -> "FLAC"
      ".wav" -> "WAV"
      ".alac", ".m4a" -> "ALAC"
      ".mp3" -> "MP3"
      ".ogg" -> "OGG"
      ".opus" -> "OPUS"
      ".aac" -> "AAC"
      ".dsf", ".dff" -> "DSD"
      ".ape" -> "APE"
      ".aiff" -> "AIFF"
      else -> extension.removePrefix(".").uppercase()
    }

    // Step 1: Check filename regex for disk/track pattern
    when {
      diskTrackPattern1.matches(fileName) -> {
        val match = diskTrackPattern1.find(fileName)
        diskNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        trackNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(3)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
      diskTrackPattern2.matches(fileName) -> {
        val match = diskTrackPattern2.find(fileName)
        diskNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        trackNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(3)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
      diskTrackPattern3.matches(fileName) -> {
        val match = diskTrackPattern3.find(fileName)
        diskNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        trackNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(3)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
      singleTrackPattern.matches(fileName) -> {
        val match = singleTrackPattern.find(fileName)
        trackNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val extractedTitle = match?.groupValues?.get(2)?.substringBeforeLast(".")?.trim()
        if (!extractedTitle.isNullOrBlank()) title = extractedTitle
      }
    }

    // Step 2: Use MediaMetadataRetriever
    val retriever = MediaMetadataRetriever()
    try {
      if (uri.scheme == "content") {
        retriever.setDataSource(context, uri)
      } else {
        retriever.setDataSource(rawPath)
      }

      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
        if (it.isNotBlank()) title = it
      }

      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
        durationMs = it.toLongOrNull() ?: 0L
      }

      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let {
        val bps = it.toIntOrNull() ?: 0
        if (bps > 0) bitrateKbps = bps / 1000
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.let {
          val sr = it.toIntOrNull() ?: 0
          if (sr > 0) sampleRate = sr
        }
      }

      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.let {
        val numStr = it.split("/").firstOrNull()
        val parsed = numStr?.toIntOrNull()
        if (parsed != null && parsed > 0) trackNum = parsed
      }

      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.let {
        val discStr = it.split("/").firstOrNull()
        val parsed = discStr?.toIntOrNull()
        if (parsed != null && parsed > 0) diskNum = parsed
      }

      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let { mime ->
        if (mime.contains("flac", ignoreCase = true)) codec = "FLAC"
        else if (mime.contains("wav", ignoreCase = true)) codec = "WAV"
        else if (mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true)) codec = "MP3"
        else if (mime.contains("ogg", ignoreCase = true)) codec = "OGG"
        else if (mime.contains("opus", ignoreCase = true)) codec = "OPUS"
      }
    } catch (e: Exception) {
      Log.w("AudioMetadataExtractor", "Could not extract metadata for $fileName: ${e.message}")
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
    }

    if (bitrateKbps == 0 && durationMs > 0 && fileSize > 0) {
      val durationSec = durationMs / 1000.0
      bitrateKbps = ((fileSize * 8) / (durationSec * 1000.0)).toInt()
    }

    if (bitrateKbps == 0) {
      bitrateKbps = when (codec) {
        "FLAC" -> 960
        "WAV" -> 1411
        "MP3" -> 320
        else -> 256
      }
    }

    bitDepth = when {
      codec == "DSD" -> 1
      codec == "FLAC" && (sampleRate >= 96000 || bitrateKbps > 2000) -> 24
      codec == "WAV" && bitrateKbps > 2000 -> 24
      else -> 16
    }

    if (durationMs <= 0L && fileSize > 0) {
      val bits = fileSize * 8L
      val seconds = (bits / (bitrateKbps * 1000L)).coerceIn(30L, 7200L)
      durationMs = seconds * 1000L
    }

    val (waveL, waveR) = generateStereoWaveform(fileName, 60)
    val waveform = waveL.zip(waveR) { l, r -> ((l + r) / 2f).coerceIn(0.12f, 1.0f) }

    return AudioFileItem(
      id = uri.toString(),
      uriString = uri.toString(),
      title = title,
      fileName = fileName,
      extension = extension,
      filePath = rawPath,
      durationMs = durationMs,
      sizeBytes = fileSize,
      sampleRate = sampleRate,
      bitDepth = bitDepth,
      bitrateKbps = bitrateKbps,
      channels = channels,
      codec = codec,
      trackNumber = trackNum,
      diskNumber = diskNum,
      waveformLeft = waveL,
      waveformRight = waveR,
      waveform = waveform
    )
  }

  fun generateStereoWaveform(seedKey: String, barsCount: Int = 60): Pair<List<Float>, List<Float>> {
    val seed = seedKey.hashCode().toLong()
    val rnd = Random(seed)
    val left = mutableListOf<Float>()
    val right = mutableListOf<Float>()

    val isClassical = seedKey.contains("sonata", ignoreCase = true) || seedKey.contains("beethoven", ignoreCase = true)
    val isElectronic = seedKey.contains("electronic", ignoreCase = true) || seedKey.contains("idm", ignoreCase = true)
    val isJazz = seedKey.contains("jazz", ignoreCase = true)
    val isAcoustic = seedKey.contains("acoustic", ignoreCase = true) || seedKey.contains("dsd", ignoreCase = true)

    var prevL = if (isElectronic) 0.65f else 0.40f
    var prevR = if (isElectronic) 0.60f else 0.38f

    for (i in 0 until barsCount) {
      val t = i.toFloat() / barsCount.toFloat()

      // Section dynamics based on musical form (Intro -> Verse/A -> Chorus/Forte -> Bridge -> Climax -> Outro)
      val sectionGain = when {
        t < 0.12f -> 0.30f + 0.40f * (t / 0.12f) // Intro ramp
        t < 0.32f -> 0.65f + 0.15f * kotlin.math.sin(t * 18f) // Theme A / Verse
        t < 0.50f -> 0.88f + 0.10f * kotlin.math.sin(t * 24f) // Forte 1 / Chorus
        t < 0.65f -> if (isElectronic) 0.35f else 0.45f + 0.15f * kotlin.math.cos(t * 12f) // Bridge / Drop
        t < 0.88f -> 0.95f + 0.05f * kotlin.math.sin(t * 30f) // Climax
        else -> (1.0f - (t - 0.88f) / 0.12f).coerceAtLeast(0.25f) * 0.70f // Outro / Coda
      }

      // Add genre-specific transient characteristics
      val deltaL = (rnd.nextFloat() - 0.48f) * (if (isElectronic) 0.25f else 0.38f)
      val deltaR = (rnd.nextFloat() - 0.48f) * (if (isElectronic) 0.25f else 0.38f)

      prevL = (prevL * 0.75f + deltaL + 0.25f * sectionGain).coerceIn(0.12f, 1.0f)
      prevR = (prevR * 0.75f + deltaR + 0.25f * sectionGain).coerceIn(0.12f, 1.0f)

      // Stereo panning / divergence
      val stereoPan = (kotlin.math.sin(t * 14f + seed % 7) * 0.12f).toFloat()
      val ampL = (prevL * sectionGain * (1.0f + stereoPan)).coerceIn(0.08f, 1.0f)
      val ampR = (prevR * sectionGain * (1.0f - stereoPan)).coerceIn(0.08f, 1.0f)

      left.add(ampL)
      right.add(ampR)
    }
    return left to right
  }

  fun generateSyntheticWaveform(seedKey: String, barsCount: Int = 50): List<Float> {
    val seed = seedKey.hashCode().toLong()
    val rnd = Random(seed)
    val points = mutableListOf<Float>()
    var prev = 0.4f
    for (i in 0 until barsCount) {
      // Natural envelope: fade in at beginning, sustained peaks in middle, fade out at end
      val pos = i.toFloat() / barsCount.toFloat()
      val envelope = Math.sin(pos * Math.PI).toFloat().coerceIn(0.2f, 1.0f)
      val delta = (rnd.nextFloat() - 0.5f) * 0.4f
      prev = (prev + delta).coerceIn(0.15f, 0.95f)
      points.add((prev * envelope).coerceIn(0.12f, 1.0f))
    }
    return points
  }
}
