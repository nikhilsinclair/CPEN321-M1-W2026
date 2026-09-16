package com.example.cpen321application

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class PenaltyGameTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun timerRevealsGameAndFiveShotsAllowReplay() {
        compose.onNodeWithText("Timer + Surprise").performClick()
        compose.onNodeWithText("Seconds").performTextReplacement("1")
        compose.onNodeWithText("Start timer").performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Surprise! Penalty shootout").fetchSemanticsNodes().isNotEmpty()
        }
        val pitch = compose.onNodeWithContentDescription("Soccer penalty pitch. Tap inside the white goal to shoot.")
        repeat(5) { shot ->
            pitch.performTouchInput { click(Offset(width * 0.5f, height * 0.3f)) }
            compose.waitUntil(6000) {
                compose.onAllNodes(hasText("Shots: ${shot + 1} / 5", substring = true)).fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText("Getting ready for the next penalty…").fetchSemanticsNodes().isEmpty() &&
                    compose.onAllNodesWithText("Here comes the shot…").fetchSemanticsNodes().isEmpty()
            }
        }
        compose.onNodeWithText("This week’s scores").assertExists()
        compose.onNodeWithText("Play again").performScrollTo().performClick()
        pitch.assertExists()
        compose.onNodeWithText("Goals: 0  •  Shots: 0 / 5").assertExists()
    }
}
