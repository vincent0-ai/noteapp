package com.example.echowithin.data.network

import com.example.echowithin.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Simple in-memory fake implementation of the API used for local development and tests.
 * It provides deterministic, small payloads and accepts any non-empty credentials.
 */
class FakeEchoWithinApiService : EchoWithinApiService {
    private val nextId = AtomicInteger(1)
    private val notes = mutableListOf<NoteDto>()

    init {
        // Seed with a couple of notes
        notes += NoteDto(
            id = nextId.getAndIncrement().toString(),
            content = "Welcome to EchoWithin!\nThis is your first note.",
            reference = "",
            tags = listOf("welcome", "example"),
            created_at = "2026-01-01T00:00:00Z",
            updated_at = "2026-01-01T00:00:00Z"
        )
        notes += NoteDto(
            id = nextId.getAndIncrement().toString(),
            content = "Echo feature: write something and it will be saved.",
            reference = "",
            tags = listOf("echo"),
            created_at = "2026-01-02T00:00:00Z",
            updated_at = "2026-01-02T00:00:00Z"
        )
    }

    override suspend fun login(body: LoginRequest): LoginResponse {
        val username = body.username
        return if (username.isNotBlank() && body.password.isNotBlank()) {
            LoginResponse(success = true, username = username, email = "$username@example.local", x_app_token = "fake-token-${Random.nextInt(1000,9999)}")
        } else {
            LoginResponse(success = false, error = "Invalid credentials")
        }
    }

    override suspend fun logout(): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun appReauth(): LoginResponse {
        return LoginResponse(success = true, username = "fakeuser", email = "fake@example.local", x_app_token = "fake-token")
    }

    override suspend fun getNotes(page: Int, perPage: Int): NotesResponse {
        val start = (page - 1) * perPage
        val pageItems = if (start >= notes.size) emptyList() else notes.subList(start, (start + perPage).coerceAtMost(notes.size))
        val pagination = PaginationDto(page = page, per_page = perPage, total = notes.size, has_more = start + perPage < notes.size)
        return NotesResponse(notes = pageItems, pagination = pagination)
    }

    override suspend fun getNoteById(noteId: String): NoteDto {
        return notes.firstOrNull { it.id == noteId } ?: throw Exception("Note not found")
    }

    override suspend fun createNote(body: CreateNoteRequest): CreateNoteResponse {
        val id = nextId.getAndIncrement().toString()
        val now = "2026-05-20T00:00:00Z"
        val dto = NoteDto(
            id = id,
            content = body.content,
            reference = body.reference,
            tags = body.tags,
            created_at = now,
            updated_at = now
        )
        notes.add(0, dto)
        return CreateNoteResponse(success = true, id = id)
    }

    override suspend fun editNote(noteId: String, body: CreateNoteRequest): CreateNoteResponse {
        val idx = notes.indexOfFirst { it.id == noteId }
        if (idx != -1) {
            val old = notes[idx]
            notes[idx] = old.copy(
                content = body.content,
                reference = body.reference,
                tags = body.tags,
                updated_at = "2026-05-20T00:00:00Z"
            )
            return CreateNoteResponse(success = true, id = noteId)
        }
        return CreateNoteResponse(success = false, error = "Note not found")
    }

    override suspend fun getProfile(): ProfileResponse {
        return ProfileResponse(username = "fakeuser", email = "fake@example.local", account_tier = "premium", has_pin = true)
    }

    override suspend fun deleteNote(noteId: String): GenericResponse {
        notes.removeAll { it.id == noteId }
        return GenericResponse(success = true)
    }

    override suspend fun toggleNoteLock(noteId: String): ToggleLockResponse {
        val idx = notes.indexOfFirst { it.id == noteId }
        if (idx != -1) {
            val old = notes[idx]
            val newLocked = !old.is_locked
            notes[idx] = old.copy(is_locked = newLocked)
            return ToggleLockResponse(success = true, is_locked = newLocked)
        }
        return ToggleLockResponse(success = false, error = "Note not found")
    }

    override suspend fun getProposals(): ProposalsListDto {
        return ProposalsListDto(proposals = emptyList())
    }

    // Sharing endpoints - provide minimal successful/no-op implementations
    override suspend fun createShare(noteId: String, body: ShareRequestDto): ShareResponseDto {
        return ShareResponseDto(success = true, share_id = "share-${noteId}", url = "https://fake.local/share/${noteId}")
    }

    override suspend fun createShareMultipart(
        noteId: String,
        permissions: RequestBody,
        expiresIn: RequestBody?,
        accessCode: RequestBody?,
        surpriseTheme: RequestBody,
        useTypewriter: RequestBody,
        autoApprove: RequestBody,
        valentinePhoto: MultipartBody.Part?,
        valentineAudio: MultipartBody.Part?
    ): ShareResponseDto {
        return ShareResponseDto(success = true, share_id = "share-${noteId}", url = "https://fake.local/share/${noteId}")
    }

    override suspend fun getShares(noteId: String): SharesListDto {
        return SharesListDto(shares = emptyList())
    }

    override suspend fun revokeShare(shareId: String): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun getShareComments(shareId: String): CommentsListDto {
        return CommentsListDto(comments = emptyList())
    }

    override suspend fun addShareComment(shareId: String, body: CommentRequestDto): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun addShareReply(shareId: String, commentId: String, body: CommentRequestDto): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun deleteShareComment(shareId: String, commentId: String): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun getShareAttachments(shareId: String): AttachmentsListDto {
        return AttachmentsListDto(attachments = emptyList())
    }

    override suspend fun getVersions(noteId: String): VersionsListDto {
        return VersionsListDto(versions = emptyList())
    }

    override suspend fun restoreVersion(noteId: String, versionId: String): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun decideProposal(versionId: String, body: ProposalDecisionDto): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun setupAppLock(body: AppLockSetupDto): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun verifyAppLock(body: AppLockVerifyDto): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun checkLockStatus(): AppLockStatusDto {
        return AppLockStatusDto(unlocked = true, has_pin = true, remaining = null)
    }

    override suspend fun removeAppLock(): GenericResponse {
        return GenericResponse(success = true)
    }



    override suspend fun register(body: RegisterRequest): RegisterResponse {
        return RegisterResponse(success = true, confirmed = false, email = body.email, message = "Check your email")
    }

    override suspend fun confirm(email: String, body: ConfirmRequest): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun searchPersonalNotes(query: String, page: Int, perPage: Int): SearchResultsDto {
        val filtered = notes.filter { it.content.contains(query, ignoreCase = true) }
        val hits = filtered.map { SearchHitDto(id = it.id, snippet = it.content.take(100), created_at = it.created_at) }
        return SearchResultsDto(results = hits, total = hits.size, query = query)
    }

    override suspend fun getPublicPosts(): List<PublicPostDto> {
        return emptyList()
    }

    override suspend fun registerFcm(body: FcmTokenDto): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun unregisterFcm(body: FcmTokenDto): GenericResponse {
        return GenericResponse(success = true)
    }

    override suspend fun activatePremium(): GenericResponse {
        return GenericResponse(success = true, message = "Premium activated")
    }

    override suspend fun getBadgeCounts(): BadgeCountsDto = BadgeCountsDto(0, 0)
    override suspend fun getNotifications(): NotificationsResponseDto = NotificationsResponseDto()
    override suspend fun markAllPostsRead(): GenericResponse = GenericResponse(success = true)
    override suspend fun markAllProposalsRead(): GenericResponse = GenericResponse(success = true)
}
