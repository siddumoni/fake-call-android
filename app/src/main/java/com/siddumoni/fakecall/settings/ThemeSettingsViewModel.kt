package com.siddumoni.fakecall.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siddumoni.fakecall.AlibiCallApplication
import com.siddumoni.fakecall.data.preferences.ThemePreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val alibiApplication = application as AlibiCallApplication
    private val repository = ThemePreferencesRepository(application)

    private val _settings = MutableStateFlow(alibiApplication.initialThemeSettings)
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { persisted ->
                _settings.value = persisted
                alibiApplication.applyApplicationNightMode(persisted.appearance)
            }
        }
    }

    fun setAppearance(appearance: ThemeAppearance) {
        updateSettings { copy(appearance = appearance) }
    }

    fun setPureBlack(enabled: Boolean) {
        updateSettings { copy(pureBlack = enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        updateSettings { copy(dynamicColor = enabled) }
    }

    private fun updateSettings(transform: ThemeSettings.() -> ThemeSettings) {
        val updated = _settings.value.transform()
        _settings.value = updated
        alibiApplication.applyApplicationNightMode(updated.appearance)

        viewModelScope.launch {
            try {
                repository.save(updated)
                _saveFailed.value = false
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _saveFailed.value = true
            }
        }
    }
}
