package com.siddumoni.fakecall.scheduling

import android.content.Context
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import kotlinx.coroutines.flow.first

enum class ReconcileResult {
    COMPLETE,
    NOTIFICATION_PERMISSION_REQUIRED,
    EXACT_ALARM_PERMISSION_REQUIRED,
}

/** Brings AlarmManager and notifications in line with the persisted schedule records. */
class ScheduleReconciler(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val repository = ScheduledCallRepository(appContext)
    private val scheduler = ScheduledCallAlarmScheduler(appContext)

    suspend fun reconcile(
        notificationsAllowed: Boolean,
        afterReboot: Boolean = false,
    ): ReconcileResult {
        var result = ReconcileResult.COMPLETE
        repository.calls.first().forEach { call ->
            val now = System.currentTimeMillis()
            val closingAction =
                if (afterReboot) {
                    ScheduleRecoveryPolicy.rebootActionFor(call, now)
                } else {
                    ScheduleRecoveryPolicy.expiryActionFor(call, now)
                }
            if (closingAction != null) {
                repository.transition(call.id, closingAction)
                scheduler.cancel(call.id)
                ScheduledCallNotifications.cancel(appContext, call.id)
                return@forEach
            }
            if (call.state != ScheduledCallState.SCHEDULED) return@forEach
            if (!notificationsAllowed) {
                scheduler.cancel(call.id)
                result = ReconcileResult.NOTIFICATION_PERMISSION_REQUIRED
                return@forEach
            }
            // Re-arm overdue RTC alarms still inside their grace window after a clock change.
            // Keep the persisted absolute trigger instant unchanged.
            val alarmNow = System.currentTimeMillis()
            val alarmCall =
                call.copy(
                    triggerAtEpochMillis =
                        ScheduleRecoveryPolicy.alarmTriggerAt(
                            call,
                            alarmNow,
                        ),
                )
            if (scheduler.schedule(alarmCall) == ScheduleAlarmResult.EXACT_ALARM_PERMISSION_REQUIRED) {
                result = ReconcileResult.EXACT_ALARM_PERMISSION_REQUIRED
            }
        }
        return result
    }
}
