package com.siddumoni.fakecall.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.siddumoni.fakecall.MainActivity
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import java.util.UUID

object ScheduledCallNotifications {
    const val CHANNEL_ID = "scheduled_simulations"

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                context
                    .getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return true
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.schedule_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.schedule_notification_channel_description)
            },
        )
    }

    fun show(
        context: Context,
        call: ScheduledCall,
        caller: CallerPreset,
    ): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false

        val contentIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = scheduleUri(call.id)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val dismissIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ScheduledCallDismissReceiver::class.java).apply {
                    action = ACTION_DISMISS
                    data = scheduleActionUri(call.id, "dismiss")
                    putExtra(EXTRA_SCHEDULE_ID, call.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val publicVersion =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.schedule_notification_title))
                .setContentText(context.getString(R.string.schedule_notification_public_text))
                .build()

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.schedule_notification_title))
                .setContentText(
                    context.getString(R.string.schedule_notification_text, caller.displayName),
                ).setSubText(context.getString(R.string.simulated_call_banner))
                .setPublicVersion(publicVersion)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(false)
                .setOnlyAlertOnce(false)
                .setTimeoutAfter(ScheduledCallAlarmScheduler.RING_TIMEOUT_MILLIS)
                .addAction(
                    0,
                    context.getString(R.string.schedule_notification_dismiss),
                    dismissIntent,
                ).build()

        return try {
            NotificationManagerCompat
                .from(context)
                .notify(notificationId(call.id), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(
        context: Context,
        scheduleId: String,
    ) {
        if (isUuid(scheduleId)) {
            NotificationManagerCompat.from(context).cancel(notificationId(scheduleId))
        }
    }

    private fun notificationId(id: String): Int = id.hashCode()

    private fun scheduleUri(id: String): Uri =
        Uri
            .Builder()
            .scheme("alibicall")
            .authority("scheduled-call")
            .appendPath(id)
            .build()

    private fun scheduleActionUri(
        id: String,
        action: String,
    ): Uri =
        Uri
            .Builder()
            .scheme("alibicall")
            .authority("scheduled-call")
            .appendPath(id)
            .appendPath(action)
            .build()

    private fun isUuid(value: String): Boolean =
        runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)

    const val ACTION_DISMISS = "com.siddumoni.fakecall.action.DISMISS_SIMULATION"
    const val EXTRA_SCHEDULE_ID = "com.siddumoni.fakecall.extra.SCHEDULE_ID"
}
