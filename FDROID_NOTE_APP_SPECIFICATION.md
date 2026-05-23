# EchoWithin Mobile Note App - F-Droid Implementation Specification

**Version:** 1.0  
**Date:** May 2026  
**Project:** EchoWithin Mobile Application (Android)  
**Target Platform:** F-Droid Inclusion Standard

---

## Executive Summary

This document outlines a complete F-Droid-compliant mobile note application that leverages the EchoWithin web platform's APIs, design system, and feature set. The app will be developed in **Kotlin** using **Jetpack Compose** and will support both connected and offline-first modes with automatic synchronization.

### Key Highlights
- **Language:** Kotlin (F-Droid preferred, fully open-source toolchain)
- **Architecture:** MVVM with Repository pattern + Clean Architecture layers
- **Target:** Android 8.0+ (API 26+) with target API 35+
- **License:** GPL-3.0 or AGPL-3.0 (F-Droid compatible)
- **No proprietary deps:** Only open-source libraries (Room, Retrofit, OkHttp, Coroutines)
- **No tracking:** Privacy-first, no analytics except opt-in telemetry
- **Offline-first:** Local SQLite sync with remote MongoDB backend

---

## Part 1: Technology Stack Justification

### Language: Kotlin
**Why Kotlin?**
- ✅ **F-Droid compliant:** Native Android language, open-source compiler
- ✅ **Modern syntax:** Null-safety, coroutines, extension functions reduce boilerplate
- ✅ **Interop:** Full Java compatibility (use existing Android/Kotlin libraries)
- ✅ **Performance:** Compiles to bytecode, same runtime as Java
- ✅ **Community:** Large Android ecosystem, well-documented
- ✅ **IDE support:** Official JetBrains IDE (Android Studio)

**Alternatives considered & rejected:**
- **Flutter:** Dart has non-open-source google closures; overkill for note app
- **React Native:** JavaScript runtime overhead; less F-Droid-friendly
- **Java:** Works but Kotlin is more modern and safer
- **Go:** Not suitable for Android UI

### Build System: Gradle
- ✅ F-Droid standard for Android
- ✅ Dependency management via Maven Central (all OSS)
- ✅ R8/ProGuard minification supported
- ✅ Deterministic builds (reproducible)

### UI Framework: Jetpack Compose
- Modern declarative UI (not XML layouts)
- Material Design 3 built-in
- Hot reload for faster development
- Better compose-to-design system translation

---

## Part 2: Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Presentation Layer (Jetpack Compose)                   │
│  - Screens (Notes List, Editor, Sharing, Premium)       │
│  - ViewModels (state + events)                           │
│  - Navigation (Compose Nav)                             │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│  Domain Layer (Pure Kotlin, no Android deps)            │
│  - Use Cases                                            │
│  - Repository Interfaces                                │
│  - Domain Models (Note, User, Premium, etc.)            │
│  - Business Rules & Validation                          │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│  Data Layer                                              │
│  ┌──────────────────┬─────────────────────────────────┐ │
│  │ Local (Room DB)  │ Remote (Retrofit + OkHttp)      │ │
│  │ - Notes          │ - REST API calls                │ │
│  │ - Drafts         │ - Authentication                │ │
│  │ - Settings       │ - Data sync                     │ │
│  │ - Offline queue  │ - Image uploads (Cloudinary)    │ │
│  └──────────────────┴─────────────────────────────────┘ │
│                                                          │
│  - Repository Implementation (Sync logic)                │
│  - Local DataSource (Room DAO)                           │
│  - Remote DataSource (API client)                        │
│  - Sync Manager (background + WorkManager)               │
└──────────────────────────────────────────────────────────┘
```

### Layer Separation Benefits
1. **Testability:** Domain layer testable without Android
2. **Reusability:** Data & domain layers can be shared with other platforms
3. **Maintainability:** Clear responsibility boundaries
4. **Offline capability:** Local layer works independently
5. **F-Droid compliance:** No vendor lock-in APIs

---

## Part 3: Detailed Feature Specification

### Core Features (MVP)

#### 3.1 Notes Management
```kotlin
// Domain Model
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val description: String?,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isPublic: Boolean = false,
    val isPinned: Boolean = false,
    val viewCount: Int = 0,
    val isSynced: Boolean = false,
    val isLocked: Boolean = false,
    val lockPassword: String? = null  // encrypted locally
)

// Local storage: Room Entity
@Entity("notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPublic: Boolean,
    val isPinned: Boolean,
    val tags: String, // JSON string
    val isSynced: Boolean = false,
    val lastSyncError: String? = null,
    val localOnly: Boolean = false // not yet synced to server
)
```

**Operations:**
- ✅ Create note (local first, sync when online)
- ✅ Read all notes with pagination
- ✅ Update note (drafts saved locally)
- ✅ Delete note (soft delete in cloud, hard delete local)
- ✅ Search notes (full-text via Room FTS or Meilisearch API)
- ✅ Sort & filter (by date, tags, status, lock status)
- ✅ Pin favorite notes
- ✅ Lock notes with password/biometric
- ✅ Export notes (PDF, JSON, Markdown)

#### 3.2 Authentication

> **IMPORTANT**: The backend uses **Flask-Login session cookies**, NOT JWT bearer
> tokens. The native app authenticates via session cookies set on login, with a
> persistent `x_app_token` cookie for session revival after cookie expiry.

```kotlin
// Login request — sent as form-encoded (not JSON)
data class LoginRequest(
    val username: String,   // accepts username OR email
    val password: String,
    val remember: Boolean = true
)

// Registration request — sent as form-encoded
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val agree_terms: Boolean = true
)

// App reauth response (JSON)
data class ReauthResponse(
    val success: Boolean,
    val username: String?
)

