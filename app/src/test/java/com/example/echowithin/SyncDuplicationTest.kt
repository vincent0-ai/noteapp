package com.example.echowithin

import android.content.SharedPreferences
import com.example.echowithin.data.local.NoteDbHelper
import com.example.echowithin.data.model.*
import com.example.echowithin.data.network.*
import com.example.echowithin.data.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SyncDuplicationTest {

    @Before
    fun setUp() {
        val prefsField = SessionManager::class.java.getDeclaredField("prefs")
        prefsField.isAccessible = true
        prefsField.set(SessionManager, FakeSharedPreferences())
    }

    @Test
    fun testCreateNote_generatesUniqueUUIDs() = runBlocking {
        // Arrange
        val savedNotes = mutableListOf<AppNote>()
        val fakeDb = object : NoteDbHelper {
            override fun saveNote(note: AppNote, isSynced: Boolean, pendingOp: String) {
                savedNotes.add(note)
            }
            override fun getNoteById(id: String): AppNote? = null
            override fun getAllNotes(): List<AppNote> = emptyList()
            override fun getPendingNotes(): List<AppNote> = emptyList()
            override fun markDeleted(id: String) {}
            override fun deletePhysically(id: String) {}
            override fun clearSyncFlags() {}
            override fun clearAll() {}
            override fun clearSyncedNotes() {}
        }
        val fakeApi = FakeApiService()
        val repository = NotesRepository(api = fakeApi, dbHelper = fakeDb)

        // Act
        val ids = (1..100).map {
            val res = repository.createNote("Content $it", "", emptyList())
            assertTrue(res.isSuccess)
            res.getOrThrow()
        }

        // Assert
        assertEquals(100, ids.toSet().size) // All 100 IDs must be unique
        assertTrue(ids.all { it.startsWith("local_") }) // Must start with "local_"
        // Check that the suffix is a valid 36-char UUID
        assertTrue(ids.all { it.substring("local_".length).length == 36 })
    }

    @Test
    fun testSyncNotes_serializesConcurrentSyncs() = runBlocking {
        // Arrange
        val syncCallCount = AtomicInteger(0)
        val activeSyncs = AtomicInteger(0)
        val maxConcurrentSyncs = AtomicInteger(0)
        
        // Populate local DB with one pending note
        val pendingNotes = mutableListOf(
            AppNote("local_uuid_123", "Title", "Content", "", emptyList(), "now", isSynced = false, pendingOp = "create")
        )

        val fakeDb = object : NoteDbHelper {
            override fun saveNote(note: AppNote, isSynced: Boolean, pendingOp: String) {
                // When saved as synced, update our local list
                if (isSynced && pendingOp == "none") {
                    val idx = pendingNotes.indexOfFirst { it.id == note.id }
                    if (idx != -1) {
                        pendingNotes[idx] = note.copy(isSynced = true, pendingOp = "none")
                    } else {
                        // If it's a new ID (server assigned), replace the local one
                        pendingNotes.clear()
                        pendingNotes.add(note)
                    }
                }
            }
            override fun getNoteById(id: String): AppNote? = pendingNotes.firstOrNull { it.id == id }
            override fun getAllNotes(): List<AppNote> = pendingNotes
            override fun getPendingNotes(): List<AppNote> = pendingNotes.filter { !it.isSynced }
            override fun markDeleted(id: String) {
                val idx = pendingNotes.indexOfFirst { it.id == id }
                if (idx != -1) {
                    pendingNotes[idx] = pendingNotes[idx].copy(pendingOp = "delete", isSynced = false)
                }
            }
            override fun deletePhysically(id: String) {
                pendingNotes.removeAll { it.id == id }
            }
            override fun clearSyncFlags() {}
            override fun clearAll() {
                pendingNotes.clear()
            }
            override fun clearSyncedNotes() {
                pendingNotes.removeAll { it.isSynced }
            }
        }

        val fakeApi = object : FakeApiService() {
            override suspend fun getNotes(page: Int, perPage: Int): NotesResponse {
                return NotesResponse(notes = emptyList())
            }
            
            override suspend fun createNote(body: CreateNoteRequest): CreateNoteResponse {
                syncCallCount.incrementAndGet()
                val current = activeSyncs.incrementAndGet()
                synchronized(maxConcurrentSyncs) {
                    if (current > maxConcurrentSyncs.get()) {
                        maxConcurrentSyncs.set(current)
                    }
                }
                
                // Introduce artificial delay to test concurrency overlap
                delay(100)
                
                activeSyncs.decrementAndGet()
                return CreateNoteResponse(success = true, id = "server_uuid_123")
            }
            
            override suspend fun dedupNotes(confirm: Boolean): DedupResponseDto {
                return DedupResponseDto(success = true, removed_count = 0, kept_count = 0)
            }
        }

        // Configure SessionManager to bypass token check
        SessionManager.token = "test_token"

        val repository = NotesRepository(api = fakeApi, dbHelper = fakeDb)

        // Act - Trigger 3 syncs concurrently
        val jobs = List(3) {
            launch(Dispatchers.Default) {
                repository.syncNotes()
            }
        }
        jobs.forEach { it.join() }

        // Assert
        // 1. They must execute sequentially. Max concurrent runs inside createNote must be exactly 1.
        assertEquals(1, maxConcurrentSyncs.get())
        // 2. The first sync pushes the note, deletes the local "local_uuid_123" note, and saves "server_uuid_123".
        // The subsequent syncs find 0 pending notes, so they don't call createNote.
        assertEquals(1, syncCallCount.get())
    }

    private open class FakeApiService : EchoWithinApiService {
        override suspend fun login(body: LoginRequest): LoginResponse = TODO()
        override suspend fun logout(): GenericResponse = TODO()
        override suspend fun appReauth(): LoginResponse = TODO()
        override suspend fun refreshToken(): LoginResponse = TODO()
        override suspend fun getNotes(page: Int, perPage: Int): NotesResponse = NotesResponse(notes = emptyList())
        override suspend fun getNoteById(noteId: String): NoteDto = TODO()
        override suspend fun createNote(body: CreateNoteRequest): CreateNoteResponse = TODO()
        override suspend fun editNote(noteId: String, body: CreateNoteRequest): CreateNoteResponse = TODO()
        override suspend fun syncNote(noteId: String): SyncNoteResponse = TODO()
        override suspend fun getProfile(): ProfileResponse = ProfileResponse("user", "email", "premium")
        override suspend fun deleteNote(noteId: String): GenericResponse = TODO()
        override suspend fun toggleNoteLock(noteId: String): ToggleLockResponse = TODO()
        override suspend fun toggleNotePin(noteId: String): TogglePinResponse = TODO()
        override suspend fun getProposals(): ProposalsListDto = TODO()
        override suspend fun createShare(noteId: String, body: ShareRequestDto): ShareResponseDto = TODO()
        override suspend fun createShareMultipart(
            noteId: String,
            permissions: okhttp3.RequestBody,
            expiresIn: okhttp3.RequestBody?,
            accessCode: okhttp3.RequestBody?,
            surpriseTheme: okhttp3.RequestBody,
            useTypewriter: okhttp3.RequestBody,
            autoApprove: okhttp3.RequestBody,
            valentinePhoto: okhttp3.MultipartBody.Part?,
            valentineAudio: okhttp3.MultipartBody.Part?
        ): ShareResponseDto = TODO()
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
        override suspend fun dedupNotes(confirm: Boolean): DedupResponseDto = TODO()
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any?>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
                tempMap[key] = values
                return this
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun remove(key: String): SharedPreferences.Editor {
                tempMap[key] = null
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                map.clear()
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                for ((k, v) in tempMap) {
                    if (v == null) map.remove(k) else map[k] = v
                }
            }
        }
    }
}
