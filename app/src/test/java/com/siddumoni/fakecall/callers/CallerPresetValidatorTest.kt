package com.siddumoni.fakecall.callers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallerPresetValidatorTest {
    @Test
    fun trimsAndAcceptsDisplayNameAndOptionalNumber() {
        assertNull(CallerPresetValidator.validate("  Mom  ", "  +1 (555) 010-1234  "))
        assertEquals(
            "Mom" to "+1 (555) 010-1234",
            CallerPresetValidator.normalize("  Mom  ", "  +1 (555) 010-1234  "),
        )
    }

    @Test
    fun rejectsBlankNamesAndNamesOverLimit() {
        assertEquals(CallerPresetValidationError.NAME_REQUIRED, CallerPresetValidator.validate("  ", ""))
        assertEquals(CallerPresetValidationError.NAME_TOO_LONG, CallerPresetValidator.validate("x".repeat(41), ""))
    }

    @Test
    fun rejectsControlCharactersAndInvalidNumberCharacters() {
        assertEquals(CallerPresetValidationError.NAME_INVALID, CallerPresetValidator.validate("Na\nme", ""))
        assertEquals(CallerPresetValidationError.NUMBER_INVALID, CallerPresetValidator.validate("Name", "555 CALL"))
        assertEquals(CallerPresetValidationError.NUMBER_TOO_LONG, CallerPresetValidator.validate("Name", "1".repeat(25)))
    }

    @Test
    fun rejectsBidirectionalFormattingControlsButAllowsRtlText() {
        assertEquals(CallerPresetValidationError.NAME_INVALID, CallerPresetValidator.validate("Alice\\u202E", ""))
        assertEquals(CallerPresetValidationError.NAME_INVALID, CallerPresetValidator.validate("\\u2066Alice\\u2069", ""))
        assertNull(CallerPresetValidator.validate("محمد", ""))
    }

    @Test
    fun permitsBlankOptionalNumberLabel() {
        assertNull(CallerPresetValidator.validate("Fictional caller", ""))
    }
}