data class User(
    val id: String,
    val email: String,
    val username: String,
    val avatar: String?,
    val accountTier: AccountTier, // free, premium
    val joinedAt: LocalDateTime,
    val premiumUntil: LocalDateTime?
)
```

**Auth Flow (Native App):**
1. User submits credentials via `POST /login` (form-encoded, with `User-Agent: EchoWithinApp/...`)
2. Backend sets `echowithin_session` + `echowithin_remember` + `x_app_token` cookies
3. All subsequent requests include cookies automatically (OkHttp `CookieJar`)
4. On session expiry, app calls `POST /api/app_reauth` (token read from `x_app_token` cookie)
5. Backend re-establishes session and returns `{"success": true, "username": "..."}`

**Operations:**
- ✅ Email/password registration (form-encoded, email confirmation required)
- ✅ Email/password login (form-encoded, sets session cookies)
- ✅ OAuth2 (Google) via system browser → `/mobile_auth` bridge
- ✅ Session revival via `POST /api/app_reauth` (persistent `x_app_token` cookie)
- ✅ Logout (`GET /logout` — clears cookies + revokes app token)
- ✅ Password reset flow (`POST /forgot_password` + `POST /reset_password/<token>`)
- ✅ Email confirmation (`POST /confirm/<email>` with 6-digit code)

#### 3.3 Sharing & Collaboration
```kotlin
data class SharedNote(
    val id: String,
    val noteId: String,
    val shareToken: String, // unique share link token
    val createdBy: String, // user who created share
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?, // optional expiration
    val accessLevel: AccessLevel, // VIEW, EDIT, ADMIN
    val sharedWith: List<String> = emptyList(), // user emails
    val allowPublicLink: Boolean = false,
    val requirePassword: Boolean = false,
    val password: String? = null
)

enum class AccessLevel {
    VIEW,      // read-only
    EDIT,      // can edit content
    ADMIN      // can manage access & delete
}
```

**Operations:**
- ✅ Create shareable link for note
- ✅ Share with specific users
- ✅ Manage access permissions
- ✅ Receive shared notes
- ✅ Edit shared notes (if permissions allow)
- ✅ Generate PDF from shared note
- ✅ Copy shared note to own vault

#### 3.4 Premium Features
```kotlin
enum class AccountTier {
    FREE,      // Limited features
    PREMIUM    // Paid tier (monthly/yearly)
}

data class Premium(
    val tier: AccountTier,
    val paidUntil: LocalDateTime?,
    val autoRenew: Boolean,
    val features: PremiumFeatures
)

data class PremiumFeatures(
    val maxNotes: Int,            // FREE: 100, PREMIUM: unlimited
    val maxShares: Int,           // FREE: 5, PREMIUM: unlimited
    val encryption: Boolean,      // FREE: false, PREMIUM: true
    val collaborators: Int,       // FREE: 0, PREMIUM: 10
    val storageGB: Int,           // FREE: 1, PREMIUM: 100
    val aiSummary: Boolean,       // PREMIUM only
    val customTags: Boolean,      // FREE: 10, PREMIUM: unlimited
    val nightMode: Boolean,       // PREMIUM only (Pro)
    val cloudSync: Boolean,       // PREMIUM: yes
    val advancedSearch: Boolean   // PREMIUM: yes
)
```

**Premium Operations:**
- ✅ Purchase premium (in-app via Paystack API)
- ✅ Manage subscription
- ✅ Trial period handling (1 day free)
- ✅ Feature gating based on tier
- ✅ Promotional codes
- ✅ Restore purchases
- ✅ Cancel subscription

#### 3.5 Offline & Sync
```kotlin
// Sync state tracking
data class SyncRecord(
    val id: String,
    val entityType: String, // "note", "share", "user"
    val entityId: String,
    val operation: SyncOperation, // CREATE, UPDATE, DELETE
    val payload: String, // JSON
    val timestamp: Long,
    val retries: Int = 0,
    val status: SyncStatus // PENDING, SUCCESS, FAILED
)

enum class SyncOperation {
    CREATE, UPDATE, DELETE
}

enum class SyncStatus {
    PENDING, SYNCED, FAILED, CONFLICT
}
```

**Sync Strategy:**
1. **Local-first:** All operations write to Room DB first
2. **Queue:** Failed syncs queued in `sync_queue` table
3. **Background:** WorkManager triggers periodic sync (5 min intervals)
4. **Conflict resolution:** Last-write-wins with user notification
5. **Bandwidth aware:** Use WorkRequest constraints (charging, network)

#### 3.6 Notifications
```kotlin
data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val data: Map<String, String>, // link to note, user, etc.
    val createdAt: LocalDateTime,
    val isRead: Boolean = false
)

enum class NotificationType {
    NOTE_SHARED,           // Someone shared a note with you
    NOTE_COMMENTED,        // Someone commented on your note
    SHARE_PERMISSION,      // Share permission changed
    SHARE_EXPIRED,         // Share link expired
    PREMIUM_EXPIRING,      // Premium subscription expiring soon
    PREMIUM_EXPIRED        // Premium subscription expired
}
```

**Push Notification:**
- ✅ Firebase Cloud Messaging (FCM) setup (but using open-source client)
- ✅ Handle notification routing
- ✅ In-app notification center
- ✅ Notification preferences (per type)

---

## Part 4: Database Schema (Room)

### Local SQLite Schema

```sql
-- Notes
CREATE TABLE notes (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    description TEXT,
    tags TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_public INTEGER DEFAULT 0,
    is_pinned INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    is_synced INTEGER DEFAULT 0,
    is_locked INTEGER DEFAULT 0,
    lock_password TEXT,
    local_only INTEGER DEFAULT 1,
    last_sync_error TEXT,
    sync_timestamp INTEGER
);

-- Full-text search index for notes
CREATE VIRTUAL TABLE notes_fts USING fts5(
    title, content, description, tags,
    content=notes,
    content_rowid=rowid
);

