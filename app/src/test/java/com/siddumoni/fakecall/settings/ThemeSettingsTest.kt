package com.siddumoni.fakecall.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSettingsTest {
    @Test
    fun defaultAppearanceFollowsSystemWithIndependentOptionsOff() {
        assertEquals(
            ThemeSettings(
                appearance = ThemeAppearance.FOLLOW_SYSTEM,
                pureBlack = false,
                dynamicColor = true,
            ),
            ThemeSettings.Default,
        )
    }

    @Test
    fun appearanceStorageRoundTripsAllOptions() {
        ThemeAppearance.entries.forEach { appearance ->
            assertEquals(
                appearance,
                ThemeAppearance.fromStorage(appearance.storageValue),
            )
        }
    }

    @Test
    fun unknownAppearanceFallsBackToFollowSystem() {
        assertEquals(
            ThemeAppearance.FOLLOW_SYSTEM,
            ThemeAppearance.fromStorage("unknown"),
        )
        assertEquals(
            ThemeAppearance.FOLLOW_SYSTEM,
            ThemeAppearance.fromStorage(null),
        )
    }

    @Test
    fun pureBlackAndDynamicColorAreIndependentOfAppearance() {
        val settings = ThemeSettings(
            appearance = ThemeAppearance.LIGHT,
            pureBlack = true,
            dynamicColor = false,
        )

        assertEquals(ThemeAppearance.LIGHT, settings.appearance)
        assertTrue(settings.pureBlack)
        assertFalse(settings.dynamicColor)
    }
}
