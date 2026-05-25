package com.example.echowithin.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.data.model.SearchHitDto
import com.example.echowithin.data.model.ProposalDto
import com.example.echowithin.data.model.ShareDto
import com.example.echowithin.data.model.NotificationDto
import com.example.echowithin.data.model.BadgeCountsDto
import com.example.echowithin.data.model.ProposalDecisionDto
import com.example.echowithin.data.repository.NotesRepository
import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch


@Immutable
data class NotesUiState(
    val notes: List<AppNote> = emptyList(),
    val searchResults: List<SearchHitDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val proposals: List<ProposalDto> = emptyList(),
    val proposalsLoading: Boolean = false,
    val activeShares: List<Pair<AppNote, List<ShareDto>>> = emptyList(),
    val sharesLoading: Boolean = false,
    val notifications: List<NotificationDto> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val badgeCounts: BadgeCountsDto = BadgeCountsDto(0, 0),
    val updateInfo: com.example.echowithin.data.network.UpdateInfo? = null,
    val downloadProgress: Float? = null
)

class NotesViewModel(
    private val repository: NotesRepository
) : ViewModel() {
    var uiState by mutableStateOf(NotesUiState())
        private set

    fun checkForUpdates(context: Context) {
        viewModelScope.launch {
            val updateManager = com.example.echowithin.data.network.AppUpdateManager(context)
            val info = updateManager.checkForUpdates()
            if (info.hasUpdate) {
                uiState = uiState.copy(updateInfo = info)
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context, apkUrl: String) {
        uiState = uiState.copy(downloadProgress = 0f)
        viewModelScope.launch {
            val updateManager = com.example.echowithin.data.network.AppUpdateManager(context)
            val success = updateManager.downloadAndInstallApk(apkUrl) { progress ->
                uiState = uiState.copy(downloadProgress = progress)
            }
            if (!success) {
                uiState = uiState.copy(downloadProgress = null, error = "Update download failed")
            }
        }
    }

    fun dismissUpdate() {
        uiState = uiState.copy(updateInfo = null, downloadProgress = null)
    }

    fun syncNotes() {
        uiState = uiState.copy(isSyncing = true, error = null)
        viewModelScope.launch {
            repository.syncNotes()
                .onSuccess {
                    uiState = uiState.copy(isSyncing = false)
                    loadNotes()
                }
                .onFailure {
                    uiState = uiState.copy(isSyncing = false, error = it.message ?: "Sync failed")
                }
        }
    }

    fun clearLocalData() {
        repository.clearLocalData()
        uiState = NotesUiState()
    }


    fun loadNotes() {
        val localNotes = repository.getLocalNotes()
        if (localNotes.isNotEmpty()) {
            uiState = uiState.copy(notes = localNotes, isLoading = false, error = null)
        } else {
            uiState = uiState.copy(isLoading = true, error = null)
        }
        viewModelScope.launch {
            repository.getNotes()
                .onSuccess { notes ->
                    uiState = uiState.copy(isLoading = false, notes = notes)
                    loadProposals()
                    loadActiveShares()
                    loadNotifications()
                }
                .onFailure {
                    uiState = uiState.copy(isLoading = false)
                    if (uiState.notes.isEmpty()) {
                        uiState = uiState.copy(error = it.message ?: "Failed to load notes")
                    }
                }
        }
    }

    fun loadProposals() {
        val hasToken = !com.example.echowithin.data.network.SessionManager.token.isNullOrBlank() && com.example.echowithin.data.network.SessionManager.token != "null"
        if (!hasToken) {
            uiState = uiState.copy(proposals = emptyList())
            return
        }
        uiState = uiState.copy(proposalsLoading = true)
        viewModelScope.launch {
            try {
                val response = com.example.echowithin.data.network.ApiClient.apiService.getProposals()
                uiState = uiState.copy(proposalsLoading = false, proposals = response.proposals)
            } catch (e: Exception) {
                uiState = uiState.copy(proposalsLoading = false)
            }
        }
    }

    fun loadActiveShares() {
        val hasToken = !com.example.echowithin.data.network.SessionManager.token.isNullOrBlank() && com.example.echowithin.data.network.SessionManager.token != "null"
        if (!hasToken) {
            uiState = uiState.copy(activeShares = emptyList())
            return
        }
        uiState = uiState.copy(sharesLoading = true)
        viewModelScope.launch {
            try {
                val deferreds = uiState.notes.map { note ->
                    async {
                        try {
                            val sharesList = com.example.echowithin.data.network.ApiClient.apiService.getShares(note.id).shares
                            if (sharesList.isNotEmpty()) note to sharesList else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                val result = awaitAll(*deferreds.toTypedArray()).filterNotNull()
                uiState = uiState.copy(sharesLoading = false, activeShares = result)
            } catch (e: Exception) {
                uiState = uiState.copy(sharesLoading = false)
            }
        }
    }

    fun loadNotifications() {
        val hasToken = !com.example.echowithin.data.network.SessionManager.token.isNullOrBlank() && com.example.echowithin.data.network.SessionManager.token != "null"
        if (!hasToken) {
            uiState = uiState.copy(
                notifications = emptyList(),
                unreadNotificationsCount = 0,
                badgeCounts = com.example.echowithin.data.model.BadgeCountsDto(0, 0)
            )
            return
        }
        viewModelScope.launch {
            try {
                val notificationsResp = com.example.echowithin.data.network.ApiClient.apiService.getNotifications()
                val badgeCountsResp = com.example.echowithin.data.network.ApiClient.apiService.getBadgeCounts()
                uiState = uiState.copy(
                    notifications = notificationsResp.posts,
                    unreadNotificationsCount = notificationsResp.unread_count,
                    badgeCounts = badgeCountsResp
                )
            } catch (_: Exception) {}
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                com.example.echowithin.data.network.ApiClient.apiService.markAllPostsRead()
                com.example.echowithin.data.network.ApiClient.apiService.markAllProposalsRead()
                loadNotifications()
                loadProposals() // Proposals might also clear
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message ?: "Failed to mark all as read")
            }
        }
    }

    fun approveProposal(versionId: String) {
        viewModelScope.launch {
            try {
                com.example.echowithin.data.network.ApiClient.apiService.decideProposal(
                    versionId,
                    com.example.echowithin.data.model.ProposalDecisionDto(decision = "approve", comment = "")
                )
                loadProposals()
                loadNotes()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message ?: "Failed to approve proposal")
            }
        }
    }

    fun rejectProposal(versionId: String) {
        viewModelScope.launch {
            try {
                com.example.echowithin.data.network.ApiClient.apiService.decideProposal(
                    versionId,
                    com.example.echowithin.data.model.ProposalDecisionDto(decision = "reject", comment = "")
                )
                loadProposals()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message ?: "Failed to reject proposal")
            }
        }
    }

    fun createNote(content: String, reference: String, tags: List<String>, onDone: (String) -> Unit) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.createNote(content = content, reference = reference, tags = tags)
                .onSuccess { id ->
                    loadNotes()
                    onDone(id)
                }
                .onFailure {
                    uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not save note")
                }
        }
    }

    fun editNote(noteId: String, content: String, reference: String, tags: List<String>, onDone: () -> Unit) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.editNote(noteId = noteId, content = content, reference = reference, tags = tags)
                .onSuccess { _ ->
                    loadNotes()
                    onDone()
                }
                .onFailure {
                    uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not update note")
                }
        }
    }

    fun deleteNote(noteId: String, onDone: () -> Unit) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.deleteNote(noteId)
                .onSuccess {
                    loadNotes()
                    onDone()
                }
                .onFailure {
                    uiState = uiState.copy(isLoading = false, error = it.message ?: "Could not delete note")
                }
        }
    }

    fun toggleNoteLock(noteId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.toggleNoteLock(noteId)
                .onSuccess { isLocked ->
                    loadNotes()
                    onDone(isLocked)
                }
                .onFailure { t ->
                    uiState = uiState.copy(error = t.message ?: "Could not toggle lock")
                }
        }
    }

    fun getNoteById(noteId: String): AppNote? = uiState.notes.firstOrNull { it.id == noteId }

    suspend fun getNoteFromServer(noteId: String): Result<AppNote> {
        return repository.getNoteById(noteId)
    }

    fun searchNotes(query: String) {
        if (query.isBlank()) {
            uiState = uiState.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            repository.searchNotes(query)
                .onSuccess {
                    uiState = uiState.copy(
                        searchResults = it.results,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = it.message ?: "Search failed"
                    )
                }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotesViewModel(NotesRepository()) as T
            }
        }
    }
}
