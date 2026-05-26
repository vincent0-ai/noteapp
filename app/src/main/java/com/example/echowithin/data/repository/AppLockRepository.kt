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
            // Offline fallback: verify against locally stored PIN hash
            if (SessionManager.localPinConfigured && SessionManager.localPinHash == hash) {
                Result.success(Unit)
            } else if (SessionManager.localPinConfigured && SessionManager.localPinHash != null) {
                Result.failure(IllegalStateException("Incorrect PIN (offline mode)"))
            } else {
                Result.failure(IllegalStateException("Cannot verify PIN while offline. No locally stored PIN found."))
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
                throw exception
            }
        }
    }

    suspend fun removeLock(): Result<Unit> {
        return runCatching {
            val response = api.removeAppLock()
            if (!response.success) {
                throw IllegalStateException(response.error ?: "Could not remove lock")
            }
        }.onSuccess {
            // Only clear on successful ONLINE server response
            SessionManager.localPinHash = null
            SessionManager.localHasPin = false
            SessionManager.localPinConfigured = false
        }.recover { exception ->
            if (exception is java.io.IOException) {
                // SECURITY: Block offline PIN removal entirely.
                // This prevents an attacker from removing the PIN while offline.
                throw IllegalStateException("Cannot remove PIN while offline. Please connect to the internet.")
            } else {
                throw exception
            }
        }
    }
}
