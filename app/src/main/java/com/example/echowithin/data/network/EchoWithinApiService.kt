package com.example.echowithin.data.network

import com.example.echowithin.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface EchoWithinApiService {
    @POST("api/v1/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/v1/logout")
    suspend fun logout(): GenericResponse

    @POST("api/v1/app_reauth")
    suspend fun appReauth(): LoginResponse

    @GET("api/v1/notes")
    suspend fun getNotes(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): NotesResponse

    @GET("api/v1/notes/{noteId}")
    suspend fun getNoteById(@Path("noteId") noteId: String): NoteDto

    @POST("api/v1/notes/create")
    suspend fun createNote(@Body body: CreateNoteRequest): CreateNoteResponse

    @POST("api/v1/notes/edit/{noteId}")
    suspend fun editNote(
        @Path("noteId") noteId: String,
        @Body body: CreateNoteRequest
    ): CreateNoteResponse

    @POST("api/v1/notes/{noteId}/sync")
    suspend fun syncNote(@Path("noteId") noteId: String): SyncNoteResponse

    @GET("api/v1/profile")
    suspend fun getProfile(): ProfileResponse

    @POST("api/v1/notes/delete/{noteId}")
    suspend fun deleteNote(@Path("noteId") noteId: String): GenericResponse

    @POST("api/v1/notes/toggle_lock/{noteId}")
    suspend fun toggleNoteLock(@Path("noteId") noteId: String): ToggleLockResponse

    @GET("api/v1/notes/proposals")
    suspend fun getProposals(): ProposalsListDto

    @POST("api/v1/notes/share/{noteId}")
    suspend fun createShare(
        @Path("noteId") noteId: String,
        @Body body: ShareRequestDto
    ): ShareResponseDto

    @Multipart
    @POST("api/v1/notes/share/{noteId}")
    suspend fun createShareMultipart(
        @Path("noteId") noteId: String,
        @Part("permissions") permissions: RequestBody,
        @Part("expires_in") expiresIn: RequestBody?,
        @Part("access_code") accessCode: RequestBody?,
        @Part("surprise_theme") surpriseTheme: RequestBody,
        @Part("use_typewriter") useTypewriter: RequestBody,
        @Part("auto_approve") autoApprove: RequestBody,
        @Part valentinePhoto: MultipartBody.Part?,
        @Part valentineAudio: MultipartBody.Part?
    ): ShareResponseDto

    @GET("api/v1/notes/shares")
    suspend fun getActiveShares(): ActiveSharesResponseDto

    @GET("api/v1/notes/shares/{noteId}")
    suspend fun getShares(@Path("noteId") noteId: String): SharesListDto

    @POST("api/v1/notes/revoke_share/{shareId}")
    suspend fun revokeShare(@Path("shareId") shareId: String): GenericResponse

    @GET("api/v1/notes/share/{shareId}/comments")
    suspend fun getShareComments(@Path("shareId") shareId: String): CommentsListDto

    @POST("api/v1/notes/share/{shareId}/comments")
    suspend fun addShareComment(@Path("shareId") shareId: String, @Body body: CommentRequestDto): GenericResponse

    @POST("api/v1/notes/share/{shareId}/comments/{commentId}/replies")
    suspend fun addShareReply(
        @Path("shareId") shareId: String,
        @Path("commentId") commentId: String,
        @Body body: CommentRequestDto
    ): GenericResponse

    @DELETE("api/v1/notes/share/{shareId}/comments/{commentId}")
    suspend fun deleteShareComment(@Path("shareId") shareId: String, @Path("commentId") commentId: String): GenericResponse

    @GET("api/v1/notes/share/{shareId}/attachments")
    suspend fun getShareAttachments(@Path("shareId") shareId: String): AttachmentsListDto

    @GET("api/v1/notes/versions/{noteId}")
    suspend fun getVersions(@Path("noteId") noteId: String): VersionsListDto

    @POST("api/v1/notes/version/restore/{postId}/{versionId}")
    suspend fun restoreVersion(@Path("postId") noteId: String, @Path("versionId") versionId: String): GenericResponse

    @POST("api/v1/notes/proposal/{versionId}/decision")
    suspend fun decideProposal(@Path("versionId") versionId: String, @Body body: ProposalDecisionDto): GenericResponse

    @POST("api/v1/app_lock/setup")
    suspend fun setupAppLock(@Body body: AppLockSetupDto): GenericResponse

    @POST("api/v1/app_lock/verify")
    suspend fun verifyAppLock(@Body body: AppLockVerifyDto): GenericResponse

    @GET("api/v1/app_lock/check_status")
    suspend fun checkLockStatus(): AppLockStatusDto

    @POST("api/v1/app_lock/remove")
    suspend fun removeAppLock(@Body body: AppLockRemoveDto): GenericResponse


    @POST("api/v1/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/v1/confirm/{email}")
    suspend fun confirm(@Path("email") email: String, @Body body: ConfirmRequest): GenericResponse

    @GET("personal_post/search")
    suspend fun searchPersonalNotes(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): SearchResultsDto

    @GET("api/posts")
    suspend fun getPublicPosts(): List<PublicPostDto>

    @POST("api/fcm/register")
    suspend fun registerFcm(@Body body: FcmTokenDto): GenericResponse

    @POST("api/fcm/unregister")
    suspend fun unregisterFcm(@Body body: FcmTokenDto): GenericResponse

    @POST("api/v1/premium/activate")
    suspend fun activatePremium(): GenericResponse

    // SECURITY/CONTRACT NOTE: These four endpoints exist in two flavors
    // on the server:
    //   • /api/<path>          — legacy blueprint, CSRF-protected (POSTs
    //                            would 400 from the Android client because
    //                            it doesn't send a CSRF token).
    //   • /api/v1/<path>       — api_bp blueprint, fully CSRF-exempt
    //                            (see main.py: csrf.exempt(api_bp)),
    //                            contract and data model identical.
    // The Android client must always hit the v1 versions.

    @GET("api/v1/notifications/badge-counts")
    suspend fun getBadgeCounts(): BadgeCountsDto

    @GET("api/v1/posts/my-commented")
    suspend fun getNotifications(): NotificationsResponseDto

    @POST("api/v1/posts/mark-all-read")
    suspend fun markAllPostsRead(): GenericResponse

    @POST("api/v1/activity/mark_read")
    suspend fun markAllProposalsRead(): GenericResponse

    @POST("api/v1/notes/dedup")
    suspend fun dedupNotes(@Query("confirm") confirm: Boolean = true): DedupResponseDto
}


data class FcmTokenDto(
    val token: String,
    val platform: String = "android"
)
