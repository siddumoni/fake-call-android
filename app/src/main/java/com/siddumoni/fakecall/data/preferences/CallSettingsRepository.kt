package com.siddumoni.fakecall.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.siddumoni.fakecall.settings.CallScreenStyle
import com.siddumoni.fakecall.settings.CallSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.callSettingsDataStore: DataStore<Preferences> by
    preferencesDataStore(
        name = "call_settings",
        corruptionHandler = recoverCorruptPreferences(),
    )

class CallSettingsRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.callSettingsDataStore

    val settings: Flow<CallSettings> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                CallSettings(
                    style =
                        CallScreenStyle.entries.firstOrNull { it.name == preferences[STYLE_KEY] }
                            ?: CallSettings().style,
                    fullScreen = preferences[FULL_SCREEN_KEY] ?: false,
                    triggerCallerId = preferences[TRIGGER_CALLER_KEY],
                    triggerDelaySeconds =
                        CallSettings.clampDelay(preferences[TRIGGER_DELAY_KEY] ?: CallSettings().triggerDelaySeconds),
                )
            }

    suspend fun setStyle(style: CallScreenStyle) {
        dataStore.edit { it[STYLE_KEY] = style.name }
    }

    suspend fun setFullScreen(enabled: Boolean) {
        dataStore.edit { it[FULL_SCREEN_KEY] = enabled }
    }

    suspend fun setTriggerCaller(callerId: String) {
        dataStore.edit { it[TRIGGER_CALLER_KEY] = callerId }
    }

    suspend fun setTriggerDelaySeconds(seconds: Int) {
        dataStore.edit { it[TRIGGER_DELAY_KEY] = CallSettings.clampDelay(seconds) }
    }

    private companion object {
        val STYLE_KEY = stringPreferencesKey("style")
        val FULL_SCREEN_KEY = booleanPreferencesKey("full_screen")
        val TRIGGER_CALLER_KEY = stringPreferencesKey("trigger_caller")
        val TRIGGER_DELAY_KEY = intPreferencesKey("trigger_delay_seconds")
    }
}
