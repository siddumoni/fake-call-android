package com.siddumoni.fakecall.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorContrastTest {
    @Test
    fun staticThemeTextRolesMeetWcagAaForNormalText() {
        AccessibilityColorSchemes.forEachIndexed { index, scheme ->
            val foregroundBackgroundPairs =
                listOf(
                    "primary" to (scheme.onPrimary to scheme.primary),
                    "primary container" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                    "secondary" to (scheme.onSecondary to scheme.secondary),
                    "secondary container" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
                    "tertiary" to (scheme.onTertiary to scheme.tertiary),
                    "tertiary container" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
                    "error" to (scheme.onError to scheme.error),
                    "error container" to (scheme.onErrorContainer to scheme.errorContainer),
                    "background" to (scheme.onBackground to scheme.background),
                    "surface" to (scheme.onSurface to scheme.surface),
                    "surface variant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
                    "inverse surface" to (scheme.inverseOnSurface to scheme.inverseSurface),
                )

            foregroundBackgroundPairs.forEach { (role, colors) ->
                val ratio = contrastRatio(colors.first, colors.second)
                assertTrue(
                    "Scheme $index role $role has contrast ratio $ratio",
                    ratio >= 4.5,
                )
            }
        }
    }

    @Test
    fun amoledSchemeUsesPureBlackMainSurfaces() {
        val amoled = AccessibilityColorSchemes.last()
        assertTrue(amoled.background == Color.Black)
        assertTrue(amoled.surface == Color.Black)
        assertTrue(amoled.surfaceBright == Color.Black)
        assertTrue(amoled.surfaceDim == Color.Black)
        assertTrue(amoled.surfaceContainerLowest == Color.Black)
        assertTrue(amoled.surfaceContainerLow == Color.Black)
        assertTrue(amoled.surfaceContainer == Color.Black)
        assertTrue(amoled.surfaceContainerHigh == Color.Black)
        assertTrue(amoled.surfaceContainerHighest == Color.Black)
        assertTrue(amoled.surfaceTint == Color.Black)
        assertTrue(amoled.surfaceVariant == Color.Black)
    }
}
