package com.example.echowithin.data.repository

import com.example.echowithin.data.model.*
import com.example.echowithin.data.network.ApiClient
import com.example.echowithin.data.network.SessionManager
import java.security.MessageDigest

class AppLockRepository {
    private val api = ApiClient.apiService

    private fun String.sha256(): String {
        val bytes = this.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun setupLock(pin: String): Result<Unit> {
        val hash = pin.sha256()
        return runCatching {
            val response = api.setupAppLock(AppLockSetupDto(pin = pin))
            if (!response.success) {
                throw IllegalStateException(response.error ?: "Could not setup lock")
            }
        }.onSuccess {
            SessionManager.localPinHash = hash
            SessionManager.localHasPin = true
            SessionManager.localPinConfigured = true
        }.recover { exception ->
            if (exception is java.io.IOException) {
                // SECURITY: Block offline PIN setup if a PIN was previously configured.
                // This prevents an attacker from overwriting the existing PIN while offline.
                if (SessionManager.localPinConfigured) {
                    throw IllegalStateException("Cannot change PIN while offline. Please connect to the internet.")
                }
                // Only allow first-time offline PIN setup
                SessionManager.localPinHash = hash
                SessionManager.localHasPin = true
                SessionManager.localPinConfigured = true
                Unit
            } else {
                throw exception
            }
        }
    }

    suspend fun verifyLock(pin: String): Result<Unit> {
        val hash = pin.sha256()
        return try {
            val response = api.verifyAppLock(AppLockVerifyDto(pin = pin))
            if (response.success) {
                SessionManager.localPinHash = hash
                SessionManager.localHasPin = true
                SessionManager.localPinConfigured = true
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(response.error ?: "Incorrect PIN"))
            }
        } catch (e: java.io.IOException) {
            // Offline fallback: verify against locally stored PIN hash.
            // The hash is persisted by setupLock() / an online verify() and
            // is intentionally preserved across logout (SessionManager.clearSession
            // keeps it) so the correct PIN still unlocks offline after sign-out.
            if (SessionManager.localPinConfigured && SessionManager.localPinHash == hash) {
                Result.success(Unit)
            } else if (SessionManager.localPinConfigured && SessionManager.localPinHash != null) {
                Result.failure(IllegalStateException("Incorrect PIN (offline mode)"))
            } else {
                Result.failure(IllegalStateException("Can't verify PIN offline. Reconnect to the internet to unlock."))
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val msg = try {
                org.json.JSONObject(errorBody ?: "").optString("error", "Incorrect PIN")
            } catch (_: Exception) { "Incorrect PIN" }
            Result.failure(IllegalStateException(msg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkStatus(): Result<AppLockStatusDto> {
        val hasToken = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
        if (!hasToken) {
            // No token — don't make network call, just return local state
            return Result.success(
                AppLockStatusDto(
                    has_pin = SessionManager.localPinConfigured,
                    unlocked = false
                )
            )
        }
        return runCatching {
            val status = api.checkLockStatus()
            SessionManager.localHasPin = status.has_pin
            if (status.has_pin) {
                SessionManager.localPinConfigured = true
            }
            status
        }.recover { exception ->
            if (exception is java.io.IOException) {
                // SECURITY: Use localPinConfigured (not localHasPin) for offline fallback.
                // This ensures that even after force-close, the PIN state persists correctly.
                AppLockStatusDto(
                    has_pin = SessionManager.localPinConfigured,
                    unlocked = false // Always treat as locked on offline cold launch
                )
            } else {
                // Any other exception (401, 403, etc.) — treat as offline fallback
                AppLockStatusDto(
                    has_pin = SessionManager.localPinConfigured,
                    unlocked = false
                )
            }
        }
    }

    suspend fun removeLock(pin: String): Result<Unit> {
        val hash = pin.sha256()
        return runCatching {
            val response = api.removeAppLock(AppLockRemoveDto(pin = pin))
            if (!response.success) {
                throw IllegalStateException(response.error ?: "Could not remove lock")
            }
        }.onSuccess {
            // Only clear local state on successful ONLINE server response.
            // The server has re-verified the PIN and removed the hash; the
            // local mirror is now safe to wipe.
            SessionManager.localPinHash = null
            SessionManager.localHasPin = false
            SessionManager.localPinConfigured = false
        }.recover { exception ->
            if (exception is java.io.IOException) {
                // SECURITY: Block offline PIN removal entirely. The PIN
                // hash is stored server-side, so a removal that only
                // touches local state would re-enable protection on the
                // next /check_status round-trip and leave the user thinking
                // they're unprotected when they aren't. Require the
                // request to actually reach the server.
                throw IllegalStateException("Cannot remove PIN while offline. Please connect to the internet.")
            } else {
                throw exception
            }
        }
    }
}
