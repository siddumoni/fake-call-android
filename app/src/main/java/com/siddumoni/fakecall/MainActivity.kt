package com.siddumoni.fakecall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.siddumoni.fakecall.settings.ThemeSettingsViewModel
import com.siddumoni.fakecall.ui.AlibiCallApp
import com.siddumoni.fakecall.ui.AppScreen

class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeSettingsViewModel by viewModels()
    private var currentScreen by mutableStateOf(AppScreen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentScreen = savedInstanceState
            ?.getString(STATE_CURRENT_SCREEN)
            ?.let { restored -> AppScreen.entries.firstOrNull { it.name == restored } }
            ?: AppScreen.HOME
        enableEdgeToEdge()
        setContent {
            val settings by themeViewModel.settings.collectAsState()
            val saveFailed by themeViewModel.saveFailed.collectAsState()

            BackHandler(enabled = currentScreen == AppScreen.THEME_SETTINGS) {
                currentScreen = AppScreen.HOME
            }

            AlibiCallApp(
                settings = settings,
                onAppearanceChanged = themeViewModel::setAppearance,
                onPureBlackChanged = themeViewModel::setPureBlack,
                onDynamicColorChanged = themeViewModel::setDynamicColor,
                saveFailed = saveFailed,
                screen = currentScreen,
                onOpenThemeSettings = { currentScreen = AppScreen.THEME_SETTINGS },
                onBack = { currentScreen = AppScreen.HOME },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_SCREEN, currentScreen.name)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_CURRENT_SCREEN = "current_screen"
    }
}
