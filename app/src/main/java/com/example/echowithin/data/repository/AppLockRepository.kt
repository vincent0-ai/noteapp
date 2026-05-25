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
        }.recover { exception ->
            if (exception is java.io.IOException) {
                // Offline fallback setup lock locally
                SessionManager.localPinHash = hash
                SessionManager.localHasPin = true
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
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(response.error ?: "Incorrect PIN"))
            }
        } catch (e: java.io.IOException) {
            // Offline fallback verify lock locally
            if (SessionManager.localHasPin && SessionManager.localPinHash == hash) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Incorrect PIN (offline mode)"))
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
            status
        }.recover { exception ->
            if (exception is java.io.IOException) {
                // Offline fallback! Return local status
                AppLockStatusDto(
                    has_pin = SessionManager.localHasPin,
                    unlocked = false // Treat as locked on cold launch offline
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
            SessionManager.localPinHash = null
            SessionManager.localHasPin = false
        }.recover { exception ->
            if (exception is java.io.IOException) {
                // Offline fallback remove lock locally
                SessionManager.localPinHash = null
                SessionManager.localHasPin = false
                Unit
            } else {
                throw exception
            }
        }
    }
}
