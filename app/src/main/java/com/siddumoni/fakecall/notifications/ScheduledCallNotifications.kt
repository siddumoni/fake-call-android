package com.siddumoni.fakecall.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.call.CallActivity
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import java.util.UUID

object ScheduledCallNotifications {
    /** Channel sound is immutable after creation, so a sound change needs a new channel ID. */
    const val CHANNEL_ID = "scheduled_simulations_v3"
    private const val LEGACY_CHANNEL_ID = "scheduled_simulations_v2"

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
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            manager.createNotificationChannel(
                buildChannel(context, manager.getNotificationChannel(LEGACY_CHANNEL_ID)),
            )
        } else {
            // Only the label can change after creation; the user's sound settings are kept.
            existing.name = context.getString(R.string.schedule_notification_channel)
            existing.description = context.getString(R.string.schedule_notification_channel_description)
            manager.createNotificationChannel(existing)
        }
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
    }

    /**
     * The system plays the channel's ringtone, so silent mode, Do Not Disturb, audio routing and
     * user overrides apply without an app-owned player. Anything the user turned down or off on the
     * [legacy] channel stays that way.
     */
    internal fun buildChannel(
        context: Context,
        legacy: NotificationChannel?,
    ): NotificationChannel {
        val importance =
            minOf(
                legacy?.importance ?: NotificationManager.IMPORTANCE_HIGH,
                NotificationManager.IMPORTANCE_HIGH,
            )
        return NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.schedule_notification_channel),
            importance,
        ).apply {
            description = context.getString(R.string.schedule_notification_channel_description)
            if (legacy == null || legacy.shouldVibrate()) {
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 1_000L, 1_000L)
            } else {
                enableVibration(false)
            }
            if (legacy != null && legacy.sound == null) {
                setSound(null, null)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let { ringtone ->
                    setSound(
                        ringtone,
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
            }
        }
    }

    fun show(
        context: Context,
        call: ScheduledCall,
        caller: CallerPreset,
        fullScreen: Boolean = false,
    ): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false

        val contentIntent = callScreenIntent(context, scheduleUri(call.id))
        // A direct Activity intent: Android blocks starting an Activity from a receiver trampoline.
        val answerIntent =
            callScreenIntent(context, scheduleActionUri(call.id, ScheduledCallLink.ANSWER_SEGMENT))
        val declineIntent = dismissBroadcast(context, call.id, "decline", ringDeadline = null)
        val removedIntent =
            dismissBroadcast(
                context,
                call.id,
                "removed",
                ringDeadline = SystemClock.elapsedRealtime() + ScheduledCallAlarmScheduler.RING_TIMEOUT_MILLIS,
            )

        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(caller.displayName)
                .setContentText(context.getString(R.string.call_incoming))
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                // Shown on the lock screen like a phone call, by the owner's decision.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(false)
                .setOnlyAlertOnce(false)
                .setTimeoutAfter(ScheduledCallAlarmScheduler.RING_TIMEOUT_MILLIS)
                .setDeleteIntent(removedIntent)
        if (fullScreen && canUseFullScreen(context)) {
            // Android only accepts the system call style together with a full-screen intent.
            val person =
                Person
                    .Builder()
                    .setName(caller.displayName)
                    .setImportant(true)
                    .build()
            builder
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, declineIntent, answerIntent))
                .setFullScreenIntent(contentIntent, true)
        } else {
            builder
                .addAction(0, context.getString(R.string.call_decline), declineIntent)
                .addAction(0, context.getString(R.string.call_answer), answerIntent)
        }
        val notification = builder.build()
        // Repeat sound and vibration until the notification is removed or the user opens the shade.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        return try {
            NotificationManagerCompat
                .from(context)
                .notify(notificationId(call.id), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun canUseFullScreen(context: Context): Boolean =
        context
            .getSystemService(NotificationManager::class.java)
            .canUseFullScreenIntent()

    private fun callScreenIntent(
        context: Context,
        uri: Uri,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            CallActivity.linkIntent(context, uri),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun cancel(
        context: Context,
        scheduleId: String,
    ) {
        if (isUuid(scheduleId)) {
            NotificationManagerCompat.from(context).cancel(notificationId(scheduleId))
        }
    }

    private fun dismissBroadcast(
        context: Context,
        scheduleId: String,
        path: String,
        ringDeadline: Long?,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ScheduledCallDismissReceiver::class.java).apply {
                action = ACTION_DISMISS
                data = scheduleActionUri(scheduleId, path)
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                ringDeadline?.let { putExtra(EXTRA_RING_DEADLINE_ELAPSED, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun notificationId(id: String): Int = id.hashCode()

    private fun scheduleUri(id: String): Uri =
        Uri
            .Builder()
            .scheme(ScheduledCallLink.SCHEME)
            .authority(ScheduledCallLink.AUTHORITY)
            .appendPath(id)
            .build()

    private fun scheduleActionUri(
        id: String,
        action: String,
    ): Uri = scheduleUri(id).buildUpon().appendPath(action).build()

    private fun isUuid(value: String): Boolean =
        runCatching {
            UUID.fromString(value).toString() == value.lowercase()
        }.getOrDefault(false)

    const val ACTION_DISMISS = "com.siddumoni.fakecall.action.DISMISS_SIMULATION"
    const val EXTRA_SCHEDULE_ID = "com.siddumoni.fakecall.extra.SCHEDULE_ID"
    const val EXTRA_RING_DEADLINE_ELAPSED = "com.siddumoni.fakecall.extra.RING_DEADLINE_ELAPSED"
}
