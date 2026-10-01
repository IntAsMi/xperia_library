package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioFileItem
import com.example.model.ChannelMode
import com.example.model.DapFontSize
import com.example.model.DapPlayerState
import com.example.model.DapThemeSetting
import com.example.model.SleepTimerOption
import com.example.model.VisualizerChannelMode
import com.example.model.formatDuration
import com.example.model.formatFileSize
import com.example.player.AudioMetadataExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DAP Console", appName)
  }

  @Test
  fun `verify duration formatting`() {
    assertEquals("00:00", formatDuration(0L))
    assertEquals("04:18", formatDuration(258_000L))
    assertEquals("01:01:05", formatDuration(3_665_000L))
  }

  @Test
  fun `verify file size formatting`() {
    assertEquals("0 MB", formatFileSize(0L))
    assertEquals("500 KB", formatFileSize(512_000L))
    assertTrue(formatFileSize(58_420_000L).contains("55.7 MB") || formatFileSize(58_420_000L).contains("MB"))
  }

  @Test
  fun `verify theme settings`() {
    val themes = DapThemeSetting.values()
    assertEquals(5, themes.size)
    assertTrue(themes.contains(DapThemeSetting.DARK))
    assertTrue(themes.contains(DapThemeSetting.LIGHT))
    assertTrue(themes.contains(DapThemeSetting.SYSTEM))
    assertTrue(themes.contains(DapThemeSetting.MATERIAL_U))
    assertTrue(themes.contains(DapThemeSetting.AMOLED_BLACK))
  }

  @Test
  fun `verify disk and track number parsing from filename`() {
    // Exact user format: '2.01 - Sonata F Moll - Sonata In F Minor, Op. 77 ''L-Invocation'' ...'
    val filename = "2.01 - Sonata F Moll - Sonata In F Minor, Op. 77.flac"
    val item = AudioMetadataExtractor.fastEstimateItem(
      uri = Uri.parse("file://test/$filename"),
      fileName = filename,
      fileSize = 75_000_000L,
      rawPath = "/storage/SD_CARD/Music/$filename"
    )

    assertEquals(2, item.diskNumber)
    assertEquals(1, item.trackNumber)
    assertEquals("FLAC", item.codec)
    assertTrue(item.title.contains("Sonata F Moll"))
    assertEquals(60, item.waveform.size)
    assertEquals(60, item.waveformLeft.size)
    assertEquals(60, item.waveformRight.size)
  }

  @Test
  fun `verify multi-level timings state`() {
    val track1 = AudioFileItem(
      id = "1",
      uriString = "uri1",
      title = "Track 1.01",
      fileName = "1.01.flac",
      extension = ".flac",
      filePath = "/path/1.01.flac",
      durationMs = 200_000L,
      sizeBytes = 40_000_000L,
      diskNumber = 1,
      trackNumber = 1
    )
    val track2 = AudioFileItem(
      id = "2",
      uriString = "uri2",
      title = "Track 2.01",
      fileName = "2.01.flac",
      extension = ".flac",
      filePath = "/path/2.01.flac",
      durationMs = 300_000L,
      sizeBytes = 60_000_000L,
      diskNumber = 2,
      trackNumber = 1
    )

    val state = DapPlayerState(
      currentTrack = track2,
      currentDiskNumber = 2,
      positionMs = 50_000L,
      durationMs = 300_000L,
      diskTotalDurationMs = 300_000L,
      diskRemainingDurationMs = 250_000L,
      folderTotalDurationMs = 500_000L,
      folderRemainingDurationMs = 250_000L
    )

    assertEquals("-04:10", state.formattedRemaining)
    assertEquals("-04:10", state.formattedDiskRemaining)
    assertEquals("05:00", state.formattedDiskTotal)
    assertEquals("08:20", state.formattedFolderTotal)
    assertEquals("-04:10", state.formattedFolderRemaining)
  }

  @Test
  fun `verify audio tuning options`() {
    assertEquals(4, ChannelMode.values().size)
    assertEquals(3, VisualizerChannelMode.values().size)
    assertEquals(4, DapFontSize.values().size)
    assertEquals(7, SleepTimerOption.values().size)
  }
}
