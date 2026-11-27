package com.siddumoni.fakecall.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.siddumoni.fakecall.notifications.ScheduledCallAlarmReceiver
import com.siddumoni.fakecall.notifications.ScheduledCallTimeoutReceiver
import java.util.UUID

/**
 * AlarmManager adapter for persisted schedule records.
 *
 * Exact schedules are never silently downgraded. Approximate schedules use an inexact
 * allow-while-idle alarm and can be delayed by Android power management.
 */
class ScheduledCallAlarmScheduler(
    context: Context,
) : ScheduleAlarmOperations {
    private val appContext = context.applicationContext
    private val alarmManager =
        appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun schedule(call: ScheduledCall): ScheduleAlarmResult {
        require(call.state == ScheduledCallState.SCHEDULED) {
            "Only a pending schedule can be registered with AlarmManager."
        }
        require(
            ScheduledCallRules.validate(
                id = call.id,
                callerPresetId = call.callerPresetId,
                triggerAtEpochMillis = call.triggerAtEpochMillis,
                nowEpochMillis = System.currentTimeMillis(),
            ) == null,
        ) {
            "Only a valid future schedule can be registered with AlarmManager."
        }
        val permissionResult =
            ScheduleDeliveryPolicy.resultFor(call.deliveryMode, canScheduleExactAlarms())
        if (permissionResult != ScheduleAlarmResult.SCHEDULED) return permissionResult

        val pendingIntent = alarmPendingIntent(call.id)
        return try {
            when (call.deliveryMode) {
                ScheduledCallDeliveryMode.EXACT ->
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        call.triggerAtEpochMillis,
                        pendingIntent,
                    )
                ScheduledCallDeliveryMode.APPROXIMATE ->
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        call.triggerAtEpochMillis,
                        pendingIntent,
                    )
            }
            ScheduleAlarmResult.SCHEDULED
        } catch (exception: SecurityException) {
            if (call.deliveryMode == ScheduledCallDeliveryMode.EXACT) {
                ScheduleAlarmResult.EXACT_ALARM_PERMISSION_REQUIRED
            } else {
                throw exception
            }
        }
    }

    fun cancelTrigger(scheduleId: String) {
        if (!isUuid(scheduleId)) return
        val pendingIntent = alarmPendingIntent(scheduleId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun cancel(scheduleId: String) {
        cancelTrigger(scheduleId)
        cancelTimeout(scheduleId)
    }

    fun scheduleTimeout(
        scheduleId: String,
        timeoutAtEpochMillis: Long,
    ) {
        if (!isUuid(scheduleId)) return
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeoutAtEpochMillis,
            timeoutPendingIntent(scheduleId),
        )
    }

    fun cancelTimeout(scheduleId: String) {
        if (!isUuid(scheduleId)) return
        val pendingIntent = timeoutPendingIntent(scheduleId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun alarmPendingIntent(scheduleId: String): PendingIntent {
        val intent =
            Intent(appContext, ScheduledCallAlarmReceiver::class.java).apply {
                action = ACTION_ALARM
                data = scheduleUri(scheduleId, "trigger")
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            }
        return PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun timeoutPendingIntent(scheduleId: String): PendingIntent {
        val intent =
            Intent(appContext, ScheduledCallTimeoutReceiver::class.java).apply {
                action = ACTION_TIMEOUT
                data = scheduleUri(scheduleId, "timeout")
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            }
        return PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun scheduleUri(
        scheduleId: String,
        action: String,
    ): Uri =
        Uri
            .Builder()
            .scheme("alibicall")
            .authority("scheduled-call")
            .appendPath(scheduleId)
            .appendPath(action)
            .build()

    private fun isUuid(value: String): Boolean =
        runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)

    companion object {
        const val ACTION_ALARM = "com.siddumoni.fakecall.action.SCHEDULED_CALL_ALARM"
        const val ACTION_TIMEOUT = "com.siddumoni.fakecall.action.SCHEDULED_CALL_TIMEOUT"
        const val EXTRA_SCHEDULE_ID = "com.siddumoni.fakecall.extra.SCHEDULE_ID"
        const val RING_TIMEOUT_MILLIS = 45_000L
    }
}
