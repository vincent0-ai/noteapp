package com.example.echowithin

import android.content.SharedPreferences
import com.example.echowithin.data.local.NoteDbHelper
import com.example.echowithin.data.model.*
import com.example.echowithin.data.network.EchoWithinApiService
import com.example.echowithin.data.network.FcmTokenDto
import com.example.echowithin.data.network.SessionManager
import okhttp3.ResponseBody.Companion.toResponseBody
import com.example.echowithin.data.repository.NotesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies Phase 2C sync behaviour: per-note "failed" state on a failed push,
 * and the conflict copy that is kept when a locally-edited note collides with
 * a newer server version.
 */
class SyncStateTest {

    @Before
    fun setUp() {
        val prefsField = SessionManager::class.java.getDeclaredField("prefs")
        prefsField.isAccessible = true
        prefsField.set(SessionManager, FakePrefs())
        SessionManager.token = "test_token"
        SessionManager.accountTier = "premium"
    }

    @Test
    fun pushFailure_marksNoteAsFailed() = runBlocking {
        val db = InMemoryDb(
            AppNote("server_1", "T", "local edit", "", emptyList(), "2026-10-20T00:00:00Z",
                isSynced = false, pendingOp = "edit", syncStatus = "pending")
        )
        val api = object : BaseFakeApi() {
            override suspend fun editNote(noteId: String, body: CreateNoteRequest): CreateNoteResponse {
                throw httpException500()
            }
        }
        NotesRepository(api = api, dbHelper = db).syncNotes()

        val note = db.byId("server_1")
        assertEquals("failed", note?.syncStatus)
        assertEquals("edit", note?.pendingOp) // still retried next sync
    }

    @Test
    fun newerServerNote_keepsConflictCopyOfLocalEdit() = runBlocking {
        val db = InMemoryDb(
            AppNote("server_1", "T", "local edit", "", emptyList(), "2026-10-20T00:00:00Z",
                isSynced = false, pendingOp = "edit", syncStatus = "pending")
        )
        // editNote throws a 500 so the local edit is NOT pushed; getNotes returns
        // a newer server version — the classic conflict.
        val api = object : BaseFakeApi() {
            override suspend fun editNote(noteId: String, body: CreateNoteRequest): CreateNoteResponse {
                throw httpException500()
            }
            override suspend fun getNotes(page: Int, perPage: Int): NotesResponse = NotesResponse(
                notes = listOf(
                    NoteDto(id = "server_1", content = "server content", updated_at = "2026-10-25T00:00:00Z")
                ),
                pagination = PaginationDto(page = 1, per_page = perPage, total = 1, has_more = false)
            )
        }
        NotesRepository(api = api, dbHelper = db).syncNotes()

        val canonical = db.byId("server_1")
        assertEquals("server content", canonical?.content) // last-write-wins (server newer)

        val conflict = db.all().firstOrNull { it.id.startsWith("local_conflict_server_1") }
        assertTrue("expected a conflict copy", conflict != null)
        assertEquals("local edit", conflict?.content) // local edit preserved
    }

    // ── Fakes ──

    private fun httpException500(): retrofit2.HttpException =
        retrofit2.HttpException(retrofit2.Response.error<Any>(500, "boom".toResponseBody(null)))

    private class InMemoryDb(initial: AppNote) : NoteDbHelper {
        private val store = linkedMapOf<String, AppNote>(initial.id to initial)
        fun byId(id: String) = store[id]
        fun all(): List<AppNote> = store.values.toList()

        override fun saveNote(note: AppNote, isSynced: Boolean, pendingOp: String) {
            store[note.id] = note
        }
        override fun getNoteById(id: String): AppNote? = store[id]
        override fun getAllNotes(): List<AppNote> = store.values.filter { it.pendingOp != "delete" && !it.isTrashed }
        override fun getPendingNotes(): List<AppNote> = store.values.filter { !it.isSynced }
        override fun markDeleted(id: String) { store[id]?.let { store[id] = it.copy(pendingOp = "delete", isSynced = false) } }
        override fun deletePhysically(id: String) { store.remove(id) }
        override fun clearSyncFlags() {}
        override fun clearAll() { store.clear() }
        override fun clearSyncedNotes() { store.entries.removeAll { it.value.isSynced } }
        override fun getTrashedNotes(): List<AppNote> = emptyList()
        override fun trashNote(id: String) {}
        override fun restoreNote(id: String) {}
        override fun emptyTrash() {}
        override fun updateReminder(id: String, reminderAt: String?) {}
        override fun updateColorTag(id: String, colorTag: String) {}
        override fun getNotesWithActiveReminders(): List<AppNote> = emptyList()
    }

