package com.siddumoni.fakecall

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScheduledCallRepositoryTest {
    @Test
    fun duplicateAlarmDeliveryAndIllegalTransitionsAreRejectedAtomically() =
        runBlocking {
            val repository =
                ScheduledCallRepository(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                )
            val call =
                ScheduledCall(
                    id = UUID.randomUUID().toString(),
                    callerPresetId = UUID.randomUUID().toString(),
                    triggerAtEpochMillis = System.currentTimeMillis() + 60_000,
                )

            try {
                repository.upsert(call)

                val alarmResults =
                    coroutineScope {
                        listOf(
                            async {
                                repository.transition(
                                    call.id,
                                    ScheduledCallAction.ALARM_FIRED,
                                )
                            },
                            async {
                                repository.transition(
                                    call.id,
                                    ScheduledCallAction.ALARM_FIRED,
                                )
                            },
                        ).awaitAll()
                    }
                assertEquals(1, alarmResults.count { result -> result })

                val ringing = repository.calls.first().single { saved -> saved.id == call.id }
                assertEquals(ScheduledCallState.RINGING, ringing.state)
                assertFalse(repository.transition(call.id, ScheduledCallAction.CANCEL))

                val attemptedReschedule =
                    runCatching {
                        repository.upsert(
                            call.copy(
                                triggerAtEpochMillis = System.currentTimeMillis() + 120_000,
                            ),
                        )
                    }
                assertTrue(attemptedReschedule.isFailure)
                assertEquals(
                    ScheduledCallState.RINGING,
                    repository.calls.first().single { saved -> saved.id == call.id }.state,
                )

                assertTrue(repository.transition(call.id, ScheduledCallAction.ANSWER))
                assertTrue(repository.transition(call.id, ScheduledCallAction.END))
                assertFalse(repository.transition(call.id, ScheduledCallAction.END))
                assertEquals(
                    ScheduledCallState.ENDED,
                    repository.calls.first().single { saved -> saved.id == call.id }.state,
                )
            } finally {
                repository.remove(call.id)
            }
        }

    @Test
    fun upsertRejectsExpiredAndAlreadyActiveRecords() =
        runBlocking {
            val repository =
                ScheduledCallRepository(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                )
            val expired =
                ScheduledCall(
                    id = UUID.randomUUID().toString(),
                    callerPresetId = UUID.randomUUID().toString(),
                    triggerAtEpochMillis = System.currentTimeMillis() - 1,
                )
            val active =
                ScheduledCall(
                    id = UUID.randomUUID().toString(),
                    callerPresetId = UUID.randomUUID().toString(),
                    triggerAtEpochMillis = System.currentTimeMillis() + 60_000,
                    state = ScheduledCallState.RINGING,
                )

            assertTrue(runCatching { repository.upsert(expired) }.isFailure)
            assertTrue(runCatching { repository.upsert(active) }.isFailure)
            assertTrue(repository.calls.first().none { saved -> saved.id == expired.id || saved.id == active.id })
        }
}
