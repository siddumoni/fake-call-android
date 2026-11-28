package com.siddumoni.fakecall.notifications

import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import java.util.UUID

/** A validated `alibicall://scheduled-call/<uuid>[/answer]` link to a persisted simulation. */
data class ScheduledCallLink(
    val scheduleId: String,
    val answer: Boolean,
) {
    companion object {
        const val SCHEME = "alibicall"
        const val AUTHORITY = "scheduled-call"
        const val ANSWER_SEGMENT = "answer"

        /** Returns null for anything other than the two exact shapes this app creates. */
        fun parse(
            scheme: String?,
            authority: String?,
            pathSegments: List<String>,
        ): ScheduledCallLink? {
            if (scheme != SCHEME || authority != AUTHORITY || pathSegments.size !in 1..2) return null
            val answer = pathSegments.size == 2
            if (answer && pathSegments[1] != ANSWER_SEGMENT) return null
            val id = pathSegments[0]
            val isUuid =
                runCatching { UUID.fromString(id).toString() == id.lowercase() }.getOrDefault(false)
            return if (isUuid) ScheduledCallLink(id, answer) else null
        }
    }
}

/**
 * The platform sends a notification's delete intent both for a user swipe and for
 * `setTimeoutAfter`. The monotonic ring deadline tells the two apart.
 */
fun notificationRemovalAction(
    ringDeadlineElapsedMillis: Long?,
    nowElapsedMillis: Long,
): ScheduledCallAction =
    if (ringDeadlineElapsedMillis != null &&
        nowElapsedMillis >= ringDeadlineElapsedMillis - RING_DEADLINE_TOLERANCE_MILLIS
    ) {
        ScheduledCallAction.TIMEOUT
    } else {
        ScheduledCallAction.DECLINE
    }

private const val RING_DEADLINE_TOLERANCE_MILLIS = 1_000L
