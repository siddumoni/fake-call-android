package com.siddumoni.fakecall

import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertArrayEquals(longArrayOf(0L, 300L, 200L, 300L), actualChannel.vibrationPattern)
        val defaultSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        if (defaultSound != null) assertEquals(defaultSound, actualChannel.sound)
    }
}
