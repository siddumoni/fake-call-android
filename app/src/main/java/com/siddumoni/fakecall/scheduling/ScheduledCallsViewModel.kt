package com.siddumoni.fakecall.scheduling

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

enum class ScheduledCallOperationState {
    IDLE,
    SAVING,
    SAVED,
    CANCELLED,
    DELETED,
    NOTIFICATION_PERMISSION_REQUIRED,
    EXACT_ALARM_PERMISSION_REQUIRED,
    CALLER_PROFILE_MISSING,
    ACTIVE_CALL_CANNOT_BE_DELETED,
    STORAGE_OR_ALARM_ERROR,
}

class ScheduledCallsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = ScheduledCallRepository(application)
    private val presetRepository = CallerPresetRepository(application)
    private val scheduler = ScheduledCallAlarmScheduler(application)
    private val reconciler = ScheduleReconciler(application)
    private val writeMutex = Mutex()

    private val _calls = MutableStateFlow<List<ScheduledCall>>(emptyList())
    val calls: StateFlow<List<ScheduledCall>> = _calls.asStateFlow()

    private val _operationState = MutableStateFlow(ScheduledCallOperationState.IDLE)
    val operationState: StateFlow<ScheduledCallOperationState> = _operationState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.calls
                .catch { exception ->
                    if (exception is IOException) {
                        _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                        emit(emptyList())
                    } else {
                        throw exception
                    }
                }.collect { _calls.value = it }
        }
    }

    fun save(call: ScheduledCall) {
        viewModelScope.launch {
            writeMutex.withLock {
                _operationState.value = ScheduledCallOperationState.SAVING
                if (!ScheduledCallNotifications.canPost(getApplication())) {
                    _operationState.value = ScheduledCallOperationState.NOTIFICATION_PERMISSION_REQUIRED
                    return@withLock
                }
                val permissionResult =
                    ScheduleDeliveryPolicy.resultFor(
                        call.deliveryMode,
                        scheduler.canScheduleExactAlarms(),
                    )
                if (permissionResult != ScheduleAlarmResult.SCHEDULED) {
                    _operationState.value = ScheduledCallOperationState.EXACT_ALARM_PERMISSION_REQUIRED
                    return@withLock
                }

                var previous: ScheduledCall? = null
                var persisted = false
                try {
                    val savedPresets = presetRepository.presets.first()
                    if (savedPresets.none { preset -> preset.id == call.callerPresetId }) {
                        _operationState.value = ScheduledCallOperationState.CALLER_PROFILE_MISSING
                        return@withLock
                    }
                    previous = repository.calls.first().firstOrNull { saved -> saved.id == call.id }
                    repository.upsert(call)
                    persisted = true
                    when (scheduler.schedule(call)) {
                        ScheduleAlarmResult.SCHEDULED -> {
                            _operationState.value = ScheduledCallOperationState.SAVED
                        }
                        ScheduleAlarmResult.EXACT_ALARM_PERMISSION_REQUIRED -> {
                            restorePrevious(previous, call.id)
                            _operationState.value = ScheduledCallOperationState.EXACT_ALARM_PERMISSION_REQUIRED
                        }
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    if (persisted) {
                        runCatching { restorePrevious(previous, call.id) }
                    }
                    _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                }
            }
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            writeMutex.withLock {
                try {
                    val changed = repository.transition(id, ScheduledCallAction.CANCEL)
                    if (changed) {
                        scheduler.cancel(id)
                        ScheduledCallNotifications.cancel(getApplication(), id)
                        _operationState.value = ScheduledCallOperationState.CANCELLED
                    } else {
                        _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            writeMutex.withLock {
                try {
                    val call = repository.calls.first().firstOrNull { saved -> saved.id == id }
                    if (call == null) {
                        _operationState.value = ScheduledCallOperationState.DELETED
                        return@withLock
                    }
                    if (call.state == ScheduledCallState.RINGING || call.state == ScheduledCallState.IN_CALL) {
                        _operationState.value = ScheduledCallOperationState.ACTIVE_CALL_CANNOT_BE_DELETED
                        return@withLock
                    }
                    // Atomically fence off a pending alarm before cancelling the external AlarmManager
                    // entry. If delivery wins this transition, re-read and refuse to delete a ringing
                    // or answered simulation instead of racing an unconditional repository removal.
                    if (call.state == ScheduledCallState.SCHEDULED &&
                        !repository.transition(id, ScheduledCallAction.CANCEL)
                    ) {
                        val latest = repository.calls.first().firstOrNull { saved -> saved.id == id }
                        if (latest == null) {
                            _operationState.value = ScheduledCallOperationState.DELETED
                            return@withLock
                        }
                        if (latest.state == ScheduledCallState.RINGING || latest.state == ScheduledCallState.IN_CALL) {
                            _operationState.value = ScheduledCallOperationState.ACTIVE_CALL_CANNOT_BE_DELETED
                            return@withLock
                        }
                        if (latest.state == ScheduledCallState.SCHEDULED) {
                            _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                            return@withLock
                        }
                    }
                    scheduler.cancel(id)
                    ScheduledCallNotifications.cancel(getApplication(), id)
                    repository.remove(id)
                    _operationState.value = ScheduledCallOperationState.DELETED
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                }
            }
        }
    }

    /**
     * Reconcile persisted pending schedules whenever the app becomes active or permissions return.
     * Exact schedules stay exact: missing special access leaves them saved but unscheduled until
     * the user grants access. Notification denial cancels delivery alarms without deleting records.
     */
    fun reconcilePendingAlarms(notificationsAllowed: Boolean) {
        viewModelScope.launch {
            writeMutex.withLock {
                try {
                    if (_operationState.value == ScheduledCallOperationState.NOTIFICATION_PERMISSION_REQUIRED ||
                        _operationState.value == ScheduledCallOperationState.EXACT_ALARM_PERMISSION_REQUIRED
                    ) {
                        _operationState.value = ScheduledCallOperationState.IDLE
                    }
                    when (reconciler.reconcile(notificationsAllowed)) {
                        ReconcileResult.COMPLETE -> Unit
                        ReconcileResult.NOTIFICATION_PERMISSION_REQUIRED ->
                            _operationState.value = ScheduledCallOperationState.NOTIFICATION_PERMISSION_REQUIRED
                        ReconcileResult.EXACT_ALARM_PERMISSION_REQUIRED ->
                            _operationState.value = ScheduledCallOperationState.EXACT_ALARM_PERMISSION_REQUIRED
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                }
            }
        }
    }

    /** An answered record with no surviving screen can never be ended by the user, so close it. */
    fun endOrphanedCalls(): Job =
        viewModelScope.launch {
            writeMutex.withLock {
                try {
                    repository.calls
                        .first()
                        .filter { call -> call.state == ScheduledCallState.IN_CALL }
                        .forEach { call -> repository.transition(call.id, ScheduledCallAction.END) }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    _operationState.value = ScheduledCallOperationState.STORAGE_OR_ALARM_ERROR
                }
            }
        }

    fun clearOperationState() {
        _operationState.value = ScheduledCallOperationState.IDLE
    }

    private suspend fun restorePrevious(
        previous: ScheduledCall?,
        id: String,
    ) {
        if (previous == null) {
            repository.remove(id)
            scheduler.cancel(id)
        } else {
            repository.upsert(previous)
            // Preserve the previous record even when exact-alarm access was revoked between
            // the preflight check and AlarmManager registration. Foreground reconciliation
            // will re-register it after the user grants access again.
            scheduler.schedule(previous)
        }
    }
}
