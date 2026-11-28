package com.siddumoni.fakecall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledCallNotificationsTest {
    @Test
    fun scheduledSimulationChannelIsHighImportanceAndDescribesItsPurpose() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ScheduledCallNotifications.ensureChannel(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(ScheduledCallNotifications.CHANNEL_ID)

        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel?.importance)
        assertEquals(
            context.getString(com.siddumoni.fakecall.R.string.schedule_notification_channel_description),
            channel?.description,
        )
        val actualChannel = requireNotNull(channel)
        assertTrue(actualChannel.shouldVibrate())
        assertArrayEquals(longArrayOf(0L, 1_000L, 1_000L), actualChannel.vibrationPattern)
    }

    @Test
    fun newChannelRingsWithTheDefaultRingtone() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val channel = ScheduledCallNotifications.buildChannel(context, legacy = null)

        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (ringtone != null) {
            assertEquals(ringtone, channel.sound)
            assertEquals(AudioAttributes.USAGE_NOTIFICATION_RINGTONE, channel.audioAttributes.usage)
        }
    }

    @Test
    fun channelUpgradeKeepsWhatTheUserTurnedOff() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val legacy =
            NotificationChannel("legacy", "Legacy", NotificationManager.IMPORTANCE_NONE).apply {
                setSound(null, null)
                enableVibration(false)
            }

        val channel = ScheduledCallNotifications.buildChannel(context, legacy)

        assertEquals(NotificationManager.IMPORTANCE_NONE, channel.importance)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())
    }
}
