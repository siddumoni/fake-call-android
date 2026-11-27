package com.siddumoni.fakecall.scheduling

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleDeliveryPolicyTest {
    @Test
    fun exactModeRequiresUserGrantedSpecialAccess() {
        assertEquals(
            ScheduleAlarmResult.EXACT_ALARM_PERMISSION_REQUIRED,
            ScheduleDeliveryPolicy.resultFor(ScheduledCallDeliveryMode.EXACT, exactAlarmAccessGranted = false),
        )
        assertEquals(
            ScheduleAlarmResult.SCHEDULED,
            ScheduleDeliveryPolicy.resultFor(ScheduledCallDeliveryMode.EXACT, exactAlarmAccessGranted = true),
        )
    }

    @Test
    fun approximateModeNeverRequiresExactAlarmSpecialAccess() {
        assertEquals(
            ScheduleAlarmResult.SCHEDULED,
            ScheduleDeliveryPolicy.resultFor(ScheduledCallDeliveryMode.APPROXIMATE, exactAlarmAccessGranted = false),
        )
    }
}
