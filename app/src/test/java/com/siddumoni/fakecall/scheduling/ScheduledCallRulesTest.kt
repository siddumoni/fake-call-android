package com.siddumoni.fakecall.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ScheduledCallRulesTest {
    private val id = UUID.randomUUID().toString()
    private val callerId = UUID.randomUUID().toString()
    private val now = 1_800_000_000_000L

    @Test
    fun validFutureScheduleIsAccepted() {
        assertNull(ScheduledCallRules.validate(id, callerId, now + 60_000, now))
    }

    @Test
    fun pastAndCurrentTimesAreRejected() {
        assertEquals(
            ScheduleValidationError.TIME_NOT_IN_FUTURE,
            ScheduledCallRules.validate(id, callerId, now, now),
        )
        assertEquals(
            ScheduleValidationError.TIME_NOT_IN_FUTURE,
            ScheduledCallRules.validate(id, callerId, now - 1, now),
        )
    }

    @Test
    fun scheduleBeyondOneYearIsRejected() {
        assertEquals(
            ScheduleValidationError.TIME_TOO_FAR,
            ScheduledCallRules.validate(
                id,
                callerId,
                now + ScheduledCallRules.MAX_SCHEDULE_HORIZON_MILLIS + 1,
                now,
            ),
        )
    }

    @Test
    fun overflowingTimestampDifferenceIsRejected() {
        assertEquals(
            ScheduleValidationError.TIME_TOO_FAR,
            ScheduledCallRules.validate(id, callerId, Long.MAX_VALUE, Long.MIN_VALUE),
        )
    }

    @Test
    fun malformedIdentifiersAreRejected() {
        assertEquals(
            ScheduleValidationError.INVALID_ID,
            ScheduledCallRules.validate("bad", callerId, now + 1, now),
        )
        assertEquals(
            ScheduleValidationError.INVALID_CALLER_ID,
            ScheduledCallRules.validate(id, "bad", now + 1, now),
        )
    }

    @Test
    fun stateMachineRejectsDuplicateOrIllegalActions() {
        assertEquals(
            ScheduledCallState.RINGING,
            ScheduledCallRules.transition(ScheduledCallState.SCHEDULED, ScheduledCallAction.ALARM_FIRED),
        )
        assertNull(ScheduledCallRules.transition(ScheduledCallState.RINGING, ScheduledCallAction.ALARM_FIRED))
        assertEquals(
            ScheduledCallState.IN_CALL,
            ScheduledCallRules.transition(ScheduledCallState.RINGING, ScheduledCallAction.ANSWER),
        )
        assertNull(ScheduledCallRules.transition(ScheduledCallState.ENDED, ScheduledCallAction.END))
    }

    @Test
    fun terminalTransitionsAreExplicit() {
        assertEquals(
            ScheduledCallState.CANCELLED,
            ScheduledCallRules.transition(ScheduledCallState.SCHEDULED, ScheduledCallAction.CANCEL),
        )
        assertEquals(
            ScheduledCallState.TIMED_OUT,
            ScheduledCallRules.transition(ScheduledCallState.RINGING, ScheduledCallAction.TIMEOUT),
        )
        assertEquals(
            ScheduledCallState.ENDED,
            ScheduledCallRules.transition(ScheduledCallState.RINGING, ScheduledCallAction.DECLINE),
        )
        assertEquals(
            ScheduledCallState.ENDED,
            ScheduledCallRules.transition(ScheduledCallState.IN_CALL, ScheduledCallAction.END),
        )
    }

    @Test
    fun daylightSavingGapAdvancesToFirstValidTime() {
        val zone = java.time.ZoneId.of("America/New_York")
        val local = java.time.LocalDateTime.of(2026, 3, 8, 2, 30)
        assertEquals(
            java.time.Instant.parse("2026-03-08T07:00:00Z"),
            ScheduleTimeResolver.resolve(local, zone),
        )
    }

    @Test
    fun daylightSavingOverlapUsesEarlierOccurrence() {
        val zone = java.time.ZoneId.of("America/New_York")
        val local = java.time.LocalDateTime.of(2026, 11, 1, 1, 30)
        assertEquals(
            java.time.Instant.parse("2026-11-01T05:30:00Z"),
            ScheduleTimeResolver.resolve(local, zone),
        )
    }
}
