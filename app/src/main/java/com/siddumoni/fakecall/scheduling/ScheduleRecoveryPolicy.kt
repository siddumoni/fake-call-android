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
            ScheduledCallState.SCHEDULED,
            ScheduledCallState.RINGING,
            -> if (expired) ScheduledCallAction.TIMEOUT else null
            else -> null
        }
    }
}
