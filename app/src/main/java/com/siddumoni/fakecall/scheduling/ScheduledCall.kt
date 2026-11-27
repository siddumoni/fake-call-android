package com.siddumoni.fakecall.scheduling

import java.util.UUID

enum class ScheduledCallState {
    SCHEDULED,
    RINGING,
    IN_CALL,
    ENDED,
    CANCELLED,
    TIMED_OUT,
}

data class ScheduledCall(
    val id: String,
    val callerPresetId: String,
    val triggerAtEpochMillis: Long,
    val state: ScheduledCallState = ScheduledCallState.SCHEDULED,
)

enum class ScheduleValidationError {
    INVALID_ID,
    INVALID_CALLER_ID,
    TIME_NOT_IN_FUTURE,
    TIME_TOO_FAR,
}

/** Pure validation and transition rules; Android alarm side effects are intentionally separate. */
object ScheduledCallRules {
    const val MAX_SCHEDULE_HORIZON_MILLIS = 365L * 24L * 60L * 60L * 1000L

    fun validate(
        id: String,
        callerPresetId: String,
        triggerAtEpochMillis: Long,
        nowEpochMillis: Long,
    ): ScheduleValidationError? {
        if (!isUuid(id)) return ScheduleValidationError.INVALID_ID
        if (!isUuid(callerPresetId)) return ScheduleValidationError.INVALID_CALLER_ID
        if (triggerAtEpochMillis <= nowEpochMillis) return ScheduleValidationError.TIME_NOT_IN_FUTURE
        val timeUntilTrigger = runCatching {
            Math.subtractExact(triggerAtEpochMillis, nowEpochMillis)
        }.getOrElse {
            return ScheduleValidationError.TIME_TOO_FAR
        }
        if (timeUntilTrigger > MAX_SCHEDULE_HORIZON_MILLIS) {
            return ScheduleValidationError.TIME_TOO_FAR
        }
        return null
    }

    fun transition(
        current: ScheduledCallState,
        action: ScheduledCallAction,
    ): ScheduledCallState? =
        when (current to action) {
            ScheduledCallState.SCHEDULED to ScheduledCallAction.ALARM_FIRED -> ScheduledCallState.RINGING
            ScheduledCallState.SCHEDULED to ScheduledCallAction.CANCEL -> ScheduledCallState.CANCELLED
            ScheduledCallState.RINGING to ScheduledCallAction.ANSWER -> ScheduledCallState.IN_CALL
            ScheduledCallState.RINGING to ScheduledCallAction.DECLINE -> ScheduledCallState.ENDED
            ScheduledCallState.RINGING to ScheduledCallAction.TIMEOUT -> ScheduledCallState.TIMED_OUT
            ScheduledCallState.IN_CALL to ScheduledCallAction.END -> ScheduledCallState.ENDED
            else -> null
        }

    private fun isUuid(value: String): Boolean =
        runCatching { UUID.fromString(value).toString() == value.lowercase() }.getOrDefault(false)
}

enum class ScheduledCallAction {
    ALARM_FIRED,
    CANCEL,
    ANSWER,
    DECLINE,
    TIMEOUT,
    END,
}

/** Resolve a wall-clock selection deterministically across timezone DST gaps and overlaps. */
object ScheduleTimeResolver {
    fun resolve(localDateTime: java.time.LocalDateTime, zoneId: java.time.ZoneId): java.time.Instant {
        val rules = zoneId.rules
        val offsets = rules.getValidOffsets(localDateTime)
        return when {
            offsets.size == 1 -> localDateTime.toInstant(offsets[0])
            offsets.size >= 2 -> localDateTime.toInstant(offsets[0])
            else -> {
                val transition = rules.getTransition(localDateTime)
                    ?: throw java.time.DateTimeException("Unable to resolve local date-time")
                transition.dateTimeAfter.toInstant(transition.offsetAfter)
            }
        }
    }
}
