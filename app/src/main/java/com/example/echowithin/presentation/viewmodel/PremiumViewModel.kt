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
    val isTrial: Boolean = false,
    val trialDaysRemaining: Int = 0,
    val successMessage: String? = null,
    val error: String? = null
)

class PremiumViewModel : ViewModel() {
    var uiState by mutableStateOf(PremiumUiState())
        private set

    init {
        val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
        uiState = uiState.copy(
            isPremium = hasToken && SessionManager.accountTier == "premium",
            isTrial = hasToken && SessionManager.isTrial,
            trialDaysRemaining = if (hasToken) SessionManager.trialDaysRemaining else 0
        )
    }

    fun refreshPremiumStatus() {
        val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
        uiState = uiState.copy(
            isPremium = hasToken && SessionManager.accountTier == "premium",
            isTrial = hasToken && SessionManager.isTrial,
            trialDaysRemaining = if (hasToken) SessionManager.trialDaysRemaining else 0
        )
        if (!hasToken) return

        viewModelScope.launch {
            try {
                val profile = ApiClient.apiService.getProfile()
                SessionManager.accountTier = profile.account_tier
                SessionManager.isTrial = profile.is_trial
                SessionManager.trialDaysRemaining = profile.trial_days_remaining
                uiState = uiState.copy(
                    isPremium = profile.account_tier == "premium",
                    isTrial = profile.is_trial,
                    trialDaysRemaining = profile.trial_days_remaining
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isPremium = SessionManager.accountTier == "premium",
                    isTrial = SessionManager.isTrial,
                    trialDaysRemaining = SessionManager.trialDaysRemaining
                )
            }
        }
    }

    fun activatePremium() {
        if (uiState.isPremium && !uiState.isTrial) return
        uiState = uiState.copy(isLoading = true, error = null, successMessage = null)
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.activatePremium()
                if (response.success) {
                    try {
                        val profile = ApiClient.apiService.getProfile()
                        SessionManager.accountTier = profile.account_tier
                        SessionManager.isTrial = profile.is_trial
                        SessionManager.trialDaysRemaining = profile.trial_days_remaining
                    } catch (_: Exception) { }
                    uiState = uiState.copy(
                        isLoading = false,
                        isPremium = true,
                        isTrial = SessionManager.isTrial,
                        trialDaysRemaining = SessionManager.trialDaysRemaining,
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
