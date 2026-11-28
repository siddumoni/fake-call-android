package com.siddumoni.fakecall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.settings.CallScreenStyle
import com.siddumoni.fakecall.ui.call.CallPhase
import com.siddumoni.fakecall.ui.call.CallScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CallScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var phase by mutableStateOf(CallPhase.INCOMING)
    private var style by mutableStateOf(CallScreenStyle.GOOGLE_PHONE)
    private var answers = 0
    private var hangUps = 0
    private val speakerChanges = mutableListOf<Boolean>()
    private val tones = mutableListOf<Char>()

    private fun show(modifier: Modifier = Modifier) {
        composeRule.setContent {
            Box(modifier) {
                CallScreen(
                    style = style,
                    name = "Asha",
                    number = "555-0100",
                    phase = phase,
                    connectedAtElapsedMillis = null,
                    onAnswer = { answers++ },
                    onHangUp = { hangUps++ },
                    onSpeakerChanged = speakerChanges::add,
                    onKeyTone = tones::add,
                )
            }
        }
    }

    @Test
    fun bothStylesShowAnIncomingCallWithoutSimulationWording() {
        show()

        CallScreenStyle.entries.forEach { next ->
            composeRule.runOnIdle { style = next }
            composeRule.onNodeWithText("Asha").assertExists()
            composeRule.onNodeWithText("Incoming call").assertExists()
            composeRule.onNodeWithText("simulat", substring = true, ignoreCase = true).assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Answer").performScrollTo().performClick()
            composeRule.onNodeWithContentDescription("Decline").performScrollTo().performClick()
        }
        composeRule.runOnIdle {
            assertEquals(2, answers)
            assertEquals(2, hangUps)
        }
    }

    @Test
    fun swipingTheGoogleStyleHandleAnswersOrDeclines() {
        show()

        composeRule.onNodeWithTag("call-swipe-handle").performScrollTo().performTouchInput {
            swipeRight(startX = centerX, endX = centerX + 2_000f, durationMillis = 400)
        }
        composeRule.runOnIdle {
            assertEquals(1, answers)
            assertEquals(0, hangUps)
        }
        composeRule.runOnIdle { phase = CallPhase.ACTIVE }
        composeRule.runOnIdle { phase = CallPhase.INCOMING }
        composeRule.onNodeWithTag("call-swipe-handle").performScrollTo().performTouchInput {
            swipeLeft(startX = centerX, endX = centerX - 2_000f, durationMillis = 400)
        }
        composeRule.runOnIdle { assertEquals(1, hangUps) }
    }

    @Test
    fun googleStyleInCallButtonsWork() {
        phase = CallPhase.ACTIVE
        show()

        toggle("Mute")
        toggle("Speaker")
        composeRule.onNodeWithText("More").performScrollTo().performClick()
        composeRule.onNodeWithText("Add call").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Hold").performScrollTo().performClick()
        composeRule.onNodeWithText("On hold").assertExists()
        pressKeypad()
        composeRule.onNodeWithContentDescription("End call").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(true, false), speakerChanges)
            assertEquals(1, hangUps)
        }
    }

    @Test
    fun onePlusStyleInCallButtonsWork() {
        phase = CallPhase.ACTIVE
        style = CallScreenStyle.ONEPLUS
        show()

        toggle("Mute")
        toggle("Speaker")
        composeRule.onNodeWithText("Video call").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Hold").performScrollTo().performClick()
        composeRule.onNodeWithText("On hold").assertExists()
        pressKeypad()
        composeRule.onNodeWithText("Hide keypad").performScrollTo().performClick()
        composeRule.onNodeWithText("Mute").assertExists()
        composeRule.onNodeWithContentDescription("End call").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, hangUps) }
    }

    @Test
    fun endedCallSaysSoAndOffersNoActions() {
        phase = CallPhase.ENDED
        show()

        composeRule.onNodeWithText("Call ended").assertExists()
        composeRule.onNodeWithContentDescription("Answer").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("End call").assertDoesNotExist()
    }

    @Test
    fun incomingActionsStayReachableInShortWindowAtDoubleFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.size(width = 320.dp, height = 240.dp)) {
                    CallScreen(
                        style = CallScreenStyle.ONEPLUS,
                        name = "A Caller With A Very Long Name",
                        number = "555-0100",
                        phase = CallPhase.INCOMING,
                        connectedAtElapsedMillis = null,
                        onAnswer = { answers++ },
                        onHangUp = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithContentDescription("Answer")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, answers) }
        composeRule.onNodeWithContentDescription("Decline").performScrollTo().assertIsDisplayed()
    }

    private fun toggle(label: String) {
        composeRule.onNodeWithText(label).performScrollTo().assertIsOff()
        composeRule.onNodeWithText(label).performClick()
        composeRule.onNodeWithText(label).assertIsOn()
        if (label == "Speaker") {
            composeRule.onNodeWithText(label).performClick()
            composeRule.onNodeWithText(label).assertIsOff()
        }
    }

    private fun pressKeypad() {
        composeRule.onNodeWithText("Keypad").performScrollTo().performClick()
        composeRule.onNodeWithText("5").performScrollTo().performClick()
        composeRule.onNodeWithText("#").performScrollTo().performClick()
        composeRule.onNodeWithText("5#").assertExists()
        composeRule.runOnIdle { assertEquals(listOf('5', '#'), tones) }
    }
}
