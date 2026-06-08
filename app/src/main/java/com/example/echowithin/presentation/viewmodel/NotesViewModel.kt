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
import com.example.echowithin.data.local.NoteDatabaseHelper
import com.example.echowithin.EchoWithinApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking


@Immutable
data class NotesUiState(
    val notes: List<AppNote> = emptyList(),
    val searchResults: List<SearchHitDto> = emptyList(),
    val isLoading: Boolean = true,
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
    val downloadProgress: Float? = null,
    // Number of notes waiting to be pushed to the server (used by the
    // offline banner to tell the user "N changes will sync when you
    // reconnect"). Recomputed from the local DB on every load.
    val pendingSyncCount: Int = 0,
    // Last successful sync timestamp (epoch millis). Powers the small
    // "Synced 5m ago" label in the top app bar.
    val lastSyncedAt: Long = 0L,
    // True while a "mark all as read" round-trip is in flight. Lets the
    // Activity tab show a spinner instead of doing nothing on tap.
    val markingAllRead: Boolean = false,
    // Last-marked count, used for the "Cleared N items" toast. Cleared
    // by the UI once the toast has been shown.
    val lastMarkedReadCount: Int = 0
)

class NotesViewModel(
    private val repository: NotesRepository
) : ViewModel() {
    var uiState by mutableStateOf(NotesUiState())
        private set

    /**
     * Coalescing channel for connectivity changes. The UI pushes "online
     * now" into this whenever [com.example.echowithin.data.network.NetworkMonitor]
     * flips to true; we de-dupe and kick a single syncNotes() round-trip.
     * Without this, every time the device jumps between Wi-Fi and cell
     * we would queue a full sync per call site.
     */
    private val _syncTrigger = MutableStateFlow(0L)
    val syncTrigger: StateFlow<Long> = _syncTrigger.asStateFlow()

    /** Timestamp of last automatic sync to enforce periodic-only behaviour. */
    private var lastAutoSyncAt = 0L
    /** Minimum interval between automatic syncs (30 minutes). */
    private val autoSyncIntervalMs = 30 * 60 * 1000L

    /** Toasts/errors that survive recomposition but are shown exactly once. */
    var ephemeralMessage by mutableStateOf<String?>(null)
        private set
    fun consumeEphemeralMessage() { ephemeralMessage = null }

    private suspend fun pendingSyncCount(): Int = withContext(Dispatchers.IO) {
        try {
            val helper = NoteDatabaseHelper(EchoWithinApplication.instance)
            helper.getPendingNotes().size
        } catch (_: Exception) { 0 }
    }

    private suspend fun refreshPendingSyncCount() {
        uiState = uiState.copy(pendingSyncCount = pendingSyncCount())
    }

    fun checkForUpdates(context: Context, showToastIfLatest: Boolean = false) {
        viewModelScope.launch {
            val updateManager = com.example.echowithin.data.network.AppUpdateManager(context)
            val info = updateManager.checkForUpdates()
            if (info.hasUpdate) {
                // Don't re-show update dialog for a version the user already dismissed
                val dismissedCode = com.example.echowithin.data.network.SessionManager.dismissedUpdateCode
                if (info.versionCode > dismissedCode) {
                    uiState = uiState.copy(updateInfo = info)
                }
            } else if (showToastIfLatest) {
                android.widget.Toast.makeText(
                    context,
                    "Your app is up to date! (v${com.example.echowithin.BuildConfig.VERSION_NAME})",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
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
        // Persist the dismissed version code so we don't re-show for the same version
        uiState.updateInfo?.let { info ->
            com.example.echowithin.data.network.SessionManager.dismissedUpdateCode = info.versionCode
        }
        uiState = uiState.copy(updateInfo = null, downloadProgress = null)
    }

    fun syncNotes() {
        if (uiState.isSyncing) return
        uiState = uiState.copy(isSyncing = true, error = null)
        viewModelScope.launch {
            repository.syncNotes()
                .onSuccess {
                    uiState = uiState.copy(
                        isSyncing = false,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    loadNotes(silent = true)
                    ephemeralMessage = "Synced with the server"
                    // One-shot dedup of any duplicates that piled up before
                    // the v1.7.1 sync-flag fix. Server endpoint is a no-op
                    // when there are no duplicates, so it's safe to call
                    // every sync — but we only surface a toast when it
                    // actually cleaned something up.
                    val removed = repository.dedupNotesOnServer()
                    if (removed > 0) {
                        loadNotes(silent = true)
                        ephemeralMessage = "Cleaned up $removed duplicate note${if (removed == 1) "" else "s"} from a previous sync"
                    }
                }
                .onFailure { t ->
                    uiState = uiState.copy(isSyncing = false, error = t.message ?: "Sync failed")
                }
        }
    }

    /**
     * Called by the root scaffold whenever the connectivity state flips
     * to "online". Runs at most one sync per on-online event, debounced
     * by 2s so a Wi-Fi → cell → Wi-Fi flip in quick succession still
     * produces only one push.
     *
     * Respects the user's syncMode preference: in "manual" mode, reconnecting
     * does NOT auto-push pending changes — the user has to tap the Sync
     * button. In "automatic" mode, sync only runs periodically (every 30
     * minutes) to avoid excessive network usage, not on every reconnect.
     * The pending count is still surfaced to the UI so the offline
     * banner can show "N changes pending".
     */
    fun onConnectivityChanged(isOnline: Boolean) {
        if (!isOnline) return
        val isAutomatic = com.example.echowithin.data.network.SessionManager.syncMode == "automatic"
        viewModelScope.launch {
            val pending = pendingSyncCount()
            uiState = uiState.copy(pendingSyncCount = pending)
            if (isAutomatic) {
                val now = System.currentTimeMillis()
                if (now - lastAutoSyncAt >= autoSyncIntervalMs) {
                    lastAutoSyncAt = now
                    _syncTrigger.value = now
                }
            }
        }
    }

    private fun hasToken(): Boolean {
        val t = com.example.echowithin.data.network.SessionManager.token
        return !t.isNullOrBlank() && t != "null"
    }

    var isInitialDataLoaded = false
        private set

    fun clearLocalData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearLocalData()
            }
            uiState = NotesUiState(isLoading = false)
            isInitialDataLoaded = false
        }
    }

    /**
     * Clears only server-synced notes, preserving offline-only notes.
     * Used on session expiry/401 so the user doesn't lose their private
     * local notes when the session dies.
     */
    fun clearOfflineData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearOfflineData()
            }
            // Reload to show only the preserved offline notes
            val (localNotes, pending) = withContext(Dispatchers.IO) {
                repository.getLocalNotes() to pendingSyncCount()
            }
            uiState = uiState.copy(
                notes = localNotes,
                isLoading = localNotes.isEmpty(),
                pendingSyncCount = pending,
                error = null
            )
            isInitialDataLoaded = false
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.wipeAllData()
            }
            uiState = NotesUiState(isLoading = false)
            isInitialDataLoaded = false
        }
    }

    fun loadAllData() {
        if (isInitialDataLoaded) return
        isInitialDataLoaded = true

        viewModelScope.launch {
            // 1. Instantly load local notes (offline-first) + count pending
            val (localNotes, pending) = withContext(Dispatchers.IO) {
                repository.getLocalNotes() to pendingSyncCount()
            }
            uiState = uiState.copy(
                notes = localNotes,
                isLoading = localNotes.isEmpty(),
                pendingSyncCount = pending,
                error = null
            )

            // 2. Perform network sync and fetches concurrently in the background
            launch {
                val hasToken = !com.example.echowithin.data.network.SessionManager.token.isNullOrBlank() && com.example.echowithin.data.network.SessionManager.token != "null"
                if (hasToken && com.example.echowithin.data.network.SessionManager.syncMode == "automatic") {
                    repository.syncNotes()
                }
                // Refresh list from updated DB
                val (updatedNotes, newPending) = withContext(Dispatchers.IO) {
                    repository.getLocalNotes() to pendingSyncCount()
                }
                uiState = uiState.copy(
                    notes = updatedNotes,
                    isLoading = false,
                    pendingSyncCount = newPending,
                    lastSyncedAt = if (newPending < pending) System.currentTimeMillis() else uiState.lastSyncedAt
                )

                // Load metadata in parallel
                loadProposals()
                loadActiveShares()
                loadNotifications()
            }
        }
    }

    fun loadNotes(silent: Boolean = false) {
        viewModelScope.launch {
            val (localNotes, pending) = withContext(Dispatchers.IO) {
                repository.getLocalNotes() to pendingSyncCount()
            }
            if (!silent) {
                uiState = uiState.copy(
                    notes = localNotes,
                    isLoading = localNotes.isEmpty(),
                    error = null,
                    pendingSyncCount = pending
                )
            } else {
                uiState = uiState.copy(
                    notes = localNotes,
                    error = null,
                    pendingSyncCount = pending
                )
            }

            repository.getNotes()
                .onSuccess { notes ->
                    uiState = uiState.copy(
                        isLoading = false,
                        notes = notes,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    refreshPendingSyncCount()
                }
                .onFailure {
                    uiState = uiState.copy(isLoading = false)
                    if (uiState.notes.isEmpty()) {
                        uiState = uiState.copy(error = it.message ?: "Failed to load notes")
                    }
                    // Even on failure, keep the pending counter fresh.
                    refreshPendingSyncCount()
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
                // Single round-trip: ask the server for every active share
                // the user owns. Falls back to the per-note loop only if
                // the endpoint is missing (older server) so we never
                // regress to a silent empty state.
                val resp = com.example.echowithin.data.network.ApiClient.apiService.getActiveShares()
                val noteById = uiState.notes.associateBy { it.id }
                val pairs = resp.shares.mapNotNull { dto ->
                    val note = dto.note_id?.let(noteById::get)
                    if (note != null) {
                        note to listOf(
                            ShareDto(
                                share_id = dto.share_id,
                                permissions = dto.permissions,
                                surprise_theme = dto.surprise_theme,
                                use_typewriter = dto.use_typewriter,
                                auto_approve = dto.auto_approve,
                                created_at = dto.created_at,
                                expires_at = dto.expires_at,
                                has_password = dto.has_password
                            )
                        )
                    } else null
                }
                if (resp.shares.isNotEmpty() && pairs.isEmpty()) {
                    // Server returned shares but none matched a local note.
                    // Surface them as standalone cards (synthesised note)
                    // so the user can still see and revoke their links.
                    // The server now returns is_locked for each share, so
                    // the card can render the lock badge even when the
                    // note itself isn't in the local cache.
                    val synthetic = resp.shares.map { dto ->
                        val title = dto.note_title.ifBlank { "Untitled note" }
                        AppNote(
                            id = dto.note_id ?: "share_${dto.share_id}",
                            title = title,
                            content = "",
                            reference = "",
                            tags = emptyList(),
                            updatedAt = dto.created_at ?: "",
                            isLocked = dto.is_locked,
                            isPinned = false,
                            isSynced = true,
                            pendingOp = "none",
                            updateAvailable = false
                        ) to listOf(
                            ShareDto(
                                share_id = dto.share_id,
                                permissions = dto.permissions,
                                surprise_theme = dto.surprise_theme,
                                use_typewriter = dto.use_typewriter,
                                auto_approve = dto.auto_approve,
                                created_at = dto.created_at,
                                expires_at = dto.expires_at,
                                has_password = dto.has_password
                            )
                        )
                    }
                    uiState = uiState.copy(sharesLoading = false, activeShares = synthetic)
                } else {
                    uiState = uiState.copy(sharesLoading = false, activeShares = pairs)
                }
            } catch (e: Exception) {
                // Fallback: per-note loop (older servers without the new
                // endpoint). Skip local-only notes (their IDs aren't
                // valid ObjectIds and would 400 the server).
                try {
                    val deferreds = uiState.notes
                        .filter { !it.id.startsWith("local_") }
                        .map { note ->
                            async {
                                try {
                                    val sharesList = com.example.echowithin.data.network.ApiClient.apiService.getShares(note.id).shares
                                    if (sharesList.isNotEmpty()) note to sharesList else null
                                } catch (_: Exception) { null }
                            }
                        }
                    val result = awaitAll(*deferreds.toTypedArray()).filterNotNull()
                    uiState = uiState.copy(sharesLoading = false, activeShares = result)
                } catch (_: Exception) {
                    uiState = uiState.copy(sharesLoading = false)
                }
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
        if (uiState.markingAllRead) return
        if (uiState.unreadNotificationsCount == 0) {
            // Nothing to do — avoid a useless round-trip and a confusing
            // "0 cleared" toast. The button is hidden in this state but
            // we double-check defensively.
            return
        }
        val previousUnread = uiState.unreadNotificationsCount
        // Optimistic UI: clear the badge instantly so the user sees
        // something happened. The reload at the end either confirms it
        // or resets it if the server rejected.
        uiState = uiState.copy(
            markingAllRead = true,
            unreadNotificationsCount = 0,
            notifications = uiState.notifications.map { it.copy(has_unread = false) }
        )
        viewModelScope.launch {
            var postsMarked = 0
            var proposalsMarked = 0
            var failed: String? = null
            try {
                val postsResp = com.example.echowithin.data.network.ApiClient.apiService.markAllPostsRead()
                postsMarked = (postsResp.message?.toIntOrNull() ?: 0)
                if (postsMarked == 0) {
                    // GenericResponse doesn't carry a count — best-effort
                    // surface a non-zero number so the toast is meaningful.
                    postsMarked = previousUnread
                }
            } catch (e: Exception) { failed = "Posts: ${e.message ?: "failed"}" }
            try {
                com.example.echowithin.data.network.ApiClient.apiService.markAllProposalsRead()
                proposalsMarked = 1
            } catch (e: Exception) { failed = (failed?.let { "$it; " } ?: "") + "Proposals: ${e.message ?: "failed"}" }

            // Always reload to reconcile server-side state — the optimistic
            // clear above makes the UI feel snappy; the reload makes it
            // correct.
            loadNotifications()
            loadProposals()

            val total = postsMarked + proposalsMarked
            uiState = uiState.copy(
                markingAllRead = false,
                lastMarkedReadCount = if (total > 0) total else previousUnread,
                error = failed
            )
            ephemeralMessage = if (failed == null) {
                if (total > 0) "Cleared $total item${if (total == 1) "" else "s"}"
                else "All caught up"
            } else {
                "Marked as read, but: $failed"
            }
        }
    }

    fun approveProposal(versionId: String, comment: String = "", autoApproveSubsequent: Boolean = false) {
        viewModelScope.launch {
            try {
                com.example.echowithin.data.network.ApiClient.apiService.decideProposal(
                    versionId,
                    com.example.echowithin.data.model.ProposalDecisionDto(
                        decision = "approve",
                        comment = comment.take(180),
                        auto_approve_subsequent = autoApproveSubsequent
                    )
                )
                loadProposals()
                loadNotes()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message ?: "Failed to approve proposal")
            }
        }
    }

    fun rejectProposal(versionId: String, comment: String = "") {
        viewModelScope.launch {
            try {
                com.example.echowithin.data.network.ApiClient.apiService.decideProposal(
                    versionId,
                    com.example.echowithin.data.model.ProposalDecisionDto(
                        decision = "reject",
                        comment = comment.take(180),
                        auto_approve_subsequent = false
                    )
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

    fun syncNoteWithOriginal(noteId: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            repository.syncNoteWithOriginal(noteId)
                .onSuccess { response ->
                    loadNotes()
                    onDone(response.message ?: "Sync completed")
                }
                .onFailure { error ->
                    uiState = uiState.copy(error = error.message ?: "Sync failed")
                    onDone(null)
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

    /**
     * Checks if there are any offline-only notes (created before login).
     */
    fun hasOfflineNotes(): Boolean = runBlocking {
        repository.getOfflineNotesCount() > 0
    }

    /**
     * Returns the count of offline-only notes (created before login).
     * Safe to call from withContext(Dispatchers.IO).
     */
    suspend fun getOfflineNotesCount(): Int {
        return repository.getOfflineNotesCount()
    }

    /**
     * Backs up offline-only notes to the server by marking them for sync.
     * Changes pendingOp from "none" to "create" so the next sync pushes them.
     */
    fun backupOfflineNotes(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val backedUp = repository.markOfflineNotesForBackup()
            if (backedUp > 0) {
                ephemeralMessage = "Preparing $backedUp offline note${if (backedUp > 1) "s" else ""} for backup..."
                syncNotes()
            }
            onComplete(backedUp)
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
