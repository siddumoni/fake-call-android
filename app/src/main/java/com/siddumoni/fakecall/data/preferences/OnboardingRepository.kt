package com.siddumoni.fakecall.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.onboardingDataStore: DataStore<Preferences> by
    preferencesDataStore(
        name = "onboarding_preferences",
        corruptionHandler = recoverCorruptPreferences(),
    )

class OnboardingRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.onboardingDataStore

    val completed: Flow<Boolean> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[COMPLETED_KEY] ?: false
            }

    suspend fun setCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[COMPLETED_KEY] = completed
        }
    }

    private companion object {
        val COMPLETED_KEY = booleanPreferencesKey("completed")
    }
}