-- Shared notes
CREATE TABLE shared_notes (
    id TEXT PRIMARY KEY,
    note_id TEXT NOT NULL,
    share_token TEXT UNIQUE NOT NULL,
    created_by TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER,
    access_level TEXT NOT NULL,
    allow_public_link INTEGER DEFAULT 0,
    require_password INTEGER DEFAULT 0,
    password TEXT,
    is_synced INTEGER DEFAULT 0,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

-- Shared note access
CREATE TABLE share_access (
    id TEXT PRIMARY KEY,
    share_id TEXT NOT NULL,
    user_email TEXT NOT NULL,
    access_level TEXT NOT NULL,
    added_at INTEGER NOT NULL,
    FOREIGN KEY (share_id) REFERENCES shared_notes(id) ON DELETE CASCADE
);

-- Sync queue
CREATE TABLE sync_queue (
    id TEXT PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    retries INTEGER DEFAULT 0,
    status TEXT DEFAULT 'PENDING',
    error_message TEXT
);

-- User session
CREATE TABLE user_session (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    user_id TEXT UNIQUE NOT NULL,
    email TEXT NOT NULL,
    token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    token_expiry INTEGER NOT NULL,
    account_tier TEXT,
    premium_until INTEGER,
    last_synced INTEGER
);

-- App settings & preferences
CREATE TABLE app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

-- Notifications
CREATE TABLE notifications (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    data TEXT,
    created_at INTEGER NOT NULL,
    is_read INTEGER DEFAULT 0
);
```

---

## Part 5: API Integration Mapping

> **IMPORTANT**: The backend is a **server-rendered Flask app** that uses session
> cookies for auth. Most routes return HTML. Only endpoints marked with ✅JSON
> below return JSON responses. The mobile app **must** use a `CookieJar` to persist
> session cookies across requests.

### Backend URLs
```kotlin
const val BASE_URL = "https://echowithin.xyz"

// ─── Authentication (form-encoded, sets session cookies) ───
POST   /register                           → CreateAccountUseCase (form-encoded, returns redirect)
POST   /confirm/<email>                    → ConfirmEmailUseCase (form: code, returns redirect)
POST   /login                              → LoginUseCase (form-encoded, sets cookies, returns redirect)
GET    /google_login                       → GoogleOAuthUseCase (opens system browser)
GET    /mobile_auth?token=<otlt>           → MobileAuthBridgeUseCase (one-time link token)
POST   /api/app_reauth                     → SessionRevivalUseCase ✅JSON (reads x_app_token cookie)
GET    /logout                             → LogoutUseCase (clears cookies + revokes app token)
POST   /forgot_password                    → ForgotPasswordUseCase (form-encoded)
POST   /reset_password/<token>             → ResetPasswordUseCase (form-encoded)

// ─── Notes CRUD (all use POST, return JSON) ───
GET    /personal_space                     → GetNotesPageUseCase (returns HTML — parse or use JS API)
POST   /personal_post/create               → CreateNoteUseCase (form-encoded, returns redirect)
POST   /personal_post/create_json          → CreateNoteUseCase ✅JSON (JSON body)
POST   /personal_post/edit/<id>            → UpdateNoteUseCase ✅JSON (JSON body, returns JSON)
POST   /personal_post/delete/<id>          → DeleteNoteUseCase ✅JSON (JSON body: {mode: "me"})
POST   /personal_post/sync/<id>            → SyncNoteUseCase ✅JSON (bidirectional sync)
GET    /personal_post/search?q=<query>     → SearchNotesUseCase ✅JSON
POST   /personal_post/reindex_notes        → ReindexNotesUseCase ✅JSON

// ─── Note Sharing ───
POST   /personal_post/share/<id>           → CreateShareUseCase ✅JSON (JSON or multipart)
GET    /share/note/<share_id>              → GetSharedNoteUseCase (returns HTML page)
POST   /share/note/<share_id>/edit         → EditSharedNoteUseCase ✅JSON
POST   /personal_post/revoke_share/<id>    → RevokeShareUseCase ✅JSON
GET    /personal_post/shares/<post_id>     → GetShareLinksUseCase ✅JSON
GET    /api/share/<share_id>/history       → GetShareHistoryUseCase ✅JSON
POST   /api/share/<share_id>/ping          → PingCollaboratorsUseCase ✅JSON

// ─── Share Comments ───
GET    /share/note/<share_id>/comments     → GetShareCommentsUseCase ✅JSON
POST   /share/note/<share_id>/comments     → CreateShareCommentUseCase ✅JSON
POST   /share/note/<id>/comments/<cid>/replies → ReplyToCommentUseCase ✅JSON
DELETE /share/note/<id>/comments/<cid>     → DeleteCommentUseCase ✅JSON

// ─── Share Attachments ───
POST   /share/note/<share_id>/upload       → UploadAttachmentUseCase ✅JSON (multipart)
GET    /share/note/<share_id>/attachments  → GetAttachmentsUseCase ✅JSON
DELETE /share/note/<id>/attachment/<aid>   → DeleteAttachmentUseCase ✅JSON

// ─── Note Versions ───
GET    /personal_post/versions/<post_id>   → GetVersionsUseCase ✅JSON
POST   /personal_post/version/restore/<post_id>/<version_id> → RestoreVersionUseCase ✅JSON
POST   /personal_post/proposal/<version_id>/decision         → DecideProposalUseCase ✅JSON

// ─── Saved Notes (from shares) ───
POST   /shared_note/save/<share_id>        → SaveSharedNoteUseCase ✅JSON
GET    /saved_note/view/<note_id>          → ViewSavedNoteUseCase (returns HTML)

// ─── App Lock ───
POST   /api/app_lock/setup                 → SetupLockUseCase ✅JSON (JSON: {pin})
POST   /api/app_lock/verify                → VerifyLockUseCase ✅JSON (JSON: {pin})
POST   /api/app_lock/remove                → RemoveLockUseCase ✅JSON
POST   /api/app_lock/relock                → RelockUseCase ✅JSON
GET    /api/app_lock/check_status           → CheckLockStatusUseCase ✅JSON
POST   /personal_post/toggle_lock/<id>     → ToggleNoteLockUseCase ✅JSON (Premium only)

// ─── Premium ───
POST   /api/paystack/initialize             → InitiatePremiumPurchaseUseCase ✅JSON
POST   /api/paystack/webhook                → HandlePremiumWebhookUseCase (server-to-server)
GET    /paystack/callback                   → PremiumCallbackUseCase (redirect)

// ─── User Profile ───
GET    /profile/<username>                  → GetUserProfileUseCase (returns HTML)
POST   /profile/<username>/settings         → UpdateProfileUseCase (form-encoded)
POST   /profile/<username>/export_data      → ExportDataUseCase ✅JSON
POST   /profile/<username>/delete_account   → DeleteAccountUseCase (form-encoded)

// ─── Push Notifications ───
POST   /api/fcm/register                    → RegisterFCMTokenUseCase ✅JSON
POST   /api/fcm/unregister                  → UnregisterFCMTokenUseCase ✅JSON
GET    /api/push/vapid-public-key           → GetVapidKeyUseCase ✅JSON
GET    /api/notifications/unread-count      → GetUnreadCountUseCase ✅JSON
GET    /api/notifications/badge-counts      → GetBadgeCountsUseCase ✅JSON

// ─── AI Features ───
POST   /api/ai/suggest-tags                 → SuggestTagsUseCase ✅JSON

// ─── Communities (Future Phase) ───
GET    /communities                         → GetCommunitiesUseCase (returns HTML)
GET    /community/<id>                      → GetCommunityUseCase (returns HTML)
POST   /api/community/create                → CreateCommunityUseCase ✅JSON
POST   /api/community/join                  → JoinCommunityUseCase ✅JSON
POST   /api/community/<id>/join-public      → JoinPublicCommunityUseCase ✅JSON
POST   /api/community/<id>/leave            → LeaveCommunityUseCase ✅JSON
POST   /api/community/<id>/note/create      → CreateCommunityNoteUseCase ✅JSON
POST   /api/community/note/<nid>/react      → ReactToCommunityNoteUseCase ✅JSON
POST   /api/community/note/<nid>/save       → SaveCommunityNoteUseCase ✅JSON
POST   /api/community/note/<nid>/delete     → DeleteCommunityNoteUseCase ✅JSON

// ─── Direct Messaging (Future Phase) ───
GET    /messages                            → GetMessagesPageUseCase (returns HTML)
GET    /api/messages/history/<user_id>      → GetChatHistoryUseCase ✅JSON
POST   /api/messages/upload_image           → UploadChatImageUseCase ✅JSON
POST   /api/messages/upload_voice           → UploadVoiceNoteUseCase ✅JSON
POST   /api/messages/react/<msg_id>         → ReactToMessageUseCase ✅JSON
POST   /api/messages/edit/<msg_id>          → EditMessageUseCase ✅JSON
POST   /api/messages/delete/<msg_id>        → DeleteMessageUseCase ✅JSON
GET    /api/messages/unread_count           → GetUnreadDMCountUseCase ✅JSON
```

### API Client Configuration
```kotlin
// Cookie-based session management (NOT JWT bearer tokens)
val cookieJar = PersistentCookieJar(
    SetCookieCache(),
    SharedPrefsCookiePersistor(context)
)

val okHttpClient = OkHttpClient.Builder()
    .cookieJar(cookieJar)   // Persists session cookies across requests
    .addInterceptor(UserAgentInterceptor())  // Adds "EchoWithinApp/1.0" User-Agent
    .addInterceptor(ErrorHandlingInterceptor())
    .addInterceptor(SessionExpiryInterceptor(reauthManager)) // Auto-reauth on 401
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

// Retrofit for REST API
// Note: No CoroutineCallAdapterFactory needed — Retrofit natively supports
// suspend functions since version 2.6+
val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

// User-Agent interceptor (triggers native app token generation on login)
class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "EchoWithinApp/1.0 Android")
            .build()
        return chain.proceed(request)
    }
}

