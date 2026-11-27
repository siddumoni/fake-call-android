package com.siddumoni.fakecall.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallCodec
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scheduledCallsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "scheduled_calls")

/** App-private persistence for scheduled-call records. Alarm scheduling remains a separate layer. */
class ScheduledCallRepository(context: Context) {
    private val dataStore = context.applicationContext.scheduledCallsDataStore

    val calls: Flow<List<ScheduledCall>> =
        dataStore.data.map { preferences ->
            preferences[CALLS_KEY]
                .orEmpty()
                .mapNotNull(ScheduledCallCodec::decode)
                .sortedBy { it.triggerAtEpochMillis }
        }

    suspend fun upsert(call: ScheduledCall) {
        dataStore.edit { preferences ->
            val entries = preferences[CALLS_KEY].orEmpty()
                .mapNotNull(ScheduledCallCodec::decode)
                .filterNot { it.id == call.id }
                .mapTo(mutableSetOf(), ScheduledCallCodec::encode)
            entries += ScheduledCallCodec.encode(call)
            preferences[CALLS_KEY] = entries
        }
    }

    suspend fun updateState(id: String, state: ScheduledCallState): Boolean {
        var found = false
        dataStore.edit { preferences ->
            val entries = preferences[CALLS_KEY].orEmpty()
                .mapNotNull(ScheduledCallCodec::decode)
                .map { call ->
                    if (call.id == id) {
                        found = true
                        call.copy(state = state)
                    } else {
                        call
                    }
                }
                .mapTo(mutableSetOf(), ScheduledCallCodec::encode)
            preferences[CALLS_KEY] = entries
        }
        return found
    }

    suspend fun remove(id: String) {
        dataStore.edit { preferences ->
            preferences[CALLS_KEY] = preferences[CALLS_KEY].orEmpty()
                .mapNotNull(ScheduledCallCodec::decode)
                .filterNot { it.id == id }
                .mapTo(mutableSetOf(), ScheduledCallCodec::encode)
        }
    }

    suspend fun removeTerminalRecords() {
        dataStore.edit { preferences ->
            preferences[CALLS_KEY] = preferences[CALLS_KEY].orEmpty()
                .mapNotNull(ScheduledCallCodec::decode)
                .filter { it.state == ScheduledCallState.SCHEDULED || it.state == ScheduledCallState.RINGING || it.state == ScheduledCallState.IN_CALL }
                .mapTo(mutableSetOf(), ScheduledCallCodec::encode)
        }
    }

    private companion object {
        val CALLS_KEY = stringSetPreferencesKey("records")
    }
}
