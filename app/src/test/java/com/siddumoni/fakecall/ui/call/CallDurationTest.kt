package com.siddumoni.fakecall.ui.call

import org.junit.Assert.assertEquals
import org.junit.Test

class CallDurationTest {
    @Test
    fun durationReadsLikeAPhoneTimer() {
        assertEquals("00:00", formatCallDuration(-1))
        assertEquals("00:09", formatCallDuration(9))
        assertEquals("12:05", formatCallDuration(725))
        assertEquals("1:00:01", formatCallDuration(3_601))
    }
}
