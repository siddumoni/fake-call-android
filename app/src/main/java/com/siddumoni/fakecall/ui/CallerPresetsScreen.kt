package com.siddumoni.fakecall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.callers.CallerPresetValidationError
import com.siddumoni.fakecall.callers.CallerPresetValidator

@Composable
fun CallerPresetsScreen(
    presets: List<CallerPreset>,
    saveError: CallerPresetValidationError?,
    onSave: (String?, String, String) -> Boolean,
    onDelete: (String) -> Unit,
    onPreview: (CallerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<CallerPreset?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CallerPreset?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp).testTag("caller-presets-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.caller_presets_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.caller_presets_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (saveError != null) {
            Text(
                text = stringResource(R.string.caller_preset_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = { editing = null; showCreate = true },
            modifier = Modifier.fillMaxWidth().testTag("caller-preset-add-button"),
        ) {
            Text(stringResource(R.string.caller_preset_add))
        }
        if (presets.isEmpty()) {
            Text(stringResource(R.string.caller_presets_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(presets, key = { it.id }) { preset ->
                    CallerPresetCard(
                        preset = preset,
                        onPreview = { onPreview(preset) },
                        onEdit = { editing = preset },
                        onDelete = { deleting = preset },
                    )
                }
            }
        }
    }

    if (showCreate || editing != null) {
        CallerPresetEditorDialog(
            preset = editing,
            onDismiss = { showCreate = false; editing = null },
            onSave = onSave,
        )
    }
    deleting?.let { preset ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.caller_preset_delete_title)) },
            text = { Text(stringResource(R.string.caller_preset_delete_message, preset.displayName)) },
            confirmButton = {
                TextButton(onClick = { onDelete(preset.id); deleting = null }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CallerPresetCard(
    preset: CallerPreset,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(preset.displayName, style = MaterialTheme.typography.titleMedium)
            if (preset.numberLabel.isNotBlank()) {
                Text(preset.numberLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPreview) { Text(stringResource(R.string.caller_preset_preview)) }
                OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
private fun CallerPresetEditorDialog(
    preset: CallerPreset?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String) -> Boolean,
) {
    var name by remember(preset?.id) { mutableStateOf(preset?.displayName.orEmpty()) }
    var number by remember(preset?.id) { mutableStateOf(preset?.numberLabel.orEmpty()) }
    var validationError by remember(preset?.id) { mutableStateOf<CallerPresetValidationError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (preset == null) R.string.caller_preset_add else R.string.caller_preset_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text(stringResource(R.string.caller_preset_name)) },
                    singleLine = true,
                    modifier = Modifier.testTag("caller-preset-name"),
                )
                TextField(
                    value = number,
                    onValueChange = { number = it; validationError = null },
                    label = { Text(stringResource(R.string.caller_preset_number_label)) },
                    supportingText = { Text(stringResource(R.string.caller_preset_number_hint)) },
                    singleLine = true,
                    modifier = Modifier.testTag("caller-preset-number"),
                )
                validationError?.let {
                    Text(
                        text = stringResource(validationMessage(it)),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val error = CallerPresetValidator.validate(name, number)
                if (error != null) {
                    validationError = error
                } else if (onSave(preset?.id, name, number)) {
                    onDismiss()
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun validationMessage(error: CallerPresetValidationError): Int =
    when (error) {
        CallerPresetValidationError.NAME_REQUIRED -> R.string.caller_preset_name_required
        CallerPresetValidationError.NAME_TOO_LONG -> R.string.caller_preset_name_too_long
        CallerPresetValidationError.NAME_INVALID -> R.string.caller_preset_name_invalid
        CallerPresetValidationError.NUMBER_TOO_LONG -> R.string.caller_preset_number_too_long
        CallerPresetValidationError.NUMBER_INVALID -> R.string.caller_preset_number_invalid
        CallerPresetValidationError.STORAGE -> R.string.caller_preset_error
    }
