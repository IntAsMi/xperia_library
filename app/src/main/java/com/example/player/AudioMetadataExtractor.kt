package com.example.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.model.AudioFileItem
import java.io.File

object AudioMetadataExtractor {

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
        trackNum = numStr?.toIntOrNull() ?: 0
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

    // Heuristics for default bitrates and bit-depths if not reported by media retriever
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
      trackNumber = trackNum
    )
  }
}
