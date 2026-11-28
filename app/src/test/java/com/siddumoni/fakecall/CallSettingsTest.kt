package com.siddumoni.fakecall

import com.siddumoni.fakecall.settings.CallSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CallSettingsTest {
    @Test
    fun delayCombinesMinutesAndSeconds() {
        assertEquals(5, CallSettings.delayFrom("", "5"))
        assertEquals(120, CallSettings.delayFrom("2", ""))
        assertEquals(90, CallSettings.delayFrom("1", "30"))
        assertEquals(7_200, CallSettings.delayFrom("120", "0"))
    }

    @Test
    fun emptyOutOfRangeOrNonNumericDelaysAreRejected() {
        assertNull(CallSettings.delayFrom("", ""))
        assertNull(CallSettings.delayFrom("0", "0"))
        assertNull(CallSettings.delayFrom("120", "1"))
        assertNull(CallSettings.delayFrom("x", "5"))
        assertNull(CallSettings.delayFrom("-1", "90"))
        assertNull(CallSettings.delayFrom("999999999", "0"))
    }

    @Test
    fun fullScreenIsOffUntilTheUserTurnsItOn() {
        assertFalse(CallSettings().fullScreen)
    }
}
