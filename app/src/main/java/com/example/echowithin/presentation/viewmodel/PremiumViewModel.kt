package com.example.echowithin.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.echowithin.data.network.ApiClient
import com.example.echowithin.data.network.SessionManager
import kotlinx.coroutines.launch

data class PremiumUiState(
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

class PremiumViewModel : ViewModel() {
    var uiState by mutableStateOf(PremiumUiState())
        private set

    init {
        uiState = uiState.copy(isPremium = SessionManager.accountTier == "premium")
    }

    fun activatePremium() {
        if (uiState.isPremium) return
        uiState = uiState.copy(isLoading = true, error = null, successMessage = null)
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.activatePremium()
                if (response.success) {
                    try {
                        val profile = ApiClient.apiService.getProfile()
                        SessionManager.accountTier = profile.account_tier
                    } catch (_: Exception) { }
                    uiState = uiState.copy(
                        isLoading = false,
                        isPremium = true,
                        successMessage = response.message ?: "Premium activated!"
                    )
                } else {
                    uiState = uiState.copy(isLoading = false, error = response.error ?: "Activation failed")
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PremiumViewModel() as T
            }
        }
    }
}
