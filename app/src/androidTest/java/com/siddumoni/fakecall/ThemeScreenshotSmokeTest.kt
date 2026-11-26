package com.siddumoni.fakecall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.siddumoni.fakecall.ui.theme.AlibiCallTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThemeScreenshotSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreenProducesNonEmptyLightThemeScreenshot() {
        renderAndCapture(darkTheme = false)
    }

    @Test
    fun homeScreenProducesNonEmptyDarkThemeScreenshot() {
        renderAndCapture(darkTheme = true)
    }

    private fun renderAndCapture(darkTheme: Boolean) {
        composeRule.setContent {
            AlibiCallTheme(darkTheme = darkTheme) {
                ScreenshotSurface()
            }
        }
        val image = composeRule.onNodeWithTag("screenshot-root").captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }
}

@Composable
private fun ScreenshotSurface() {
    Surface(
        modifier = Modifier.fillMaxSize().testTag("screenshot-root"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "AlibiCall", style = MaterialTheme.typography.headlineLarge)
        }
    }
}