// Auto-reauth interceptor: catches 401 responses and re-authenticates
class SessionExpiryInterceptor(
    private val reauthManager: ReauthManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            // Try to revive session using persistent app token
            val reauthSuccess = reauthManager.tryReauth()
            if (reauthSuccess) {
                // Retry original request with fresh session
                response.close()
                return chain.proceed(chain.request())
            }
        }
        return response
    }
}

// Service definition
interface EchowithinApiService {
    // --- Auth (form-encoded) ---
    @FormUrlEncoded
    @POST("register")
    suspend fun register(
        @Field("username") username: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("agree_terms") agreeTerms: String = "on"
    ): Response<ResponseBody>  // Returns redirect (check Location header)

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("remember") remember: String = "on"
    ): Response<ResponseBody>  // Sets session cookies via CookieJar

    @POST("api/app_reauth")
    suspend fun reauthenticate(): ReauthResponse  // Uses x_app_token cookie

    // --- Notes (JSON) ---
    @POST("personal_post/create_json")
    suspend fun createNote(@Body request: CreateNoteRequest): CreateNoteResponse

    @POST("personal_post/edit/{id}")
    suspend fun updateNote(
        @Path("id") noteId: String,
        @Body request: UpdateNoteRequest
    ): UpdateNoteResponse

    @POST("personal_post/delete/{id}")
    suspend fun deleteNote(
        @Path("id") noteId: String,
        @Body request: DeleteNoteRequest
    ): GenericResponse

    @GET("personal_post/search")
    suspend fun searchNotes(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): SearchNotesResponse

    // --- Sharing ---
    @POST("personal_post/share/{id}")
    suspend fun createShareLink(
        @Path("id") noteId: String,
        @Body request: CreateShareRequest
    ): CreateShareResponse

    // --- Lock ---
    @POST("api/app_lock/setup")
    suspend fun setupLock(@Body request: SetupLockRequest): GenericResponse

    @POST("api/app_lock/verify")
    suspend fun verifyLock(@Body request: VerifyLockRequest): GenericResponse

    @GET("api/app_lock/check_status")
    suspend fun checkLockStatus(): LockStatusResponse

    // --- AI ---
    @POST("api/ai/suggest-tags")
    suspend fun suggestTags(@Body request: SuggestTagsRequest): SuggestTagsResponse

    // ... more endpoints
}
```

---

## Part 6: UI/UX Design System

### Color Palette (Material Design 3)
```kotlin
// Matches EchoWithin web design
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),        // Indigo
    onPrimary = Color.White,
    secondary = Color(0xFF8B5CF6),      // Violet
    onSecondary = Color.White,
    tertiary = Color(0xFF10B981),       // Emerald
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1F2937),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    error = Color(0xFFDC2626)           // Red
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),        // Light Indigo
    onPrimary = Color(0xFF1F2937),
    secondary = Color(0xFFA78BFA),      // Light Violet
    onSecondary = Color(0xFF1F2937),
    tertiary = Color(0xFF34D399),       // Light Emerald
    onTertiary = Color(0xFF1F2937),
    background = Color(0xFF111827),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFF3F4F6),
    error = Color(0xFFFCA5A5)
)
```

### Screen Layout Structure
```
┌─ Navigation Rail / Bottom Bar (premium feature: theme toggle)
│
├─ NOTES LIST SCREEN
│  ├─ AppBar: Search + Menu
│  ├─ FAB: New Note
│  ├─ Note Cards (lazy column)
│  │  ├─ Title
│  │  ├─ Preview (first 100 chars)
│  │  ├─ Tags
│  │  ├─ Timestamp
│  │  └─ Actions: Share, Lock, Pin, More
│  └─ Empty state + offline indicator
│
├─ NOTE EDITOR SCREEN
│  ├─ AppBar: Title + Back + More
│  ├─ Title field
│  ├─ Tags input
│  ├─ Content editor (RichText via Markdown)
│  ├─ Attachments list
│  ├─ Sharing button
│  └─ Save (auto-save + manual)
│
├─ SHARE SCREEN
│  ├─ Copy link button
│  ├─ Share method selector
│  ├─ Access level picker
│  ├─ Expiration selector
│  ├─ Password protection toggle
│  └─ Shared with list
│
├─ LOCK SCREEN
│  ├─ Biometric/Pattern/PIN setup
│  ├─ Re-authentication
│  └─ Auto-lock timeout setting
│
├─ PREMIUM SCREEN
│  ├─ Feature comparison table
│  ├─ Pricing info
│  ├─ Purchase button (via Paystack)
│  ├─ Subscription status
│  └─ Manage subscription
│
└─ SETTINGS SCREEN
   ├─ Account settings
   ├─ Sync settings
   ├─ Notification preferences
   ├─ Theme/Language
   ├─ Storage usage
   ├─ Data export
   └─ About/Help
