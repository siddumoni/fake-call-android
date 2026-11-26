package com.siddumoni.fakecall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings
import com.siddumoni.fakecall.ui.AlibiCallApp
import com.siddumoni.fakecall.ui.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThemeSettingsInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appearanceAndIndependentSwitchesUpdateTheirState() {
        var settings by mutableStateOf(ThemeSettings.Default)
        composeRule.setContent {
            AlibiCallApp(
                settings = settings,
                onAppearanceChanged = { appearance ->
                    settings = settings.copy(appearance = appearance)
                },
                onPureBlackChanged = { enabled ->
                    settings = settings.copy(pureBlack = enabled)
                },
                onDynamicColorChanged = { enabled ->
                    settings = settings.copy(dynamicColor = enabled)
                },
                saveFailed = false,
                screen = AppScreen.THEME_SETTINGS,
                onOpenThemeSettings = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Dark").performClick()
        composeRule.runOnIdle {
            assertEquals(ThemeAppearance.DARK, settings.appearance)
            assertTrue(settings.dynamicColor)
            assertTrue(!settings.pureBlack)
        }
        composeRule.onNodeWithText("Dark").assertIsSelected()

        composeRule.onNodeWithText("Pure black (AMOLED)").performClick()
        composeRule.runOnIdle {
            assertTrue(settings.pureBlack)
            assertEquals(ThemeAppearance.DARK, settings.appearance)
        }

        composeRule.onNodeWithText("Dynamic colour").performClick()
        composeRule.runOnIdle {
            assertTrue(!settings.dynamicColor)
            assertTrue(settings.pureBlack)
        }
        composeRule.onNodeWithText("Dynamic colour").assertIsOn()
    }
}
