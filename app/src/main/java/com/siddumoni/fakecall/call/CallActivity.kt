package com.siddumoni.fakecall.call

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.siddumoni.fakecall.data.preferences.CallSettingsRepository
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.notifications.ScheduledCallLink
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import com.siddumoni.fakecall.settings.CallSettings
import com.siddumoni.fakecall.ui.call.CallPhase
import com.siddumoni.fakecall.ui.call.CallScreen
import com.siddumoni.fakecall.ui.theme.AlibiCallTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private data class CallDetails(
    /** Null while previewing the screen from inside the app. */
    val scheduleId: String?,
    val name: String,
    val number: String,
)

/**
 * The incoming and in-call screens. It is not exported: only this app and its own notification
 * can open it, and it offers no way into the rest of the app, so it is safe over the lock screen.
 */
class CallActivity : ComponentActivity() {
    private var details by mutableStateOf<CallDetails?>(null)
    private var phase by mutableStateOf(CallPhase.INCOMING)
    private var connectedAt by mutableStateOf<Long?>(null)
    private var speakerOn = false
    private var loadJob: Job? = null
    private var closeJob: Job? = null
    private var proximityLock: PowerManager.WakeLock? = null
    private var toneGenerator: ToneGenerator? = null

    private val schedules by lazy { ScheduledCallRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings: CallSettings =
            runBlocking(Dispatchers.IO) { CallSettingsRepository(applicationContext).settings.first() }
        if (settings.fullScreen) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(0), navigationBarStyle = SystemBarStyle.dark(0))

