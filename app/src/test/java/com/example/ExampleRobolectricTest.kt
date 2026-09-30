package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CreativeLook
import com.example.model.LensOption
import com.example.model.ShootingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("Photography Pro", appName)
  }

  @Test
  fun `verify sony xperia lenses`() {
    val lenses = LensOption.values()
    assertEquals(4, lenses.size)
    assertEquals("16mm", LensOption.ULTRA_WIDE_16MM.focalLength)
    assertEquals("24mm", LensOption.WIDE_24MM.focalLength)
    assertEquals("48mm", LensOption.WIDE_48MM.focalLength)
    assertEquals("85-125mm", LensOption.TELE_85_125MM.focalLength)
  }

  @Test
  fun `verify shooting modes and creative looks`() {
    assertEquals("BASIC", ShootingMode.BASIC.label)
    assertEquals("AUTO", ShootingMode.AUTO.label)
    assertEquals("P", ShootingMode.P.label)
    assertEquals("S", ShootingMode.S.label)
    assertEquals("M", ShootingMode.M.label)
    assertEquals("MR", ShootingMode.MR.label)

    assertNotNull(CreativeLook.FL)
    assertNotNull(CreativeLook.ST)
    assertNotNull(CreativeLook.BW)
  }
}
