package com.siddumoni.fakecall.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import java.util.UUID
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository as PresetRepository

class ScheduledCallAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val id =
            intent
                ?.getStringExtra(ScheduledCallAlarmScheduler.EXTRA_SCHEDULE_ID)
                ?.takeIf(::isUuid)
                ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val schedules = ScheduledCallRepository(appContext)
                val call =
                    schedules.calls
                        .first()
                        .firstOrNull { it.id == id }
                        ?: return@launch
                if (call.state != ScheduledCallState.SCHEDULED) return@launch

                if (call.triggerAtEpochMillis > System.currentTimeMillis()) {
                    try {
                        // Permission revocation must not silently change an exact schedule. Leave
                        // the record pending; foreground reconciliation can restore it after access.
                        ScheduledCallAlarmScheduler(appContext).schedule(call)
                    } catch (_: Exception) {
                        schedules.transition(id, ScheduledCallAction.TIMEOUT)
                        ScheduledCallAlarmScheduler(appContext).cancelTrigger(id)
                        ScheduledCallNotifications.cancel(appContext, id)
                    }
                    return@launch
                }

                val caller =
                    PresetRepository(appContext)
                        .presets
                        .first()
                        .firstOrNull { it.id == call.callerPresetId }
                if (caller == null || !ScheduledCallNotifications.canPost(appContext)) {
                    schedules.transition(id, ScheduledCallAction.TIMEOUT)
                    return@launch
                }

                if (schedules.transition(id, ScheduledCallAction.ALARM_FIRED)) {
                    val ringingCall = call.copy(state = ScheduledCallState.RINGING)
                    if (ScheduledCallNotifications.show(appContext, ringingCall, caller)) {
                        try {
                            ScheduledCallAlarmScheduler(appContext).scheduleTimeout(
                                id,
                                System.currentTimeMillis() + ScheduledCallAlarmScheduler.RING_TIMEOUT_MILLIS,
                            )
                        } catch (_: Exception) {
                            // The notification has its own platform timeout, but without this alarm
                            // persisted state could remain RINGING forever. Fail closed and clean up.
                            schedules.transition(id, ScheduledCallAction.TIMEOUT)
                            ScheduledCallNotifications.cancel(appContext, id)
                        }
                    } else {
                        schedules.transition(id, ScheduledCallAction.TIMEOUT)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isUuid(value: String): Boolean =
        runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)
}

class ScheduledCallTimeoutReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val id =
            intent
                ?.getStringExtra(ScheduledCallAlarmScheduler.EXTRA_SCHEDULE_ID)
                ?.takeIf { value -> runCatching { UUID.fromString(value) }.isSuccess }
                ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ScheduledCallRepository(appContext).transition(id, ScheduledCallAction.TIMEOUT)
                ScheduledCallNotifications.cancel(appContext, id)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ScheduledCallDismissReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val id =
            intent
                ?.getStringExtra(ScheduledCallNotifications.EXTRA_SCHEDULE_ID)
                ?.takeIf { value -> runCatching { UUID.fromString(value) }.isSuccess }
                ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ScheduledCallRepository(appContext).transition(id, ScheduledCallAction.DECLINE)
                ScheduledCallAlarmScheduler(appContext).cancelTimeout(id)
                ScheduledCallNotifications.cancel(appContext, id)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
