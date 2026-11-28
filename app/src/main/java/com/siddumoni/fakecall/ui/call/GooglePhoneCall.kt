package com.siddumoni.fakecall.ui.call

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.ui.theme.CallPalette
import kotlin.math.roundToInt

/** Layout modelled on the Google Phone app. */
@Composable
internal fun ColumnScope.GooglePhoneCall(
    content: CallContent,
    actions: CallActions,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CallText(callStatus(content), 16, color = CallPalette.secondaryText)
        CallText(content.name, 36)
        if (content.number.isNotBlank()) CallText(content.number, 16, color = CallPalette.secondaryText)
    }
    Box(Modifier.padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        if (content.phase == CallPhase.ACTIVE && content.controls.keypadVisible) {
            CallKeypad(content.controls.dialed, actions.onKey)
        } else {
            CallerAvatar(content.name, 144.dp)
        }
    }
    when (content.phase) {
        CallPhase.INCOMING -> SwipeToAnswer(actions.onAnswer, actions.onHangUp)
        CallPhase.ACTIVE -> GoogleInCallControls(content.controls, actions)
        CallPhase.ENDED -> Box(Modifier.height(72.dp))
    }
}

/** Drag the handset right to answer or left to decline; both ends can also be tapped. */
@Composable
private fun SwipeToAnswer(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    // Positive towards Answer, whichever side of the screen that is.
    var offset by remember { mutableFloatStateOf(0f) }
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(CallPalette.control),
        contentAlignment = Alignment.Center,
    ) {
        val travel = with(LocalDensity.current) { ((maxWidth - 96.dp) / 2).toPx() }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CallActionButton(
                icon = R.drawable.ic_call_end,
                description = stringResource(R.string.call_decline),
                color = CallPalette.decline,
                onClick = onDecline,
                modifier = Modifier.size(72.dp),
            )
            CallActionButton(
                icon = R.drawable.ic_call_handset,
                description = stringResource(R.string.call_answer),
                color = CallPalette.answer,
                onClick = onAnswer,
                modifier = Modifier.size(72.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .absoluteOffset { IntOffset((offset * direction).roundToInt(), 0) }
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CallPalette.controlSelected)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        reverseDirection = direction < 0,
                        state =
                            rememberDraggableState { delta ->
                                offset = (offset + delta).coerceIn(-travel, travel)
                            },
                        onDragStopped = {
                            when {
                                offset >= travel * SWIPE_THRESHOLD -> onAnswer()
                                offset <= -travel * SWIPE_THRESHOLD -> onDecline()
                                else -> animate(offset, 0f) { value, _ -> offset = value }
                            }
                        },
                    ).testTag("call-swipe-handle"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_call_handset),
                contentDescription = null,
                tint = CallPalette.onControlSelected,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun GoogleInCallControls(
    controls: CallControls,
    actions: CallActions,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (controls.moreVisible) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallControlButton(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_call_hold,
                    label = stringResource(R.string.call_hold),
                    shape = shape,
                    selected = controls.onHold,
                    onClick = actions.onToggleHold,
                )
                // Shown greyed out, as a phone does when the network does not offer them.
                CallControlButton(
                    R.drawable.ic_call_add,
                    stringResource(R.string.call_add),
                    shape,
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
                CallControlButton(
                    R.drawable.ic_call_video,
                    stringResource(R.string.call_video),
                    shape,
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_keypad,
                label = stringResource(R.string.call_keypad),
                shape = shape,
                selected = controls.keypadVisible,
                onClick = actions.onToggleKeypad,
            )
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_mute,
                label = stringResource(R.string.call_mute),
                shape = shape,
                selected = controls.muted,
                onClick = actions.onToggleMute,
            )
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_speaker,
                label = stringResource(R.string.call_speaker),
                shape = shape,
                selected = controls.speaker,
                onClick = actions.onToggleSpeaker,
            )
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_more,
                label = stringResource(R.string.call_more),
                shape = shape,
                selected = controls.moreVisible,
                onClick = actions.onToggleMore,
            )
        }
        CallActionButton(
            icon = R.drawable.ic_call_end,
            description = stringResource(R.string.call_end),
            color = CallPalette.decline,
            onClick = actions.onHangUp,
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(36.dp),
        )
    }
}

private const val SWIPE_THRESHOLD = 0.6f
