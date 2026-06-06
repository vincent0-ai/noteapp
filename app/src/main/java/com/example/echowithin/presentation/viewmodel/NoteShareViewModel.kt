package com.example.echowithin.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.echowithin.data.model.AttachmentDto
import com.example.echowithin.data.model.ShareDto
import com.example.echowithin.data.repository.ShareRepository
import kotlinx.coroutines.launch

data class NoteShareUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val noteId: String = "",
    val shares: List<ShareDto> = emptyList(),
    val selectedShareId: String? = null,
    val attachments: List<AttachmentDto> = emptyList()
)

class NoteShareViewModel(
    private val repository: ShareRepository
) : ViewModel() {
    var uiState by mutableStateOf(NoteShareUiState())
        private set

    fun load(noteId: String) {
        uiState = uiState.copy(isLoading = true, error = null, noteId = noteId)
        viewModelScope.launch {
            repository.getShares(noteId)
                .onSuccess { shares ->
                    val selected = uiState.selectedShareId ?: shares.firstOrNull()?.share_id
                    uiState = uiState.copy(isLoading = false, shares = shares, selectedShareId = selected)
                    selected?.let { loadShareDetails(it) }
                }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not load shares") }
        }
    }

    fun createShare(
        context: android.content.Context,
        permissions: String = "view",
        expiresIn: String? = null,
        accessCode: String? = null,
        surpriseTheme: String = "none",
        useTypewriter: Boolean = false,
        autoApprove: Boolean = false,
        photoUri: String? = null,
        audioUri: String? = null
    ) {
        val noteId = uiState.noteId
        if (noteId.isBlank()) return
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.createShare(context, noteId, permissions, expiresIn, accessCode, surpriseTheme, useTypewriter, autoApprove, photoUri, audioUri)
                .onSuccess { load(noteId) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not create share") }
        }
    }

    fun selectShare(shareId: String) {
        uiState = uiState.copy(selectedShareId = shareId)
        loadShareDetails(shareId)
    }

    fun loadShareDetails(shareId: String) {
        uiState = uiState.copy(isLoading = true, error = null, selectedShareId = shareId)
        viewModelScope.launch {
            repository.getAttachments(shareId)
                .onSuccess { attachments ->
                    uiState = uiState.copy(
                        isLoading = false,
                        attachments = attachments
                    )
                }
                .onFailure { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = exception.message ?: "Could not load share details"
                    )
                }
        }
    }

    fun revokeShare(shareId: String) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.revokeShare(shareId)
                .onSuccess { load(uiState.noteId) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not revoke share") }
        }
    }

    /**
     * Flip the link-wide `auto_approve` flag on an existing share. The
     * server returns 403 with `upgrade_required: true` for free-tier
     * accounts; we surface that as a user-readable error in the
     * existing `uiState.error` slot.
     */
    fun toggleAutoApprove(shareId: String, enabled: Boolean) {
        // Optimistic UI update — flip the local flag immediately so the
        // switch feels snappy. The actual server call happens in the
        // background; on failure we revert.
        val previous = uiState.shares
        val optimistic = previous.map { s ->
            if (s.share_id == shareId) s.copy(auto_approve = enabled) else s
        }
        uiState = uiState.copy(shares = optimistic, error = null)

        viewModelScope.launch {
            repository.toggleShareAutoApprove(shareId, enabled)
                .onSuccess { /* state already updated */ }
                .onFailure { err ->
                    // Revert and surface the error
                    uiState = uiState.copy(
                        shares = previous,
                        error = err.message ?: "Could not update auto-approve"
                    )
                }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = NoteShareViewModel(ShareRepository()) as T
        }
    }
}