        // Like a phone call, Back does not hang up. A preview can simply be closed.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (details?.scheduleId == null) finishAndRemoveTask()
                }
            },
        )
        setContent {
            AlibiCallTheme {
                val current = details
                if (current != null) {
                    CallScreen(
                        style = settings.style,
                        name = current.name,
                        number = current.number,
                        phase = phase,
                        connectedAtElapsedMillis = connectedAt,
                        onAnswer = ::answer,
                        onHangUp = ::hangUp,
                        onSpeakerChanged = { enabled ->
                            speakerOn = enabled
                            updateProximityLock()
                        },
                        onKeyTone = ::playKeyTone,
                    )
                }
            }
        }
        observeCallState()
        load(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        load(intent)
    }

    override fun onResume() {
        super.onResume()
        updateProximityLock()
    }

    override fun onStop() {
        releaseProximityLock()
        super.onStop()
    }

    override fun onDestroy() {
        val id = details?.scheduleId
        if (isFinishing && id != null && phase == CallPhase.ACTIVE) {
            persist(id, ScheduledCallAction.END)
        }
        if (isFinishing && CallSession.activeCallId == id) CallSession.activeCallId = null
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    /** The volume keys silence a ringing call, as they do for a phone call. */
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        val id = details?.scheduleId
        val volumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        if (volumeKey && id != null && phase == CallPhase.INCOMING) {
            ScheduledCallNotifications.cancel(this, id)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun load(intent: Intent) {
        val data = intent.data ?: return finishIfEmpty()
        loadJob?.cancel()
        loadJob =
            lifecycleScope.launch {
                if (data.scheme == ScheduledCallLink.SCHEME && data.authority == PREVIEW_AUTHORITY) {
                    loadPreview(data.lastPathSegment)
                } else {
                    val link = ScheduledCallLink.parse(data.scheme, data.authority, data.pathSegments)
                    if (link != null) loadScheduled(link)
                }
                finishIfEmpty()
            }
    }

    private suspend fun loadPreview(callerId: String?) {
        // A preview never replaces a call that is on screen.
        if (details?.scheduleId != null && phase != CallPhase.ENDED) return
        val caller = findCaller(callerId) ?: return
        resetFor(CallDetails(null, caller.displayName, caller.numberLabel), CallPhase.INCOMING)
    }

    private suspend fun loadScheduled(link: ScheduledCallLink) {
        val call = schedules.calls.first().firstOrNull { it.id == link.scheduleId } ?: return
        if (call.state != ScheduledCallState.RINGING && call.state != ScheduledCallState.IN_CALL) return
        val caller = findCaller(call.callerPresetId) ?: return
        val previous = details?.scheduleId
        if (previous != call.id) {
            // Switching calls must not leave the previous one answered forever.
            if (previous != null && phase == CallPhase.ACTIVE) persist(previous, ScheduledCallAction.END)
            val initial = if (call.state == ScheduledCallState.IN_CALL) CallPhase.ACTIVE else CallPhase.INCOMING
            resetFor(CallDetails(call.id, caller.displayName, caller.numberLabel), initial)
        }
        if (link.answer && phase == CallPhase.INCOMING) answer()
    }

    private suspend fun findCaller(id: String?) =
        CallerPresetRepository(applicationContext)
            .presets
            .first()
            .firstOrNull { it.id == id }

    private fun resetFor(
        next: CallDetails,
        initial: CallPhase,
    ) {
        closeJob?.cancel()
        speakerOn = false
        connectedAt = null
        details = next
        if (initial == CallPhase.ACTIVE) showConnected() else phase = initial
    }

    private fun finishIfEmpty() {
        if (details == null) finishAndRemoveTask()
    }

    /** Follows changes made elsewhere: the notification's Decline button or the ring timeout. */
    private fun observeCallState() {
        lifecycleScope.launch {
            schedules.calls.collect { calls ->
                val id = details?.scheduleId ?: return@collect
                when (calls.firstOrNull { it.id == id }?.state) {
                    ScheduledCallState.ENDED,
                    ScheduledCallState.TIMED_OUT,
                    ScheduledCallState.CANCELLED,
                    -> showEnded()
                    else -> Unit
                }
            }
        }
    }

    private fun answer() {
        val id = details?.scheduleId
        if (phase != CallPhase.INCOMING) return
        if (id == null) {
            showConnected()
            return
        }
        lifecycleScope.launch {
            if (schedules.transition(id, ScheduledCallAction.ANSWER)) {
                ScheduledCallAlarmScheduler(applicationContext).cancelTimeout(id)
                ScheduledCallNotifications.cancel(applicationContext, id)
                showConnected()
            } else if (schedules.calls.first().none { it.id == id && it.state == ScheduledCallState.IN_CALL }) {
                // The call timed out or was declined in the meantime.
                showEnded()
            }
        }
    }

    private fun hangUp() {
        if (phase == CallPhase.ENDED) return
        val id = details?.scheduleId
        if (id != null) {
            persist(id, if (phase == CallPhase.ACTIVE) ScheduledCallAction.END else ScheduledCallAction.DECLINE)
        }
        showEnded()
    }

    /** Runs outside the activity scope so that closing the screen cannot lose the change. */
    private fun persist(
        id: String,
        action: ScheduledCallAction,
    ) {
        val appContext = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            ScheduledCallRepository(appContext).transition(id, action)
            ScheduledCallAlarmScheduler(appContext).cancelTimeout(id)
            ScheduledCallNotifications.cancel(appContext, id)
        }
    }

    private fun showConnected() {
        connectedAt = connectedAt ?: SystemClock.elapsedRealtime()
        phase = CallPhase.ACTIVE
        CallSession.activeCallId = details?.scheduleId
        updateProximityLock()
    }

    private fun showEnded() {
        if (phase == CallPhase.ENDED) return
        phase = CallPhase.ENDED
        CallSession.activeCallId = null
        releaseProximityLock()
        closeJob =
            lifecycleScope.launch {
                delay(CALL_ENDED_MILLIS)
                finishAndRemoveTask()
            }
    }

    /** Turns the screen off while the phone is held to the ear, unless the speaker is selected. */
    private fun updateProximityLock() {
        if (phase != CallPhase.ACTIVE || speakerOn) {
            releaseProximityLock()
            return
        }
        val powerManager = getSystemService(PowerManager::class.java)
        if (!powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
        val lock =
            proximityLock
                ?: powerManager
                    .newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "alibicall:proximity")
                    .also {
                        it.setReferenceCounted(false)
                        proximityLock = it
                    }
        if (!lock.isHeld) lock.acquire(MAX_PROXIMITY_MILLIS)
    }

    private fun releaseProximityLock() {
        proximityLock?.takeIf { it.isHeld }?.release()
    }

    private fun playKeyTone(key: Char) {
        val tone = DTMF_TONES[key] ?: return
        val generator =
            toneGenerator
                ?: runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 80) }
                    .getOrNull()
                    ?.also { toneGenerator = it }
                ?: return
        generator.startTone(tone, 150)
    }

    companion object {
        private const val PREVIEW_AUTHORITY = "preview"
        private const val CALL_ENDED_MILLIS = 1_200L
        private const val MAX_PROXIMITY_MILLIS = 60L * 60L * 1000L

        private val DTMF_TONES =
            mapOf(
                '0' to ToneGenerator.TONE_DTMF_0,
                '1' to ToneGenerator.TONE_DTMF_1,
                '2' to ToneGenerator.TONE_DTMF_2,
                '3' to ToneGenerator.TONE_DTMF_3,
                '4' to ToneGenerator.TONE_DTMF_4,
                '5' to ToneGenerator.TONE_DTMF_5,
                '6' to ToneGenerator.TONE_DTMF_6,
                '7' to ToneGenerator.TONE_DTMF_7,
                '8' to ToneGenerator.TONE_DTMF_8,
                '9' to ToneGenerator.TONE_DTMF_9,
                '*' to ToneGenerator.TONE_DTMF_S,
                '#' to ToneGenerator.TONE_DTMF_P,
            )

        /** An intent for a `alibicall://scheduled-call/<uuid>[/answer]` link. */
        fun linkIntent(
            context: Context,
            uri: Uri,
        ): Intent =
            Intent(context, CallActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = uri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        fun previewIntent(
            context: Context,
            callerId: String,
        ): Intent =
            linkIntent(
                context,
                Uri
                    .Builder()
                    .scheme(ScheduledCallLink.SCHEME)
                    .authority(PREVIEW_AUTHORITY)
                    .appendPath(callerId)
                    .build(),
            )
    }
}
