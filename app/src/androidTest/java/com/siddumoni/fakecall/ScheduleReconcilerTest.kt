package com.siddumoni.fakecall

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.scheduling.ReconcileResult
import com.siddumoni.fakecall.scheduling.ScheduleAlarmOperations
import com.siddumoni.fakecall.scheduling.ScheduleAlarmResult
import com.siddumoni.fakecall.scheduling.ScheduleReconciler
import com.siddumoni.fakecall.scheduling.ScheduleRecoveryPolicy
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallDeliveryMode
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScheduleReconcilerTest {
    @Test
    fun overdueScheduleInsideGraceWindowIsRearmedWithoutChangingStoredTrigger() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repository = ScheduledCallRepository(context)
            val call = newCall()
            val simulatedNow = call.triggerAtEpochMillis + 30_000
            val alarmOperations = RecordingScheduleAlarmOperations()

            try {
                repository.upsert(call)

                val result =
                    ScheduleReconciler(
                        context,
                        scheduler = alarmOperations,
                        nowEpochMillis = { simulatedNow },
                    ).reconcile(notificationsAllowed = true)

                assertEquals(ReconcileResult.COMPLETE, result)
                assertEquals(1, alarmOperations.scheduledCalls.size)
                assertEquals(
                    simulatedNow + ScheduleRecoveryPolicy.OVERDUE_REDELIVERY_DELAY_MILLIS,
                    alarmOperations.scheduledCalls.single().triggerAtEpochMillis,
                )
                assertEquals(
                    call.triggerAtEpochMillis,
                    repository.calls
                        .first()
                        .single { it.id == call.id }
                        .triggerAtEpochMillis,
                )
            } finally {
                repository.remove(call.id)
            }
        }

    @Test
    fun notificationPermissionLossCancelsAlarmButPreservesPendingRecord() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repository = ScheduledCallRepository(context)
            val call = newCall()
            val alarmOperations = RecordingScheduleAlarmOperations()

            try {
                repository.upsert(call)

                val result =
                    ScheduleReconciler(
                        context,
                        scheduler = alarmOperations,
                    ).reconcile(notificationsAllowed = false)

                assertEquals(ReconcileResult.NOTIFICATION_PERMISSION_REQUIRED, result)
                assertTrue(alarmOperations.scheduledCalls.isEmpty())
                assertEquals(listOf(call.id), alarmOperations.cancelledIds)
                assertEquals(
                    ScheduledCallState.SCHEDULED,
                    repository.calls
                        .first()
                        .single { it.id == call.id }
                        .state,
                )
            } finally {
                repository.remove(call.id)
            }
        }

    @Test
    fun exactAlarmAccessLossIsReportedWithoutDeletingPendingRecord() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repository = ScheduledCallRepository(context)
            val call = newCall(mode = ScheduledCallDeliveryMode.EXACT)
            val alarmOperations =
                RecordingScheduleAlarmOperations(
                    scheduleResult = ScheduleAlarmResult.EXACT_ALARM_PERMISSION_REQUIRED,
                )

            try {
                repository.upsert(call)

                val result =
                    ScheduleReconciler(
                        context,
                        scheduler = alarmOperations,
                    ).reconcile(notificationsAllowed = true)

                assertEquals(ReconcileResult.EXACT_ALARM_PERMISSION_REQUIRED, result)
                assertEquals(
                    ScheduledCallState.SCHEDULED,
                    repository.calls
                        .first()
                        .single { it.id == call.id }
                        .state,
                )
            } finally {
                repository.remove(call.id)
            }
        }

    @Test
    fun rebootRestoresFutureSchedulesAndClosesRingingOnes() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repository = ScheduledCallRepository(context)
            val pending = newCall()
            val ringing = newCall()

            try {
                repository.upsert(pending)
                repository.upsert(ringing)
                repository.transition(ringing.id, ScheduledCallAction.ALARM_FIRED)

                ScheduleReconciler(context).reconcile(notificationsAllowed = true, afterReboot = true)

                val states = repository.calls.first().associate { it.id to it.state }
                assertEquals(ScheduledCallState.SCHEDULED, states[pending.id])
                assertEquals(ScheduledCallState.TIMED_OUT, states[ringing.id])
            } finally {
                ScheduleReconciler(context).reconcile(notificationsAllowed = false)
                repository.remove(pending.id)
                repository.remove(ringing.id)
            }
        }

    private fun newCall(mode: ScheduledCallDeliveryMode = ScheduledCallDeliveryMode.APPROXIMATE) =
        ScheduledCall(
            id = UUID.randomUUID().toString(),
            callerPresetId = UUID.randomUUID().toString(),
            triggerAtEpochMillis = System.currentTimeMillis() + 600_000,
            deliveryMode = mode,
        )

    private class RecordingScheduleAlarmOperations(
        private val scheduleResult: ScheduleAlarmResult = ScheduleAlarmResult.SCHEDULED,
    ) : ScheduleAlarmOperations {
        val scheduledCalls = mutableListOf<ScheduledCall>()
        val cancelledIds = mutableListOf<String>()

        override fun schedule(call: ScheduledCall): ScheduleAlarmResult {
            scheduledCalls += call
            return scheduleResult
        }

        override fun cancel(scheduleId: String) {
            cancelledIds += scheduleId
        }
    }
}
