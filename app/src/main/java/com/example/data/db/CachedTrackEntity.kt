package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AudioFileItem

@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
  @PrimaryKey val uriString: String,
  val parentFolderUri: String,
  val fileName: String,
  val title: String,
  val extension: String,
  val filePath: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val sampleRate: Int,
  val bitDepth: Int,
  val bitrateKbps: Int,
  val channels: Int,
  val codec: String,
  val trackNumber: Int,
  val diskNumber: Int,
  val waveformCsv: String,
  val lastScanned: Long = System.currentTimeMillis()
) {
  fun toAudioFileItem(): AudioFileItem {
    val waveformPoints = if (waveformCsv.isBlank()) {
      emptyList()
    } else {
      waveformCsv.split(",").mapNotNull { it.toFloatOrNull() }
    }

    return AudioFileItem(
      id = uriString,
      uriString = uriString,
      title = title,
      fileName = fileName,
      extension = extension,
      filePath = filePath,
      durationMs = durationMs,
      sizeBytes = sizeBytes,
      sampleRate = sampleRate,
      bitDepth = bitDepth,
      bitrateKbps = bitrateKbps,
      channels = channels,
      codec = codec,
      trackNumber = trackNumber,
      diskNumber = diskNumber,
      waveform = waveformPoints
    )
  }

  companion object {
    fun fromAudioFileItem(item: AudioFileItem, parentFolderUri: String): CachedTrackEntity {
      val waveformCsv = item.waveform.joinToString(",") { String.format(java.util.Locale.US, "%.2f", it) }
      return CachedTrackEntity(
        uriString = item.uriString,
        parentFolderUri = parentFolderUri,
        fileName = item.fileName,
        title = item.title,
        extension = item.extension,
        filePath = item.filePath,
        durationMs = item.durationMs,
        sizeBytes = item.sizeBytes,
        sampleRate = item.sampleRate,
        bitDepth = item.bitDepth,
        bitrateKbps = item.bitrateKbps,
        channels = item.channels,
        codec = item.codec,
        trackNumber = item.trackNumber,
        diskNumber = item.diskNumber,
        waveformCsv = waveformCsv
      )
    }
  }
}
