package com.example.echowithin.data.repository

import com.example.echowithin.data.model.*
import com.example.echowithin.data.network.ApiClient

class AppLockRepository {
    private val api = ApiClient.apiService

    suspend fun setupLock(pin: String): Result<Unit> {
        return runCatching {
            val response = api.setupAppLock(AppLockSetupDto(pin = pin))
            if (!response.success) {
                throw IllegalStateException(response.error ?: "Could not setup lock")
            }
        }
    }

    suspend fun verifyLock(pin: String): Result<Unit> {
        return try {
            val response = api.verifyAppLock(AppLockVerifyDto(pin = pin))
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(response.error ?: "Incorrect PIN"))
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
            api.checkLockStatus()
        }
    }

    suspend fun removeLock(): Result<Unit> {
        return runCatching {
            val response = api.removeAppLock()
            if (!response.success) {
                throw IllegalStateException(response.error ?: "Could not remove lock")
            }
        }
    }
}
