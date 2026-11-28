package com.siddumoni.fakecall

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.call.CallActivity
import com.siddumoni.fakecall.data.preferences.CallSettingsRepository
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import com.siddumoni.fakecall.trigger.QuickCallOutcome
import com.siddumoni.fakecall.trigger.QuickCallTrigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class QuickCallTriggerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val schedules = ScheduledCallRepository(context)
    private val presets = CallerPresetRepository(context)
    private val callerId = UUID.randomUUID().toString()

    @Before
    fun setUp() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        runBlocking {
            clearSchedules()
            presets.save(callerId, "Asha", "")
            CallSettingsRepository(context).setTriggerCaller(callerId)
            CallSettingsRepository(context).setTriggerDelaySeconds(600)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            clearSchedules()
            presets.delete(callerId)
            CallSettingsRepository(context).setTriggerDelaySeconds(10)
        }
    }

    @Test
    fun triggerSchedulesTheChosenCallerAfterTheDelayAndIgnoresRepeats() =
        runBlocking {
            val before = System.currentTimeMillis()

            val outcome = QuickCallTrigger(context).create(pendingId = null)

            val call = (outcome as QuickCallOutcome.Scheduled).call
            assertEquals(callerId, call.callerPresetId)
            assertTrue(call.triggerAtEpochMillis in (before + 600_000L)..(System.currentTimeMillis() + 600_000L))
            assertEquals(
                ScheduledCallState.SCHEDULED,
                schedules.calls
                    .first()
                    .single()
                    .state,
            )
            assertEquals(QuickCallOutcome.Busy, QuickCallTrigger(context).create(pendingId = call.id))
            assertEquals(1, schedules.calls.first().size)
        }

    @Test
    fun callScreenIsPrivateAndNoAccessibilityServiceIsPackaged() {
        val manager = context.packageManager
        val activity = manager.getActivityInfo(ComponentName(context, CallActivity::class.java), 0)
        val services =
            manager.getPackageInfo(context.packageName, PackageManager.GET_SERVICES).services.orEmpty()

        assertFalse(activity.exported)
        assertTrue(services.none { service -> service.permission == Manifest.permission.BIND_ACCESSIBILITY_SERVICE })
    }

    private suspend fun clearSchedules() {
        schedules.calls.first().forEach {
            ScheduledCallAlarmScheduler(context).cancel(it.id)
            schedules.remove(it.id)
        }
    }
}
