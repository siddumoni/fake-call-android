package com.siddumoni.fakecall.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.siddumoni.fakecall.scheduling.ScheduleReconciler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Reconciles RTC alarms after restart, app update, time changes and exact-alarm access grants. */
class ScheduleRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action = intent?.action ?: return
        if (action !in HANDLED_ACTIONS) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ScheduledCallNotifications.ensureChannel(appContext)
                ScheduleReconciler(appContext).reconcile(
                    notificationsAllowed = ScheduledCallNotifications.canPost(appContext),
                    afterReboot = action == Intent.ACTION_BOOT_COMPLETED,
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                // The next foreground reconciliation retries. No record details are logged.
                Log.w(TAG, "Schedule restore failed")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ScheduleRestore"
        val HANDLED_ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
    }
}
