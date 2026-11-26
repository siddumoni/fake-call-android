package com.siddumoni.fakecall

import android.app.Application
import android.app.UiModeManager
import com.siddumoni.fakecall.data.preferences.ThemePreferencesRepository
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AlibiCallApplication : Application() {
    lateinit var initialThemeSettings: ThemeSettings
        private set

    override fun onCreate() {
        super.onCreate()
        val repository = ThemePreferencesRepository(this)
        initialThemeSettings = runBlocking(Dispatchers.IO) {
            repository.settings.first()
        }
        applyApplicationNightMode(initialThemeSettings.appearance)
    }

    fun applyApplicationNightMode(appearance: ThemeAppearance) {
        val mode = when (appearance) {
            ThemeAppearance.FOLLOW_SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeAppearance.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeAppearance.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
    }
}
