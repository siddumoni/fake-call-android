package com.siddumoni.fakecall.scheduling

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
