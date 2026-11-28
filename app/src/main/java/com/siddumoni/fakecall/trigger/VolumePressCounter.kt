package com.siddumoni.fakecall.trigger

/** Detects [requiredPresses] volume-down presses that all happen within [windowMillis]. */
class VolumePressCounter(
    private val requiredPresses: Int = REQUIRED_PRESSES,
    private val windowMillis: Long = WINDOW_MILLIS,
) {
    private val presses = ArrayDeque<Long>()

    /** Records one press and returns true exactly when it completes the sequence. */
    fun register(timeMillis: Long): Boolean {
        presses.addLast(timeMillis)
        while (timeMillis - presses.first() > windowMillis) presses.removeFirst()
        if (presses.size < requiredPresses) return false
        presses.clear()
        return true
    }

    companion object {
        const val REQUIRED_PRESSES = 5
        const val WINDOW_MILLIS = 3_000L
    }
}
