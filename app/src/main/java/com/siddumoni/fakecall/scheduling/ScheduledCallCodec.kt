package com.siddumoni.fakecall.scheduling

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/** Versioned, delimiter-safe storage encoding. Unknown or malformed records are ignored. */
object ScheduledCallCodec {
    private const val VERSION = "1"

    fun encode(call: ScheduledCall): String {
        val callerId =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(call.callerPresetId.toByteArray(StandardCharsets.UTF_8))
        return listOf(
            VERSION,
            call.id,
            callerId,
            call.triggerAtEpochMillis.toString(),
            call.state.name,
        ).joinToString("|")
    }

    fun decode(raw: String): ScheduledCall? {
        val parts = raw.split("|")
        if (parts.size != 5 || parts[0] != VERSION) return null
        return runCatching {
            val callerId = String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8)
            if (!isUuid(parts[1]) || !isUuid(callerId)) return null
            ScheduledCall(
                id = parts[1],
                callerPresetId = callerId,
                triggerAtEpochMillis = parts[3].toLong(),
                state = ScheduledCallState.valueOf(parts[4]),
            )
        }.getOrNull()
    }

    private fun isUuid(value: String): Boolean {
        return runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)
    }
}
