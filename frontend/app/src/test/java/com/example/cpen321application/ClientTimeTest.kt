package com.example.cpen321application

import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientTimeTest {
    @Test fun formatsUtcWithExplicitZeroOffset() {
        assertEquals("00:04:05 GMT+00:00", clientLocalTime(ZonedDateTime.parse("2026-01-15T00:04:05Z")))
    }
    @Test fun formatsNegativeAndFractionalOffsets() {
        assertEquals("20:54:57 GMT-07:00", clientLocalTime(ZonedDateTime.parse("2026-09-15T20:54:57-07:00")))
        assertEquals("05:34:05 GMT+05:30", clientLocalTime(ZonedDateTime.parse("2026-01-15T05:34:05+05:30")))
    }
}
