package com.example.echowithin.data.repository

import com.example.echowithin.data.model.*
import com.example.echowithin.data.network.EchoWithinApiService
import retrofit2.HttpException
import org.json.JSONObject

class AuthRepository(private val api: EchoWithinApiService) {
    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.success(call())
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val message = try {
                if (!errorBody.isNullOrBlank()) {
                    val jsonObj = JSONObject(errorBody)
                    jsonObj.optString("error", jsonObj.optString("message", "API Error"))
                } else {
                    null
                }
            } catch (jsonEx: Exception) {
                null
            } ?: e.message() ?: "API Error"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = safeApiCall {
        api.login(request)
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> = safeApiCall {
        api.register(request)
    }

    suspend fun confirm(email: String, code: String): Result<GenericResponse> = safeApiCall {
        api.confirm(email, ConfirmRequest(code))
    }

    suspend fun logout(): Result<GenericResponse> = safeApiCall {
        api.logout()
    }

    suspend fun appReauth(): Result<LoginResponse> = safeApiCall {
        api.appReauth()
    }
}
