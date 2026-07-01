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

    // Access to notes API for refreshing the local note after restore
    private val notesApi = com.example.echowithin.data.network.ApiClient.apiService
    private val dbHelper = com.example.echowithin.data.local.NoteDatabaseHelper(
        com.example.echowithin.EchoWithinApplication.instance
    )

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
                    // Refresh the local note from the server so the user sees
                    // the restored content when navigating back
                    try {
                        val freshNote = notesApi.getNoteById(noteId)
                        val titleCandidate = freshNote.content.lineSequence().firstOrNull()?.trim().orEmpty()
                        val title = if (titleCandidate.isBlank()) "Untitled" else titleCandidate.take(60)
                        val appNote = com.example.echowithin.data.model.AppNote(
                            id = freshNote.id,
                            title = title,
                            content = freshNote.content,
                            reference = freshNote.reference.orEmpty(),
                            tags = freshNote.tags,
                            updatedAt = freshNote.updated_at ?: freshNote.created_at ?: "",
                            isLocked = freshNote.is_locked,
                            isPinned = freshNote.is_pinned,
                            isSynced = true,
                            pendingOp = "none",
                            updateAvailable = freshNote.update_available,
                            sourceNoteId = freshNote.source_note_id,
                            sourceShareId = freshNote.source_share_id,
                            folder = freshNote.folder
                        )
                        dbHelper.saveNote(appNote, isSynced = true, pendingOp = "none")
                    } catch (_: Exception) {
                        // Sync failure is non-fatal; the note will refresh on next sync
                    }
                    uiState = uiState.copy(restoreSuccess = true)
                    load(noteId)
                }
                .onFailure { uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not restore version") }
        }
    }

    fun clearFeedback() {
        uiState = uiState.copy(error = null, restoreSuccess = false)
    }

    fun decide(
        versionId: String,
        approve: Boolean,
        comment: String = "",
        autoApproveSubsequent: Boolean = false
    ) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.decideProposal(
                versionId,
                if (approve) "approve" else "reject",
                comment,
                autoApproveSubsequent
            )
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
