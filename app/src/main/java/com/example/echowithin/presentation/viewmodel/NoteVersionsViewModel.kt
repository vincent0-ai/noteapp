package com.example.echowithin.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.echowithin.data.model.VersionDto
import com.example.echowithin.data.repository.ShareRepository
import kotlinx.coroutines.launch

data class NoteVersionsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val noteId: String = "",
    val versions: List<VersionDto> = emptyList(),
    val restoreSuccess: Boolean = false
)

class NoteVersionsViewModel(
    private val repository: ShareRepository
) : ViewModel() {
    var uiState by mutableStateOf(NoteVersionsUiState())
        private set

    fun load(noteId: String) {
        uiState = uiState.copy(isLoading = true, error = null, noteId = noteId, restoreSuccess = false)
        viewModelScope.launch {
            repository.getVersions(noteId)
                .onSuccess { versions -> uiState = uiState.copy(isLoading = false, versions = versions) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not load versions") }
        }
    }

    fun restore(versionId: String) {
        val noteId = uiState.noteId
        if (noteId.isBlank()) return
        uiState = uiState.copy(isLoading = true, error = null, restoreSuccess = false)
        viewModelScope.launch {
            repository.restoreVersion(noteId, versionId)
                .onSuccess {
                    uiState = uiState.copy(restoreSuccess = true)
                    load(noteId)
                }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not restore version") }
        }
    }

    fun clearFeedback() {
        uiState = uiState.copy(error = null, restoreSuccess = false)
    }

    fun decide(versionId: String, approve: Boolean, comment: String = "") {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.decideProposal(versionId, if (approve) "approve" else "reject", comment)
                .onSuccess { load(uiState.noteId) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not update proposal") }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = NoteVersionsViewModel(ShareRepository()) as T
        }
    }
}
