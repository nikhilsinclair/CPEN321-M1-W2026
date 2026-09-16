package com.example.cpen321application

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PixelArtScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var colors by remember { mutableStateOf(List(256) { Color.White }) }
    var filled by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Connecting…") }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                val grid = PixelGrid()
                colors = List(256) { Color.White }
                filled = 0
                status = "Connecting…"
                try {
                    PixelStream.updates(BuildConfig.API_BASE_URL).collect { pixel ->
                        grid.apply(pixel, SystemClock.elapsedRealtime())
                        colors = grid.colors.map { Color(it.toInt()) }
                        filled = grid.filled
                        status = if (filled == 256) "Image complete — waiting for the next one…" else "Receiving live pixels"
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    status = "Connection interrupted. Reconnecting…"
                }
                delay(3000)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(status)
        Canvas(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                .semantics { contentDescription = "Live pixel art, $filled of 256 cells received" }
        ) {
            val cellWidth = size.width / 16
            val cellHeight = size.height / 16
            for (y in 0..15) for (x in 0..15) {
                drawRect(colors[y * 16 + x], Offset(x * cellWidth, y * cellHeight), Size(cellWidth, cellHeight))
            }
            for (line in 0..16) {
                drawLine(Color.LightGray, Offset(line * cellWidth, 0f), Offset(line * cellWidth, size.height), 1f)
                drawLine(Color.LightGray, Offset(0f, line * cellHeight), Offset(size.width, line * cellHeight), 1f)
            }
        }
        Text("$filled / 256 cells received")
        Text("New images appear automatically. Watch the canvas fill in.")
    }
}
