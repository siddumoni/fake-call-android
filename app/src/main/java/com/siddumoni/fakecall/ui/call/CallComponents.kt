package com.siddumoni.fakecall.ui.call

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.settings.CallScreenStyle
import com.siddumoni.fakecall.ui.theme.CallPalette

@Composable
internal fun CallBackground(
    style: CallScreenStyle,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val brush =
        when (style) {
            CallScreenStyle.GOOGLE_PHONE ->
                Brush.verticalGradient(listOf(CallPalette.background, CallPalette.background))
            CallScreenStyle.ONEPLUS ->
                Brush.verticalGradient(listOf(CallPalette.onePlusTop, CallPalette.onePlusBottom))
        }
    Box(modifier.fillMaxSize().background(brush)) { content() }
}

@Composable
internal fun CallText(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    color: Color = CallPalette.onBackground,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        lineHeight = (size * 1.25f).sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun CallerAvatar(
    name: String,
    diameter: Dp,
) {
    Box(
        modifier = Modifier.size(diameter).clip(CircleShape).background(CallPalette.avatar),
        contentAlignment = Alignment.Center,
    ) {
        val initial =
            name
                .trim()
                .takeIf { it.isNotEmpty() }
                ?.let { String(Character.toChars(it.codePointAt(0))).uppercase() }
                .orEmpty()
        CallText(initial, (diameter.value * 0.4f).toInt())
    }
}

/** The status line under the caller: ringing, the running timer, hold, or the end of the call. */
@Composable
internal fun callStatus(content: CallContent): String =
    when (content.phase) {
        CallPhase.INCOMING -> stringResource(R.string.call_incoming)
        CallPhase.ACTIVE ->
            if (content.controls.onHold) {
                stringResource(R.string.call_on_hold)
            } else {
                content.duration.orEmpty()
            }
        CallPhase.ENDED -> stringResource(R.string.call_ended)
    }

/** A large round (or pill) button that only has an icon, named for screen readers. */
@Composable
internal fun CallActionButton(
    @DrawableRes icon: Int,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(76.dp),
    shape: Shape = CircleShape,
) {
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(color)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = CallPalette.onBackground, modifier = Modifier.size(34.dp))
    }
}

/** A labelled in-call control. [selected] is null for a button that is not a toggle. */
@Composable
internal fun CallControlButton(
    @DrawableRes icon: Int,
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val highlighted = selected == true
    val interaction =
        if (selected != null) {
            Modifier.toggleable(value = selected, enabled = enabled, role = Role.Switch, onValueChange = { onClick() })
        } else {
            Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        }
    val tint =
        when {
            !enabled -> CallPalette.disabled
            highlighted -> CallPalette.onControlSelected
            else -> CallPalette.onBackground
        }
    Column(
        modifier = modifier.widthIn(min = 64.dp).then(interaction).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(shape)
                    .background(if (highlighted) CallPalette.controlSelected else CallPalette.control),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        }
        CallText(label, 13, color = if (enabled) CallPalette.secondaryText else CallPalette.disabled)
    }
}

@Composable
internal fun CallKeypad(
    dialed: String,
    onKey: (Char) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CallText(dialed, 28)
        listOf("123", "456", "789", "*0#").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CallPalette.control)
                                .clickable(role = Role.Button) { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        CallText(key.toString(), 24)
                    }
                }
            }
        }
    }
}
