package com.example.cpen321application

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Live integration check: requires internet and the deployed M1 backend. */
@RunWith(AndroidJUnit4::class)
class BackendHttpsTest {
    @Test fun retrievesAllThreeApisWithAndroidCertificateValidation() = runBlocking {
        val info = fetchServerInfo(BuildConfig.API_BASE_URL)
        assertEquals("35.222.61.184", info.ip)
        assertEquals("Nikhil Sinclair", info.name)
        assertTrue(info.time.matches(Regex("\\d{2}:\\d{2}:\\d{2} GMT[+-]\\d{2}:\\d{2}")))
        assertTrue(clientIpAddress() != "Unavailable")
    }
}
