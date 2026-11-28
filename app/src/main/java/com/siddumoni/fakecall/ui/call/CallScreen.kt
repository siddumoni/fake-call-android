package com.siddumoni.fakecall.ui.call

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.settings.CallScreenStyle
import kotlinx.coroutines.delay

enum class CallPhase {
    INCOMING,
    ACTIVE,
    ENDED,
}

/** The in-call buttons. They change only what is on screen; there is no audio connection. */
data class CallControls(
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val onHold: Boolean = false,
    val keypadVisible: Boolean = false,
    val moreVisible: Boolean = false,
    val dialed: String = "",
)

internal data class CallActions(
    val onAnswer: () -> Unit,
    val onHangUp: () -> Unit,
    val onToggleMute: () -> Unit,
    val onToggleSpeaker: () -> Unit,
    val onToggleHold: () -> Unit,
    val onToggleKeypad: () -> Unit,
    val onToggleMore: () -> Unit,
    val onKey: (Char) -> Unit,
)

/** `m:ss` as phone apps show it, with hours only once they are needed. */
internal fun formatCallDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    val hours = seconds / 3_600
    val minutes = seconds % 3_600 / 60
    val rest = (seconds % 60).toString().padStart(2, '0')
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:$rest"
    } else {
        "${minutes.toString().padStart(2, '0')}:$rest"
    }
}

@Composable
fun CallScreen(
    style: CallScreenStyle,
    name: String,
    number: String,
    phase: CallPhase,
    connectedAtElapsedMillis: Long?,
    onAnswer: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier,
    onSpeakerChanged: (Boolean) -> Unit = {},
    onKeyTone: (Char) -> Unit = {},
) {
    var muted by rememberSaveable(name) { mutableStateOf(false) }
    var speaker by rememberSaveable(name) { mutableStateOf(false) }
    var onHold by rememberSaveable(name) { mutableStateOf(false) }
    var keypad by rememberSaveable(name) { mutableStateOf(false) }
    var more by rememberSaveable(name) { mutableStateOf(false) }
    var dialed by rememberSaveable(name) { mutableStateOf("") }

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    // Stops at the moment of hanging up, so the final duration stays on the "Call ended" screen.
    LaunchedEffect(phase, connectedAtElapsedMillis) {
        while (phase == CallPhase.ACTIVE) {
            now = SystemClock.elapsedRealtime()
            delay(500L)
        }
    }
    val duration =
        connectedAtElapsedMillis?.let { formatCallDuration((now - it) / 1_000L) }

    val controls = CallControls(muted, speaker, onHold, keypad, more, dialed)
    val actions =
        CallActions(
            onAnswer = onAnswer,
            onHangUp = onHangUp,
            onToggleMute = { muted = !muted },
            onToggleSpeaker = {
                speaker = !speaker
                onSpeakerChanged(speaker)
            },
            onToggleHold = { onHold = !onHold },
            onToggleKeypad = { keypad = !keypad },
            onToggleMore = { more = !more },
            onKey = { key ->
                dialed = (dialed + key).takeLast(MAX_DIALED)
                onKeyTone(key)
            },
        )

    CallBackground(style, modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Scrolls in landscape or at large font sizes; otherwise it fills the screen.
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                val content = CallContent(name, number, phase, duration, controls)
                when (style) {
                    CallScreenStyle.GOOGLE_PHONE -> GooglePhoneCall(content, actions)
                    CallScreenStyle.ONEPLUS -> OnePlusCall(content, actions)
                }
            }
        }
    }
}

internal data class CallContent(
    val name: String,
    val number: String,
    val phase: CallPhase,
    val duration: String?,
    val controls: CallControls,
)

private const val MAX_DIALED = 24
