package com.example.echowithin.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.echowithin.data.model.AttachmentDto
import com.example.echowithin.data.model.CommentDto
import com.example.echowithin.data.model.ShareDto
import com.example.echowithin.data.repository.ShareRepository
import kotlinx.coroutines.launch

data class NoteShareUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val noteId: String = "",
    val shares: List<ShareDto> = emptyList(),
    val selectedShareId: String? = null,
    val comments: List<CommentDto> = emptyList(),
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
            val commentsResult = repository.getComments(shareId)
            val attachmentsResult = repository.getAttachments(shareId)
            when {
                commentsResult.isSuccess && attachmentsResult.isSuccess -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        comments = commentsResult.getOrDefault(emptyList()),
                        attachments = attachmentsResult.getOrDefault(emptyList())
                    )
                }
                else -> uiState = uiState.copy(
                    isLoading = false,
                    error = commentsResult.exceptionOrNull()?.message
                        ?: attachmentsResult.exceptionOrNull()?.message
                        ?: "Could not load share details"
                )
            }
        }
    }

    fun addComment(content: String) {
        val shareId = uiState.selectedShareId ?: return
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.addComment(shareId, content)
                .onSuccess { loadShareDetails(shareId) }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not add comment") }
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

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = NoteShareViewModel(ShareRepository()) as T
        }
    }
}
