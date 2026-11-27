package com.siddumoni.fakecall.scheduling

/** Pure rules for closing records whose delivery or ringing window has clearly passed. */
object ScheduleRecoveryPolicy {
    /** Inexact alarms can be delivered well after the requested time. */
    const val APPROXIMATE_GRACE_MILLIS = 60L * 60L * 1000L
    const val EXACT_GRACE_MILLIS = 2L * 60L * 1000L
    const val OVERDUE_REDELIVERY_DELAY_MILLIS = 1_000L

    /** A restart removes alarms and notifications, so only future schedules remain valid. */
    fun rebootActionFor(
        call: ScheduledCall,
        nowEpochMillis: Long,
    ): ScheduledCallAction? =
        when (call.state) {
            ScheduledCallState.SCHEDULED ->
                if (call.triggerAtEpochMillis <= nowEpochMillis) ScheduledCallAction.TIMEOUT else null
            ScheduledCallState.RINGING -> ScheduledCallAction.TIMEOUT
            ScheduledCallState.IN_CALL -> ScheduledCallAction.END
            else -> null
        }

    /** Return a future AlarmManager trigger for a record still inside its grace window. */
    fun alarmTriggerAt(
        call: ScheduledCall,
        nowEpochMillis: Long,
    ): Long =
        if (call.triggerAtEpochMillis <= nowEpochMillis) {
            nowEpochMillis + OVERDUE_REDELIVERY_DELAY_MILLIS
        } else {
            call.triggerAtEpochMillis
        }

    /** Convert a wall-clock timeout deadline to elapsed realtime so clock changes cannot shift it. */
    fun timeoutAlarmElapsedRealtime(
        timeoutAtEpochMillis: Long,
        wallNowEpochMillis: Long,
        elapsedRealtimeMillis: Long,
    ): Long {
        val remainingMillis =
            runCatching { Math.subtractExact(timeoutAtEpochMillis, wallNowEpochMillis) }
                .getOrElse { if (timeoutAtEpochMillis > wallNowEpochMillis) Long.MAX_VALUE else 0L }
                .coerceAtLeast(0L)
        return runCatching { Math.addExact(elapsedRealtimeMillis, remainingMillis) }
            .getOrDefault(Long.MAX_VALUE)
    }

    fun expiryActionFor(
        call: ScheduledCall,
        nowEpochMillis: Long,
    ): ScheduledCallAction? {
        val grace =
            when (call.deliveryMode) {
                ScheduledCallDeliveryMode.EXACT -> EXACT_GRACE_MILLIS
                ScheduledCallDeliveryMode.APPROXIMATE -> APPROXIMATE_GRACE_MILLIS
            }
        val expired = nowEpochMillis - call.triggerAtEpochMillis > grace
        return when (call.state) {
            ScheduledCallState.SCHEDULED -> if (expired) ScheduledCallAction.TIMEOUT else null
            // Ring timeout is measured from alarm delivery using elapsed realtime, not the
            // original wall-clock schedule time. Clock/timezone reconciliation must not end
            // an already ringing simulation early.
            else -> null
        }
    }
}
