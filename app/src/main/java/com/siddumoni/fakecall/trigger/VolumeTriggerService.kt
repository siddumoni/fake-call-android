package com.siddumoni.fakecall.trigger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.siddumoni.fakecall.notifications.ScheduledCallDelivery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Opt-in shortcut: five quick volume-down presses start the configured call.
 *
 * Only volume-down key presses are inspected. The service requests no window content and no
 * accessibility events, and it never consumes the key, so the volume still changes normally.
 */
class VolumeTriggerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val counter = VolumePressCounter()
    private var triggerJob: Job? = null

    @Volatile
    private var pendingId: String? = null

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0 &&
            counter.register(event.eventTime)
        ) {
            startCall()
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startCall() {
        val previous = triggerJob
        triggerJob =
            scope.launch {
                val outcome =
                    try {
                        QuickCallTrigger(applicationContext).create(pendingId)
                    } catch (exception: Exception) {
                        if (exception is CancellationException) throw exception
                        QuickCallOutcome.Failed
                    }
                if (outcome == QuickCallOutcome.Busy) return@launch
                confirm(accepted = outcome is QuickCallOutcome.Scheduled)
                if (outcome !is QuickCallOutcome.Scheduled) return@launch
                previous?.cancel()
                pendingId = outcome.call.id
                // Android delays alarms that are only seconds away, so ring from here while this
                // process is alive. The alarm stays registered as a backup; delivery happens once.
                while (System.currentTimeMillis() < outcome.call.triggerAtEpochMillis) {
                    delay((outcome.call.triggerAtEpochMillis - System.currentTimeMillis()).coerceIn(1L, 1_000L))
                }
                ScheduledCallDelivery.deliver(applicationContext, outcome.call.id)
            }
    }

    /** One short buzz means the call is coming; two mean it could not be set up. */
    private fun confirm(accepted: Boolean) {
        val effect =
            if (accepted) {
                VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createWaveform(longArrayOf(0L, 80L, 120L, 80L), -1)
            }
        getSystemService(VibratorManager::class.java)
            .defaultVibrator
            .vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY))
    }

    companion object {
        fun isEnabled(context: Context): Boolean =
            context
                .getSystemService(AccessibilityManager::class.java)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { info ->
                    val service = info.resolveInfo.serviceInfo
                    service.packageName == context.packageName &&
                        service.name == VolumeTriggerService::class.java.name
                }
    }
}
