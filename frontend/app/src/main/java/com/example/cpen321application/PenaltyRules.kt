package com.example.cpen321application

import kotlin.math.ceil

internal fun timerDurationMillis(minutes: String, seconds: String): Long? {
    val m = minutes.toIntOrNull() ?: return null
    val s = seconds.toIntOrNull() ?: return null
    if (m !in 0..999 || s !in 0..59 || m * 60 + s == 0) return null
    return (m * 60L + s) * 1000L
}

internal fun remainingSeconds(deadline: Long, now: Long): Long =
    ceil((deadline - now).coerceAtLeast(0).toDouble() / 1000).toLong()

internal fun isPenaltySaved(x: Float, y: Float, keeperX: Float, keeperY: Float): Boolean {
    val dx = (x - keeperX) / 0.23f
    val dy = (y - keeperY) / 0.32f
    return dx * dx + dy * dy <= 1f
}
