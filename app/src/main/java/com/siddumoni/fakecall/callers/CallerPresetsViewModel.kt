package com.siddumoni.fakecall.callers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class CallerPresetsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = CallerPresetRepository(application)
    private val writeMutex = Mutex()

    private val _presets = MutableStateFlow<List<CallerPreset>>(emptyList())
    val presets: StateFlow<List<CallerPreset>> = _presets.asStateFlow()

    private val _saveError = MutableStateFlow<CallerPresetValidationError?>(null)
    val saveError: StateFlow<CallerPresetValidationError?> = _saveError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.presets
                .catch { exception ->
                    if (exception is IOException) {
                        _saveError.value = CallerPresetValidationError.STORAGE
                        emit(emptyList())
                    } else {
                        throw exception
                    }
                }.collect { _presets.value = it }
        }
    }

    fun savePreset(
        id: String?,
        displayName: String,
        numberLabel: String,
    ): Boolean {
        val validationError = CallerPresetValidator.validate(displayName, numberLabel)
        if (validationError != null) {
            _saveError.value = validationError
            return false
        }

        viewModelScope.launch {
            try {
                writeMutex.withLock {
                    repository.save(id, displayName, numberLabel)
                }
                _saveError.value = null
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _saveError.value = CallerPresetValidationError.STORAGE
            }
        }
        return true
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            try {
                writeMutex.withLock {
                    repository.delete(id)
                }
                _saveError.value = null
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _saveError.value = CallerPresetValidationError.STORAGE
            }
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }
}
