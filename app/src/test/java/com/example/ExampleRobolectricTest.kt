package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AiSubjectTracking
import com.example.model.CreativeLook
import com.example.model.DeviceProfile
import com.example.model.DriveMode
import com.example.model.LensOption
import com.example.model.ShootingMode
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
    assertEquals("Photography Pro", appName)
  }

  @Test
  fun `verify sony xperia 1 VIII lenses and telemacro`() {
    assertEquals("16mm", LensOption.ULTRA_WIDE_16MM.focalLength)
    assertEquals("24mm", LensOption.WIDE_24MM.focalLength)
    assertEquals("48mm", LensOption.WIDE_48MM.focalLength)
    assertEquals("85-170mm", LensOption.TELE_85_170MM.focalLength)
    assertTrue(LensOption.TELE_85_170MM.isOpticalZoomModule)
    assertTrue(LensOption.TELE_85_170MM.isTeleMacroCapable)
  }

  @Test
  fun `verify device profiles and 60fps burst`() {
    assertEquals("Xperia 1 VIII", DeviceProfile.XPERIA_1_VIII.modelName)
    assertEquals("Xperia 1 V", DeviceProfile.XPERIA_1_V.modelName)
    assertEquals(60, DriveMode.BURST_ULTRA.fps)
    assertEquals("60", DriveMode.BURST_ULTRA.symbol)
    assertNotNull(AiSubjectTracking.HUMAN)
    assertNotNull(AiSubjectTracking.ANIMAL_BIRD)
    assertNotNull(AiSubjectTracking.VEHICLE)
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
