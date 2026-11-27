package com.siddumoni.fakecall.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siddumoni.fakecall.AlibiCallApplication
import com.siddumoni.fakecall.data.preferences.OnboardingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = OnboardingRepository(application)

    private val _completed =
        MutableStateFlow(
            (application as AlibiCallApplication).initialOnboardingCompleted,
        )
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun complete() {
        viewModelScope.launch {
            _saveFailed.value = false
            try {
                repository.setCompleted(true)
                (getApplication() as AlibiCallApplication).markOnboardingCompleted()
                _completed.value = true
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _saveFailed.value = true
            }
        }
    }
}
