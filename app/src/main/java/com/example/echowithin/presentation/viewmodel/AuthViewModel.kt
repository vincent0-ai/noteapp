package com.example.echowithin.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.echowithin.data.repository.AuthRepository
import com.example.echowithin.data.model.LoginRequest
import com.example.echowithin.data.model.RegisterRequest
import com.example.echowithin.data.network.ApiClient
import com.example.echowithin.data.network.SessionManager
import kotlinx.coroutines.launch
import android.util.Log

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class UnconfirmedEmail(val email: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    var loginState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    var registerState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    var confirmState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun login(username: String, password: String) {
        viewModelScope.launch {
            loginState = AuthUiState.Loading
            repository.login(LoginRequest(username, password))
                .onSuccess { response ->
                    if (response.success) {
                        SessionManager.token = response.x_app_token
                        SessionManager.username = response.username
                        try {
                            val profile = ApiClient.apiService.getProfile()
                            SessionManager.accountTier = profile.account_tier
                            SessionManager.isTrial = profile.is_trial
                            SessionManager.trialDaysRemaining = profile.trial_days_remaining
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to fetch profile on login", e)
                        }
                        loginState = AuthUiState.Success
                        ApiClient.registerFcmToken(com.example.echowithin.EchoWithinApplication.instance)
                    } else {
                        if (response.confirmed == false && !response.email.isNullOrBlank()) {
                            loginState = AuthUiState.UnconfirmedEmail(response.email)
                        } else {
                            loginState = AuthUiState.Error(response.error ?: "Login failed")
                        }
                    }
                }
                .onFailure { t ->
                    loginState = AuthUiState.Error(t.message ?: "Network error")
                }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            registerState = AuthUiState.Loading
            repository.register(RegisterRequest(username, email, password))
                .onSuccess { response ->
                    if (response.success) {
                        registerState = AuthUiState.Success
                    } else {
                        registerState = AuthUiState.Error(response.message ?: "Registration failed")
                    }
                }
                .onFailure { t ->
                    registerState = AuthUiState.Error(t.message ?: "Network error")
                }
        }
    }

    fun confirm(email: String, code: String) {
        viewModelScope.launch {
            confirmState = AuthUiState.Loading
            repository.confirm(email, code)
                .onSuccess { response ->
                    if (response.success) {
                        confirmState = AuthUiState.Success
                    } else {
                        confirmState = AuthUiState.Error(response.message ?: "Confirmation failed")
                    }
                }
                .onFailure { t ->
                    confirmState = AuthUiState.Error(t.message ?: "Network error")
                }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        ApiClient.unregisterFcmToken(com.example.echowithin.EchoWithinApplication.instance)
        viewModelScope.launch {
            try {
                repository.logout()
            } catch (_: Exception) {}
            // clearSession() — NOT clear(): preserves the device-local PIN
            // hash/flags (so the correct PIN still unlocks offline after
            // sign-out), sync-mode preference, and the dismissed-update /
            // offline-privacy flags. Only account/session data is wiped.
            SessionManager.clearSession()
            onSuccess()
        }
    }

    fun resetState() {
        loginState = AuthUiState.Idle
        registerState = AuthUiState.Idle
        confirmState = AuthUiState.Idle
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(AuthRepository(ApiClient.apiService)) as T
            }
        }
    }
}
