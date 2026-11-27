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
    preferencesDataStore(
        name = "scheduled_calls",
    )

/** App-private persistence for scheduled-call records. Alarm scheduling remains a separate layer. */
class ScheduledCallRepository(context: Context) {
    private val dataStore = context.applicationContext.scheduledCallsDataStore

    val calls: Flow<List<ScheduledCall>> =
        dataStore.data.map { preferences ->
            preferences[CALLS_KEY]
                .orEmpty()
                .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                .sortedBy { call -> call.triggerAtEpochMillis }
        }

    suspend fun upsert(call: ScheduledCall) {
        dataStore.edit { preferences ->
            val entries = mutableSetOf<String>()
            entries.addAll(
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .filterNot { existing -> existing.id == call.id }
                    .map { existing -> ScheduledCallCodec.encode(existing) },
            )
            entries.add(ScheduledCallCodec.encode(call))
            preferences[CALLS_KEY] = entries
        }
    }

    suspend fun updateState(
        id: String,
        state: ScheduledCallState,
    ): Boolean {
        var found = false
        dataStore.edit { preferences ->
            val entries = mutableSetOf<String>()
            entries.addAll(
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .map { call ->
                        if (call.id == id) {
                            found = true
                            ScheduledCallCodec.encode(call.copy(state = state))
                        } else {
                            ScheduledCallCodec.encode(call)
                        }
                    },
            )
            preferences[CALLS_KEY] = entries
        }
        return found
    }

    suspend fun remove(id: String) {
        dataStore.edit { preferences ->
            val entries = mutableSetOf<String>()
            entries.addAll(
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .filterNot { call -> call.id == id }
                    .map { call -> ScheduledCallCodec.encode(call) },
            )
            preferences[CALLS_KEY] = entries
        }
    }

    suspend fun removeTerminalRecords() {
        dataStore.edit { preferences ->
            val entries = mutableSetOf<String>()
            entries.addAll(
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .filter { call ->
                        call.state == ScheduledCallState.SCHEDULED ||
                            call.state == ScheduledCallState.RINGING ||
                            call.state == ScheduledCallState.IN_CALL
                    }.map { call -> ScheduledCallCodec.encode(call) },
            )
            preferences[CALLS_KEY] = entries
        }
    }

    private companion object {
        val CALLS_KEY = stringSetPreferencesKey("records")
    }
}
