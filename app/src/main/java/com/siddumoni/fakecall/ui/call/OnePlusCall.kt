package com.siddumoni.fakecall.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.ui.theme.CallPalette

/** Layout modelled on the OnePlus dialer. */
@Composable
internal fun ColumnScope.OnePlusCall(
    content: CallContent,
    actions: CallActions,
) {
    Column(
        modifier = Modifier.padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CallText(content.name, 34)
        if (content.number.isNotBlank()) CallText(content.number, 16, color = CallPalette.secondaryText)
        CallText(callStatus(content), 16, color = CallPalette.secondaryText)
    }
    Box(Modifier.padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        when {
            content.phase != CallPhase.ACTIVE -> CallerAvatar(content.name, 112.dp)
            content.controls.keypadVisible -> CallKeypad(content.controls.dialed, actions.onKey)
            else -> OnePlusControlGrid(content.controls, actions)
        }
    }
    when (content.phase) {
        CallPhase.INCOMING ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LabelledAction(R.drawable.ic_call_end, R.string.call_decline, CallPalette.decline, actions.onHangUp)
                LabelledAction(R.drawable.ic_call_handset, R.string.call_answer, CallPalette.answer, actions.onAnswer)
            }
        CallPhase.ACTIVE ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CallActionButton(
                    icon = R.drawable.ic_call_end,
                    description = stringResource(R.string.call_end),
                    color = CallPalette.decline,
                    onClick = actions.onHangUp,
                )
                if (content.controls.keypadVisible) {
                    CallControlButton(
                        icon = R.drawable.ic_call_keypad,
                        label = stringResource(R.string.call_hide_keypad),
                        shape = CircleShape,
                        onClick = actions.onToggleKeypad,
                    )
                }
            }
        CallPhase.ENDED -> Box(Modifier.height(76.dp))
    }
}

@Composable
private fun LabelledAction(
    icon: Int,
    label: Int,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CallActionButton(icon, stringResource(label), color, onClick)
        CallText(stringResource(label), 14, color = CallPalette.secondaryText)
    }
}

@Composable
private fun OnePlusControlGrid(
    controls: CallControls,
    actions: CallActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_mute,
                label = stringResource(R.string.call_mute),
                shape = CircleShape,
                selected = controls.muted,
                onClick = actions.onToggleMute,
            )
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_keypad,
                label = stringResource(R.string.call_keypad),
                shape = CircleShape,
                onClick = actions.onToggleKeypad,
            )
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_speaker,
                label = stringResource(R.string.call_speaker),
                shape = CircleShape,
                selected = controls.speaker,
                onClick = actions.onToggleSpeaker,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // Shown greyed out, as a phone does when the network does not offer them.
            CallControlButton(
                R.drawable.ic_call_add,
                stringResource(R.string.call_add),
                CircleShape,
                enabled = false,
                modifier = Modifier.weight(1f),
            )
            CallControlButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_call_hold,
                label = stringResource(R.string.call_hold),
                shape = CircleShape,
                selected = controls.onHold,
                onClick = actions.onToggleHold,
            )
            CallControlButton(
                R.drawable.ic_call_video,
                stringResource(R.string.call_video),
                CircleShape,
                enabled = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
