package com.example.cpen321application

import org.junit.Assert.*
import org.junit.Test

class PenaltyRulesTest {
    @Test fun validatesMinutesAndSeconds() {
        assertEquals(65000L, timerDurationMillis("1", "5"))
        for ((m, s) in listOf("0" to "0", "0" to "60", "-1" to "2", "" to "1", "1000" to "0")) {
            assertNull(timerDurationMillis(m, s))
        }
    }
    @Test fun countdownUsesDeadlineInsteadOfAccumulatedTicks() {
        assertEquals(2L, remainingSeconds(5000, 3001))
        assertEquals(0L, remainingSeconds(5000, 9000))
    }
    @Test fun keeperSavesNearbyShotsButNotFarCorners() {
        assertTrue(isPenaltySaved(0.5f, 0.5f, 0.5f, 0.5f))
        assertFalse(isPenaltySaved(0f, 0f, 0.8f, 0.6f))
        assertFalse(isPenaltySaved(1f, 1f, 0.1f, 0.3f))
    }
}
