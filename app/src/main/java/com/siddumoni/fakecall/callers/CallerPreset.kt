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
        if (name.any(Char::isISOControl)) return CallerPresetValidationError.NAME_INVALID
        if (number.length > MAX_NUMBER_LABEL_LENGTH) return CallerPresetValidationError.NUMBER_TOO_LONG
        if (number.any { it !in "+0123456789(). -" }) {
            return CallerPresetValidationError.NUMBER_INVALID
        }
        return null
    }

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
