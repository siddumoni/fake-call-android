package com.siddumoni.fakecall.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.themePreferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "theme_preferences")

class ThemePreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.themePreferencesDataStore

    val settings: Flow<ThemeSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeSettings(
                appearance = ThemeAppearance.fromStorage(preferences[APPEARANCE_KEY]),
                pureBlack = preferences[PURE_BLACK_KEY] ?: false,
                dynamicColor = preferences[DYNAMIC_COLOR_KEY] ?: true,
            )
        }

    suspend fun save(settings: ThemeSettings) {
        dataStore.edit { preferences ->
            preferences[APPEARANCE_KEY] = settings.appearance.storageValue
            preferences[PURE_BLACK_KEY] = settings.pureBlack
            preferences[DYNAMIC_COLOR_KEY] = settings.dynamicColor
        }
    }

    private companion object {
        val APPEARANCE_KEY = stringPreferencesKey("appearance")
        val PURE_BLACK_KEY = booleanPreferencesKey("pure_black")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    }
}