```

### Key UI Components (Compose)
```kotlin
// Custom note card
@Composable
fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Reusable across all note listings
}

// Rich text editor
@Composable
fun NoteEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Markdown-based editor with formatting toolbar
}

// Sync status indicator
@Composable
fun SyncStatusBadge(status: SyncStatus) {
    // Shows in real-time as notes sync
}

// Premium feature gate
@Composable
fun PremiumFeatureGate(
    isPremium: Boolean,
    featureName: String,
    content: @Composable () -> Unit
) {
    if (isPremium) {
        content()
    } else {
        UpgradeBanner(featureName)
    }
}
```

---

## Part 7: State Management

### ViewModel Architecture
```kotlin
// Use Cases (Domain logic)
class GetNotesUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Note>> {
        return repository.getNotes(limit, offset)
    }
}

// ViewModel (Composition with ViewModelFactory or Hilt)
@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val getNotes: GetNotesUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val searchNotes: SearchNotesUseCase,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Synced)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = getNotes()) {
                is Result.Success -> {
                    _uiState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = UiState.Error(result.exception.message)
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            when (val result = deleteNote(noteId)) {
                is Result.Success -> {
                    loadNotes() // refresh
                }
                is Result.Error -> {
                    _uiState.value = UiState.Error(result.exception.message)
                }
            }
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = searchNotes(query)) {
                is Result.Success -> {
                    _uiState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = UiState.Error(result.exception.message)
                }
            }
        }
    }
}

sealed class UiState {
    data object Loading : UiState()
    data class Success(val notes: List<Note>) : UiState()
    data class Error(val message: String?) : UiState()
}
```

### Dependency Injection with Hilt
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Singleton
    @Provides
    fun provideNotesDatabase(context: Context): NotesDatabase {
        return Room.databaseBuilder(
            context,
            NotesDatabase::class.java,
            "echowithin_notes.db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideNotesDao(db: NotesDatabase): NotesDao {
        return db.notesDao()
    }

    @Singleton
    @Provides
    fun provideApiService(): EchowithinApiService {
        return EchowithinApi.create()
    }

    @Singleton
    @Provides
    fun provideNoteRepository(
        local: NotesDao,
        remote: EchowithinApiService,
        tokenManager: TokenManager
    ): NoteRepository {
        return NoteRepositoryImpl(local, remote, tokenManager)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetNotesUseCase(
        repository: NoteRepository
    ): GetNotesUseCase {
        return GetNotesUseCase(repository)
    }

    // ... more use cases
}
```

---

## Part 8: Security Implementation

### 1. Session & App Token Management

> **NOTE**: The backend uses Flask-Login session cookies, NOT JWT bearer tokens.
> The `x_app_token` cookie is a persistent token for session revival.

```kotlin
/**
 * Manages session state for the native app.
 * Authentication is cookie-based via OkHttp PersistentCookieJar.
 * The x_app_token cookie enables session revival without re-login.
 */
class SessionManager @Inject constructor(
    private val apiService: EchowithinApiService,
    private val cookieJar: PersistentCookieJar
) {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    /**
     * Attempt to revive session using the persistent x_app_token cookie.
     * Called on app launch and when receiving 401 responses.
     */
    suspend fun tryReauth(): Boolean {
        return try {
            val response = apiService.reauthenticate()
            if (response.success) {
                _isLoggedIn.value = true
                _username.value = response.username
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun clearSession() {
        cookieJar.clear()
        _isLoggedIn.value = false
        _username.value = null
    }
}
```
```

### 2. Password Encryption (for note locks)
```kotlin
class PasswordEncryption {

    fun encryptPassword(password: String, salt: ByteArray): String {
        val key = deriveKey(password.toByteArray(), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, 0, key.size, "AES"))
        val ciphertext = cipher.doFinal(password.toByteArray())
        return Base64.getEncoder().encodeToString(ciphertext)
    }

    fun verifyPassword(input: String, stored: String, salt: ByteArray): Boolean {
        val key = deriveKey(input.toByteArray(), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, 0, key.size, "AES"))
        val plaintext = cipher.doFinal(Base64.getDecoder().decode(stored))
        return plaintext.decodeToString() == input
    }

    private fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            password.map { it.toInt().toChar() }.toCharArray(),
            salt,
            10000, // iterations
            256    // key length
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
```

### 3. Biometric Authentication
```kotlin
class BiometricAuthManager(private val context: Context) {

    private val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                // Unlock notes
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                // Handle error
            }
        }
    )

    fun setupBiometric() {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock EchoWithin")
            .setNegativeButtonText("Cancel")
            .build()
        biometricPrompt.authenticate(info)
    }
}
```

### 4. Data Security (Encryption at Rest)
```kotlin
// Use EncryptedSharedPreferences for sensitive data
// Use DataStore with Tink encryption

