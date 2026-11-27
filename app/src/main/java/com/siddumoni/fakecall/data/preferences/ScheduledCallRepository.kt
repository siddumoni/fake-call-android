package com.siddumoni.fakecall.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallCodec
import com.siddumoni.fakecall.scheduling.ScheduledCallRules
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.scheduledCallsDataStore: DataStore<Preferences> by
    preferencesDataStore(
        name = "scheduled_calls",
    )

/** App-private persistence for scheduled-call records. Alarm scheduling remains a separate layer. */
class ScheduledCallRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.scheduledCallsDataStore

    val calls: Flow<List<ScheduledCall>> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .sortedBy { call -> call.triggerAtEpochMillis }
            }

    /**
     * Creates a schedule or edits an existing pending schedule.
     *
     * Only valid future schedules may be written, and an active/terminal call can never be
     * reset to SCHEDULED by an upsert. State changes must go through [transition].
     */
    suspend fun upsert(call: ScheduledCall) {
        require(call.state == ScheduledCallState.SCHEDULED) {
            "A new or edited schedule must be in SCHEDULED state."
        }
        require(
            ScheduledCallRules.validate(
                id = call.id,
                callerPresetId = call.callerPresetId,
                triggerAtEpochMillis = call.triggerAtEpochMillis,
                nowEpochMillis = System.currentTimeMillis(),
            ) == null,
        ) {
            "Schedule identifiers and trigger time are invalid."
        }

        dataStore.edit { preferences ->
            val existing =
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .firstOrNull { saved -> saved.id == call.id }
            require(existing == null || existing.state == ScheduledCallState.SCHEDULED) {
                "Only a pending schedule can be edited."
            }

            val entries =
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .filterNot { saved -> saved.id == call.id }
                    .plus(call)
                    .mapTo(mutableSetOf(), ScheduledCallCodec::encode)
            preferences[CALLS_KEY] = entries
        }
    }

    /**
     * Applies one state-machine action atomically.
     *
     * Returns false for an unknown ID, an illegal transition, or a duplicate delivery. DataStore
     * serializes edits, so concurrent delivery of the same alarm can advance the state only once.
     */
    suspend fun transition(
        id: String,
        action: ScheduledCallAction,
    ): Boolean {
        if (!isUuid(id)) return false

        var transitioned = false
        dataStore.edit { preferences ->
            val records =
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
            val current = records.firstOrNull { saved -> saved.id == id }
                ?: return@edit
            val nextState = ScheduledCallRules.transition(current.state, action)
                ?: return@edit

            val entries =
                records
                    .filterNot { saved -> saved.id == id }
                    .plus(current.copy(state = nextState))
                    .mapTo(mutableSetOf(), ScheduledCallCodec::encode)
            preferences[CALLS_KEY] = entries
            transitioned = true
        }
        return transitioned
    }

    suspend fun remove(id: String) {
        dataStore.edit { preferences ->
            val entries =
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .filterNot { call -> call.id == id }
                    .mapTo(mutableSetOf(), ScheduledCallCodec::encode)
            preferences[CALLS_KEY] = entries
        }
    }

    suspend fun removeTerminalRecords() {
        dataStore.edit { preferences ->
            val entries =
                preferences[CALLS_KEY]
                    .orEmpty()
                    .mapNotNull { encoded -> ScheduledCallCodec.decode(encoded) }
                    .filter { call ->
                        call.state == ScheduledCallState.SCHEDULED ||
                            call.state == ScheduledCallState.RINGING ||
                            call.state == ScheduledCallState.IN_CALL
                    }.mapTo(mutableSetOf(), ScheduledCallCodec::encode)
            preferences[CALLS_KEY] = entries
        }
    }

    private fun isUuid(value: String): Boolean =
        runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)

    private companion object {
        val CALLS_KEY = stringSetPreferencesKey("records")
    }
}
