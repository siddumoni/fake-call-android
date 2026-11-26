package com.siddumoni.fakecall

import java.io.File
import java.io.FileOutputStream

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
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
    fun homeAndThemeSettingsRenderAndExportInLightDarkAndAmoledAppearances() {
        var settings by mutableStateOf(ThemeSettings(appearance = ThemeAppearance.LIGHT))
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

        val appearances =
            listOf(
                "light" to
                    ThemeSettings(
                        appearance = ThemeAppearance.LIGHT,
                        pureBlack = false,
                        dynamicColor = false,
                    ),
                "dark" to
                    ThemeSettings(
                        appearance = ThemeAppearance.DARK,
                        pureBlack = false,
                        dynamicColor = false,
                    ),
                "amoled" to
                    ThemeSettings(
                        appearance = ThemeAppearance.DARK,
                        pureBlack = true,
                        dynamicColor = false,
                    ),
            )

        appearances.forEach { (appearanceName, appearance) ->
            composeRule.runOnIdle {
                settings = appearance
                screen = AppScreen.HOME
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("home-screen").assertExists()
            val homeScreenshot =
                composeRule
                    .onNodeWithTag("app-root")
                    .captureToImage()
            assertScreenshotDimensions(homeScreenshot, "home-$appearanceName")
            saveScreenshot("home-$appearanceName.png", homeScreenshot)

            composeRule.runOnIdle {
                screen = AppScreen.THEME_SETTINGS
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("theme-settings-screen").assertExists()
            val settingsScreenshot =
                composeRule
                    .onNodeWithTag("app-root")
                    .captureToImage()
            assertScreenshotDimensions(settingsScreenshot, "appearance-$appearanceName")
            saveScreenshot("appearance-$appearanceName.png", settingsScreenshot)
        }
    }

    private fun assertScreenshotDimensions(
        image: ImageBitmap,
        label: String,
    ) {
        assertTrue("$label screenshot has no width", image.width > 0)
        assertTrue("$label screenshot has no height", image.height > 0)
    }

    private fun saveScreenshot(
        fileName: String,
        image: ImageBitmap,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appFiles =
            checkNotNull(context.getExternalFilesDir(null)) {
                "App-specific external files directory is unavailable"
            }
        val screenshotDirectory = File(appFiles, "screenshots")
        check(screenshotDirectory.mkdirs() || screenshotDirectory.isDirectory) {
            "Could not create screenshot output directory"
        }

        val screenshotFile = File(screenshotDirectory, fileName)
        FileOutputStream(screenshotFile).use { output ->
            check(image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not encode screenshot $fileName"
            }
            output.flush()
        }
        assertTrue(
            "$fileName was not written",
            screenshotFile.isFile && screenshotFile.length() > 0L,
        )
    }
}
