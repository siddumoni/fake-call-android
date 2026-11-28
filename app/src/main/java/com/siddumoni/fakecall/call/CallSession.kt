package com.siddumoni.fakecall.call

/** Which call, if any, currently has a live in-call screen in this process. */
object CallSession {
    @Volatile
    var activeCallId: String? = null
}
