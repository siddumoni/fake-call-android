package com.siddumoni.fakecall

import android.app.Application
import android.app.UiModeManager
import com.siddumoni.fakecall.data.preferences.OnboardingRepository
import com.siddumoni.fakecall.data.preferences.ThemePreferencesRepository
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AlibiCallApplication : Application() {
    lateinit var initialThemeSettings: ThemeSettings
        private set

    var initialOnboardingCompleted: Boolean = false
        private set

    fun markOnboardingCompleted() {
        initialOnboardingCompleted = true
    }

    override fun onCreate() {
        super.onCreate()
        val themeRepository = ThemePreferencesRepository(this)
        val onboardingRepository = OnboardingRepository(this)
        runBlocking(Dispatchers.IO) {
            initialThemeSettings = themeRepository.settings.first()
            initialOnboardingCompleted = onboardingRepository.completed.first()
        }
        applyApplicationNightMode(initialThemeSettings.appearance)
    }

    fun applyApplicationNightMode(appearance: ThemeAppearance) {
        // For application night mode, AUTO maps to an undefined package override,
        // allowing the system's current dark-mode configuration to flow through.
        val mode =
            when (appearance) {
                ThemeAppearance.FOLLOW_SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeAppearance.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeAppearance.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
    }
}
