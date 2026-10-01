package com.example.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.db.CachedTrackEntity
import com.example.data.db.DapDatabase
import com.example.model.AudioFileItem
import com.example.model.FolderItem
import com.example.player.AudioMetadataExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FolderRepository(private val context: Context) {

  private val database = DapDatabase.getInstance(context)
  private val dao = database.audioTrackDao()

  private val supportedExtensions = setOf(
    "flac", "wav", "mp3", "m4a", "alac", "ogg", "opus", "aac", "dsf", "dff", "ape", "aiff"
  )

  /**
   * Ultra-fast folder loading:
   * 1. Checks Room cache first (retrieves in < 5ms).
   * 2. If not in cache, loads instant lightweight directory items (< 20ms) without blocking IPC,
   *    and saves them to cache while background extraction enriches them.
   */
  suspend fun loadFolderContents(
    folderUriString: String?,
    rootUriString: String?
  ): Pair<List<FolderItem>, List<AudioFileItem>> = withContext(Dispatchers.IO) {
    if (folderUriString == null && rootUriString == null) {
      return@withContext getBundledDemoFolders() to emptyList()
    }

    val targetUriStr = folderUriString ?: rootUriString ?: return@withContext emptyList<FolderItem>() to emptyList()
    val targetUri = Uri.parse(targetUriStr)

    if (targetUri.scheme == "virtual_demo") {
      return@withContext getVirtualDemoContents(targetUri.toString())
    }

    val subfolders = mutableListOf<FolderItem>()
    val audioFiles = mutableListOf<AudioFileItem>()

    // Check Room cache first!
    val cachedTracks = try {
      dao.getTracksForFolder(targetUriStr)
    } catch (_: Exception) {
      emptyList()
    }

    if (cachedTracks.isNotEmpty()) {
      val mappedFiles = cachedTracks.map { it.toAudioFileItem() }
      // Quickly get subfolders only
      try {
        val docFolder = DocumentFile.fromTreeUri(context, targetUri)
        if (docFolder != null && docFolder.isDirectory) {
          val files = docFolder.listFiles()
          for (f in files) {
            if (f.isDirectory) {
              subfolders.add(
                FolderItem(
                  uriString = f.uri.toString(),
                  name = f.name ?: "Folder",
                  path = f.uri.path ?: "",
                  fileCount = 0,
                  subfolderCount = 0
                )
              )
            }
          }
        }
      } catch (_: Exception) {}

      subfolders.sortBy { it.name.lowercase() }
      return@withContext subfolders to mappedFiles
    }

    // Fast listing without deep recursive inspection
    val entitiesToCache = mutableListOf<CachedTrackEntity>()

    try {
      val docFolder = DocumentFile.fromTreeUri(context, targetUri)
      if (docFolder != null && docFolder.isDirectory) {
        val files: Array<DocumentFile> = docFolder.listFiles()
        for (file in files) {
          if (file.isDirectory) {
            // Shallow folder addition - DO NOT recursively query child files on every folder open!
            subfolders.add(
              FolderItem(
                uriString = file.uri.toString(),
                name = file.name ?: "Folder",
                path = file.uri.path ?: "",
                fileCount = 0,
                subfolderCount = 0
              )
            )
          } else {
            val name = file.name ?: continue
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext in supportedExtensions) {
              // Fast heuristic estimation (< 0.1ms per file instead of 500ms MediaMetadataRetriever)
              val item = AudioMetadataExtractor.fastEstimateItem(
                uri = file.uri,
                fileName = name,
                fileSize = file.length(),
                rawPath = file.uri.path ?: name
              )
              audioFiles.add(item)
              entitiesToCache.add(CachedTrackEntity.fromAudioFileItem(item, targetUriStr))
            }
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // Sort subfolders alphabetically and tracks by disk number, track number, or name
    subfolders.sortBy { it.name.lowercase() }
    audioFiles.sortWith(compareBy({ it.diskNumber }, { it.trackNumber }, { it.fileName.lowercase() }))

    // Cache the fast-loaded tracks into Room in background
    if (entitiesToCache.isNotEmpty()) {
      try {
        dao.insertTracks(entitiesToCache)
      } catch (_: Exception) {}
    }

    return@withContext subfolders to audioFiles
  }

  /**
   * Deep library indexing: scans metadata with full MediaMetadataRetriever and generates waveforms
   */
  suspend fun scanFolderDeep(folderUriStr: String, onProgress: (Int, Int) -> Unit): List<AudioFileItem> = withContext(Dispatchers.IO) {
    val targetUri = Uri.parse(folderUriStr)
    val enrichedFiles = mutableListOf<AudioFileItem>()
    val entities = mutableListOf<CachedTrackEntity>()

    try {
      val docFolder = DocumentFile.fromTreeUri(context, targetUri) ?: return@withContext emptyList()
      val files = docFolder.listFiles().filter { !it.isDirectory && (it.name?.substringAfterLast('.', "")?.lowercase() in supportedExtensions) }
      val total = files.size

      for ((index, file) in files.withIndex()) {
        val name = file.name ?: continue
        val item = AudioMetadataExtractor.extractMetadata(
          context = context,
          uri = file.uri,
          fileName = name,
          fileSize = file.length(),
          rawPath = file.uri.path ?: name
        )
        enrichedFiles.add(item)
        entities.add(CachedTrackEntity.fromAudioFileItem(item, folderUriStr))
        onProgress(index + 1, total)
      }

      if (entities.isNotEmpty()) {
        dao.clearFolder(folderUriStr)
        dao.insertTracks(entities)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    enrichedFiles.sortWith(compareBy({ it.diskNumber }, { it.trackNumber }, { it.fileName.lowercase() }))
    return@withContext enrichedFiles
  }

  fun getBundledDemoFolders(): List<FolderItem> {
    return listOf(
      FolderItem(
        uriString = "virtual_demo://root/24bit_96kHz_FLAC",
        name = "24-bit 96kHz FLAC Studio Master",
        path = "/storage/SD_CARD/Music/24bit_96kHz_FLAC",
        fileCount = 4,
        subfolderCount = 0,
        totalDurationMs = (4 * 60 + 18 + 3 * 60 + 45 + 5 * 60 + 12 + 4 * 60 + 50) * 1000L,
        isVirtual = true
      ),
      FolderItem(
        uriString = "virtual_demo://root/Beethoven_Sonatas_BoxSet",
        name = "Beethoven - Piano Sonatas (2-Disk Hi-Res)",
        path = "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet",
        fileCount = 6,
        subfolderCount = 0,
        totalDurationMs = (7 * 60 + 15 + 6 * 60 + 30 + 8 * 60 + 10 + 5 * 60 + 40 + 7 * 60 + 20 + 9 * 60 + 5) * 1000L,
        isVirtual = true
      ),
      FolderItem(
        uriString = "virtual_demo://root/DSD_Acoustic_Sessions",
        name = "DSD128 & 32-bit Acoustic Sessions",
        path = "/storage/SD_CARD/Music/DSD_Acoustic_Sessions",
        fileCount = 3,
        subfolderCount = 0,
        totalDurationMs = (5 * 60 + 30 + 4 * 60 + 15 + 6 * 60 + 5) * 1000L,
        isVirtual = true
      ),
      FolderItem(
        uriString = "virtual_demo://root/Lossless_Electronic_IDM",
        name = "WAV & Lossless Electronic IDM",
        path = "/storage/SD_CARD/Music/Lossless_Electronic_IDM",
        fileCount = 4,
        subfolderCount = 0,
        totalDurationMs = (6 * 60 + 10 + 4 * 60 + 40 + 5 * 60 + 25 + 3 * 60 + 55) * 1000L,
        isVirtual = true
      ),
      FolderItem(
        uriString = "virtual_demo://root/Vintage_Jazz_Trio",
        name = "Vintage 1959 Jazz Trio (Mono & Stereo)",
        path = "/storage/SD_CARD/Music/Vintage_Jazz_Trio",
        fileCount = 3,
        subfolderCount = 0,
        totalDurationMs = (5 * 60 + 15 + 7 * 60 + 20 + 4 * 60 + 45) * 1000L,
        isVirtual = true
      )
    )
  }

  private fun createDemoTrack(
    id: String,
    title: String,
    fileName: String,
    filePath: String,
    durationMs: Long,
    sizeBytes: Long,
    sampleRate: Int = 96000,
    bitDepth: Int = 24,
    bitrateKbps: Int = 2850,
    channels: Int = 2,
    codec: String = "FLAC",
    trackNumber: Int = 1,
    diskNumber: Int = 1,
    seedKey: String
  ): AudioFileItem {
    val (left, right) = AudioMetadataExtractor.generateStereoWaveform(seedKey, 60)
    val combined = left.zip(right) { l, r -> ((l + r) / 2f).coerceIn(0.12f, 1.0f) }
    return AudioFileItem(
      id = id,
      uriString = "android.resource://${context.packageName}/raw/demo_synth",
      title = title,
      fileName = fileName,
      extension = if (fileName.contains(".")) ".${fileName.substringAfterLast('.')}" else ".${codec.lowercase()}",
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
      waveformLeft = left,
      waveformRight = right,
      waveform = combined
    )
  }

  private fun getVirtualDemoContents(virtualUri: String): Pair<List<FolderItem>, List<AudioFileItem>> {
    when {
      virtualUri.contains("Beethoven_Sonatas_BoxSet") -> {
        // Multi-disk box set matching user example: Ex. '2.01 - Sonata F Moll - Sonata In F Minor, Op. 77...'
        val tracks = listOf(
          createDemoTrack("demo_sonata_1_01", "1.01 - Sonata C-Dur - Sonata In C Major, Op. 53 ''Waldstein'' I. Allegro", "1.01 - Sonata C-Dur - Sonata In C Major, Op. 53.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.01 - Sonata C-Dur.flac", 435_000L, 85_000_000L, 96000, 24, 2850, 2, "FLAC", 1, 1, "sonata_1_01"),
          createDemoTrack("demo_sonata_1_02", "1.02 - Sonata C-Dur - Sonata In C Major, Op. 53 II. Introduzione", "1.02 - Sonata C-Dur - Introduzione.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.02 - Introduzione.flac", 390_000L, 76_000_000L, 96000, 24, 2780, 2, "FLAC", 2, 1, "sonata_1_02"),
          createDemoTrack("demo_sonata_1_03", "1.03 - Sonata C-Dur - Sonata In C Major, Op. 53 III. Rondo", "1.03 - Sonata C-Dur - Rondo.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.03 - Rondo.flac", 490_000L, 94_000_000L, 96000, 24, 2920, 2, "FLAC", 3, 1, "sonata_1_03"),
          // DISK 2:
          createDemoTrack("demo_sonata_2_01", "2.01 - Sonata F Moll - Sonata In F Minor, Op. 77 ''L-Invocation'' I. Andante", "2.01 - Sonata F Moll - Sonata In F Minor, Op. 77 ''L-Invocation''.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/2.01 - Sonata F Moll - Sonata In F Minor, Op. 77 ''L-Invocation''.flac", 340_000L, 68_000_000L, 96000, 24, 2910, 2, "FLAC", 1, 2, "sonata_2_01"),
          createDemoTrack("demo_sonata_2_02", "2.02 - Sonata F Moll - Sonata In F Minor, Op. 77 II. Scherzo", "2.02 - Sonata F Moll - Scherzo.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/2.02 - Scherzo.flac", 440_000L, 89_000_000L, 96000, 24, 2900, 2, "FLAC", 2, 2, "sonata_2_02"),
          createDemoTrack("demo_sonata_2_03", "2.03 - Sonata F Moll - Sonata In F Minor, Op. 77 III. Finale", "2.03 - Sonata F Moll - Finale.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/2.03 - Finale.flac", 545_000L, 110_000_000L, 96000, 24, 2950, 2, "FLAC", 3, 2, "sonata_2_03")
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("24bit_96kHz_FLAC") -> {
        val tracks = listOf(
          createDemoTrack("demo_flac_1", "1.01 - Analog Dreamscape (96kHz Remaster)", "1.01 - Analog Dreamscape.flac", "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.01 - Analog Dreamscape.flac", 258_000L, 58_420_000L, 96000, 24, 2950, 2, "FLAC", 1, 1, "flac_1"),
          createDemoTrack("demo_flac_2", "1.02 - Velvet Skyline (Acoustic Resonance)", "1.02 - Velvet Skyline.flac", "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.02 - Velvet Skyline.flac", 225_000L, 51_180_000L, 96000, 24, 2820, 2, "FLAC", 2, 1, "flac_2"),
          createDemoTrack("demo_flac_3", "1.03 - Midnight Frequency (Analog Tape)", "1.03 - Midnight Frequency.flac", "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.03 - Midnight Frequency.flac", 312_000L, 69_840_000L, 96000, 24, 3110, 2, "FLAC", 3, 1, "flac_3"),
          createDemoTrack("demo_flac_4", "1.04 - Sub-bass Reflections (Direct Cut)", "1.04 - Sub-bass Reflections.flac", "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.04 - Sub-bass Reflections.flac", 290_000L, 64_200_000L, 96000, 24, 2980, 2, "FLAC", 4, 1, "flac_4")
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("DSD_Acoustic_Sessions") -> {
        val tracks = listOf(
          createDemoTrack("demo_dsd_1", "01 - Prelude in G Minor (1-bit Direct Stream)", "01 - Prelude in G Minor.dsf", "/storage/SD_CARD/Music/DSD_Acoustic_Sessions/01 - Prelude in G Minor.dsf", 330_000L, 112_000_000L, 192000, 1, 5644, 2, "DSD128", 1, 1, "dsd_1"),
          createDemoTrack("demo_dsd_2", "02 - Cellos in Autumn (Room Mic Pair)", "02 - Cellos in Autumn.dsf", "/storage/SD_CARD/Music/DSD_Acoustic_Sessions/02 - Cellos in Autumn.dsf", 255_000L, 89_000_000L, 192000, 1, 5644, 2, "DSD128", 2, 1, "dsd_2"),
          createDemoTrack("demo_dsd_3", "03 - Acoustic Reverie (Uncompressed)", "03 - Acoustic Reverie.dsf", "/storage/SD_CARD/Music/DSD_Acoustic_Sessions/03 - Acoustic Reverie.dsf", 365_000L, 124_000_000L, 192000, 1, 5644, 2, "DSD128", 3, 1, "dsd_3")
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("Lossless_Electronic_IDM") -> {
        val tracks = listOf(
          createDemoTrack("demo_wav_1", "01 - Modular Sequence A", "01 - Modular Sequence A.wav", "/storage/SD_CARD/Music/Lossless_Electronic_IDM/01 - Modular Sequence A.wav", 370_000L, 65_000_000L, 48000, 24, 2304, 2, "WAV", 1, 1, "wav_1"),
          createDemoTrack("demo_wav_2", "02 - Squarewave Glitch 04", "02 - Squarewave Glitch 04.wav", "/storage/SD_CARD/Music/Lossless_Electronic_IDM/02 - Squarewave Glitch 04.wav", 280_000L, 49_000_000L, 48000, 24, 2304, 2, "WAV", 2, 1, "wav_2"),
          createDemoTrack("demo_wav_3", "03 - Tape Loop Echo Chamber", "03 - Tape Loop Echo Chamber.wav", "/storage/SD_CARD/Music/Lossless_Electronic_IDM/03 - Tape Loop Echo Chamber.wav", 325_000L, 57_000_000L, 48000, 24, 2304, 2, "WAV", 3, 1, "wav_3"),
          createDemoTrack("demo_wav_4", "04 - Outro Pulse 120BPM", "04 - Outro Pulse 120BPM.wav", "/storage/SD_CARD/Music/Lossless_Electronic_IDM/04 - Outro Pulse 120BPM.wav", 235_000L, 41_000_000L, 48000, 24, 2304, 2, "WAV", 4, 1, "wav_4")
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("Vintage_Jazz_Trio") -> {
        val tracks = listOf(
          createDemoTrack("demo_jazz_1", "01 - Blue Monologue (Mono 1959)", "01 - Blue Monologue.flac", "/storage/SD_CARD/Music/Vintage_Jazz_Trio/01 - Blue Monologue.flac", 315_000L, 28_000_000L, 44100, 16, 710, 1, "FLAC", 1, 1, "jazz_1"),
          createDemoTrack("demo_jazz_2", "02 - Midnight Brushes & Upright", "02 - Midnight Brushes & Upright.flac", "/storage/SD_CARD/Music/Vintage_Jazz_Trio/02 - Midnight Brushes & Upright.flac", 440_000L, 46_000_000L, 44100, 16, 840, 2, "FLAC", 2, 1, "jazz_2"),
          createDemoTrack("demo_jazz_3", "03 - Autumn in Manhattan", "03 - Autumn in Manhattan.flac", "/storage/SD_CARD/Music/Vintage_Jazz_Trio/03 - Autumn in Manhattan.flac", 285_000L, 31_000_000L, 44100, 16, 870, 2, "FLAC", 3, 1, "jazz_3")
        )
        return emptyList<FolderItem>() to tracks
      }
      else -> return getBundledDemoFolders() to emptyList()
    }
  }
}
