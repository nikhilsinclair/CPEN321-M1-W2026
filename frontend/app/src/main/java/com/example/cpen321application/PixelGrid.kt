package com.example.cpen321application

internal class PixelGrid {
    val colors = LongArray(256) { 0xffffffffL }
    private val seen = BooleanArray(256)
    var filled = 0
        private set
    private var lastUpdate: Long? = null

    fun apply(pixel: PixelUpdate, elapsedMillis: Long) {
        require(pixel.x in 0..15 && pixel.y in 0..15)
        // The course pauses five seconds between images; it sends no frame marker.
        if (filled == 256 || lastUpdate?.let { elapsedMillis - it >= 4000 } == true) {
            colors.fill(0xffffffffL)
            seen.fill(false)
            filled = 0
        }
        val index = pixel.y * 16 + pixel.x
        colors[index] = pixel.argb
        if (!seen[index]) { seen[index] = true; filled++ }
        lastUpdate = elapsedMillis
    }
}
