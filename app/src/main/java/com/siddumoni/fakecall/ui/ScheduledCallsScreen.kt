package com.siddumoni.fakecall.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.scheduling.ScheduleTimeResolver
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallDeliveryMode
import com.siddumoni.fakecall.scheduling.ScheduledCallOperationState
import com.siddumoni.fakecall.scheduling.ScheduledCallRules
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

@Composable
fun ScheduledCallsScreen(
    calls: List<ScheduledCall>,
    callerPresets: List<CallerPreset>,
    operationState: ScheduledCallOperationState,
    notificationsAllowed: Boolean,
    exactAlarmAllowed: Boolean,
    onSave: (ScheduledCall) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onClearOperationState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ScheduledCall?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ScheduledCall?>(null) }

    LaunchedEffect(operationState) {
        if (operationState == ScheduledCallOperationState.SAVED) {
            creating = false
            editing = null
            onClearOperationState()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(20.dp)
                .testTag("scheduled-calls-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.schedules_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.schedules_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!notificationsAllowed) {
            PermissionNotice(
                message = stringResource(R.string.schedule_notifications_required),
                actionLabel = stringResource(R.string.schedule_enable_notifications),
                onAction = onRequestNotifications,
            )
        }
        Button(
            onClick = {
                editing = null
                creating = true
            },
            enabled = callerPresets.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("schedule-add-button"),
        ) {
            Text(stringResource(R.string.schedule_add))
        }
        if (callerPresets.isEmpty()) {
            Text(
                text = stringResource(R.string.schedule_requires_caller),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OperationMessage(
            state = operationState,
            onRequestNotifications = onRequestNotifications,
            onRequestExactAlarms = onRequestExactAlarms,
        )
        if (calls.isEmpty()) {
            Text(
                text = stringResource(R.string.schedules_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(calls, key = { it.id }) { call ->
                    ScheduledCallCard(
                        call = call,
                        caller = callerPresets.firstOrNull { it.id == call.callerPresetId },
                        onEdit = { editing = call },
                        onCancel = { onCancel(call.id) },
                        onDelete = { deleting = call },
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        ScheduleEditorDialog(
            existing = editing,
            callerPresets = callerPresets,
            operationState = operationState,
            exactAlarmAllowed = exactAlarmAllowed,
            notificationsAllowed = notificationsAllowed,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = onSave,
            onRequestNotifications = onRequestNotifications,
            onRequestExactAlarms = onRequestExactAlarms,
        )
    }

    deleting?.let { call ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.schedule_delete_title)) },
            text = { Text(stringResource(R.string.schedule_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(call.id)
                        deleting = null
                    },
                    modifier = Modifier.testTag("schedule-delete-confirm"),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ScheduledCallCard(
    call: ScheduledCall,
    caller: CallerPreset?,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }
    val formattedTime =
        remember(call.triggerAtEpochMillis, zone) {
            Instant.ofEpochMilli(call.triggerAtEpochMillis).atZone(zone).format(dateFormatter)
        }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = caller?.displayName ?: stringResource(R.string.schedule_caller_missing),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(formattedTime, style = MaterialTheme.typography.bodyMedium)
            Text(
                text =
                    stringResource(
                        if (call.deliveryMode == ScheduledCallDeliveryMode.EXACT) {
                            R.string.schedule_exact_label
                        } else {
                            R.string.schedule_approximate_label
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(scheduleStateLabel(call.state)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (call.state == ScheduledCallState.SCHEDULED) {
                    TextButton(onClick = onEdit, modifier = Modifier.testTag("schedule-edit-${call.id}")) {
                        Text(stringResource(R.string.edit))
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.testTag("schedule-cancel-${call.id}")) {
                        Text(stringResource(R.string.schedule_cancel))
                    }
                    TextButton(onClick = onDelete, modifier = Modifier.testTag("schedule-delete-${call.id}")) {
                        Text(stringResource(R.string.delete))
                    }
                } else if (call.state != ScheduledCallState.RINGING && call.state != ScheduledCallState.IN_CALL) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEditorDialog(
    existing: ScheduledCall?,
    callerPresets: List<CallerPreset>,
    operationState: ScheduledCallOperationState,
    exactAlarmAllowed: Boolean,
    notificationsAllowed: Boolean,
    onDismiss: () -> Unit,
    onSave: (ScheduledCall) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val defaultDateTime =
        remember(existing?.id) {
            existing?.let { saved ->
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(saved.triggerAtEpochMillis),
                    zone,
                )
            } ?: LocalDateTime
                .now(zone)
                .plusMinutes(5)
                .withSecond(0)
                .withNano(0)
        }
    var selectedDate by remember(existing?.id) { mutableStateOf(defaultDateTime.toLocalDate()) }
    var selectedTime by remember(existing?.id) { mutableStateOf(defaultDateTime.toLocalTime()) }
    val scheduleId = remember(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    var selectedCallerId by remember(existing?.id) {
        mutableStateOf(existing?.callerPresetId ?: callerPresets.firstOrNull()?.id.orEmpty())
    }
    var selectedMode by remember(existing?.id) {
        mutableStateOf(existing?.deliveryMode ?: ScheduledCallDeliveryMode.APPROXIMATE)
    }
    var callerMenuExpanded by remember { mutableStateOf(false) }
    var validationMessage by remember(existing?.id) { mutableStateOf<String?>(null) }

    val selectedCaller = callerPresets.firstOrNull { it.id == selectedCallerId }
    val localDateTime = LocalDateTime.of(selectedDate, selectedTime)
    val resolvedInstant = remember(localDateTime, zone) { ScheduleTimeResolver.resolve(localDateTime, zone) }
    val effectiveLocalTime = resolvedInstant.atZone(zone).toLocalDateTime()
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (existing == null) R.string.schedule_add else R.string.schedule_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { callerMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("schedule-caller-select"),
                    ) {
                        Text(selectedCaller?.displayName ?: stringResource(R.string.schedule_select_caller))
                    }
                    DropdownMenu(
                        expanded = callerMenuExpanded,
                        onDismissRequest = { callerMenuExpanded = false },
                    ) {
                        callerPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.displayName) },
                                onClick = {
                                    selectedCallerId = preset.id
                                    callerMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                selectedDate = LocalDate.of(year, month + 1, day)
                            },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.schedule_date, selectedDate.format(dateFormatter)))
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                selectedTime = LocalTime.of(hour, minute)
                            },
                            selectedTime.hour,
                            selectedTime.minute,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.schedule_time, effectiveLocalTime.format(timeFormatter)))
                }
                Text(
                    text = stringResource(R.string.schedule_delivery_heading),
                    style = MaterialTheme.typography.titleSmall,
                )
                DeliveryModeOption(
                    selected = selectedMode == ScheduledCallDeliveryMode.EXACT,
                    title = stringResource(R.string.schedule_exact_title),
                    description = stringResource(R.string.schedule_exact_description),
                    onSelect = { selectedMode = ScheduledCallDeliveryMode.EXACT },
                )
                DeliveryModeOption(
                    selected = selectedMode == ScheduledCallDeliveryMode.APPROXIMATE,
                    title = stringResource(R.string.schedule_approximate_title),
                    description = stringResource(R.string.schedule_approximate_description),
                    onSelect = { selectedMode = ScheduledCallDeliveryMode.APPROXIMATE },
                )
                if (selectedMode == ScheduledCallDeliveryMode.EXACT && !exactAlarmAllowed) {
                    PermissionNotice(
                        message = stringResource(R.string.schedule_exact_permission_required),
                        actionLabel = stringResource(R.string.schedule_open_exact_settings),
                        onAction = onRequestExactAlarms,
                    )
                }
                if (!notificationsAllowed) {
                    PermissionNotice(
                        message = stringResource(R.string.schedule_notifications_required),
                        actionLabel = stringResource(R.string.schedule_enable_notifications),
                        onAction = onRequestNotifications,
                    )
                }
                validationMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedCaller != null && operationState != ScheduledCallOperationState.SAVING,
                onClick = {
                    if (selectedCaller == null) {
                        validationMessage = context.getString(R.string.schedule_select_caller)
                    } else {
                        val timestamp = resolvedInstant.toEpochMilli()
                        val now = System.currentTimeMillis()
                        if (timestamp <= now) {
                            validationMessage = context.getString(R.string.schedule_time_must_be_future)
                        } else if (timestamp - now > ScheduledCallRules.MAX_SCHEDULE_HORIZON_MILLIS) {
                            validationMessage = context.getString(R.string.schedule_time_too_far)
                        } else {
                            onSave(
                                ScheduledCall(
                                    id = scheduleId,
                                    callerPresetId = selectedCaller.id,
                                    triggerAtEpochMillis = timestamp,
                                    deliveryMode = selectedMode,
                                ),
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("schedule-save-button"),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun DeliveryModeOption(
    selected: Boolean,
    title: String,
    description: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionNotice(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun OperationMessage(
    state: ScheduledCallOperationState,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
) {
    when (state) {
        ScheduledCallOperationState.NOTIFICATION_PERMISSION_REQUIRED ->
            PermissionNotice(
                message = stringResource(R.string.schedule_notifications_required),
                actionLabel = stringResource(R.string.schedule_enable_notifications),
                onAction = onRequestNotifications,
            )
        ScheduledCallOperationState.EXACT_ALARM_PERMISSION_REQUIRED ->
            PermissionNotice(
                message = stringResource(R.string.schedule_exact_permission_required),
                actionLabel = stringResource(R.string.schedule_open_exact_settings),
                onAction = onRequestExactAlarms,
            )
        ScheduledCallOperationState.CALLER_PROFILE_MISSING ->
            Text(stringResource(R.string.schedule_caller_missing), color = MaterialTheme.colorScheme.error)
        ScheduledCallOperationState.ACTIVE_CALL_CANNOT_BE_DELETED ->
            Text(stringResource(R.string.schedule_active_cannot_delete), color = MaterialTheme.colorScheme.error)
        ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR ->
            Text(stringResource(R.string.schedule_operation_error), color = MaterialTheme.colorScheme.error)
        ScheduledCallOperationState.CANCELLED ->
            Text(stringResource(R.string.schedule_cancelled), color = MaterialTheme.colorScheme.primary)
        ScheduledCallOperationState.DELETED ->
            Text(stringResource(R.string.schedule_deleted), color = MaterialTheme.colorScheme.primary)
        ScheduledCallOperationState.IDLE,
        ScheduledCallOperationState.SAVING,
        ScheduledCallOperationState.SAVED,
        -> Unit
    }
}

private fun scheduleStateLabel(state: ScheduledCallState): Int =
    when (state) {
        ScheduledCallState.SCHEDULED -> R.string.schedule_state_scheduled
        ScheduledCallState.RINGING -> R.string.schedule_state_ringing
        ScheduledCallState.IN_CALL -> R.string.schedule_state_in_call
        ScheduledCallState.ENDED -> R.string.schedule_state_ended
        ScheduledCallState.CANCELLED -> R.string.schedule_state_cancelled
        ScheduledCallState.TIMED_OUT -> R.string.schedule_state_timed_out
    }
