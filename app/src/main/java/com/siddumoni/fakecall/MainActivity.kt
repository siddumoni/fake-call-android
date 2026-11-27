package com.siddumoni.fakecall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.callers.CallerPresetsViewModel
import com.siddumoni.fakecall.onboarding.OnboardingViewModel
import com.siddumoni.fakecall.settings.ThemeSettingsViewModel
import com.siddumoni.fakecall.ui.AlibiCallApp
import com.siddumoni.fakecall.ui.AppScreen

class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeSettingsViewModel by viewModels()
    private val callerPresetsViewModel: CallerPresetsViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private var currentScreen by mutableStateOf(AppScreen.HOME)
    private var selectedCaller by mutableStateOf<CallerPreset?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialOnboardingComplete = (application as AlibiCallApplication).initialOnboardingCompleted
        val restoredScreen =
            savedInstanceState
                ?.getString(STATE_CURRENT_SCREEN)
                ?.let { restored -> AppScreen.entries.firstOrNull { it.name == restored } }
        currentScreen =
            if (initialOnboardingComplete) {
                restoredScreen
                    ?.takeUnless { restored -> restored == AppScreen.ONBOARDING }
                    ?: AppScreen.HOME
            } else {
                AppScreen.ONBOARDING
            }
        selectedCaller =
            savedInstanceState?.let {
                val id = it.getString(STATE_CALLER_ID)
                val name = it.getString(STATE_CALLER_NAME)
                val number = it.getString(STATE_CALLER_NUMBER)
                if (id != null && name != null && number != null) CallerPreset(id, name, number) else null
            }
        enableEdgeToEdge()
        setContent {
            val settings by themeViewModel.settings.collectAsState()
            val saveFailed by themeViewModel.saveFailed.collectAsState()
            val callerPresets by callerPresetsViewModel.presets.collectAsState()
            val callerPresetSaveError by callerPresetsViewModel.saveError.collectAsState()
            val onboardingCompleted by onboardingViewModel.completed.collectAsState()
            val onboardingSaveFailed by onboardingViewModel.saveFailed.collectAsState()

            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted && currentScreen == AppScreen.ONBOARDING) {
                    currentScreen = AppScreen.HOME
                }
            }

            BackHandler(
                enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.ONBOARDING,
            ) {
                currentScreen = AppScreen.HOME
                selectedCaller = null
            }

            AlibiCallApp(
                settings = settings,
                onAppearanceChanged = themeViewModel::setAppearance,
                onPureBlackChanged = themeViewModel::setPureBlack,
                onDynamicColorChanged = themeViewModel::setDynamicColor,
                saveFailed = saveFailed,
                onboardingSaveFailed = onboardingSaveFailed,
                onCompleteOnboarding = onboardingViewModel::complete,
                screen = currentScreen,
                onOpenThemeSettings = { currentScreen = AppScreen.THEME_SETTINGS },
                onOpenCallerPresets = { currentScreen = AppScreen.CALLER_PRESETS },
                onBack = { currentScreen = AppScreen.HOME },
                callerPresets = callerPresets,
                callerPresetSaveError = callerPresetSaveError,
                onSaveCallerPreset = callerPresetsViewModel::savePreset,
                onDeleteCallerPreset = callerPresetsViewModel::deletePreset,
                selectedCaller = selectedCaller,
                onPreviewCaller = { caller ->
                    selectedCaller = caller
                    currentScreen = AppScreen.SIMULATED_CALL
                },
                onAnswerSimulatedCall = { currentScreen = AppScreen.SIMULATED_IN_CALL },
                onEndSimulatedCall = {
                    selectedCaller = null
                    currentScreen = AppScreen.HOME
                },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_SCREEN, currentScreen.name)
        selectedCaller?.let { caller ->
            outState.putString(STATE_CALLER_ID, caller.id)
            outState.putString(STATE_CALLER_NAME, caller.displayName)
            outState.putString(STATE_CALLER_NUMBER, caller.numberLabel)
        }
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_CURRENT_SCREEN = "current_screen"
        const val STATE_CALLER_ID = "selected_caller_id"
        const val STATE_CALLER_NAME = "selected_caller_name"
        const val STATE_CALLER_NUMBER = "selected_caller_number"
    }
}
