package com.siddumoni.fakecall.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.siddumoni.fakecall.callers.CallerPreset
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SimulatedCallScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun incomingPreviewIsExplicitlyLocalAndAnswerIsOnlyUiState() {
        var answered = false
        var ended = false
        composeRule.setContent {
            SimulatedCallScreen(
                caller = CallerPreset("id", "Fictional Caller", "555-0100"),
                answered = false,
                onAnswer = { answered = true },
                onEnd = { ended = true },
            )
        }
        composeRule.onNodeWithText("SIMULATED CALL • NO REAL CALL").assertExists()
        composeRule.onNodeWithText("Fictional Caller").assertExists()
        composeRule.onNodeWithText("Answer").performClick()
        composeRule.runOnIdle { assertTrue(answered) }
        composeRule.onNodeWithText("Decline").performClick()
        composeRule.runOnIdle { assertTrue(ended) }
    }

    @Test
    fun answeredPreviewShowsEndSimulationAction() {
        composeRule.setContent {
            SimulatedCallScreen(
                caller = CallerPreset("id", "Fictional Caller", ""),
                answered = true,
                onAnswer = {},
                onEnd = {},
            )
        }
        composeRule.onNodeWithText("Simulation answered").assertExists()
        composeRule.onNodeWithText("End simulation").assertExists()
    }
}
