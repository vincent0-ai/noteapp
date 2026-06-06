package com.example.echowithin.data.repository

import com.example.echowithin.EchoWithinApplication
import com.example.echowithin.data.local.NoteDatabaseHelper
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.data.model.CreateNoteRequest
import com.example.echowithin.data.model.NoteDto
import com.example.echowithin.data.model.SearchHitDto
import com.example.echowithin.data.model.SearchResultsDto
import com.example.echowithin.data.network.ApiClient
import com.example.echowithin.data.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotesRepository {
    private val api = ApiClient.apiService
    private val dbHelper = NoteDatabaseHelper(EchoWithinApplication.instance)

    suspend fun getNotes(page: Int = 1, perPage: Int = 20): Result<List<AppNote>> = withContext(Dispatchers.IO) {
        runCatching {
            // Return local notes immediately.
            // Foreground sync should be triggered explicitly by the view model/UI layer (e.g. pull-to-refresh or app launch).
            dbHelper.getAllNotes()
        }
    }

    suspend fun syncNotes(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
            if (hasToken) {
                syncNotesInternal()
            }
        }
    }

    /**
     * Asks the server to clean up any duplicate notes the user may have
     * accumulated from a pre-v1.7.1 sync bug. Returns the number of notes
     * the server removed (0 if there were no duplicates). Safe to call
     * on every successful sync — the server is a no-op when there's
     * nothing to dedupe.
     */
    suspend fun dedupNotesOnServer(): Int = withContext(Dispatchers.IO) {
        try {
            val response = api.dedupNotes(confirm = true)
            if (response.success) response.removed_count else 0
        } catch (_: Exception) {
            0
        }
    }

    private suspend fun syncNotesInternal() {
        val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
        if (!hasToken) return

        val isFree = SessionManager.accountTier == "free"
        val maxNoteSize = if (isFree) 20000 else 100000
        val maxServerNotes = 50

        // 1. Fetch current server notes to determine server count
        val initialResponse = try {
            api.getNotes(page = 1, perPage = 100)
        } catch (_: Exception) {
            null
        }
        var serverCount = initialResponse?.notes?.size ?: 0

        // 2. Push pending local changes to the server
        val pending = dbHelper.getPendingNotes()
        for (note in pending) {
            // Check character size limits
            if (note.content.length > maxNoteSize) {
                continue
            }

            // The discriminator for "should this be pushed" is the ID prefix,
            // not the pending_op value. A "local_*" id means the note was
            // created on the device and never reached the server, so it must
            // be pushed as CREATE. A real server id (UUID) means the note was
            // already on the server, so we only push if the user explicitly
            // edited or deleted it offline (pending_op = edit/delete). Any
            // other combination (e.g. is_synced=0 with a server id and
            // pending_op="none") is a stale sync flag from a logout/401 and
            // must be SKIPPED — re-pushing it would create a duplicate note
            // on the server.
            val isLocal = note.id.startsWith("local_")
            val op = when {
                note.pendingOp == "edit" -> "edit"
                note.pendingOp == "delete" -> "delete"
                note.pendingOp == "create" -> "create"
                isLocal -> "create"
                else -> "skip"
            }

            if (op == "skip") {
                // Stale row — leave the local copy as-is. The pull step below
                // will refresh it from the server (or, if it really was
                // deleted on the server, remove it in step 5).
                continue
            }

            // Check total server note count limit for Free users
            if (op == "create" && isFree && serverCount >= maxServerNotes) {
                continue
            }

            try {
                when (op) {
                    "create" -> {
                        val response = api.createNote(
                            CreateNoteRequest(
                                content = note.content,
                                reference = note.reference,
                                tags = note.tags
                             )
                        )
                        if (response.success && !response.id.isNullOrBlank()) {
                            dbHelper.deletePhysically(note.id)
                            dbHelper.saveNote(note.copy(id = response.id, isSynced = true, pendingOp = "none"))
                            serverCount++
                        }
                    }
                    "edit" -> {
                        val response = api.editNote(
                            noteId = note.id,
                            body = CreateNoteRequest(
                                content = note.content,
                                reference = note.reference,
                                tags = note.tags
                            )
                        )
                        if (response.success) {
                            dbHelper.saveNote(note.copy(isSynced = true, pendingOp = "none"))
                        }
                    }
                    "delete" -> {
                        try {
                            val response = api.deleteNote(note.id)
                            if (response.success || response.error?.contains("not found", ignoreCase = true) == true) {
                                dbHelper.deletePhysically(note.id)
                                serverCount--
                            }
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) {
                                dbHelper.deletePhysically(note.id)
                                serverCount--
                            } else {
                                throw e
                             }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Stop syncing remaining items if network error occurs to preserve ordering
                break
            }
        }

        // 3. Pull latest notes list from server
        val response = api.getNotes(page = 1, perPage = 100)
        val serverNotes = response.notes.map { it.toAppNote() }
        val serverIds = serverNotes.map { it.id }.toSet()

        // 4. Reconcile server notes with local notes
        for (serverNote in serverNotes) {
            val local = dbHelper.getNoteById(serverNote.id)
            if (local == null) {
                dbHelper.saveNote(serverNote)
            } else if (local.isSynced) {
                dbHelper.saveNote(serverNote)
            }
        }

        // 5. Remove local notes that were deleted on the server and are already synced
        val localNotes = dbHelper.getAllNotes()
        for (localNote in localNotes) {
            if (!localNote.id.startsWith("local_") && !serverIds.contains(localNote.id) && localNote.isSynced) {
                dbHelper.deletePhysically(localNote.id)
            }
        }
    }

    suspend fun createNote(content: String, reference: String, tags: List<String>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tempId = "local_" + System.currentTimeMillis()
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .format(java.util.Date())
            
            val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
            val pendingOp = if (!hasToken) "none" else "create"
            val note = AppNote(
                id = tempId,
                title = content.lineSequence().firstOrNull()?.trim()?.take(60) ?: "Untitled",
                content = content,
                reference = reference,
                tags = tags,
                updatedAt = now,
                isLocked = false,
                isPinned = false,
                isSynced = false,
                pendingOp = pendingOp
            )
            
            dbHelper.saveNote(note, isSynced = false, pendingOp = pendingOp)
            tempId
        }
    }

    suspend fun editNote(noteId: String, content: String, reference: String, tags: List<String>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .format(java.util.Date())
            
            val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
            val existing = dbHelper.getNoteById(noteId)
            val pendingOp = if (!hasToken) "none" else (if (existing?.pendingOp == "create") "create" else "edit")
            
            val note = AppNote(
                id = noteId,
                title = content.lineSequence().firstOrNull()?.trim()?.take(60) ?: "Untitled",
                content = content,
                reference = reference,
                tags = tags,
                updatedAt = now,
                isLocked = existing?.isLocked ?: false,
                isPinned = existing?.isPinned ?: false,
                isSynced = false,
                pendingOp = pendingOp
            )
            
            dbHelper.saveNote(note, isSynced = false, pendingOp = pendingOp)
            noteId
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
            if (!hasToken) {
                dbHelper.deletePhysically(noteId)
            } else {
                dbHelper.markDeleted(noteId)
            }
        }
    }

    fun getLocalNotes(): List<AppNote> {
        return dbHelper.getAllNotes()
    }

    suspend fun searchNotes(query: String): Result<SearchResultsDto> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                api.searchPersonalNotes(query)
            } catch (e: Exception) {
                // API is down or user is offline/unauthenticated — fallback to offline search.
                // IMPORTANT: filter out locked notes here too. The server-side
                // search already excludes them (see /personal_post/search in
                // blueprints/notes.py), and the web platform never shows them
                // until the user unlocks them. We mirror that behaviour so
                // locked note content can never leak through the offline
                // fallback path either.
                val localNotes = dbHelper.getAllNotes().filter { !it.isLocked }
                val filteredNotes = localNotes.filter { note ->
                    note.content.contains(query, ignoreCase = true) ||
                    note.reference.contains(query, ignoreCase = true) ||
                    note.tags.any { it.contains(query, ignoreCase = true) }
                }
                
                val hits = filteredNotes.map { note ->
                    val snippet = highlightText(note.content, query)
                    SearchHitDto(
                        id = note.id,
                        content_highlighted = snippet,
                        snippet = snippet,
                        created_at = note.updatedAt
                    )
                }
                
                SearchResultsDto(
                    results = hits,
                    total = hits.size,
                    query = query
                )
            }
        }
    }

    private fun highlightText(content: String, query: String): String {
        if (query.isBlank()) return content
        val queryLower = query.lowercase()
        val contentLower = content.lowercase()
        val index = contentLower.indexOf(queryLower)
        if (index == -1) {
            return content.take(150)
        }
        
        val start = (index - 60).coerceAtLeast(0)
        val end = (index + query.length + 90).coerceAtMost(content.length)
        val rawSnippet = content.substring(start, end)
        
        val snippetLower = rawSnippet.lowercase()
        var matchIndex = snippetLower.indexOf(queryLower)
        val sb = StringBuilder()
        var current = 0
        while (matchIndex != -1) {
            sb.append(rawSnippet.substring(current, matchIndex))
            sb.append("<mark class=\"search-highlight\">")
            sb.append(rawSnippet.substring(matchIndex, matchIndex + query.length))
            sb.append("</mark>")
            current = matchIndex + query.length
            matchIndex = snippetLower.indexOf(queryLower, current)
        }
        sb.append(rawSnippet.substring(current))
        
        var result = sb.toString()
        if (start > 0) result = "...$result"
        if (end < content.length) result = "$result..."
        return result
    }

    suspend fun getNoteById(noteId: String): Result<AppNote> = withContext(Dispatchers.IO) {
        runCatching {
            val local = dbHelper.getNoteById(noteId)
            if (local != null) {
                local
            } else {
                val response = api.getNoteById(noteId)
                val note = response.toAppNote()
                dbHelper.saveNote(note)
                note
            }
        }
    }

    suspend fun toggleNoteLock(noteId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.toggleNoteLock(noteId)
            if (response.success) {
                val local = dbHelper.getNoteById(noteId)
                if (local != null) {
                    dbHelper.saveNote(local.copy(isLocked = response.is_locked))
                }
                response.is_locked
            } else {
                throw Exception(response.error ?: "Toggle lock failed")
            }
        }
    }

    suspend fun syncNoteWithOriginal(noteId: String): Result<com.example.echowithin.data.model.SyncNoteResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.syncNote(noteId)
            if (response.success) {
                if (response.content != null && !response.pending_approval) {
                    val local = dbHelper.getNoteById(noteId)
                    if (local != null) {
                        val titleCandidate = response.content.lineSequence().firstOrNull()?.trim().orEmpty()
                        val title = if (titleCandidate.isBlank()) "Untitled" else titleCandidate.take(60)
                        
                        dbHelper.saveNote(
                            local.copy(
                                content = response.content,
                                title = title,
                                updateAvailable = false,
                                isSynced = true
                            )
                        )
                    }
                }
            } else {
                throw Exception(response.error ?: response.message ?: "Sync failed")
            }
            response
        }
    }

    fun clearLocalData() {
        // Wipe the local cache on logout / 401 / splash token check. The
        // previous implementation called clearSyncFlags() which marked every
        // row as is_synced=0 + pending_op="none" and caused the sync loop to
        // re-push every already-synced note as a new CREATE (duplicate-notes
        // bug). Doing a full clear here means the next login starts from a
        // clean slate and the pull step simply repopulates from the server.
        dbHelper.clearAll()
    }

    fun wipeAllData() {
        dbHelper.clearAll()
    }

    private fun NoteDto.toAppNote(isSynced: Boolean = true, pendingOp: String = "none"): AppNote {
        val titleCandidate = content.lineSequence().firstOrNull()?.trim().orEmpty()
        val title = if (titleCandidate.isBlank()) "Untitled" else titleCandidate.take(60)
        return AppNote(
            id = id,
            title = title,
            content = content,
            reference = reference.orEmpty(),
            tags = tags,
            updatedAt = updated_at ?: created_at ?: "",
            isLocked = is_locked,
            isPinned = is_pinned,
            isSynced = isSynced,
            pendingOp = pendingOp,
            updateAvailable = update_available,
            sourceNoteId = source_note_id,
            sourceShareId = source_share_id
        )
    }
}
