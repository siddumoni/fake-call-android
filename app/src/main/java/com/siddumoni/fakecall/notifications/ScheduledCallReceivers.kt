package com.siddumoni.fakecall.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class ScheduledCallAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val id =
            intent
                ?.getStringExtra(ScheduledCallAlarmScheduler.EXTRA_SCHEDULE_ID)
                ?.takeIf(::isUuid)
                ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ScheduledCallDelivery.deliver(appContext, id)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isUuid(value: String): Boolean =
        runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)
}

class ScheduledCallTimeoutReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val id =
            intent
                ?.getStringExtra(ScheduledCallAlarmScheduler.EXTRA_SCHEDULE_ID)
                ?.takeIf { value -> runCatching { UUID.fromString(value) }.isSuccess }
                ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ScheduledCallRepository(appContext).transition(id, ScheduledCallAction.TIMEOUT)
                ScheduledCallNotifications.cancel(appContext, id)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ScheduledCallDismissReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val id =
            intent
                ?.getStringExtra(ScheduledCallNotifications.EXTRA_SCHEDULE_ID)
                ?.takeIf { value -> runCatching { UUID.fromString(value) }.isSuccess }
                ?: return
        val deadline =
            intent
                .takeIf { it.hasExtra(ScheduledCallNotifications.EXTRA_RING_DEADLINE_ELAPSED) }
                ?.getLongExtra(ScheduledCallNotifications.EXTRA_RING_DEADLINE_ELAPSED, 0L)
        val action = notificationRemovalAction(deadline, SystemClock.elapsedRealtime())
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ScheduledCallRepository(appContext).transition(id, action)
                ScheduledCallAlarmScheduler(appContext).cancelTimeout(id)
                ScheduledCallNotifications.cancel(appContext, id)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
