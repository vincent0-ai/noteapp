package com.example.echowithin.data.repository

import com.example.echowithin.data.model.AttachmentDto
import com.example.echowithin.data.model.CommentDto
import com.example.echowithin.data.model.CommentRequestDto
import com.example.echowithin.data.model.ProposalDecisionDto
import com.example.echowithin.data.model.ShareRequestDto
import com.example.echowithin.data.model.ShareDto
import com.example.echowithin.data.model.VersionDto
import com.example.echowithin.data.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ShareRepository {
    private val api = ApiClient.apiService

    private fun uriToMultipartPart(context: android.content.Context, uriString: String?, partName: String): okhttp3.MultipartBody.Part? {
        if (uriString.isNullOrBlank()) return null
        val uri = android.net.Uri.parse(uriString) ?: return null
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            
            var fileName = "file"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIdx != -1) {
                        fileName = it.getString(displayNameIdx)
                    }
                }
            }
            
            val bytes = inputStream.readBytes()
            val requestFile = bytes.toRequestBody(
                mimeType.toMediaTypeOrNull()
            )
            okhttp3.MultipartBody.Part.createFormData(partName, fileName, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createShare(
        context: android.content.Context,
        noteId: String,
        permissions: String = "view",
        expiresIn: String? = null,
        accessCode: String? = null,
        surpriseTheme: String = "none",
        useTypewriter: Boolean = false,
        autoApprove: Boolean = false,
        photoUri: String? = null,
        audioUri: String? = null
    ): Result<String> {
        return runCatching {
            val response = if (surpriseTheme != "none") {
                val permissionsBody = permissions.toRequestBody("text/plain".toMediaTypeOrNull())
                val expiresBody = expiresIn?.toRequestBody("text/plain".toMediaTypeOrNull())
                val accessCodeBody = accessCode?.ifBlank { null }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val themeBody = surpriseTheme.toRequestBody("text/plain".toMediaTypeOrNull())
                val typewriterBody = useTypewriter.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val autoApproveBody = autoApprove.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                
                val photoPart = uriToMultipartPart(context, photoUri, "valentine_photo")
                val audioPart = uriToMultipartPart(context, audioUri, "valentine_audio")
                
                api.createShareMultipart(
                    noteId = noteId,
                    permissions = permissionsBody,
                    expiresIn = expiresBody,
                    accessCode = accessCodeBody,
                    surpriseTheme = themeBody,
                    useTypewriter = typewriterBody,
                    autoApprove = autoApproveBody,
                    valentinePhoto = photoPart,
                    valentineAudio = audioPart
                )
            } else {
                api.createShare(
                    noteId = noteId,
                    body = ShareRequestDto(
                        permissions = permissions,
                        expires_in = expiresIn,
                        access_code = accessCode?.ifBlank { null },
                        surprise_theme = surpriseTheme,
                        use_typewriter = useTypewriter,
                        auto_approve = autoApprove
                    )
                )
            }
            if (!response.success || response.share_id.isNullOrBlank()) {
                throw IllegalStateException(response.error ?: "Could not create share")
            }
            response.share_id
        }
    }

    suspend fun getShares(noteId: String): Result<List<ShareDto>> {
        return runCatching {
            val response = api.getShares(noteId = noteId)
            response.shares
        }
    }

    suspend fun revokeShare(shareId: String): Result<Unit> {
        return runCatching {
            val response = api.revokeShare(shareId = shareId)
            if (!response.success) {
                throw IllegalStateException(response.error ?: "Could not revoke share")
            }
        }
    }

    suspend fun getComments(shareId: String): Result<List<CommentDto>> = runCatching {
        api.getShareComments(shareId).comments
    }

    suspend fun addComment(shareId: String, content: String): Result<Unit> = runCatching {
        val response = api.addShareComment(shareId, CommentRequestDto(content = content))
        if (!response.success) throw IllegalStateException(response.error ?: "Could not add comment")
    }

    suspend fun addReply(shareId: String, commentId: String, content: String): Result<Unit> = runCatching {
        val response = api.addShareReply(shareId, commentId, CommentRequestDto(content = content))
        if (!response.success) throw IllegalStateException(response.error ?: "Could not add reply")
    }

    suspend fun deleteComment(shareId: String, commentId: String): Result<Unit> = runCatching {
        val response = api.deleteShareComment(shareId, commentId)
        if (!response.success) throw IllegalStateException(response.error ?: "Could not delete comment")
    }

    suspend fun getAttachments(shareId: String): Result<List<AttachmentDto>> = runCatching {
        api.getShareAttachments(shareId).attachments
    }

    suspend fun getVersions(noteId: String): Result<List<VersionDto>> = runCatching {
        api.getVersions(noteId).versions
    }

    suspend fun restoreVersion(noteId: String, versionId: String): Result<Unit> = runCatching {
        val response = api.restoreVersion(noteId, versionId)
        if (!response.success) throw IllegalStateException(response.message ?: response.error ?: "Could not restore version")
    }

    suspend fun decideProposal(versionId: String, decision: String, comment: String = ""): Result<Unit> = runCatching {
        val response = api.decideProposal(versionId, ProposalDecisionDto(decision = decision, comment = comment))
        if (!response.success) throw IllegalStateException(response.message ?: response.error ?: "Could not update proposal")
    }
}

