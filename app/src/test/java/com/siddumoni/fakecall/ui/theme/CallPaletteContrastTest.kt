package com.siddumoni.fakecall.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG 2.2: 4.5:1 for text and 3:1 for the icons on the Answer and Decline buttons. */
class CallPaletteContrastTest {
    @Test
    fun callScreenTextIsReadable() {
        with(CallPalette) {
            listOf(
                onBackground to background,
                onBackground to onePlusTop,
                onBackground to onePlusBottom,
                onBackground to avatar,
                secondaryText to background,
                secondaryText to onePlusTop,
                secondaryText to onePlusBottom,
                secondaryText to control,
                onControlSelected to controlSelected,
            ).forEach { (foreground, behind) ->
                val ratio = contrastRatio(foreground, behind)
                assertTrue("$foreground on $behind is $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun answerAndDeclineIconsStandOut() {
        with(CallPalette) {
            listOf(answer, decline).forEach { button ->
                val ratio = contrastRatio(onBackground, button)
                assertTrue("icon on $button is $ratio", ratio >= 3.0)
            }
        }
    }
}
