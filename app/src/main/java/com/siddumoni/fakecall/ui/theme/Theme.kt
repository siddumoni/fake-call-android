package com.siddumoni.fakecall.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings

private val AlibiLightColors =
    lightColorScheme(
        primary = Color(0xFF39316F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE8E2FF),
        onPrimaryContainer = Color(0xFF211A58),
        secondary = Color(0xFF5A536E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5DFF4),
        onSecondaryContainer = Color(0xFF211B32),
        tertiary = Color(0xFF67405F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD7F1),
        onTertiaryContainer = Color(0xFF2B1028),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBF8FF),
        onBackground = Color(0xFF1B1B22),
        surface = Color(0xFFFBF8FF),
        onSurface = Color(0xFF1B1B22),
        surfaceVariant = Color(0xFFE7E1F0),
        onSurfaceVariant = Color(0xFF494452),
        outline = Color(0xFF797386),
        outlineVariant = Color(0xFFCAC4D4),
        inverseSurface = Color(0xFF303038),
        inverseOnSurface = Color(0xFFF2EFF7),
        inversePrimary = Color(0xFFC9C1FF),
    )

private val AlibiDarkColors =
    darkColorScheme(
        primary = Color(0xFFCDC3FF),
        onPrimary = Color(0xFF2C236A),
        primaryContainer = Color(0xFF453C80),
        onPrimaryContainer = Color(0xFFE8E1FF),
        secondary = Color(0xFFC9C1D9),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4359),
        onSecondaryContainer = Color(0xFFE6DEF6),
        tertiary = Color(0xFFF0B7E7),
        onTertiary = Color(0xFF4B264B),
        tertiaryContainer = Color(0xFF633D60),
        onTertiaryContainer = Color(0xFFFFD7F4),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131019),
        onBackground = Color(0xFFECE6F4),
        surface = Color(0xFF131019),
        onSurface = Color(0xFFECE6F4),
        surfaceVariant = Color(0xFF484253),
        onSurfaceVariant = Color(0xFFCDC5D6),
        outline = Color(0xFF958DA0),
        outlineVariant = Color(0xFF484253),
        inverseSurface = Color(0xFFECE6F4),
        inverseOnSurface = Color(0xFF302D36),
        inversePrimary = Color(0xFF51488D),
    )

private val AlibiAmoledColors =
    AlibiDarkColors.copy(
        background = Color(0xFF000000),
        onBackground = Color(0xFFF0EBF7),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF0EBF7),
        surfaceBright = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF000000),
        surfaceContainer = Color(0xFF000000),
        surfaceContainerHigh = Color(0xFF16141B),
        surfaceContainerHighest = Color(0xFF1E1B24),
        surfaceTint = Color(0xFF000000),
        surfaceVariant = Color(0xFF000000),
        onSurfaceVariant = Color(0xFFD0C9D8),
        outline = Color(0xFF938B9D),
        outlineVariant = Color(0xFF36313F),
        inverseSurface = Color(0xFFF0EBF7),
        inverseOnSurface = Color(0xFF302D36),
    )

@Composable
fun AlibiCallTheme(
    settings: ThemeSettings = ThemeSettings.Default,
    content: @Composable () -> Unit,
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme =
        when (settings.appearance) {
            ThemeAppearance.FOLLOW_SYSTEM -> systemDarkTheme
            ThemeAppearance.LIGHT -> false
            ThemeAppearance.DARK -> true
        }
    val context = LocalContext.current
    val colorScheme =
        remember(settings, darkTheme, context) {
            when {
                !darkTheme -> {
                    if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        dynamicLightColorScheme(context)
                    } else {
                        AlibiLightColors
                    }
                }
                settings.pureBlack -> AlibiAmoledColors
                settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    dynamicDarkColorScheme(context)
                else -> AlibiDarkColors
            }.withAccessibleText()
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

internal val AccessibilityColorSchemes =
    listOf(
        AlibiLightColors,
        AlibiDarkColors,
        AlibiAmoledColors,
    )

internal fun ColorScheme.withAccessibleText(): ColorScheme =
    copy(
        onPrimary = readableTextColor(primary, onPrimary),
        onPrimaryContainer = readableTextColor(primaryContainer, onPrimaryContainer),
        onSecondary = readableTextColor(secondary, onSecondary),
        onSecondaryContainer = readableTextColor(secondaryContainer, onSecondaryContainer),
        onTertiary = readableTextColor(tertiary, onTertiary),
        onTertiaryContainer = readableTextColor(tertiaryContainer, onTertiaryContainer),
        onError = readableTextColor(error, onError),
        onErrorContainer = readableTextColor(errorContainer, onErrorContainer),
        onBackground = readableTextColor(background, onBackground),
        onSurface = readableTextColor(surface, onSurface),
        onSurfaceVariant = readableTextColor(surfaceVariant, onSurfaceVariant),
    )

internal fun contrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val first = foreground.luminance().toDouble()
    val second = background.luminance().toDouble()
    val lighter = maxOf(first, second)
    val darker = minOf(first, second)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun readableTextColor(
    background: Color,
    preferred: Color,
): Color {
    if (contrastRatio(preferred, background) >= 4.5) return preferred

    val blackContrast = contrastRatio(Color.Black, background)
    val whiteContrast = contrastRatio(Color.White, background)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

/** Fixed colours of the call screens. Like phone apps, they do not follow the app theme. */
object CallPalette {
    val background = Color(0xFF101418)
    val onBackground = Color(0xFFFFFFFF)
    val secondaryText = Color(0xFFC3C7CF)
    val control = Color(0xFF2A2F36)
    val controlSelected = Color(0xFFE2E6EE)
    val onControlSelected = Color(0xFF101418)
    val disabled = Color(0xFF7C8189)
    val avatar = Color(0xFF3F4A5C)
    val answer = Color(0xFF1E8E3E)
    val decline = Color(0xFFD93025)
    val onePlusTop = Color(0xFF1C2330)
    val onePlusBottom = Color(0xFF05070A)
}
