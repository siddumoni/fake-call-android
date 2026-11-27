package com.siddumoni.fakecall.callers

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

internal object CallerPresetCodec {
    fun encode(preset: CallerPreset): String {
        val displayName = encodeText(preset.displayName)
        val numberLabel = encodeText(preset.numberLabel)
        return "${preset.id}.$displayName.$numberLabel"
    }

    fun decode(encoded: String): CallerPreset? {
        val parts = encoded.split('.', limit = 3)
        if (parts.size != 3) return null

        val id = runCatching { UUID.fromString(parts[0]).toString() }.getOrNull() ?: return null
        val displayName = decodeText(parts[1]) ?: return null
        val numberLabel = decodeText(parts[2]) ?: return null
        if (CallerPresetValidator.validate(displayName, numberLabel) != null) return null

        return CallerPreset(
            id = id,
            displayName = displayName,
            numberLabel = numberLabel,
        )
    }

    private fun encodeText(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String? =
        runCatching {
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        }.getOrNull()
}
