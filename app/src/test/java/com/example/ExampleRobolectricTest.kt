package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioFileItem
import com.example.model.DapThemeSetting
import com.example.model.formatDuration
import com.example.model.formatFileSize
import org.junit.Assert.assertEquals
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
  fun `verify audio file specs formatting`() {
    val track = AudioFileItem(
      id = "test_1",
      uriString = "content://media/1",
      title = "Symphony No. 5",
      fileName = "01. Symphony.flac",
      extension = ".flac",
      filePath = "/Music/Beethoven/01. Symphony.flac",
      durationMs = 240_000L,
      sizeBytes = 45_000_000L,
      sampleRate = 96000,
      bitDepth = 24,
      bitrateKbps = 2850,
      channels = 2,
      codec = "FLAC"
    )
    assertEquals("04:00", track.formattedDuration)
    assertTrue(track.formattedSpecs.contains("FLAC"))
    assertTrue(track.formattedSpecs.contains("96.0 kHz") || track.formattedSpecs.contains("96 kHz"))
    assertTrue(track.formattedSpecs.contains("24-bit"))
    assertTrue(track.formattedSpecs.contains("2850 kbps"))
  }
}
