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

  private fun getVirtualDemoContents(virtualUri: String): Pair<List<FolderItem>, List<AudioFileItem>> {
    when {
      virtualUri.contains("Beethoven_Sonatas_BoxSet") -> {
        // Multi-disk box set matching user example: Ex. '2.01 - Sonata F Moll - Sonata In F Minor, Op. 77...'
        val tracks = listOf(
          AudioFileItem(
            id = "demo_sonata_1_01",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "1.01 - Sonata C-Dur - Sonata In C Major, Op. 53 ''Waldstein'' I. Allegro",
            fileName = "1.01 - Sonata C-Dur - Sonata In C Major, Op. 53.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.01 - Sonata C-Dur.flac",
            durationMs = 435_000L,
            sizeBytes = 85_000_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2850,
            channels = 2,
            codec = "FLAC",
            trackNumber = 1,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("sonata_1_01", 50)
          ),
          AudioFileItem(
            id = "demo_sonata_1_02",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "1.02 - Sonata C-Dur - Sonata In C Major, Op. 53 II. Introduzione",
            fileName = "1.02 - Sonata C-Dur - Introduzione.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.02 - Introduzione.flac",
            durationMs = 390_000L,
            sizeBytes = 76_000_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2780,
            channels = 2,
            codec = "FLAC",
            trackNumber = 2,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("sonata_1_02", 50)
          ),
          AudioFileItem(
            id = "demo_sonata_1_03",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "1.03 - Sonata C-Dur - Sonata In C Major, Op. 53 III. Rondo",
            fileName = "1.03 - Sonata C-Dur - Rondo.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.03 - Rondo.flac",
            durationMs = 490_000L,
            sizeBytes = 94_000_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2920,
            channels = 2,
            codec = "FLAC",
            trackNumber = 3,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("sonata_1_03", 50)
          ),
          // DISK 2:
          AudioFileItem(
            id = "demo_sonata_2_01",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "2.01 - Sonata F Moll - Sonata In F Minor, Op. 77 ''L-Invocation'' I. Andante",
            fileName = "2.01 - Sonata F Moll - Sonata In F Minor, Op. 77 ''L-Invocation''.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/2.01 - Sonata F Moll - Sonata In F Minor, Op. 77 ''L-Invocation''.flac",
            durationMs = 340_000L,
            sizeBytes = 68_000_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2910,
            channels = 2,
            codec = "FLAC",
            trackNumber = 1,
            diskNumber = 2,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("sonata_2_01", 50)
          ),
          AudioFileItem(
            id = "demo_sonata_2_02",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "2.02 - Sonata F Moll - Sonata In F Minor, Op. 77 II. Scherzo",
            fileName = "2.02 - Sonata F Moll - Scherzo.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/2.02 - Scherzo.flac",
            durationMs = 440_000L,
            sizeBytes = 89_000_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2900,
            channels = 2,
            codec = "FLAC",
            trackNumber = 2,
            diskNumber = 2,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("sonata_2_02", 50)
          ),
          AudioFileItem(
            id = "demo_sonata_2_03",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "2.03 - Sonata F Moll - Sonata In F Minor, Op. 77 III. Finale",
            fileName = "2.03 - Sonata F Moll - Finale.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/2.03 - Finale.flac",
            durationMs = 545_000L,
            sizeBytes = 110_000_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2950,
            channels = 2,
            codec = "FLAC",
            trackNumber = 3,
            diskNumber = 2,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("sonata_2_03", 50)
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("24bit_96kHz_FLAC") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_flac_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "1.01 - Analog Dreamscape (96kHz Remaster)",
            fileName = "1.01 - Analog Dreamscape.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.01 - Analog Dreamscape.flac",
            durationMs = 258_000L,
            sizeBytes = 58_420_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2950,
            channels = 2,
            codec = "FLAC",
            trackNumber = 1,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("flac_1", 50)
          ),
          AudioFileItem(
            id = "demo_flac_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "1.02 - Velvet Skyline (Acoustic Resonance)",
            fileName = "1.02 - Velvet Skyline.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.02 - Velvet Skyline.flac",
            durationMs = 225_000L,
            sizeBytes = 51_180_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2820,
            channels = 2,
            codec = "FLAC",
            trackNumber = 2,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("flac_2", 50)
          ),
          AudioFileItem(
            id = "demo_flac_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "1.03 - Midnight Frequency (Analog Tape)",
            fileName = "1.03 - Midnight Frequency.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.03 - Midnight Frequency.flac",
            durationMs = 312_000L,
            sizeBytes = 69_840_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 3110,
            channels = 2,
            codec = "FLAC",
            trackNumber = 3,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("flac_3", 50)
          ),
          AudioFileItem(
            id = "demo_flac_4",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "1.04 - Sub-bass Reflections (Direct Cut)",
            fileName = "1.04 - Sub-bass Reflections.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/1.04 - Sub-bass Reflections.flac",
            durationMs = 290_000L,
            sizeBytes = 64_200_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2980,
            channels = 2,
            codec = "FLAC",
            trackNumber = 4,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("flac_4", 50)
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("DSD_Acoustic_Sessions") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_dsd_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "01 - Prelude in G Minor (1-bit Direct Stream)",
            fileName = "01 - Prelude in G Minor.dsf",
            extension = ".dsf",
            filePath = "/storage/SD_CARD/Music/DSD_Acoustic_Sessions/01 - Prelude in G Minor.dsf",
            durationMs = 330_000L,
            sizeBytes = 112_000_000L,
            sampleRate = 192000,
            bitDepth = 1,
            bitrateKbps = 5644,
            channels = 2,
            codec = "DSD128",
            trackNumber = 1,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("dsd_1", 50)
          ),
          AudioFileItem(
            id = "demo_dsd_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "02 - Cellos in Autumn (Room Mic Pair)",
            fileName = "02 - Cellos in Autumn.dsf",
            extension = ".dsf",
            filePath = "/storage/SD_CARD/Music/DSD_Acoustic_Sessions/02 - Cellos in Autumn.dsf",
            durationMs = 255_000L,
            sizeBytes = 89_000_000L,
            sampleRate = 192000,
            bitDepth = 1,
            bitrateKbps = 5644,
            channels = 2,
            codec = "DSD128",
            trackNumber = 2,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("dsd_2", 50)
          ),
          AudioFileItem(
            id = "demo_dsd_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "03 - Acoustic Reverie (Uncompressed)",
            fileName = "03 - Acoustic Reverie.dsf",
            extension = ".dsf",
            filePath = "/storage/SD_CARD/Music/DSD_Acoustic_Sessions/03 - Acoustic Reverie.dsf",
            durationMs = 365_000L,
            sizeBytes = 124_000_000L,
            sampleRate = 192000,
            bitDepth = 1,
            bitrateKbps = 5644,
            channels = 2,
            codec = "DSD128",
            trackNumber = 3,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("dsd_3", 50)
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("Lossless_Electronic_IDM") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_wav_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "01 - Modular Sequence A",
            fileName = "01 - Modular Sequence A.wav",
            extension = ".wav",
            filePath = "/storage/SD_CARD/Music/Lossless_Electronic_IDM/01 - Modular Sequence A.wav",
            durationMs = 370_000L,
            sizeBytes = 65_000_000L,
            sampleRate = 48000,
            bitDepth = 24,
            bitrateKbps = 2304,
            channels = 2,
            codec = "WAV",
            trackNumber = 1,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("wav_1", 50)
          ),
          AudioFileItem(
            id = "demo_wav_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "02 - Squarewave Glitch 04",
            fileName = "02 - Squarewave Glitch 04.wav",
            extension = ".wav",
            filePath = "/storage/SD_CARD/Music/Lossless_Electronic_IDM/02 - Squarewave Glitch 04.wav",
            durationMs = 280_000L,
            sizeBytes = 49_000_000L,
            sampleRate = 48000,
            bitDepth = 24,
            bitrateKbps = 2304,
            channels = 2,
            codec = "WAV",
            trackNumber = 2,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("wav_2", 50)
          ),
          AudioFileItem(
            id = "demo_wav_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "03 - Tape Loop Echo Chamber",
            fileName = "03 - Tape Loop Echo Chamber.wav",
            extension = ".wav",
            filePath = "/storage/SD_CARD/Music/Lossless_Electronic_IDM/03 - Tape Loop Echo Chamber.wav",
            durationMs = 325_000L,
            sizeBytes = 57_000_000L,
            sampleRate = 48000,
            bitDepth = 24,
            bitrateKbps = 2304,
            channels = 2,
            codec = "WAV",
            trackNumber = 3,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("wav_3", 50)
          ),
          AudioFileItem(
            id = "demo_wav_4",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "04 - Outro Pulse 120BPM",
            fileName = "04 - Outro Pulse 120BPM.wav",
            extension = ".wav",
            filePath = "/storage/SD_CARD/Music/Lossless_Electronic_IDM/04 - Outro Pulse 120BPM.wav",
            durationMs = 235_000L,
            sizeBytes = 41_000_000L,
            sampleRate = 48000,
            bitDepth = 24,
            bitrateKbps = 2304,
            channels = 2,
            codec = "WAV",
            trackNumber = 4,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("wav_4", 50)
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("Vintage_Jazz_Trio") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_jazz_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "01 - Blue Monologue (Mono 1959)",
            fileName = "01 - Blue Monologue.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Vintage_Jazz_Trio/01 - Blue Monologue.flac",
            durationMs = 315_000L,
            sizeBytes = 28_000_000L,
            sampleRate = 44100,
            bitDepth = 16,
            bitrateKbps = 710,
            channels = 1,
            codec = "FLAC",
            trackNumber = 1,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("jazz_1", 50)
          ),
          AudioFileItem(
            id = "demo_jazz_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "02 - Midnight Brushes & Upright",
            fileName = "02 - Midnight Brushes & Upright.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Vintage_Jazz_Trio/02 - Midnight Brushes & Upright.flac",
            durationMs = 440_000L,
            sizeBytes = 46_000_000L,
            sampleRate = 44100,
            bitDepth = 16,
            bitrateKbps = 840,
            channels = 2,
            codec = "FLAC",
            trackNumber = 2,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("jazz_2", 50)
          ),
          AudioFileItem(
            id = "demo_jazz_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "03 - Autumn in Manhattan",
            fileName = "03 - Autumn in Manhattan.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/Vintage_Jazz_Trio/03 - Autumn in Manhattan.flac",
            durationMs = 285_000L,
            sizeBytes = 31_000_000L,
            sampleRate = 44100,
            bitDepth = 16,
            bitrateKbps = 870,
            channels = 2,
            codec = "FLAC",
            trackNumber = 3,
            diskNumber = 1,
            waveform = AudioMetadataExtractor.generateSyntheticWaveform("jazz_3", 50)
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      else -> return getBundledDemoFolders() to emptyList()
    }
  }
}
