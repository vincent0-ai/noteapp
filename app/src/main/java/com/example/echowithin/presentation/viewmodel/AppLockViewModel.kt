package com.example.echowithin.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.echowithin.data.repository.AppLockRepository
import com.example.echowithin.data.model.AppLockStatusDto
import kotlinx.coroutines.launch

data class AppLockUiState(
    val isLoading: Boolean = false,
    val isLocked: Boolean = false,
    val hasPin: Boolean = false,
    val error: String? = null
)

class AppLockViewModel(
    private val repository: AppLockRepository
) : ViewModel() {
    var uiState by mutableStateOf(AppLockUiState())
        private set

    fun refreshStatus() {
        viewModelScope.launch {
            repository.checkStatus()
                .onSuccess { status ->
                    uiState = uiState.copy(
                        hasPin = status.has_pin,
                        isLocked = status.has_pin && !status.unlocked,
                        error = null
                    )
                }
                .onFailure { uiState = uiState.copy(error = it.message ?: "Could not load lock status") }
        }
    }

    fun setup(pin: String) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.setupLock(pin)
                .onSuccess { uiState = uiState.copy(isLoading = false, hasPin = true, isLocked = false) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not set lock") }
        }
    }

    fun verify(pin: String) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.verifyLock(pin)
                .onSuccess { uiState = uiState.copy(isLoading = false, isLocked = false) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Incorrect PIN") }
        }
    }

    fun remove(pin: String) {
        if (pin.length != 4) {
            uiState = uiState.copy(error = "Enter your current 4-digit PIN to remove protection.")
            return
        }
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.removeLock(pin)
                .onSuccess { uiState = uiState.copy(isLoading = false, hasPin = false, isLocked = false) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not remove lock") }
        }
    }

    /** Clear the error message — called from the screen when the user
     *  starts typing a new PIN so the red error text disappears. */
    fun clearError() {
        if (uiState.error != null) {
            uiState = uiState.copy(error = null)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AppLockViewModel(AppLockRepository()) as T
        }
    }
}
