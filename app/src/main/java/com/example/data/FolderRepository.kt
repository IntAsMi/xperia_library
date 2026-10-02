package com.example.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.R
import com.example.data.db.CachedFolderEntity
import com.example.data.db.CachedTrackEntity
import com.example.data.db.DapDatabase
import com.example.model.AudioFileItem
import com.example.model.FolderItem
import com.example.player.AudioMetadataExtractor
import com.example.player.AudioWaveformExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class FolderRepository(private val context: Context) {

  private val database = DapDatabase.getInstance(context)
  private val dao = database.audioTrackDao()

  // In-memory L1 Cache for 0ms instantaneous folder switching
  private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, Pair<List<FolderItem>, List<AudioFileItem>>>()

  private val supportedExtensions = setOf(
    "flac", "wav", "mp3", "m4a", "alac", "ogg", "opus", "aac", "dsf", "dff", "ape", "aiff"
  )

  /**
   * Ultra-fast folder loading:
   * 1. Checks in-memory L1 cache first (< 0.1ms).
   * 2. Checks Room L2 cache second (< 1ms).
   * 3. If uncached, loads from SAF and caches in both Room and L1 memory.
   */
  suspend fun loadFolderContents(
    folderUriString: String?,
    rootUriString: String?
  ): Pair<List<FolderItem>, List<AudioFileItem>> = withContext(Dispatchers.IO) {
    if (folderUriString == null && rootUriString == null) {
      return@withContext getBundledDemoFolders() to emptyList()
    }

    val targetUriStr = folderUriString ?: rootUriString ?: return@withContext emptyList<FolderItem>() to emptyList()

    // L1 Memory Cache Check
    memoryCache[targetUriStr]?.let { return@withContext it }

    val targetUri = Uri.parse(targetUriStr)

    if (targetUri.scheme == "virtual_demo") {
      val res = getVirtualDemoContents(targetUri.toString())
      memoryCache[targetUriStr] = res
      return@withContext res
    }

    // L2 Room Cache Check
    val cachedFolders = try {
      dao.getSubfoldersForFolder(targetUriStr)
    } catch (_: Exception) {
      emptyList()
    }
    val cachedTracks = try {
      dao.getTracksForFolder(targetUriStr)
    } catch (_: Exception) {
      emptyList()
    }

    if (cachedFolders.isNotEmpty() || cachedTracks.isNotEmpty()) {
      val mappedFolders = cachedFolders.map { it.toFolderItem() }
      val mappedFiles = cachedTracks.map { it.toAudioFileItem() }
      val result = mappedFolders to mappedFiles
      memoryCache[targetUriStr] = result
      return@withContext result
    }

    // Step 2: Uncached folder - query SAF once and save both subfolders and tracks into Room
    val subfolders = mutableListOf<FolderItem>()
    val audioFiles = mutableListOf<AudioFileItem>()
    val folderEntities = mutableListOf<CachedFolderEntity>()
    val trackEntities = mutableListOf<CachedTrackEntity>()

    try {
      val docFolder = DocumentFile.fromTreeUri(context, targetUri)
      if (docFolder != null && docFolder.isDirectory) {
        val files: Array<DocumentFile> = docFolder.listFiles()
        for (file in files) {
          if (file.isDirectory) {
            val fItem = FolderItem(
              uriString = file.uri.toString(),
              name = file.name ?: "Folder",
              path = file.uri.path ?: "",
              fileCount = 0,
              subfolderCount = 0
            )
            subfolders.add(fItem)
            folderEntities.add(CachedFolderEntity.fromFolderItem(fItem, targetUriStr))
          } else {
            val name = file.name ?: continue
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext in supportedExtensions) {
              val item = AudioMetadataExtractor.fastEstimateItem(
                uri = file.uri,
                fileName = name,
                fileSize = file.length(),
                rawPath = file.uri.path ?: name
              )
              audioFiles.add(item)
              trackEntities.add(CachedTrackEntity.fromAudioFileItem(item, targetUriStr))
            }
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    subfolders.sortBy { it.name.lowercase() }
    audioFiles.sortWith(compareBy({ it.diskNumber }, { it.trackNumber }, { it.fileName.lowercase() }))

    // Cache both subfolders and tracks in Room for instantaneous subsequent visits
    if (folderEntities.isNotEmpty()) {
      try { dao.insertFolders(folderEntities) } catch (_: Exception) {}
    }
    if (trackEntities.isNotEmpty()) {
      try { dao.insertTracks(trackEntities) } catch (_: Exception) {}
    }

    return@withContext subfolders to audioFiles
  }

  /**
   * Deep library indexing: recursively indexes the entire folder tree into Room database.
   * After scanning, browsing any folder in the library loads in < 2ms without SAF delays!
   */
  suspend fun scanFolderDeep(rootFolderUriStr: String, onProgress: (Int, Int) -> Unit): List<AudioFileItem> = withContext(Dispatchers.IO) {
    if (rootFolderUriStr.startsWith("virtual_demo://") || rootFolderUriStr.isBlank()) {
      // Index all demo folders into Room & L1 memory cache
      val demoFolders = getBundledDemoFolders()
      val allTracks = mutableListOf<AudioFileItem>()
      val folderEntities = mutableListOf<CachedFolderEntity>()
      val trackEntities = mutableListOf<CachedTrackEntity>()

      var count = 0
      val totalDemoTracks = 20

      for (folder in demoFolders) {
        val (sub, tracks) = getVirtualDemoContents(folder.uriString)
        memoryCache[folder.uriString] = sub to tracks
        allTracks.addAll(tracks)

        folderEntities.add(CachedFolderEntity.fromFolderItem(folder, "virtual_demo://root"))
        for (t in tracks) {
          trackEntities.add(CachedTrackEntity.fromAudioFileItem(t, folder.uriString))
          count++
          onProgress(count, totalDemoTracks)
          kotlinx.coroutines.delay(20L) // Visual progression feedback
        }
      }

      try {
        dao.insertFolders(folderEntities)
        dao.insertTracks(trackEntities)
      } catch (_: Exception) {}

      memoryCache["virtual_demo://root"] = demoFolders to emptyList()
      onProgress(count, totalDemoTracks)
      return@withContext allTracks
    }

    val targetUri = Uri.parse(rootFolderUriStr)
    val rootDoc = DocumentFile.fromTreeUri(context, targetUri) ?: return@withContext emptyList()

    val folderQueue = ArrayDeque<Pair<DocumentFile, String>>() // DocumentFile to parentUriStr
    folderQueue.add(rootDoc to rootFolderUriStr)

    val allFoldersToCache = mutableListOf<CachedFolderEntity>()
    val allTracksToCache = mutableListOf<CachedTrackEntity>()
    val rootFolderTracks = mutableListOf<AudioFileItem>()

    var totalFilesProcessed = 0
    var estimatedTotal = 20

    while (folderQueue.isNotEmpty()) {
      val (currentDoc, currentParentUriStr) = folderQueue.removeFirst()
      val currentFolderUriStr = currentDoc.uri.toString()

      try {
        val children = currentDoc.listFiles()
        val childFolders = children.filter { it.isDirectory }
        val childAudio = children.filter { !it.isDirectory && (it.name?.substringAfterLast('.', "")?.lowercase() in supportedExtensions) }

        estimatedTotal += childAudio.size

        // Add subfolders to queue and cache
        for (f in childFolders) {
          folderQueue.add(f to currentFolderUriStr)
          val folderItem = FolderItem(
            uriString = f.uri.toString(),
            name = f.name ?: "Folder",
            path = f.uri.path ?: "",
            fileCount = 0,
            subfolderCount = 0
          )
          allFoldersToCache.add(CachedFolderEntity.fromFolderItem(folderItem, currentFolderUriStr))
        }

        // Process audio files in this folder
        for (audioFile in childAudio) {
          val name = audioFile.name ?: continue
          val item = AudioMetadataExtractor.extractMetadata(
            context = context,
            uri = audioFile.uri,
            fileName = name,
            fileSize = audioFile.length(),
            rawPath = audioFile.uri.path ?: name
          )

          allTracksToCache.add(CachedTrackEntity.fromAudioFileItem(item, currentFolderUriStr))
          if (currentFolderUriStr == rootFolderUriStr) {
            rootFolderTracks.add(item)
          }

          totalFilesProcessed++
          onProgress(totalFilesProcessed, estimatedTotal)
        }

      } catch (e: Exception) {
        e.printStackTrace()
      }

      // Batch persist to Room database periodically
      if (allFoldersToCache.size >= 50) {
        try { dao.insertFolders(allFoldersToCache.toList()); allFoldersToCache.clear() } catch (_: Exception) {}
      }
      if (allTracksToCache.size >= 50) {
        try { dao.insertTracks(allTracksToCache.toList()); allTracksToCache.clear() } catch (_: Exception) {}
      }
    }

    // Flush any remaining records into Room
    if (allFoldersToCache.isNotEmpty()) {
      try { dao.insertFolders(allFoldersToCache) } catch (_: Exception) {}
    }
    if (allTracksToCache.isNotEmpty()) {
      try { dao.insertTracks(allTracksToCache) } catch (_: Exception) {}
    }

    onProgress(totalFilesProcessed, totalFilesProcessed)
    rootFolderTracks.sortWith(compareBy({ it.diskNumber }, { it.trackNumber }, { it.fileName.lowercase() }))
    return@withContext rootFolderTracks
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
        name = "24-bit 48kHz Electronic Master",
        path = "/storage/SD_CARD/Music/Lossless_Electronic_IDM",
        fileCount = 4,
        subfolderCount = 0,
        totalDurationMs = (6 * 60 + 10 + 4 * 60 + 40 + 5 * 60 + 25 + 3 * 60 + 55) * 1000L,
        isVirtual = true
      ),
      FolderItem(
        uriString = "virtual_demo://root/Vintage_Jazz_Trio",
        name = "Vintage Jazz Trio (Mono & Stereo)",
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
        val tracks = listOf(
          createDemoTrack("demo_sonata_1_01", "1.01 - Sonata C-Dur - Sonata In C Major, Op. 53 ''Waldstein'' I. Allegro", "1.01 - Sonata C-Dur - Sonata In C Major, Op. 53.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.01 - Sonata C-Dur.flac", 435_000L, 85_000_000L, 96000, 24, 2850, 2, "FLAC", 1, 1, "sonata_1_01"),
          createDemoTrack("demo_sonata_1_02", "1.02 - Sonata C-Dur - Sonata In C Major, Op. 53 II. Introduzione", "1.02 - Sonata C-Dur - Introduzione.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.02 - Introduzione.flac", 390_000L, 76_000_000L, 96000, 24, 2780, 2, "FLAC", 2, 1, "sonata_1_02"),
          createDemoTrack("demo_sonata_1_03", "1.03 - Sonata C-Dur - Sonata In C Major, Op. 53 III. Rondo", "1.03 - Sonata C-Dur - Rondo.flac", "/storage/SD_CARD/Music/Beethoven_Sonatas_BoxSet/1.03 - Rondo.flac", 490_000L, 94_000_000L, 96000, 24, 2920, 2, "FLAC", 3, 1, "sonata_1_03"),
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