    private open class BaseFakeApi : EchoWithinApiService {
        override suspend fun getNotes(page: Int, perPage: Int): NotesResponse = NotesResponse(notes = emptyList())
        override suspend fun editNote(noteId: String, body: CreateNoteRequest): CreateNoteResponse = CreateNoteResponse(success = true)
        override suspend fun login(body: LoginRequest): LoginResponse = TODO()
        override suspend fun logout(): GenericResponse = TODO()
        override suspend fun appReauth(): LoginResponse = TODO()
        override suspend fun refreshToken(): LoginResponse = TODO()
        override suspend fun getNoteById(noteId: String): NoteDto = TODO()
        override suspend fun createNote(body: CreateNoteRequest): CreateNoteResponse = TODO()
        override suspend fun syncNote(noteId: String): SyncNoteResponse = TODO()
        override suspend fun getProfile(): ProfileResponse = ProfileResponse("u", "e", "premium")
        override suspend fun deleteNote(noteId: String): GenericResponse = TODO()
        override suspend fun toggleNoteLock(noteId: String): ToggleLockResponse = TODO()
        override suspend fun toggleNotePin(noteId: String): TogglePinResponse = TODO()
        override suspend fun getProposals(): ProposalsListDto = TODO()
        override suspend fun createShare(noteId: String, body: ShareRequestDto): ShareResponseDto = TODO()
        override suspend fun createShareMultipart(noteId: String, permissions: okhttp3.RequestBody, expiresIn: okhttp3.RequestBody?, accessCode: okhttp3.RequestBody?, surpriseTheme: okhttp3.RequestBody, useTypewriter: okhttp3.RequestBody, autoApprove: okhttp3.RequestBody, valentinePhoto: okhttp3.MultipartBody.Part?, valentineAudio: okhttp3.MultipartBody.Part?): ShareResponseDto = TODO()
        override suspend fun getActiveShares(): ActiveSharesResponseDto = TODO()
        override suspend fun getShares(noteId: String): SharesListDto = TODO()
        override suspend fun revokeShare(shareId: String): GenericResponse = TODO()
        override suspend fun getShareComments(shareId: String): CommentsListDto = TODO()
        override suspend fun addShareComment(shareId: String, body: CommentRequestDto): GenericResponse = TODO()
        override suspend fun addShareReply(shareId: String, commentId: String, body: CommentRequestDto): GenericResponse = TODO()
        override suspend fun deleteShareComment(shareId: String, commentId: String): GenericResponse = TODO()
        override suspend fun getShareAttachments(shareId: String): AttachmentsListDto = TODO()
        override suspend fun getVersions(noteId: String): VersionsListDto = TODO()
        override suspend fun restoreVersion(noteId: String, versionId: String): GenericResponse = TODO()
        override suspend fun decideProposal(versionId: String, body: ProposalDecisionDto): GenericResponse = TODO()
        override suspend fun toggleShareAutoApprove(shareId: String, body: ToggleShareAutoApproveDto): GenericResponse = TODO()
        override suspend fun setupAppLock(body: AppLockSetupDto): GenericResponse = TODO()
        override suspend fun verifyAppLock(body: AppLockVerifyDto): GenericResponse = TODO()
        override suspend fun checkLockStatus(): AppLockStatusDto = TODO()
        override suspend fun removeAppLock(body: AppLockRemoveDto): GenericResponse = TODO()
        override suspend fun register(body: RegisterRequest): RegisterResponse = TODO()
        override suspend fun confirm(email: String, body: ConfirmRequest): GenericResponse = TODO()
        override suspend fun searchPersonalNotes(query: String, page: Int, perPage: Int): SearchResultsDto = TODO()
        override suspend fun getPublicPosts(): List<PublicPostDto> = TODO()
        override suspend fun registerFcm(body: FcmTokenDto): GenericResponse = TODO()
        override suspend fun unregisterFcm(body: FcmTokenDto): GenericResponse = TODO()
        override suspend fun activatePremium(): GenericResponse = TODO()
        override suspend fun getBadgeCounts(): BadgeCountsDto = TODO()
        override suspend fun getNotifications(): NotificationsResponseDto = TODO()
        override suspend fun markAllPostsRead(): GenericResponse = TODO()
        override suspend fun markAllProposalsRead(): GenericResponse = TODO()
        override suspend fun dedupNotes(confirm: Boolean): DedupResponseDto = DedupResponseDto(success = true)
        override suspend fun getTrash(page: Int, perPage: Int): TrashResponse = TrashResponse(notes = emptyList())
        override suspend fun restoreNote(noteId: String): GenericResponse = GenericResponse(success = true)
        override suspend fun emptyTrash(): GenericResponse = GenericResponse(success = true)
        override suspend fun getFolders(): FoldersResponse = FoldersResponse(folders = emptyList())
        override suspend fun moveNoteToFolder(noteId: String, body: Map<String, String?>): GenericResponse = GenericResponse(success = true)
        override suspend fun setReminder(noteId: String, body: ReminderRequest): GenericResponse = GenericResponse(success = true)
        override suspend fun setColor(noteId: String, body: ColorRequest): GenericResponse = GenericResponse(success = true)
        override suspend fun pingCollaborators(shareId: String): PingResponse = PingResponse(success = true)
    }

    private class FakePrefs : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()
        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val t = mutableMapOf<String, Any?>()
            override fun putString(key: String, value: String?) = apply { t[key] = value }
            override fun putStringSet(key: String, values: Set<String>?) = apply { t[key] = values }
            override fun putInt(key: String, value: Int) = apply { t[key] = value }
            override fun putLong(key: String, value: Long) = apply { t[key] = value }
            override fun putFloat(key: String, value: Float) = apply { t[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { t[key] = value }
            override fun remove(key: String) = apply { t[key] = null }
            override fun clear() = apply { map.clear() }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() { t.forEach { (k, v) -> if (v == null) map.remove(k) else map[k] = v } }
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }
}
