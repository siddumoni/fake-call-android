package com.siddumoni.fakecall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.settings.CallScreenStyle
import com.siddumoni.fakecall.settings.CallSettings

/** Android permissions and services the call features depend on, as currently granted. */
data class CallAccess(
    val notifications: Boolean = true,
    val fullScreen: Boolean = false,
)

@Composable
fun CallSettingsScreen(
    settings: CallSettings,
    callers: List<CallerPreset>,
    access: CallAccess,
    saveFailed: Boolean,
    onStyleChanged: (CallScreenStyle) -> Unit,
    onFullScreenChanged: (Boolean) -> Unit,
    onTriggerCallerChanged: (String) -> Unit,
    onTriggerDelayChanged: (Int) -> Unit,
    onPreview: (String) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onStartQuickCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = callers.firstOrNull { it.id == settings.triggerCallerId } ?: callers.firstOrNull()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("call-settings-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Body(stringResource(R.string.call_settings_disclaimer))
        if (saveFailed) Warning(stringResource(R.string.call_settings_save_error))
        if (!access.notifications) {
            Warning(stringResource(R.string.schedule_notifications_required))
            Button(onClick = onRequestNotifications) { Text(stringResource(R.string.schedule_enable_notifications)) }
        }

        Heading(stringResource(R.string.call_style_heading))
        Body(stringResource(R.string.call_style_description))
        Choice(
            label = stringResource(R.string.call_style_google),
            selected = settings.style == CallScreenStyle.GOOGLE_PHONE,
            onClick = { onStyleChanged(CallScreenStyle.GOOGLE_PHONE) },
        )
        Choice(
            label = stringResource(R.string.call_style_oneplus),
            selected = settings.style == CallScreenStyle.ONEPLUS,
            onClick = { onStyleChanged(CallScreenStyle.ONEPLUS) },
        )
        OutlinedButton(onClick = { selected?.let { onPreview(it.id) } }, enabled = selected != null) {
            Text(stringResource(R.string.call_style_preview))
        }

        HorizontalDivider()
        Heading(stringResource(R.string.call_full_screen_heading))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = settings.fullScreen, role = Role.Switch, onValueChange = onFullScreenChanged)
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.call_full_screen_title), style = MaterialTheme.typography.bodyLarge)
                Body(stringResource(R.string.call_full_screen_description))
            }
            Switch(checked = settings.fullScreen, onCheckedChange = null)
        }
        if (settings.fullScreen && !access.fullScreen) {
            Warning(stringResource(R.string.call_full_screen_permission))
            Button(onClick = onOpenFullScreenSettings) { Text(stringResource(R.string.call_full_screen_open)) }
        }

        HorizontalDivider()
        Heading(stringResource(R.string.quick_call_heading))
        Body(stringResource(R.string.quick_call_description))
        Text(stringResource(R.string.quick_call_caller), style = MaterialTheme.typography.titleSmall)
        if (callers.isEmpty()) {
            Warning(stringResource(R.string.quick_call_requires_caller))
        } else {
            callers.forEach { caller ->
                Choice(
                    label = caller.displayName,
                    selected = caller.id == selected?.id,
                    onClick = { onTriggerCallerChanged(caller.id) },
                )
            }
        }
        DelayEditor(settings.triggerDelaySeconds, onTriggerDelayChanged)
        Button(
            onClick = onStartQuickCall,
            enabled = selected != null && access.notifications,
        ) {
            Text(stringResource(R.string.quick_call_start))
        }
    }
}

@Composable
private fun DelayEditor(
    savedSeconds: Int,
    onSave: (Int) -> Unit,
) {
    var minutes by rememberSaveable(savedSeconds) { mutableStateOf((savedSeconds / 60).toString()) }
    var seconds by rememberSaveable(savedSeconds) { mutableStateOf((savedSeconds % 60).toString()) }
    val parsed = CallSettings.delayFrom(minutes, seconds)
    Text(stringResource(R.string.quick_call_delay), style = MaterialTheme.typography.titleSmall)
    Body(stringResource(R.string.quick_call_delay_current, savedSeconds / 60, savedSeconds % 60))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = minutes,
            onValueChange = { minutes = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(R.string.quick_call_minutes)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("delay-minutes"),
        )
        OutlinedTextField(
            value = seconds,
            onValueChange = { seconds = it.filter(Char::isDigit).take(4) },
            label = { Text(stringResource(R.string.quick_call_seconds)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("delay-seconds"),
        )
    }
    if (parsed == null) Warning(stringResource(R.string.quick_call_delay_invalid))
    Button(
        onClick = { parsed?.let(onSave) },
        enabled = parsed != null && parsed != savedSeconds,
    ) {
        Text(stringResource(R.string.quick_call_delay_save))
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Warning(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
