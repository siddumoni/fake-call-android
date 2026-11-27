package com.siddumoni.fakecall

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.scheduling.ScheduleAlarmResult
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallDeliveryMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScheduledCallAlarmSchedulerTest {
    @Test
    fun approximateAlarmCanBeScheduledAndCancelledWithoutExactAlarmAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduler = ScheduledCallAlarmScheduler(context)
        val id = UUID.randomUUID().toString()
        val call =
            ScheduledCall(
                id = id,
                callerPresetId = UUID.randomUUID().toString(),
                triggerAtEpochMillis = System.currentTimeMillis() + 300_000,
                deliveryMode = ScheduledCallDeliveryMode.APPROXIMATE,
            )

        try {
            assertEquals(ScheduleAlarmResult.SCHEDULED, scheduler.schedule(call))
        } finally {
            scheduler.cancel(id)
        }
    }

    @Test
    fun exactAlarmReportsPermissionRequirementOrSchedulesWhenAlreadyGranted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduler = ScheduledCallAlarmScheduler(context)
        val id = UUID.randomUUID().toString()
        val call =
            ScheduledCall(
                id = id,
                callerPresetId = UUID.randomUUID().toString(),
                triggerAtEpochMillis = System.currentTimeMillis() + 300_000,
                deliveryMode = ScheduledCallDeliveryMode.EXACT,
            )

        try {
            val expected =
                if (scheduler.canScheduleExactAlarms()) {
                    ScheduleAlarmResult.SCHEDULED
                } else {
                    ScheduleAlarmResult.EXACT_ALARM_PERMISSION_REQUIRED
                }
            assertEquals(expected, scheduler.schedule(call))
        } finally {
            scheduler.cancel(id)
        }
    }
}
