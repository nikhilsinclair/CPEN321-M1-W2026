package com.example.cpen321application

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

@Composable
fun TimerSurpriseScreen() {
    var minutes by rememberSaveable { mutableStateOf("0") }
    var seconds by rememberSaveable { mutableStateOf("10") }
    var deadline by rememberSaveable { mutableStateOf<Long?>(null) }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var remaining by remember { mutableStateOf(0L) }

    LaunchedEffect(deadline) {
        val end = deadline ?: return@LaunchedEffect
        do {
            remaining = remainingSeconds(end, SystemClock.elapsedRealtime())
            if (remaining == 0L) {
                deadline = null
                reveal = true
                break
            }
            delay(100)
        } while (true)
    }

    if (reveal) {
        PenaltyGame(onNewTimer = { reveal = false })
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Set a countdown. A surprise is waiting at the final whistle.")
            if (deadline == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) minutes = it },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) seconds = it },
                        label = { Text("Seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    val duration = timerDurationMillis(minutes, seconds)
                    if (duration == null) {
                        error = "Enter 0–999 minutes and 0–59 seconds, with a total above zero."
                    } else {
                        error = null
                        remaining = duration / 1000
                        deadline = SystemClock.elapsedRealtime() + duration
                    }
                }) { Text("Start timer") }
            } else {
                Text(
                    String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.displayLarge
                )
                Text("Your surprise opens when the countdown reaches zero.")
                OutlinedButton(onClick = { deadline = null }) { Text("Cancel timer") }
            }
        }
    }
}

@Composable
private fun PenaltyGame(onNewTimer: () -> Unit) {
    var shots by rememberSaveable { mutableStateOf(0) }
    var goals by rememberSaveable { mutableStateOf(0) }
    var shooting by rememberSaveable { mutableStateOf(false) }
    var targetX by rememberSaveable { mutableStateOf(0.5f) }
    var targetY by rememberSaveable { mutableStateOf(0.5f) }
    var keeperX by rememberSaveable { mutableStateOf(0.5f) }
    var keeperY by rememberSaveable { mutableStateOf(0.6f) }
    var result by rememberSaveable { mutableStateOf("") }
    val flight = remember { Animatable(0f) }

    LaunchedEffect(shooting) {
        if (shooting) {
            flight.snapTo(0f)
            flight.animateTo(1f, tween(850))
            val saved = isPenaltySaved(targetX, targetY, keeperX, keeperY)
            shots++
            if (!saved) goals++
            result = if (saved) "SAVED! The keeper got there." else "GOAL! Into the net!"
            // Hold the outcome briefly, then reset the pitch before accepting another shot.
            delay(1200)
            flight.snapTo(0f)
            keeperX = 0.5f
            keeperY = 0.6f
            shooting = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Surprise! Penalty shootout", style = MaterialTheme.typography.titleLarge)
        Text("Five shots. One keeper. Can you score five?")
        Text("Goals: $goals  •  Shots: $shots / 5", style = MaterialTheme.typography.titleMedium)
        Text(when {
            shooting && result.isNotEmpty() -> "Getting ready for the next penalty…"
            shooting -> "Here comes the shot…"
            shots == 5 -> "Full time! You scored $goals out of 5."
            else -> "Tap inside the goal to aim and shoot."
        })
        val progress = if (shooting) flight.value else 0f
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(0.95f)
                    .semantics { contentDescription = "Soccer penalty pitch. Tap inside the white goal to shoot." }
                    .pointerInput(shooting, shots) {
                        detectTapGestures { tap ->
                            val x = (tap.x / size.width - 0.08f) / 0.84f
                            val y = (tap.y / size.height - 0.12f) / 0.38f
                            if (!shooting && shots < 5 && x in 0f..1f && y in 0f..1f) {
                                targetX = x
                                targetY = y
                                keeperX = Random.nextFloat() * 0.8f + 0.1f
                                keeperY = Random.nextFloat() * 0.65f + 0.2f
                                result = ""
                                shooting = true
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                drawRect(Color(0xff176b43))
                repeat(8) { stripe ->
                    if (stripe % 2 == 0) drawRect(Color(0xff207c4e), Offset(0f, stripe * h / 8), Size(w, h / 8))
                }
                val left = w * 0.08f
                val top = h * 0.12f
                val goalW = w * 0.84f
                val goalH = h * 0.38f
                drawRect(Color(0xff123d35), Offset(left, top), Size(goalW, goalH))
                for (i in 0..12) drawLine(Color(0xff547b72), Offset(left + goalW * i / 12, top), Offset(left + goalW * i / 12, top + goalH), 1.dp.toPx())
                for (i in 0..5) drawLine(Color(0xff547b72), Offset(left, top + goalH * i / 5), Offset(left + goalW, top + goalH * i / 5), 1.dp.toPx())
                drawRect(Color.White, Offset(left, top), Size(goalW, goalH), style = Stroke(4.dp.toPx()))
                drawRect(Color.White.copy(alpha = 0.5f), Offset(w * 0.04f, h * 0.5f), Size(w * 0.92f, h * 0.42f), style = Stroke(2.dp.toPx()))
                val dive = (progress * 1.2f).coerceAtMost(1f)
                val keeper = Offset(left + goalW * (0.5f + (keeperX - 0.5f) * dive), top + goalH * (0.65f + (keeperY - 0.65f) * dive))
                val body = w * 0.045f
                drawLine(Color(0xffffc857), keeper + Offset(0f, -body), keeper + Offset(0f, body), body)
                drawCircle(Color(0xfff1c6a5), body * 0.7f, keeper + Offset(0f, -body * 1.9f))
                drawLine(Color(0xffffc857), keeper + Offset(-body * 2.3f, -body), keeper + Offset(body * 2.3f, -body), body * 0.55f)
                drawCircle(Color.White, body * 0.45f, keeper + Offset(-body * 2.3f, -body))
                drawCircle(Color.White, body * 0.45f, keeper + Offset(body * 2.3f, -body))
                drawLine(Color(0xff172d4b), keeper, keeper + Offset(-body, body * 2.5f), body * 0.6f)
                drawLine(Color(0xff172d4b), keeper, keeper + Offset(body, body * 2.5f), body * 0.6f)
                val start = Offset(w * 0.5f, h * 0.82f)
                val end = Offset(left + goalW * targetX, top + goalH * targetY)
                val ball = start + (end - start) * progress
                val radius = w * (0.038f - progress * 0.016f)
                drawCircle(Color.Black.copy(alpha = 0.2f), radius, ball + Offset(3f, 6f))
                drawCircle(Color.White, radius, ball)
                drawCircle(Color(0xff182632), radius * 0.4f, ball)

            }
        if (result.isNotEmpty()) Text(result, style = MaterialTheme.typography.titleMedium)
        if (shots == 5) {
            Button(enabled = !shooting, onClick = {
                shots = 0; goals = 0; result = ""; keeperX = 0.5f; keeperY = 0.6f
            }) { Text("Play again") }
            if (!shooting) WeeklyScoresPanel()
        }
        OutlinedButton(enabled = !shooting, onClick = onNewTimer) { Text("Set another timer") }
    }
}
