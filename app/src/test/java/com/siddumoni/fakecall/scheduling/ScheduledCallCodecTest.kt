package com.siddumoni.fakecall.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ScheduledCallCodecTest {
    @Test fun roundTripPreservesRecord() {
        val original = ScheduledCall(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 1_800_000_000_000L, ScheduledCallState.RINGING)
        assertEquals(original, ScheduledCallCodec.decode(ScheduledCallCodec.encode(original)))
    }

    @Test fun malformedAndUnsupportedRecordsAreIgnored() {
        assertNull(ScheduledCallCodec.decode("broken"))
        assertNull(ScheduledCallCodec.decode("2|id|caller|100|SCHEDULED"))
        assertNull(ScheduledCallCodec.decode("1|id|%%%|100|NOT_A_STATE"))
    }
}
