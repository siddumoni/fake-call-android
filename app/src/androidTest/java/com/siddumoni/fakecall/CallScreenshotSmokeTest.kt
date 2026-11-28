package com.siddumoni.fakecall

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.settings.CallScreenStyle
import com.siddumoni.fakecall.ui.call.CallPhase
import com.siddumoni.fakecall.ui.call.CallScreen
import com.siddumoni.fakecall.ui.theme.AlibiCallTheme
import org.junit.Rule
import org.junit.Test

/** Renders both call screen styles so that their look can be reviewed from the CI artifacts. */
class CallScreenshotSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exportIncomingAndActiveScreensForBothStyles() {
        var style by mutableStateOf(CallScreenStyle.GOOGLE_PHONE)
        var phase by mutableStateOf(CallPhase.INCOMING)
        val connectedAt = SystemClock.elapsedRealtime() - 42_000L
        composeRule.setContent {
            AlibiCallTheme {
                CallScreen(
                    style = style,
                    name = "Mom",
                    number = "+91 98765 43210",
                    phase = phase,
                    connectedAtElapsedMillis = connectedAt,
                    onAnswer = {},
                    onHangUp = {},
                    modifier = Modifier.testTag("call-root"),
                )
            }
        }

        listOf(CallScreenStyle.GOOGLE_PHONE to "google", CallScreenStyle.ONEPLUS to "oneplus")
            .forEach { (nextStyle, styleName) ->
                listOf(CallPhase.INCOMING to "incoming", CallPhase.ACTIVE to "active")
                    .forEach { (nextPhase, phaseName) ->
                        composeRule.runOnIdle {
                            style = nextStyle
                            phase = nextPhase
                        }
                        composeRule.waitForIdle()
                        save(
                            "call-$styleName-$phaseName.png",
                            composeRule.onNodeWithTag("call-root").captureToImage().asAndroidBitmap(),
                        )
                    }
            }
    }

    private fun save(
        fileName: String,
        image: Bitmap,
    ) {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AlibiCall")
            }
        val uri =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create MediaStore entry for $fileName")
        resolver.openOutputStream(uri).use { output ->
            checkNotNull(output) { "Could not open output stream for $fileName" }
            check(image.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode $fileName" }
        }
    }
}
