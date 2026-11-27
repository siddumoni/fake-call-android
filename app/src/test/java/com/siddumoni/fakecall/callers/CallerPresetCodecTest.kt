package com.siddumoni.fakecall.callers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class CallerPresetCodecTest {
    @Test
    fun roundTripsUnicodeAndPunctuation() {
        val source = CallerPreset(UUID.randomUUID().toString(), "Māma • ❤️", "+1 (555) 010-1234")
        assertEquals(source, CallerPresetCodec.decode(CallerPresetCodec.encode(source)))
    }

    @Test
    fun rejectsMalformedRecords() {
        assertNull(CallerPresetCodec.decode("bad"))
        assertNull(CallerPresetCodec.decode("not-a-uuid.name.number"))
        assertNull(CallerPresetCodec.decode("00000000-0000-0000-0000-000000000000.%%%"))
    }

    @Test
    fun rejectsInvalidStoredFields() {
        val invalid =
            CallerPreset(
                id = UUID.randomUUID().toString(),
                displayName = "",
                numberLabel = "",
            )
        assertNull(CallerPresetCodec.decode(CallerPresetCodec.encode(invalid)))
    }
}
