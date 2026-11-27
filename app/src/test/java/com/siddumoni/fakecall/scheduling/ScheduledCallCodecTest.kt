package com.siddumoni.fakecall.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

class ScheduledCallCodecTest {
    @Test
    fun roundTripPreservesExactDeliveryMode() {
        val original =
            ScheduledCall(
                id = UUID.randomUUID().toString(),
                callerPresetId = UUID.randomUUID().toString(),
                triggerAtEpochMillis = 1_800_000_000_000L,
                state = ScheduledCallState.RINGING,
                deliveryMode = ScheduledCallDeliveryMode.EXACT,
            )
        assertEquals(original, ScheduledCallCodec.decode(ScheduledCallCodec.encode(original)))
    }

    @Test
    fun versionOneRecordsMigrateToApproximateMode() {
        val id = "550e8400-e29b-41d4-a716-446655440000"
        val callerId = "6ba7b810-9dad-41d1-80b4-00c04fd430c8"
        val encodedCaller =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(callerId.toByteArray(StandardCharsets.UTF_8))
        val legacy = listOf("1", id, encodedCaller, "1800000000000", "SCHEDULED").joinToString("|")

        val decoded = ScheduledCallCodec.decode(legacy)

        assertEquals(ScheduledCallDeliveryMode.APPROXIMATE, decoded?.deliveryMode)
        assertEquals(id, decoded?.id)
        assertEquals(callerId, decoded?.callerPresetId)
    }

    @Test
    fun approximateModeIsEncodedExplicitlyInVersionTwo() {
        val record =
            ScheduledCall(
                id = UUID.randomUUID().toString(),
                callerPresetId = UUID.randomUUID().toString(),
                triggerAtEpochMillis = 1_800_000_000_000L,
                deliveryMode = ScheduledCallDeliveryMode.APPROXIMATE,
            )
        val encoded = ScheduledCallCodec.encode(record)
        assertEquals("2", encoded.substringBefore("|"))
        assertEquals(record, ScheduledCallCodec.decode(encoded))
    }

    @Test
    fun malformedAndUnsupportedRecordsAreIgnored() {
        assertNull(ScheduledCallCodec.decode("broken"))
        assertNull(ScheduledCallCodec.decode("3|id|caller|100|SCHEDULED|EXACT"))
        assertNull(ScheduledCallCodec.decode("1|id|%%%|100|NOT_A_STATE"))
        assertNull(ScheduledCallCodec.decode("2|id|caller|100|SCHEDULED|NOT_A_MODE"))
    }

    @Test
    fun recordsWithInvalidIdentifiersAreIgnored() {
        assertNull(
            ScheduledCallCodec.decode(
                "1|not-a-uuid|bm90LWEtdXVpZA|100|SCHEDULED",
            ),
        )
        val callerId = "not-a-uuid"
        val encodedCaller =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(callerId.toByteArray(StandardCharsets.UTF_8))
        assertNull(
            ScheduledCallCodec.decode(
                "1|550e8400-e29b-41d4-a716-446655440000|$encodedCaller|100|SCHEDULED",
            ),
        )
    }
}
