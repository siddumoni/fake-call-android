package com.siddumoni.fakecall

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.scheduling.ScheduleReconciler
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScheduleReconcilerTest {
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

    private fun newCall() =
        ScheduledCall(
            id = UUID.randomUUID().toString(),
            callerPresetId = UUID.randomUUID().toString(),
            triggerAtEpochMillis = System.currentTimeMillis() + 600_000,
        )
}
