package com.siddumoni.fakecall.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ScheduledCallCodecTest {
    @Test
    fun roundTripPreservesRecord() {
        val original =
            ScheduledCall(
                id = UUID.randomUUID().toString(),
                callerPresetId = UUID.randomUUID().toString(),
                triggerAtEpochMillis = 1_800_000_000_000L,
                state = ScheduledCallState.RINGING,
            )
        assertEquals(original, ScheduledCallCodec.decode(ScheduledCallCodec.encode(original)))
    }

    @Test
    fun malformedAndUnsupportedRecordsAreIgnored() {
        assertNull(ScheduledCallCodec.decode("broken"))
        assertNull(ScheduledCallCodec.decode("2|id|caller|100|SCHEDULED"))
        assertNull(ScheduledCallCodec.decode("1|id|%%%|100|NOT_A_STATE"))
    }

    @Test
    fun recordsWithInvalidIdentifiersAreIgnored() {
        assertNull(ScheduledCallCodec.decode("1|not-a-uuid|bm90LWEtdXVpZA|100|SCHEDULED"))
        assertNull(
            ScheduledCallCodec.decode(
                "1|550e8400-e29b-41d4-a716-446655440000|bm90LWEtdXVpZA|100|SCHEDULED",
            ),
        )
    }
}
