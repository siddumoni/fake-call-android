package com.siddumoni.fakecall.trigger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumePressCounterTest {
    @Test
    fun fifthQuickPressTriggersOnce() {
        val counter = VolumePressCounter()

        val results = (0..5).map { counter.register(it * 200L) }

        assertEquals(listOf(false, false, false, false, true, false), results)
    }

    @Test
    fun slowPressesNeverTrigger() {
        val counter = VolumePressCounter()

        assertFalse((0..20).any { counter.register(it * 800L) })
    }

    @Test
    fun oldPressesDropOutOfTheWindow() {
        val counter = VolumePressCounter()
        assertFalse(counter.register(0L))
        // Four more within three seconds of each other, but the first is already too old.
        assertFalse((0..3).any { counter.register(5_000L + it * 100L) })

        assertTrue(counter.register(5_400L))
    }

    @Test
    fun pressesExactlySpanningTheWindowCount() {
        val counter = VolumePressCounter()
        listOf(0L, 500L, 1_000L, 2_000L).forEach { assertFalse(counter.register(it)) }

        assertTrue(counter.register(VolumePressCounter.WINDOW_MILLIS))
    }
}
