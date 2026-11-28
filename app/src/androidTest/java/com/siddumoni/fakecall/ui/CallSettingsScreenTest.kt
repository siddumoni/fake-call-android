package com.siddumoni.fakecall.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.settings.CallScreenStyle
import com.siddumoni.fakecall.settings.CallSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CallSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun userChoosesStyleCallerDelayAndOpensTheSystemSettings() {
        var style: CallScreenStyle? = null
        var caller: String? = null
        var delay: Int? = null
        var fullScreen: Boolean? = null
        var quickCallStarted = false
        composeRule.setContent {
            CallSettingsScreen(
                settings = CallSettings(),
                callers = listOf(CallerPreset("a", "Asha", ""), CallerPreset("b", "Ben", "")),
                access = CallAccess(),
                saveFailed = false,
                onStyleChanged = { style = it },
                onFullScreenChanged = { fullScreen = it },
                onTriggerCallerChanged = { caller = it },
                onTriggerDelayChanged = { delay = it },
                onPreview = {},
                onRequestNotifications = {},
                onOpenFullScreenSettings = {},
                onStartQuickCall = { quickCallStarted = true },
            )
        }

        composeRule.onNodeWithText("OnePlus dialer").performScrollTo().performClick()
        composeRule.onNodeWithText("Full-screen incoming call").performScrollTo().performClick()
        composeRule.onNodeWithText("Schedule simulated call").performScrollTo().performClick()
        composeRule.onNodeWithText("Ben").performScrollTo().performClick()
        composeRule.onNodeWithTag("delay-minutes").performScrollTo().performTextReplacement("2")
        composeRule.onNodeWithTag("delay-seconds").performScrollTo().performTextReplacement("15")
        composeRule.onNodeWithText("Save delay").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(CallScreenStyle.ONEPLUS, style)
            assertEquals(true, fullScreen)
            assertEquals(true, quickCallStarted)
            assertEquals("b", caller)
            assertEquals(135, delay)
        }
        composeRule.onNodeWithTag("delay-minutes").performScrollTo().performTextReplacement("0")
        composeRule.onNodeWithTag("delay-seconds").performScrollTo().performTextReplacement("0")
        composeRule.onNodeWithText("Save delay").performScrollTo().assertIsNotEnabled()
    }
}
