package com.siddumoni.fakecall.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.callers.CallerPresetCodec
import com.siddumoni.fakecall.callers.CallerPresetValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import java.util.UUID

private val Context.callerPresetsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "caller_presets")

class CallerPresetRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.callerPresetsDataStore

    val presets: Flow<List<CallerPreset>> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[PRESETS_KEY]
                    .orEmpty()
                    .mapNotNull(CallerPresetCodec::decode)
                    .sortedBy { it.displayName.lowercase(Locale.ROOT) }
            }

    suspend fun save(
        id: String?,
        displayName: String,
        numberLabel: String,
    ): CallerPreset {
        val (name, number) = CallerPresetValidator.normalize(displayName, numberLabel)
        val preset =
            CallerPreset(
                id = id?.let { UUID.fromString(it).toString() } ?: UUID.randomUUID().toString(),
                displayName = name,
                numberLabel = number,
            )

        dataStore.edit { preferences ->
            val updated =
                preferences[PRESETS_KEY]
                    .orEmpty()
                    .mapNotNull(CallerPresetCodec::decode)
                    .filterNot { it.id == preset.id }
                    .mapTo(mutableSetOf(), CallerPresetCodec::encode)
            updated += CallerPresetCodec.encode(preset)
            preferences[PRESETS_KEY] = updated
        }
        return preset
    }

    suspend fun delete(id: String) {
        dataStore.edit { preferences ->
            val updated =
                preferences[PRESETS_KEY]
                    .orEmpty()
                    .mapNotNull(CallerPresetCodec::decode)
                    .filterNot { it.id == id }
                    .mapTo(mutableSetOf(), CallerPresetCodec::encode)
            preferences[PRESETS_KEY] = updated
        }
    }

    private companion object {
        val PRESETS_KEY = stringSetPreferencesKey("entries")
    }
}
