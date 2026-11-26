package com.siddumoni.fakecall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings
import com.siddumoni.fakecall.ui.AlibiCallApp
import com.siddumoni.fakecall.ui.AppScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThemeScreenshotSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeAndThemeSettingsRenderInLightDarkAndAmoledAppearances() {
        var settings by mutableStateOf(
            ThemeSettings(
                appearance = ThemeAppearance.LIGHT,
                pureBlack = false,
                dynamicColor = false,
            ),
        )
        var screen by mutableStateOf(AppScreen.HOME)

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
                screen = screen,
                onOpenThemeSettings = { screen = AppScreen.THEME_SETTINGS },
                onBack = { screen = AppScreen.HOME },
            )
        }

        val appearances = listOf(
            ThemeSettings(
                appearance = ThemeAppearance.LIGHT,
                pureBlack = false,
                dynamicColor = false,
            ),
            ThemeSettings(
                appearance = ThemeAppearance.DARK,
                pureBlack = false,
                dynamicColor = false,
            ),
            ThemeSettings(
                appearance = ThemeAppearance.DARK,
                pureBlack = true,
                dynamicColor = false,
            ),
        )

        appearances.forEachIndexed { index, appearance ->
            composeRule.runOnIdle {
                settings = appearance
                screen = AppScreen.HOME
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("home-screen").assertExists()
            val homeScreenshot = composeRule
                .onNodeWithTag("app-root")
                .captureToImage()
            assertTrue("Home screenshot $index has no width", homeScreenshot.width > 0)
            assertTrue("Home screenshot $index has no height", homeScreenshot.height > 0)

            composeRule.runOnIdle {
                screen = AppScreen.THEME_SETTINGS
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("theme-settings-screen").assertExists()
            val settingsScreenshot = composeRule
                .onNodeWithTag("app-root")
                .captureToImage()
            assertTrue("Settings screenshot $index has no width", settingsScreenshot.width > 0)
            assertTrue("Settings screenshot $index has no height", settingsScreenshot.height > 0)
        }
    }
}
