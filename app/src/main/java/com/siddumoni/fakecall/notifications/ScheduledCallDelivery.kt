package com.siddumoni.fakecall.notifications

import android.content.Context
import com.siddumoni.fakecall.data.preferences.CallSettingsRepository
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.flow.first

/** Starts ringing a due schedule. Safe to call more than once for the same schedule. */
object ScheduledCallDelivery {
    suspend fun deliver(
        context: Context,
        id: String,
    ) {
        val appContext = context.applicationContext
        val schedules = ScheduledCallRepository(appContext)
        val scheduler = ScheduledCallAlarmScheduler(appContext)
        val call =
            schedules.calls
                .first()
                .firstOrNull { it.id == id }
                ?: return
        if (call.state != ScheduledCallState.SCHEDULED) return

        if (call.triggerAtEpochMillis > System.currentTimeMillis()) {
            try {
                // Permission revocation must not silently change an exact schedule. Leave
                // the record pending; foreground reconciliation can restore it after access.
                scheduler.schedule(call)
            } catch (_: Exception) {
                schedules.transition(id, ScheduledCallAction.TIMEOUT)
                scheduler.cancelTrigger(id)
                ScheduledCallNotifications.cancel(appContext, id)
            }
            return
        }

        val caller =
            CallerPresetRepository(appContext)
                .presets
                .first()
                .firstOrNull { it.id == call.callerPresetId }
        if (caller == null || !ScheduledCallNotifications.canPost(appContext)) {
            schedules.transition(id, ScheduledCallAction.TIMEOUT)
            return
        }
        val fullScreen =
            CallSettingsRepository(appContext)
                .settings
                .first()
                .fullScreen

        if (!schedules.transition(id, ScheduledCallAction.ALARM_FIRED)) return
        scheduler.cancelTrigger(id)
        val ringingCall = call.copy(state = ScheduledCallState.RINGING)
        if (!ScheduledCallNotifications.show(appContext, ringingCall, caller, fullScreen)) {
            schedules.transition(id, ScheduledCallAction.TIMEOUT)
            return
        }
        try {
            scheduler.scheduleTimeout(
                id,
                System.currentTimeMillis() + ScheduledCallAlarmScheduler.RING_TIMEOUT_MILLIS,
            )
        } catch (_: Exception) {
            // The notification has its own platform timeout, but without this alarm
            // persisted state could remain RINGING forever. Fail closed and clean up.
            schedules.transition(id, ScheduledCallAction.TIMEOUT)
            ScheduledCallNotifications.cancel(appContext, id)
        }
    }
}
