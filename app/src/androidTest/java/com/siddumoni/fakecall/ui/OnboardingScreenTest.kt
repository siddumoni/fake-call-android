package com.siddumoni.fakecall.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.siddumoni.fakecall.settings.ThemeSettings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingExplainsLocalSimulationAndContinuesOnlyAfterAcknowledgement() {
        val acknowledged = mutableStateOf(false)

        composeRule.setContent {
            AlibiCallApp(
                settings = ThemeSettings.Default,
                onAppearanceChanged = {},
                onPureBlackChanged = {},
                onDynamicColorChanged = {},
                saveFailed = false,
                onboardingSaveFailed = false,
                screen = AppScreen.ONBOARDING,
                onOpenThemeSettings = {},
                onBack = {},
                onCompleteOnboarding = { acknowledged.value = true },
            )
        }

        composeRule.onNodeWithTag("onboarding-screen").assertExists()
        composeRule
            .onNodeWithText(
                "Offline only: your profiles and schedules stay in app-private storage. " +
                    "The app does not connect to a server or send analytics.",
            ).assertExists()
        composeRule
            .onNodeWithText(
                "No real calls: AlibiCall does not dial numbers, spoof caller ID, access contacts " +
                    "or call logs, or impersonate real people or organisations.",
            ).assertExists()
        composeRule.onNodeWithTag("onboarding-continue-button").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertTrue(acknowledged.value)
        }
    }
}
