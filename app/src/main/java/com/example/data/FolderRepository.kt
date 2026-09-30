package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.model.AudioFileItem
import com.example.model.FolderItem
import com.example.player.AudioMetadataExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FolderRepository(private val context: Context) {

  private val supportedExtensions = setOf(
    "flac", "wav", "mp3", "m4a", "alac", "ogg", "opus", "aac", "dsf", "dff", "ape", "aiff"
  )

  suspend fun loadFolderContents(
    folderUriString: String?,
    rootUriString: String?
  ): Pair<List<FolderItem>, List<AudioFileItem>> = withContext(Dispatchers.IO) {
    if (folderUriString == null && rootUriString == null) {
      return@withContext getBundledDemoFolders() to emptyList()
    }

    val targetUri = Uri.parse(folderUriString ?: rootUriString)

    if (targetUri.scheme == "virtual_demo") {
      return@withContext getVirtualDemoContents(targetUri.toString())
    }

    val subfolders = mutableListOf<FolderItem>()
    val audioFiles = mutableListOf<AudioFileItem>()

    try {
      val docFolder = DocumentFile.fromTreeUri(context, targetUri)
      if (docFolder != null && docFolder.isDirectory) {
        val files: Array<DocumentFile> = docFolder.listFiles()
        for (file in files) {
          if (file.isDirectory) {
            val childFiles: Array<DocumentFile> = file.listFiles()
            val audioCount = childFiles.count { f: DocumentFile ->
              val ext = f.name?.substringAfterLast('.', "")?.lowercase() ?: ""
              ext in supportedExtensions
            }
            val subCount = childFiles.count { it.isDirectory }
            subfolders.add(
              FolderItem(
                uriString = file.uri.toString(),
                name = file.name ?: "Folder",
                path = file.uri.path ?: "",
                fileCount = audioCount,
                subfolderCount = subCount
              )
            )
          } else {
            val name = file.name ?: continue
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext in supportedExtensions) {
              val item = AudioMetadataExtractor.extractMetadata(
                context = context,
                uri = file.uri,
                fileName = name,
                fileSize = file.length(),
                rawPath = file.uri.path ?: name
              )
              audioFiles.add(item)
            }
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // Sort subfolders alphabetically and files by track number or name
    subfolders.sortBy { it.name.lowercase() }
    audioFiles.sortBy { if (it.trackNumber > 0) it.trackNumber.toString().padStart(4, '0') else it.fileName.lowercase() }

    return@withContext subfolders to audioFiles
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
      virtualUri.contains("24bit_96kHz_FLAC") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_flac_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Analog Dreamscape (96kHz Remaster)",
            fileName = "01 - Analog Dreamscape.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/01 - Analog Dreamscape.flac",
            durationMs = 258_000L,
            sizeBytes = 58_420_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2950,
            channels = 2,
            codec = "FLAC",
            trackNumber = 1
          ),
          AudioFileItem(
            id = "demo_flac_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Velvet Skyline (Acoustic Resonance)",
            fileName = "02 - Velvet Skyline.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/02 - Velvet Skyline.flac",
            durationMs = 225_000L,
            sizeBytes = 51_180_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2820,
            channels = 2,
            codec = "FLAC",
            trackNumber = 2
          ),
          AudioFileItem(
            id = "demo_flac_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Midnight Frequency (Analog Tape)",
            fileName = "03 - Midnight Frequency.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/03 - Midnight Frequency.flac",
            durationMs = 312_000L,
            sizeBytes = 69_840_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 3110,
            channels = 2,
            codec = "FLAC",
            trackNumber = 3
          ),
          AudioFileItem(
            id = "demo_flac_4",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Sub-bass Reflections (Direct Cut)",
            fileName = "04 - Sub-bass Reflections.flac",
            extension = ".flac",
            filePath = "/storage/SD_CARD/Music/24bit_96kHz_FLAC/04 - Sub-bass Reflections.flac",
            durationMs = 290_000L,
            sizeBytes = 64_200_000L,
            sampleRate = 96000,
            bitDepth = 24,
            bitrateKbps = 2980,
            channels = 2,
            codec = "FLAC",
            trackNumber = 4
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("DSD_Acoustic_Sessions") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_dsd_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Prelude in G Minor (1-bit Direct Stream)",
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
            trackNumber = 1
          ),
          AudioFileItem(
            id = "demo_dsd_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Cellos in Autumn (Room Mic Pair)",
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
            trackNumber = 2
          ),
          AudioFileItem(
            id = "demo_dsd_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Acoustic Reverie (Uncompressed)",
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
            trackNumber = 3
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("Lossless_Electronic_IDM") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_wav_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Modular Sequence A",
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
            trackNumber = 1
          ),
          AudioFileItem(
            id = "demo_wav_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Squarewave Glitch 04",
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
            trackNumber = 2
          ),
          AudioFileItem(
            id = "demo_wav_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Tape Loop Echo Chamber",
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
            trackNumber = 3
          ),
          AudioFileItem(
            id = "demo_wav_4",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Outro Pulse 120BPM",
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
            trackNumber = 4
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      virtualUri.contains("Vintage_Jazz_Trio") -> {
        val tracks = listOf(
          AudioFileItem(
            id = "demo_jazz_1",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Blue Monologue (Mono 1959)",
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
            trackNumber = 1
          ),
          AudioFileItem(
            id = "demo_jazz_2",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Midnight Brushes & Upright",
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
            trackNumber = 2
          ),
          AudioFileItem(
            id = "demo_jazz_3",
            uriString = "android.resource://${context.packageName}/raw/demo_synth",
            title = "Autumn in Manhattan",
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
            trackNumber = 3
          )
        )
        return emptyList<FolderItem>() to tracks
      }
      else -> return getBundledDemoFolders() to emptyList()
    }
  }
}