class EncryptedDataStore(private val context: Context) {

    private val preferencesDataStore = context.createDataStore(
        fileName = "encrypted_settings",
        serializer = SettingsSerializer,
        produceMigrations = { listOf() }
    )

    fun saveEncryptedNote(note: Note): Flow<Boolean> = flow {
        try {
            preferencesDataStore.updateData { settings ->
                settings.copy(
                    notes = settings.notes + EncryptedNoteProto.newBuilder()
                        .setId(note.id)
                        .setContent(encryptContent(note.content))
                        .build()
                )
            }
            emit(true)
        } catch (e: Exception) {
            emit(false)
        }
    }

    private fun encryptContent(content: String): String {
        // Use Tink encryption
        return content // encrypted
    }
}
```

### 5. HTTPS & Certificate Pinning
```kotlin
// OkHttp with certificate pinning
val certificatePinner = CertificatePinner.Builder()
    .add("echowithin.xyz", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

---

## Part 9: Offline-First Sync Strategy

### Sync Architecture
```
┌─────────────────────┐
│  Local Changes      │
│  (Room Database)    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Sync Queue         │
│  (Track operations) │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Connectivity Check │
│  (Network state)    │
└──────────┬──────────┘
           │
           ├─ No Connection → Enqueue
           │
           └─ Connected ──────────┐
                                  ▼
                        ┌─────────────────────┐
                        │  Batch API Calls    │
                        │  (Retrofit)         │
                        └──────────┬──────────┘
                                   │
                                   ├─ Success → Mark synced
                                   │
                                   └─ Failure → Retry with backoff
```

### Sync Manager Implementation
```kotlin
@Singleton
class SyncManager @Inject constructor(
    private val apiService: EchowithinApiService,
    private val notesDao: NotesDao,
    private val syncQueueDao: SyncQueueDao,
    private val scope: CoroutineScope  // Application-scoped, NOT viewModelScope
) {

    private val gson = Gson()

    suspend fun syncChanges() {
        // Get all pending syncs
        val pendingOps = syncQueueDao.getPendingOperations()

        for (op in pendingOps) {
            try {
                when (op.operation) {
                    SyncOperation.CREATE -> {
                        val note = gson.fromJson(op.payload, Note::class.java)
                        apiService.createNote(CreateNoteRequest(
                            content = note.content,
                            reference = note.description ?: "",
                            tags = note.tags.joinToString(",")
                        ))
                    }
                    SyncOperation.UPDATE -> {
                        val note = gson.fromJson(op.payload, Note::class.java)
                        apiService.updateNote(note.id, UpdateNoteRequest(
                            content = note.content,
                            reference = note.description ?: "",
                            tags = note.tags.joinToString(",")
                        ))
                    }
                    SyncOperation.DELETE -> {
                        val payload = gson.fromJson(op.payload, Map::class.java)
                        val noteId = payload["id"] as String
                        apiService.deleteNote(noteId, DeleteNoteRequest(mode = "me"))
                    }
                }

                // Mark as synced
                syncQueueDao.markSynced(op.id)
                notesDao.updateSyncStatus(op.entityId, isSynced = true)

            } catch (e: Exception) {
                // Increment retries, schedule for later
                syncQueueDao.incrementRetries(op.id)
                if (op.retries > 5) {
                    syncQueueDao.markFailed(op.id, e.message)
                }
            }
        }
    }

    fun enqueueSyncOperation(
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        payload: String
    ) {
        scope.launch {
            val record = SyncRecord(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                timestamp = System.currentTimeMillis(),
                status = SyncStatus.PENDING
            )
            syncQueueDao.insert(record)
        }
    }
}

// WorkManager for background sync
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncManager.syncChanges()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "note_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
        }
    }
}
```

---

## Part 10: F-Droid Compliance Checklist

### ✅ License & Source Code
- [ ] **License**: GPL-3.0 or AGPL-3.0 (fully compatible)
- [ ] **Source Code**: Public GitHub repository (public access)
- [ ] **Build reproducibility**: gradle/wrapper/gradle-wrapper.properties pinned to exact version
- [ ] **No proprietary jars**: Only Maven Central dependencies

### ✅ Dependencies (All Open Source)
```gradle
// Core Android
implementation 'androidx.core:core:1.10.1'
implementation 'androidx.appcompat:appcompat:1.6.1'

// Jetpack Compose
implementation 'androidx.compose.ui:ui:1.5.1'
implementation 'androidx.compose.material3:material3:1.1.1'

// Room database
implementation 'androidx.room:room-runtime:2.5.2'
kapt 'androidx.room:room-compiler:2.5.2'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.11.0'

// JSON
implementation 'com.google.code.gson:gson:2.10.1'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'

// Dependency injection
implementation 'com.google.dagger:hilt-android:2.46'
kapt 'com.google.dagger:hilt-compiler:2.46'

// Security
implementation 'androidx.security:security-crypto:1.1.0-alpha06'

// Work scheduling
implementation 'androidx.work:work-runtime-ktx:2.8.1'

// Testing (no Firebase/Analytics)
testImplementation 'junit:junit:4.13.2'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
```

### ✅ No Proprietary Features
- [ ] **No Google Play Services**: Use alternatives (Retrofit for HTTP, WorkManager for scheduling)
- [ ] **No Firebase Analytics**: Manual opt-in telemetry only (via custom server endpoint)
- [ ] **No proprietary UI libraries**: Use Jetpack Compose
- [ ] **No vendor lock-in**: APIs work with custom servers too

### ✅ Privacy & Security
- [ ] **No tracking**: No analytics library bundled
- [ ] **User consent**: Explicit opt-in for any data collection
- [ ] **Data encryption**: End-to-end encryption for sensitive notes
- [ ] **Minimal permissions**: Only request needed permissions
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <uses-permission android:name="android.permission.USE_BIOMETRIC" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" /> <!-- Android 12+ -->
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
  ```

### ✅ AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="xyz.echowithin.app">

    <!-- Required permissions only -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Optional - request via runtime -->
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <!-- No analytics/tracking permissions -->

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.EchoWithin">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Deep linking support for shared notes -->
        <activity
            android:name=".ui.SharedNoteActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="https"
                    android:host="echowithin.xyz"
                    android:pathPrefix="/share/note/" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## Part 11: Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
- [ ] Project setup (Gradle, dependencies, package structure)
- [ ] Database schema & Room DAOs
- [ ] Authentication API integration
- [ ] Local token storage with encryption
- [ ] Basic navigation structure

### Phase 2: Core Features (Weeks 3-5)
- [ ] Note CRUD operations (UI + API integration)
- [ ] Notes list screen with pagination
- [ ] Note editor screen
- [ ] Offline-first sync with WorkManager
- [ ] Search functionality (local FTS + API)

### Phase 3: Sharing & Collaboration (Week 6)
- [ ] Share note functionality
- [ ] Access level management
- [ ] Shared note viewing
- [ ] Password-protected shares

### Phase 4: Security & Premium (Week 7)
- [ ] App lock (biometric + PIN)
- [ ] Note encryption
- [ ] Premium tier gating
- [ ] Paystack payment integration

### Phase 5: Polish & Optimization (Week 8)
- [ ] UI/UX refinement
- [ ] Performance optimization
- [ ] Comprehensive testing
- [ ] F-Droid submission prep

### Phase 6: Post-Launch (Ongoing)
- [ ] Community responses & bug fixes
- [ ] Advanced features (AI summary, etc.)
- [ ] Internationalization (i18n)
- [ ] Accessibility improvements

---

## Part 12: Testing Strategy

### Unit Tests
```kotlin
// Domain layer tests (no Android dependencies)
class GetNotesUseCaseTest {
    
    private val mockRepository = mockk<NoteRepository>()
    private val getNotesUseCase = GetNotesUseCase(mockRepository)

    @Test
    fun `getNotes returns list of notes`() = runTest {
        // Given
        val notes = listOf(
            Note(id = "1", title = "Test"),
            Note(id = "2", title = "Test 2")
        )
        coEvery { mockRepository.getNotes() } returns Result.Success(notes)

        // When
        val result = getNotesUseCase()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.size)
    }
}
```

### Integration Tests
```kotlin
// Room database tests
@RunWith(AndroidTestRunner::class)
@SmallTest
class NotesDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: NotesDatabase
    private lateinit var notesDao: NotesDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            NotesDatabase::class.java
        ).build()
        notesDao = db.notesDao()
    }

    @Test
    fun insertAndRetrieveNote() = runTest {
        // Given
        val note = NoteEntity(
            id = "1",
            title = "Test Note",
            content = "Content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // When
        notesDao.insert(note)
        val retrieved = notesDao.getNoteById("1")

        // Then
        assertNotNull(retrieved)
        assertEquals("Test Note", retrieved?.title)
    }
}
```

### UI Tests
```kotlin
// Compose UI tests
@RunWith(AndroidTestRunner::class)
@MediumTest
class NotesListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysNotesInList() {
        val notes = listOf(
            Note(id = "1", title = "Note 1"),
            Note(id = "2", title = "Note 2")
        )

