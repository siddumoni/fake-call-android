package com.siddumoni.fakecall.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siddumoni.fakecall.data.preferences.CallSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class CallSettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = CallSettingsRepository(application)

    val settings: StateFlow<CallSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, CallSettings())

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun setStyle(style: CallScreenStyle) = save { repository.setStyle(style) }

    fun setFullScreen(enabled: Boolean) = save { repository.setFullScreen(enabled) }

    fun setTriggerCaller(callerId: String) = save { repository.setTriggerCaller(callerId) }

    fun setTriggerDelaySeconds(seconds: Int) = save { repository.setTriggerDelaySeconds(seconds) }

    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
                _saveFailed.value = false
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                if (exception !is IOException) throw exception
                _saveFailed.value = true
            }
        }
    }
}
