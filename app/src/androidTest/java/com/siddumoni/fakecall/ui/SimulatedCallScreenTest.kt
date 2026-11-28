package com.siddumoni.fakecall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
        composeRule.onNodeWithText("Answer").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(answered) }
        composeRule.onNodeWithText("Decline").performScrollTo().performClick()
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

    @Test
    fun actionsStayReachableInShortWindowAtDoubleFontScale() {
        var answered = false
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.size(width = 320.dp, height = 240.dp)) {
                    SimulatedCallScreen(
                        caller = CallerPreset("id", "Fictional Caller With A Very Long Name", "555-0100"),
                        answered = false,
                        onAnswer = { answered = true },
                        onEnd = {},
                    )
                }
            }
        }
        composeRule
            .onNodeWithText("Answer")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertTrue(answered) }
        composeRule.onNodeWithText("Decline").performScrollTo().assertIsDisplayed()
    }
}
