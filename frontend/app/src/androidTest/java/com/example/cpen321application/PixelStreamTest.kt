package com.example.cpen321application

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelStreamTest {
    @Test fun parsesPixelAndRejectsOutOfBoundsCoordinates() {
        assertEquals(PixelUpdate(1, 15, 0xffaabbccL), parsePixel("""{"x":1,"y":15,"color":"#aAbBcC"}"""))
        for (bad in listOf("""{"x":16,"y":0,"color":"#ffffff"}""", """{"x":0,"y":0,"color":"red"}""")) {
            assertTrue(runCatching { parsePixel(bad) }.isFailure)
        }
    }
    /** Requires the deployed relay and internet access. */
    @Test fun receivesPixelsThroughVerifiedSecureRelay() = runBlocking {
        val pixels = withTimeout(20_000) {
            PixelStream.updates(BuildConfig.API_BASE_URL).take(3).toList()
        }
        assertEquals(3, pixels.size)
        assertTrue(pixels.all { it.x in 0..15 && it.y in 0..15 })
    }
}
