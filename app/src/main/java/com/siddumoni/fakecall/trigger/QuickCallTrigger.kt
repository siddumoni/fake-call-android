package com.siddumoni.fakecall.trigger

import android.content.Context
import com.siddumoni.fakecall.call.CallSession
import com.siddumoni.fakecall.data.preferences.CallSettingsRepository
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import com.siddumoni.fakecall.scheduling.ScheduleAlarmResult
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallDeliveryMode
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import kotlinx.coroutines.flow.first
import java.util.UUID

sealed interface QuickCallOutcome {
    data class Scheduled(
        val call: ScheduledCall,
    ) : QuickCallOutcome

    /** A call is already waiting, ringing or in progress. */
    data object Busy : QuickCallOutcome

    data object NoCaller : QuickCallOutcome

    data object NotificationsBlocked : QuickCallOutcome

    data object Failed : QuickCallOutcome
}

/** Schedules an in-app quick call using the saved caller and delay. */
class QuickCallTrigger(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val schedules = ScheduledCallRepository(appContext)
    private val scheduler = ScheduledCallAlarmScheduler(appContext)

    suspend fun create(pendingId: String?): QuickCallOutcome {
        val settings = CallSettingsRepository(appContext).settings.first()
        val callers = CallerPresetRepository(appContext).presets.first()
        val caller =
            callers.firstOrNull { it.id == settings.triggerCallerId }
                ?: callers.firstOrNull()
                ?: return QuickCallOutcome.NoCaller
        if (!ScheduledCallNotifications.canPost(appContext)) return QuickCallOutcome.NotificationsBlocked

        for (existing in schedules.calls.first()) {
            when {
                existing.id == pendingId && existing.state == ScheduledCallState.SCHEDULED ->
                    return QuickCallOutcome.Busy
                existing.state == ScheduledCallState.RINGING -> return QuickCallOutcome.Busy
                existing.state == ScheduledCallState.IN_CALL -> {
                    if (CallSession.activeCallId == existing.id) return QuickCallOutcome.Busy
                    // Left over from a call screen that no longer exists.
                    schedules.transition(existing.id, ScheduledCallAction.END)
                }
            }
        }

        val call =
            ScheduledCall(
                id = UUID.randomUUID().toString(),
                callerPresetId = caller.id,
                triggerAtEpochMillis = System.currentTimeMillis() + settings.triggerDelaySeconds * 1_000L,
                deliveryMode =
                    if (scheduler.canScheduleExactAlarms()) {
                        ScheduledCallDeliveryMode.EXACT
                    } else {
                        ScheduledCallDeliveryMode.APPROXIMATE
                    },
            )
        schedules.upsert(call)
        if (scheduler.schedule(call) != ScheduleAlarmResult.SCHEDULED) {
            schedules.remove(call.id)
            return QuickCallOutcome.Failed
        }
        return QuickCallOutcome.Scheduled(call)
    }
}
