package com.siddumoni.fakecall.scheduling

/** Pure rules for closing records whose delivery or ringing window has clearly passed. */
object ScheduleRecoveryPolicy {
    /** Inexact alarms can be delivered well after the requested time. */
    const val APPROXIMATE_GRACE_MILLIS = 60L * 60L * 1000L
    const val EXACT_GRACE_MILLIS = 2L * 60L * 1000L

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
