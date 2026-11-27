package com.siddumoni.fakecall.scheduling

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/**
 * Versioned, delimiter-safe storage encoding.
 *
 * Version 1 records predate delivery-mode selection and are conservatively migrated to
 * APPROXIMATE so restoring an old record never silently opts the user into exact alarms.
 */
object ScheduledCallCodec {
    private const val VERSION_1 = "1"
    private const val VERSION_2 = "2"

    fun encode(call: ScheduledCall): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val callerId =
            encoder.encodeToString(
                call.callerPresetId.toByteArray(StandardCharsets.UTF_8),
            )
        return listOf(
            VERSION_2,
            call.id,
            callerId,
            call.triggerAtEpochMillis.toString(),
            call.state.name,
            call.deliveryMode.name,
        ).joinToString("|")
    }

    fun decode(raw: String): ScheduledCall? {
        val parts = raw.split("|")
        val version = parts.firstOrNull() ?: return null
        val validShape =
            (version == VERSION_1 && parts.size == 5) ||
                (version == VERSION_2 && parts.size == 6)
        if (!validShape) return null

        return runCatching {
            val callerId = String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8)
            if (!isUuid(parts[1]) || !isUuid(callerId)) return null
            val deliveryMode =
                if (version == VERSION_1) {
                    ScheduledCallDeliveryMode.APPROXIMATE
                } else {
                    ScheduledCallDeliveryMode.valueOf(parts[5])
                }
            ScheduledCall(
                id = parts[1],
                callerPresetId = callerId,
                triggerAtEpochMillis = parts[3].toLong(),
                state = ScheduledCallState.valueOf(parts[4]),
                deliveryMode = deliveryMode,
            )
        }.getOrNull()
    }

    private fun isUuid(value: String): Boolean =
        runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)
}
