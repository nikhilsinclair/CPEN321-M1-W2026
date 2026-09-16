package com.example.cpen321application

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelGridTest {
    @Test fun updatesCorrectCellAndDoesNotDoubleCount() {
        val grid = PixelGrid()
        grid.apply(PixelUpdate(2, 3, 0xffaabbccL), 0)
        grid.apply(PixelUpdate(2, 3, 0xff123456L), 1)
        assertEquals(1, grid.filled)
        assertEquals(0xff123456L, grid.colors[50])
        assertEquals(0xffffffffL, grid.colors[2])
    }
    @Test fun holdsCompletedImageThenClearsOnNextPixel() {
        val grid = PixelGrid()
        repeat(256) { grid.apply(PixelUpdate(it % 16, it / 16, 0xff000000L), it.toLong()) }
        assertEquals(256, grid.filled)
        grid.apply(PixelUpdate(0, 0, 0xffff0000L), 6000)
        assertEquals(1, grid.filled)
        assertEquals(0xffffffffL, grid.colors[1])
    }
    @Test fun clearsPartialImageAfterInterImagePause() {
        val grid = PixelGrid()
        grid.apply(PixelUpdate(0, 0, 0xff000000L), 0)
        grid.apply(PixelUpdate(1, 0, 0xff000000L), 5000)
        assertEquals(1, grid.filled)
        assertEquals(0xffffffffL, grid.colors[0])
    }
}
