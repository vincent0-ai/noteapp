package com.example.echowithin.data.model

data class ApiError(
    val error: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String,
    val remember: Boolean = true
)

data class LoginResponse(
    val success: Boolean = false,
    val username: String? = null,
    val email: String? = null,
    val x_app_token: String? = null,
    val error: String? = null,
    val confirmed: Boolean? = true
)

data class CreateNoteRequest(
    val content: String,
    val reference: String = "",
    val tags: List<String> = emptyList()
)

data class CreateNoteResponse(
    val success: Boolean = false,
    val id: String? = null,
    val error: String? = null
)

data class NoteDto(
    val id: String,
    val content: String,
    val reference: String? = null,
    val tags: List<String> = emptyList(),
    val is_locked: Boolean = false,
    val is_pinned: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class PaginationDto(
    val page: Int,
    val per_page: Int,
    val total: Int,
    val has_more: Boolean
)

data class NotesResponse(
    val notes: List<NoteDto> = emptyList(),
    val pagination: PaginationDto? = null
)

data class GenericResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

data class AppNote(
    val id: String,
    val title: String,
    val content: String,
    val reference: String,
    val tags: List<String>,
    val updatedAt: String,
    val isLocked: Boolean = false,
    val isPinned: Boolean = false,
    val isSynced: Boolean = true,
    val pendingOp: String = "none"
)

data class ProfileResponse(
    val username: String,
    val email: String,
    val account_tier: String,
    val premium_until: String? = null,
    val has_pin: Boolean = false,
    val is_trial: Boolean = false,
    val trial_days_remaining: Int = 0
)


// Sharing DTOs
data class ShareRequestDto(
    val permissions: String = "view",
    val expires_in: String? = null,
    val access_code: String? = null,
    val surprise_theme: String = "none",
    val use_typewriter: Boolean = false,
    val auto_approve: Boolean = false
)

data class ShareResponseDto(
    val success: Boolean = false,
    val share_id: String? = null,
    val url: String? = null,
    val error: String? = null
)

data class ShareDto(
    val share_id: String,
    val permissions: String,
    val surprise_theme: String = "none",
    val use_typewriter: Boolean = false,
    val auto_approve: Boolean = false,
    val created_at: String? = null,
    val expires_at: String? = null,
    val has_password: Boolean = false
)

data class SharesListDto(
    val shares: List<ShareDto> = emptyList()
)

data class CommentDto(
    val id: String,
    val author_name: String = "Unknown",
    val author_id: String = "",
    val content: String,
    val created_at: String? = null,
    val replies: List<CommentDto> = emptyList()
)

data class CommentsListDto(
    val comments: List<CommentDto> = emptyList()
)

data class CommentRequestDto(
    val content: String
)

data class AttachmentDto(
    val id: String,
    val filename: String? = null,
    val file_url: String? = null,
    val file_type: String? = null,
    val uploaded_by: String = "Unknown",
    val uploaded_at: String? = null
)

data class AttachmentsListDto(
    val attachments: List<AttachmentDto> = emptyList()
)

// Version DTOs
data class VersionDto(
    val version_id: String,
    val content: String,
    val author_username: String = "Unknown",
    val created_at: String? = null,
    val is_proposal: Boolean = false,
    val status: String = "approved"
)

data class VersionsListDto(
    val versions: List<VersionDto> = emptyList()
)

data class ProposalDecisionDto(
    val decision: String,
    val comment: String = ""
)

data class ProposalDto(
    val version_id: String,
    val note_id: String,
    val note_preview: String = "",
    val content: String = "",
    val author_username: String = "Unknown",
    val created_at: String? = null,
    val status: String = "pending"
)

data class ProposalsListDto(
    val proposals: List<ProposalDto> = emptyList()
)

data class ToggleLockResponse(
    val success: Boolean = false,
    val is_locked: Boolean = false,
    val error: String? = null
)

// App Lock DTOs
data class AppLockSetupDto(
    val pin: String
)

data class AppLockVerifyDto(
    val pin: String
)

data class AppLockStatusDto(
    val unlocked: Boolean = false,
    val has_pin: Boolean = false,
    val remaining: Int? = null
)



data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val agree_terms: Boolean = true
)

data class RegisterResponse(
    val success: Boolean,
    val confirmed: Boolean,
    val email: String,
    val message: String,
    val error: String? = null
)

data class ConfirmRequest(
    val code: String
)

data class SearchResultsDto(
    val results: List<SearchHitDto>,
    val total: Int,
    val query: String
)

data class SearchHitDto(
    val id: String,
    val content_highlighted: String? = null,
    val snippet: String? = null,
    val created_at: String? = null
)

data class PublicPostDto(
    val _id: String,
    val title: String,
    val slug: String,
    val content: String,
    val author: String,
    val author_id: String,
    val timestamp: String,
    val url: String,
    val likes_count: Int? = 0,
    val share_count: Int? = 0,
    val view_count: Int? = 0
)

data class BadgeCountsDto(val notif_count: Int, val msg_count: Int)

data class NotificationDto(
    val _id: String,
    val title: String,
    val content: String,
    val author: String,
    val timestamp: String,
    val has_unread: Boolean,
    val activity_type: String,
    val share_id: String? = null,
    val surprise_theme: String? = null,
    val latest_comment_at: String? = null
)

data class NotificationsResponseDto(
    val posts: List<NotificationDto> = emptyList(),
    val unread_count: Int = 0,
    val last_checked: String? = null
)
