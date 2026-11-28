package com.siddumoni.fakecall

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScheduledCallActionsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val schedules = ScheduledCallRepository(context)
    private val presets = CallerPresetRepository(context)
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val caller = CallerPreset(UUID.randomUUID().toString(), "Fictional Caller", "")
    private val call =
        ScheduledCall(
            id = UUID.randomUUID().toString(),
            callerPresetId = caller.id,
            triggerAtEpochMillis = System.currentTimeMillis() + 60_000L,
            state = ScheduledCallState.RINGING,
        )

    @Before
    fun setUp() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        runBlocking {
            presets.save(caller.id, caller.displayName, caller.numberLabel)
            // New records must start as SCHEDULED; fire the alarm transition to make it ring.
            schedules.upsert(call.copy(state = ScheduledCallState.SCHEDULED))
            check(schedules.transition(call.id, ScheduledCallAction.ALARM_FIRED))
        }
    }

    @After
    fun tearDown() {
        ScheduledCallNotifications.cancel(context, call.id)
        runBlocking {
            schedules.remove(call.id)
            presets.delete(caller.id)
        }
    }

    @Test
    fun declineActionEndsRingingCallAndRemovesNotification() {
        val notification = postNotification()
        assertEquals(listOf("Decline", "Answer"), notification.actions.map { it.title.toString() })

        notification.actions[0].actionIntent.send()

        awaitState(ScheduledCallState.ENDED)
        await { manager.activeNotifications.none { it.id == call.id.hashCode() } }
    }

    @Test
    fun swipingTheNotificationAwayDeclinesTheCall() {
        postNotification().deleteIntent.send()

        awaitState(ScheduledCallState.ENDED)
    }

    @Test
    fun answerLinkMovesRingingCallToInCall() {
        postNotification()

        ActivityScenario.launch<MainActivity>(link("answer")).use {
            awaitState(ScheduledCallState.IN_CALL)
            await { manager.activeNotifications.none { it.id == call.id.hashCode() } }
        }
    }

    private fun link(action: String): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("alibicall://scheduled-call/${call.id}/$action"))

    private fun postNotification(): Notification {
        check(ScheduledCallNotifications.show(context, call, caller))
        await { manager.activeNotifications.any { it.id == call.id.hashCode() } }
        return manager.activeNotifications.first { it.id == call.id.hashCode() }.notification
    }

    private fun awaitState(expected: ScheduledCallState) =
        await {
            schedules.calls
                .first()
                .firstOrNull { it.id == call.id }
                ?.state == expected
        }

    private fun await(condition: suspend () -> Boolean) =
        runBlocking {
            withTimeout(10_000L) {
                while (!condition()) delay(50L)
            }
        }
}
