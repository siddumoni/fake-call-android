package com.siddumoni.fakecall

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.notifications.ScheduledCallAlarmReceiver
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import com.siddumoni.fakecall.scheduling.ReconcileResult
import com.siddumoni.fakecall.scheduling.ScheduleReconciler
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Exercises alarm delivery through the real receiver, without any Activity running. */
@RunWith(AndroidJUnit4::class)
class ScheduledCallDeliveryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val schedules = ScheduledCallRepository(context)
    private val presets = CallerPresetRepository(context)
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val callerId = UUID.randomUUID().toString()
    private val scheduleId = UUID.randomUUID().toString()

    @Before
    fun setUp() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        runBlocking { presets.save(callerId, "Fictional Caller", "") }
    }

    @After
    fun tearDown() {
        ScheduledCallAlarmScheduler(context).cancel(scheduleId)
        ScheduledCallNotifications.cancel(context, scheduleId)
        runBlocking {
            schedules.remove(scheduleId)
            presets.delete(callerId)
        }
    }

    @Test
    fun dueAlarmRingsOnceEvenWhenDeliveredTwice() =
        runBlocking {
            createDueSchedule()

            deliverAlarm()
            deliverAlarm()

            assertEquals(ScheduledCallState.RINGING, state())
            awaitNotificationCount(1)
        }

    @Test
    fun dueAlarmForDeletedCallerTimesOutWithoutNotification() =
        runBlocking {
            createDueSchedule()
            presets.delete(callerId)

            deliverAlarm()

            assertEquals(ScheduledCallState.TIMED_OUT, state())
            assertEquals(0, notificationCount())
        }

    @Test
    fun alarmArrivingAfterCancellationIsIgnored() =
        runBlocking {
            createDueSchedule()
            assertTrue(schedules.transition(scheduleId, ScheduledCallAction.CANCEL))

            deliverAlarm()

            assertEquals(ScheduledCallState.CANCELLED, state())
            assertEquals(0, notificationCount())
        }

    @Test
    fun revokedNotificationsKeepThePendingScheduleUntilAccessReturns() =
        runBlocking {
            schedules.upsert(schedule(System.currentTimeMillis() + 600_000L))
            val reconciler = ScheduleReconciler(context)

            assertEquals(
                ReconcileResult.NOTIFICATION_PERMISSION_REQUIRED,
                reconciler.reconcile(notificationsAllowed = false),
            )
            assertEquals(ScheduledCallState.SCHEDULED, state())

            assertEquals(ReconcileResult.COMPLETE, reconciler.reconcile(notificationsAllowed = true))
            assertEquals(ScheduledCallState.SCHEDULED, state())
        }

    private fun schedule(triggerAt: Long) =
        ScheduledCall(
            id = scheduleId,
            callerPresetId = callerId,
            triggerAtEpochMillis = triggerAt,
        )

    /** Only future schedules can be written, so write one just ahead and let it become due. */
    private suspend fun createDueSchedule() {
        val triggerAt = System.currentTimeMillis() + 1_000L
        schedules.upsert(schedule(triggerAt))
        while (System.currentTimeMillis() <= triggerAt) delay(100L)
    }

    /** The final receiver of an ordered broadcast runs after the app receiver has finished. */
    private fun deliverAlarm() {
        val finished = CountDownLatch(1)
        val finalReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) = finished.countDown()
            }
        context.sendOrderedBroadcast(
            Intent(context, ScheduledCallAlarmReceiver::class.java)
                .putExtra(ScheduledCallAlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId),
            null,
            finalReceiver,
            null,
            0,
            null,
            null,
        )
        assertTrue(finished.await(10, TimeUnit.SECONDS))
    }

    private suspend fun state() =
        schedules.calls
            .first()
            .firstOrNull { it.id == scheduleId }
            ?.state

    private fun notificationCount() = manager.activeNotifications.count { it.id == scheduleId.hashCode() }

    private suspend fun awaitNotificationCount(expected: Int) {
        repeat(100) {
            if (notificationCount() == expected) return
            delay(50L)
        }
        assertEquals(expected, notificationCount())
    }
}