        composeRule.setContent {
            NotesListScreen(notes = notes)
        }

        composeRule.onNodeWithText("Note 1").assertIsDisplayed()
        composeRule.onNodeWithText("Note 2").assertIsDisplayed()
    }

    @Test
    fun openNoteEditorOnCardClick() {
        composeRule.setContent {
            NotesListScreen(
                notes = listOf(Note(id = "1", title = "Test")),
                onNoteClick = { assertThat(it).isEqualTo("1") }
            )
        }

        composeRule.onNodeWithText("Test").performClick()
    }
}
```

---

## Part 13: Deployment & Distribution

### Build Configuration
```gradle
android {
    compileSdk 35
    minSdk 26
    targetSdk 35

    defaultConfig {
        applicationId = "xyz.echowithin.app"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.release
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
```

### F-Droid Metadata (metadata/xyz.echowithin.app.yml)
```yaml
Categories:
  - Writing
License: AGPL-3.0-only
AuthorName: EchoWithin Team
AuthorEmail: dev@echowithin.xyz
WebSite: https://echowithin.xyz

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    gradle:
      - yes
    srclibs:
      - kotlin-stdlib@1.9.0
    prebuild:
      - sed -i 's/@android:compileSdkVersion@/35/g' build.gradle.kts
```

### GitHub Actions CI/CD
```yaml
name: Build & Test
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: 11
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
      - name: Run instrumented tests
        run: ./gradlew connectedAndroidTest
      - name: Generate APK
        run: ./gradlew assembleRelease
      - name: Upload to F-Droid Beta
        if: github.ref == 'refs/heads/main'
        run: |
          # Deploy to F-Droid repo
```

---

## Part 14: Security & Privacy Policy

### Privacy Principles
1. **Data Minimization:** Only collect essential user data
2. **User Control:** Users can export/delete all their data anytime
3. **Encryption:** Sensitive data encrypted at rest & in transit
4. **No 3rd party tracking:** No analytics, no ads, no data brokers
5. **Transparent:** Privacy policy available in-app

### Compliance
- GDPR: Right to access, delete, export data
- CCPA: User data rights explicitly honored
- Open Source: Code auditability ensures no hidden tracking

### Data Flow
```
User Device (App)
  ↓ [HTTPS + TLS 1.3]
API Server (Flask)
  ↓
Database (MongoDB)
  ↓
Backup (S3 - encrypted)
```

---

## Part 15: Support & Documentation

### In-App Help
- Tooltips on complex features
- "?" icons linking to help docs
- FAQ screen (synced from API)
- Contextual support overlays

### External Documentation
- User guide (Markdown → PDF export)
- API documentation (Swagger/OpenAPI)
- Developer guide (contributing)
- Security audit reports

### Community Support
- GitHub Issues for bugs
- Discussions for feature requests
- Matrix/IRC chat for real-time help
- Email support: support@echowithin.xyz

---

## Part 16: Version Control & Branching Strategy

```
main (stable releases)
  ↓
develop (integration branch)
  ↓
feature/description (feature branches)
  ↓
bugfix/description (bug branches)
```

### Release Process
1. Create release branch: `release/v1.1.0`
2. Update version numbers & changelog
3. Create GitHub release with notes
4. Tag commit: `v1.1.0`
5. Build signed APK
6. Submit to F-Droid
7. Merge back to main & develop

---

## Appendix A: Project Structure

```
echowithin-mobile/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/xyz/echowithin/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── di/                    # Dependency injection
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── DataModule.kt
│   │   │   │   │   └── UseCaseModule.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/             # Room
│   │   │   │   │   │   ├── NotesDatabase.kt
│   │   │   │   │   │   ├── NotesDao.kt
│   │   │   │   │   │   └── entities/
│   │   │   │   │   ├── remote/            # Retrofit
│   │   │   │   │   │   ├── EchowithinApiService.kt
│   │   │   │   │   │   └── dto/
│   │   │   │   │   └── repository/        # Implementation
│   │   │   │   │       └── NoteRepositoryImpl.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/             # Domain models
│   │   │   │   │   │   ├── Note.kt
│   │   │   │   │   │   ├── User.kt
│   │   │   │   │   │   └── SharedNote.kt
│   │   │   │   │   ├── repository/        # Interfaces
│   │   │   │   │   │   └── NoteRepository.kt
│   │   │   │   │   └── usecase/           # Use cases
│   │   │   │   │       ├── GetNotesUseCase.kt
│   │   │   │   │       ├── CreateNoteUseCase.kt
│   │   │   │   │       └── ...
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── NotesListScreen.kt
│   │   │   │   │   │   ├── NoteEditorScreen.kt
│   │   │   │   │   │   ├── ShareScreen.kt
│   │   │   │   │   │   ├── PremiumScreen.kt
│   │   │   │   │   │   └── ...
│   │   │   │   │   ├── components/        # Reusable Compose components
│   │   │   │   │   │   ├── NoteCard.kt
│   │   │   │   │   │   ├── NoteEditor.kt
│   │   │   │   │   │   ├── SyncStatusBadge.kt
│   │   │   │   │   │   └── ...
│   │   │   │   │   ├── viewmodels/
│   │   │   │   │   │   ├── NotesListViewModel.kt
│   │   │   │   │   │   ├── NoteEditorViewModel.kt
│   │   │   │   │   │   └── ...
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── NavGraph.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Type.kt
│   │   │   │   │       └── Theme.kt
│   │   │   │   └── util/
│   │   │   │       ├── security/
│   │   │   │       │   ├── TokenManager.kt
│   │   │   │       │   ├── PasswordEncryption.kt
│   │   │   │       │   └── BiometricAuthManager.kt
│   │   │   │       ├── sync/
│   │   │   │       │   ├── SyncManager.kt
│   │   │   │       │   └── SyncWorker.kt
│   │   │   │       ├── network/
│   │   │   │       │   ├── AuthInterceptor.kt
│   │   │   │       │   ├── ErrorHandlingInterceptor.kt
│   │   │   │       │   └── EchowithinApi.kt
│   │   │   │       ├── Constants.kt
│   │   │   │       └── Extensions.kt
│   │   │   └── res/
│   │   │       ├── values/
│   │   │       │   ├── strings.xml
│   │   │       │   ├── colors.xml
│   │   │       │   ├── dimens.xml
│   │   │       │   └── themes.xml
│   │   │       ├── drawable/
│   │   │       │   └── ic_launcher_foreground.xml
│   │   │       └── mipmap/
│   │   │           └── ic_launcher.png
│   │   ├── test/
│   │   │   └── xyz/echowithin/
│   │   │       ├── domain/
│   │   │       │   └── usecase/
│   │   │       │       └── GetNotesUseCaseTest.kt
│   │   │       └── ...
│   │   └── androidTest/
│   │       └── xyz/echowithin/
│   │           ├── data/
│   │           │   └── NotesDaoTest.kt
│   │           └── presentation/
│   │               └── screens/
│   │                   └── NotesListScreenTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── .gitignore
├── .github/
│   └── workflows/
│       ├── build.yml
│       └── release.yml
├── README.md
├── CONTRIBUTING.md
├── PRIVACY.md
├── LICENSE (AGPL-3.0)
└── CHANGELOG.md
```

---

## Appendix B: API Response Examples

```json
{
  "notes": [
    {
      "id": "uuid-1",
      "title": "My First Note",
      "content": "# Markdown Content\n\nRich text support with **bold** and *italic*",
      "description": "A brief summary",
      "tags": ["productivity", "personal"],
      "createdAt": "2026-05-19T10:30:00Z",
      "updatedAt": "2026-05-19T15:45:00Z",
      "isPublic": false,
      "isPinned": true,
      "viewCount": 5,
      "isSynced": true,
      "isLocked": false
    }
  ],
  "pagination": {
    "limit": 20,
    "offset": 0,
    "total": 150,
    "hasMore": true
  }
}
```

---

## Appendix C: Summary of F-Droid Compliance

| Requirement | Status | Notes |
|---|---|---|
| Open Source License | ✅ AGPL-3.0 | Free software, source code auditable |
| No proprietary dependencies | ✅ All Maven Central | Zero Google Play Services |
| No analytics/tracking | ✅ Optional telemetry | User consent-based only |
| Reproducible builds | ✅ Gradle pinned | Same APK every build |
| Minimum API level | ✅ 26 | Android 8.0+ |
| No anti-features | ✅ None | No ads, no tracking, no DRM |
| Documentation | ✅ Comprehensive | In-app help + online docs |
| Privacy policy | ✅ Included | GDPR/CCPA compliant |
| Source code available | ✅ GitHub public | vincent0-ai/echowithin-mobile |

---

## Conclusion

This specification provides a complete roadmap for building an F-Droid-compliant, feature-rich mobile note application leveraging the EchoWithin platform. The Kotlin + Jetpack Compose stack ensures modern, maintainable code while the offline-first architecture guarantees reliability regardless of network conditions.

**Key deliverables:**
1. ✅ Fully specified Android application (Kotlin)
2. ✅ F-Droid compliance guarantees
3. ✅ Complete API integration mapping
4. ✅ Security & privacy implementation details
5. ✅ Testing & deployment strategy
6. ✅ 8-week development roadmap

The application maintains design/UX parity with the web platform while optimizing for mobile interaction patterns. Premium features are properly gated, and all user data remains under user control with transparent encryption and optional export capabilities.

---

**Document Version**: 1.0  
**Last Updated**: May 19, 2026  
**Status**: Ready for Agent Implementation
