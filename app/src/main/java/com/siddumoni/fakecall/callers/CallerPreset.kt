package com.siddumoni.fakecall.callers

data class CallerPreset(
    val id: String,
    val displayName: String,
    val numberLabel: String,
)

enum class CallerPresetValidationError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    NAME_INVALID,
    NUMBER_TOO_LONG,
    NUMBER_INVALID,
    STORAGE,
}

object CallerPresetValidator {
    const val MAX_NAME_LENGTH = 40
    const val MAX_NUMBER_LABEL_LENGTH = 24

    fun validate(
        displayName: String,
        numberLabel: String,
    ): CallerPresetValidationError? {
        val name = displayName.trim()
        val number = numberLabel.trim()

        if (name.isEmpty()) return CallerPresetValidationError.NAME_REQUIRED
        if (name.length > MAX_NAME_LENGTH) return CallerPresetValidationError.NAME_TOO_LONG
        if (name.any { it.isISOControl() || it.isBidiFormattingControl() }) return CallerPresetValidationError.NAME_INVALID
        if (number.length > MAX_NUMBER_LABEL_LENGTH) return CallerPresetValidationError.NUMBER_TOO_LONG
        if (number.any { it !in "+0123456789(). -" }) {
            return CallerPresetValidationError.NUMBER_INVALID
        }
        return null
    }

    private fun Char.isBidiFormattingControl(): Boolean =
        code in 0x202A..0x202E || code in 0x2066..0x206F || code == 0x061C || code == 0x200E || code == 0x200F

    fun normalize(
        displayName: String,
        numberLabel: String,
    ): Pair<String, String> {
        require(validate(displayName, numberLabel) == null) {
            "Caller preset fields are invalid."
        }
        return displayName.trim() to numberLabel.trim()
    }
}
