package com.siddumoni.fakecall.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ScheduleRecoveryPolicyTest {
    private val trigger = 1_800_000_000_000L

    private fun call(
        mode: ScheduledCallDeliveryMode,
        state: ScheduledCallState = ScheduledCallState.SCHEDULED,
    ) = ScheduledCall(
        id = UUID.randomUUID().toString(),
        callerPresetId = UUID.randomUUID().toString(),
        triggerAtEpochMillis = trigger,
        state = state,
        deliveryMode = mode,
    )

    @Test
    fun lateApproximateAlarmIsKeptDuringGraceWindow() {
        val now = trigger + ScheduleRecoveryPolicy.APPROXIMATE_GRACE_MILLIS

        assertNull(ScheduleRecoveryPolicy.expiryActionFor(call(ScheduledCallDeliveryMode.APPROXIMATE), now))
    }

    @Test
    fun overdueRecordsTimeOutAfterGraceWindow() {
        val now = trigger + ScheduleRecoveryPolicy.APPROXIMATE_GRACE_MILLIS + 1

        assertEquals(
            ScheduledCallAction.TIMEOUT,
            ScheduleRecoveryPolicy.expiryActionFor(call(ScheduledCallDeliveryMode.APPROXIMATE), now),
        )
        assertNull(
            ScheduleRecoveryPolicy.expiryActionFor(
                call(ScheduledCallDeliveryMode.APPROXIMATE, ScheduledCallState.RINGING),
                now,
            ),
        )
    }

    @Test
    fun exactRecordsUseTheShorterWindow() {
        val now = trigger + ScheduleRecoveryPolicy.EXACT_GRACE_MILLIS + 1

        assertEquals(
            ScheduledCallAction.TIMEOUT,
            ScheduleRecoveryPolicy.expiryActionFor(call(ScheduledCallDeliveryMode.EXACT), now),
        )
    }

    @Test
    fun answeredAndFinishedRecordsAreNotExpired() {
        val now = trigger + ScheduleRecoveryPolicy.APPROXIMATE_GRACE_MILLIS * 24

        listOf(ScheduledCallState.IN_CALL, ScheduledCallState.ENDED, ScheduledCallState.CANCELLED).forEach { state ->
            assertNull(ScheduleRecoveryPolicy.expiryActionFor(call(ScheduledCallDeliveryMode.EXACT, state), now))
        }
    }

    @Test
    fun overdueScheduleWithinGraceIsRearmedForImmediateDelivery() {
        val now = trigger + 30_000
        val pending = call(ScheduledCallDeliveryMode.APPROXIMATE)

        assertEquals(
            now + ScheduleRecoveryPolicy.OVERDUE_REDELIVERY_DELAY_MILLIS,
            ScheduleRecoveryPolicy.alarmTriggerAt(pending, now),
        )
    }

    @Test
    fun ringingRecordDoesNotExpireBasedOnOriginalScheduleTime() {
        val now = trigger + ScheduleRecoveryPolicy.APPROXIMATE_GRACE_MILLIS * 10

        assertNull(
            ScheduleRecoveryPolicy.expiryActionFor(
                call(ScheduledCallDeliveryMode.EXACT, ScheduledCallState.RINGING),
                now,
            ),
        )
    }

    @Test
    fun timeoutDeadlineIsConvertedToElapsedRealtime() {
        assertEquals(
            45_000L,
            ScheduleRecoveryPolicy.timeoutAlarmElapsedRealtime(
                timeoutAtEpochMillis = 1_045_000L,
                wallNowEpochMillis = 1_000_000L,
                elapsedRealtimeMillis = 0L,
            ),
        )
        assertEquals(
            10_000L,
            ScheduleRecoveryPolicy.timeoutAlarmElapsedRealtime(
                timeoutAtEpochMillis = 900_000L,
                wallNowEpochMillis = 1_000_000L,
                elapsedRealtimeMillis = 10_000L,
            ),
        )
    }

    @Test
    fun futureScheduleKeepsItsOriginalTriggerInstant() {
        val now = trigger - 30_000
        val pending = call(ScheduledCallDeliveryMode.EXACT)

        assertEquals(trigger, ScheduleRecoveryPolicy.alarmTriggerAt(pending, now))
    }

    @Test
    fun rebootKeepsOnlyFutureSchedules() {
        val pending = call(ScheduledCallDeliveryMode.APPROXIMATE)

        assertNull(ScheduleRecoveryPolicy.rebootActionFor(pending, trigger - 1))
        assertEquals(ScheduledCallAction.TIMEOUT, ScheduleRecoveryPolicy.rebootActionFor(pending, trigger))
    }

    @Test
    fun rebootClosesInterruptedCalls() {
        val ringing = call(ScheduledCallDeliveryMode.EXACT, ScheduledCallState.RINGING)
        val answered = call(ScheduledCallDeliveryMode.EXACT, ScheduledCallState.IN_CALL)

        assertEquals(ScheduledCallAction.TIMEOUT, ScheduleRecoveryPolicy.rebootActionFor(ringing, trigger))
        assertEquals(ScheduledCallAction.END, ScheduleRecoveryPolicy.rebootActionFor(answered, trigger))
    }
}
