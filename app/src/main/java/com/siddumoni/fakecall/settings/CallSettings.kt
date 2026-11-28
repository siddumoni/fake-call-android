package com.siddumoni.fakecall.settings

/** Which phone app the call screens resemble. */
enum class CallScreenStyle {
    /** The Google Phone app, the default dialer on many OnePlus phones. */
    GOOGLE_PHONE,

    /** The OnePlus dialer. */
    ONEPLUS,
}

data class CallSettings(
    val style: CallScreenStyle = CallScreenStyle.GOOGLE_PHONE,
    /** Opt-in: show the incoming call full screen, including over the lock screen. */
    val fullScreen: Boolean = false,
    val triggerCallerId: String? = null,
    val triggerDelaySeconds: Int = 10,
) {
    companion object {
        const val MIN_DELAY_SECONDS = 1
        const val MAX_DELAY_SECONDS = 2 * 60 * 60

        fun clampDelay(seconds: Int): Int = seconds.coerceIn(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS)

        /** Combines the minutes and seconds fields; blank means zero, anything else invalid is null. */
        fun delayFrom(
            minutes: String,
            seconds: String,
        ): Int? {
            val minutePart = if (minutes.isBlank()) 0 else minutes.toIntOrNull() ?: return null
            val secondPart = if (seconds.isBlank()) 0 else seconds.toIntOrNull() ?: return null
            if (minutePart !in 0..MAX_DELAY_SECONDS / 60 || secondPart !in 0..MAX_DELAY_SECONDS) return null
            val total = minutePart * 60 + secondPart
            return total.takeIf { it in MIN_DELAY_SECONDS..MAX_DELAY_SECONDS }
        }
    }
}
