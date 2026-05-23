package com.example.echowithin.data.repository

import com.example.echowithin.EchoWithinApplication
import com.example.echowithin.data.local.NoteDatabaseHelper
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.data.model.CreateNoteRequest
import com.example.echowithin.data.model.NoteDto
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
            // 1. Immediately return local notes
            val localNotes = dbHelper.getAllNotes()
            
            // 2. If Automatic Sync is active, trigger sync in the background
            if (SessionManager.syncMode == "automatic" && SessionManager.accountTier != "free") {
                try {
                    syncNotesInternal()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dbHelper.getAllNotes()
            } else {
                localNotes
            }
        }
    }

    suspend fun syncNotes(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (SessionManager.accountTier != "free") {
                syncNotesInternal()
            }
        }
    }

    private suspend fun syncNotesInternal() {
        // 1. Push pending local changes to the server
        val pending = dbHelper.getPendingNotes()
        for (note in pending) {
            try {
                when (note.pendingOp) {
                    "create" -> {
                        val response = api.createNote(
                            CreateNoteRequest(
                                content = note.content,
                                reference = note.reference,
                                tags = note.tags
                            )
                        )
                        if (response.success && !response.id.isNullOrBlank()) {
                            // Delete local temp note and save new synced note
                            dbHelper.deletePhysically(note.id)
                            dbHelper.saveNote(note.copy(id = response.id, isSynced = true, pendingOp = "none"))
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
                            }
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) {
                                dbHelper.deletePhysically(note.id)
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

        // 2. Pull latest notes list from server
        val response = api.getNotes(page = 1, perPage = 100)
        val serverNotes = response.notes.map { it.toAppNote() }
        val serverIds = serverNotes.map { it.id }.toSet()

        // 3. Reconcile server notes with local notes
        for (serverNote in serverNotes) {
            val local = dbHelper.getNoteById(serverNote.id)
            if (local == null) {
                // Not in database, insert
                dbHelper.saveNote(serverNote)
            } else if (local.isSynced) {
                // Local is synced, overwrite with server's latest
                dbHelper.saveNote(serverNote)
            }
            // If local is modified but not synced, keep local version
        }

        // 4. Remove local notes that were deleted on the server and are already synced
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
            
            val isFree = SessionManager.accountTier == "free"
            if (isFree) {
                val currentCount = dbHelper.getAllNotes().size
                if (currentCount >= 50) {
                    throw Exception("Free plan limit reached: You can create up to 50 notes. Upgrade to Premium for unlimited notes.")
                }
            }
            
            val limit = if (isFree) 20000 else 100000
            if (content.length > limit) {
                val planName = if (isFree) "Free" else "Premium"
                throw Exception("$planName plan limit reached: Notes cannot exceed ${limit / 1000}k characters.")
            }
            
            val pendingOp = if (isFree) "none" else "create"
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

            if (SessionManager.syncMode == "automatic" && !isFree) {
                try {
                    syncNotesInternal()
                    // Try to get updated note ID
                    val updatedNote = dbHelper.getAllNotes().firstOrNull { it.content == content && it.reference == reference }
                    updatedNote?.id ?: tempId
                } catch (e: Exception) {
                    tempId
                }
            } else {
                tempId
            }
        }
    }

    suspend fun editNote(noteId: String, content: String, reference: String, tags: List<String>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .format(java.util.Date())
            
            val isFree = SessionManager.accountTier == "free"
            val limit = if (isFree) 20000 else 100000
            if (content.length > limit) {
                val planName = if (isFree) "Free" else "Premium"
                throw Exception("$planName plan limit reached: Notes cannot exceed ${limit / 1000}k characters.")
            }
            
            val existing = dbHelper.getNoteById(noteId)
            val pendingOp = if (isFree) "none" else (if (existing?.pendingOp == "create") "create" else "edit")
            
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

            if (SessionManager.syncMode == "automatic" && !isFree) {
                try {
                    syncNotesInternal()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            noteId
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val isFree = SessionManager.accountTier == "free"
            if (isFree) {
                dbHelper.deletePhysically(noteId)
            } else {
                dbHelper.markDeleted(noteId)
                if (SessionManager.syncMode == "automatic") {
                    try {
                        syncNotesInternal()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    suspend fun searchNotes(query: String): Result<SearchResultsDto> = runCatching {
        api.searchPersonalNotes(query)
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

    fun clearLocalData() {
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
            pendingOp = pendingOp
        )
    }
}
