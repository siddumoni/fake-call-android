package com.siddumoni.fakecall.scheduling

import com.siddumoni.fakecall.notifications.ScheduledCallLink
import com.siddumoni.fakecall.notifications.notificationRemovalAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduledCallLinkTest {
    private val id = "123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun parsesOpenAndAnswerLinks() {
        assertEquals(ScheduledCallLink(id, answer = false), parse(id))
        assertEquals(ScheduledCallLink(id, answer = true), parse(id, "answer"))
    }

    @Test
    fun rejectsEveryOtherShape() {
        assertNull(parse())
        assertNull(parse("not-a-uuid"))
        assertNull(parse(id, "decline"))
        assertNull(parse(id, "answer", "extra"))
        assertNull(ScheduledCallLink.parse("https", "scheduled-call", listOf(id)))
        assertNull(ScheduledCallLink.parse("alibicall", "other", listOf(id)))
        assertNull(ScheduledCallLink.parse(null, null, listOf(id)))
    }

    @Test
    fun removalBeforeRingDeadlineIsDeclineAndAtDeadlineIsTimeout() {
        assertEquals(ScheduledCallAction.DECLINE, notificationRemovalAction(null, 50_000L))
        assertEquals(ScheduledCallAction.DECLINE, notificationRemovalAction(45_000L, 10_000L))
        assertEquals(ScheduledCallAction.TIMEOUT, notificationRemovalAction(45_000L, 44_500L))
        assertEquals(ScheduledCallAction.TIMEOUT, notificationRemovalAction(45_000L, 60_000L))
    }

    private fun parse(vararg segments: String) = ScheduledCallLink.parse("alibicall", "scheduled-call", segments.toList())
}
