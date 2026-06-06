package com.example.echowithin.data.network

import com.example.echowithin.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import android.util.Log
import com.example.echowithin.EchoWithinApplication
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

object ApiClient {
    /** Set by the Compose navigation layer. Invoked on any HTTP 401 response. */
    var onUnauthorized: (() -> Unit)? = null

    val isHandlingUnauthorized = AtomicBoolean(false)

    @Volatile
    private var lastUnauthorizedTime = 0L

    private interface ClearableCookieJar : CookieJar {
        fun clear()
    }

    private val persistentCookieJar = object : ClearableCookieJar {
        private val prefs = EchoWithinApplication.instance.getSharedPreferences("cookies_pref", Context.MODE_PRIVATE)
        private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
        private val lock = Any()

        init {
            synchronized(lock) {
                loadFromPrefs()
            }
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(lock) {
                val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
                hostCookies.removeAll { existing -> cookies.any { it.name == existing.name } }
                hostCookies.addAll(cookies)
                saveToPrefs()
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            synchronized(lock) {
                val now = System.currentTimeMillis()
                val hostCookies = cookieStore[url.host] ?: return emptyList()
                val valid = hostCookies.filter { it.expiresAt >= now }
                if (valid.size != hostCookies.size) {
                    cookieStore[url.host] = valid.toMutableList()
                    saveToPrefs()
                }
                return valid
            }
        }

        private fun saveToPrefs() = synchronized(lock) {
            val editor = prefs.edit()
            cookieStore.forEach { (host, cookies) ->
                val serialized = cookies.joinToString("|") { serializeCookie(it) }
                editor.putString(host, serialized)
            }
            editor.apply()
        }

        private fun loadFromPrefs() = synchronized(lock) {
            prefs.all.forEach { (host, serialized) ->
                if (serialized is String) {
                    val cookies = serialized.split("|")
                        .mapNotNull { deserializeCookie(it, host) }
                        .toMutableList()
                    cookieStore[host] = cookies
                }
            }
        }

        private fun serializeCookie(cookie: Cookie): String {
            return "${cookie.name};${cookie.value};${cookie.expiresAt};${cookie.domain};${cookie.path};${cookie.secure};${cookie.httpOnly};${cookie.persistent};${cookie.hostOnly}"
        }

        private fun deserializeCookie(s: String, host: String): Cookie? {
            return try {
                val parts = s.split(";")
                if (parts.size < 9) return null
                Cookie.Builder()
                    .name(parts[0])
                    .value(parts[1])
                    .expiresAt(parts[2].toLong())
                    .domain(parts[3])
                    .path(parts[4])
                    .apply { if (parts[5].toBoolean()) secure() }
                    .apply { if (parts[6].toBoolean()) httpOnly() }
                    // persistent, hostOnly are derived
                    .build()
            } catch (e: Exception) {
                null
            }
        }

        override fun clear() = synchronized(lock) {
            cookieStore.clear()
            prefs.edit().clear().apply()
        }
    }

    fun clearCookies() {
        persistentCookieJar.clear()
    }

    fun registerFcmToken(context: Context) {
        if (SessionManager.token.isNullOrBlank() || SessionManager.token == "null") return

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                if (!token.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            apiService.registerFcm(FcmTokenDto(token = token))
                            Log.d("FCM", "Successfully registered FCM token: $token")
                        } catch (e: Exception) {
                            Log.e("FCM", "Failed to register FCM token with server", e)
                        }
                    }
                }
            }
    }

    fun unregisterFcmToken(context: Context) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val token = task.result
                if (!token.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            apiService.unregisterFcm(FcmTokenDto(token = token))
                            Log.d("FCM", "Successfully unregistered FCM token: $token")
                        } catch (e: Exception) {
                            Log.e("FCM", "Failed to unregister FCM token", e)
                        }
                    }
                }
            }
    }


    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cookieJar(persistentCookieJar)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", "EchoWithinApp/1.0 Android")
                
                SessionManager.token?.let {
                    requestBuilder.header("X-App-Token", it)
                }
                
                val response = chain.proceed(requestBuilder.build())
                
                val path = originalRequest.url.encodedPath
                val isAuthEndpoint = path.contains("/login") || 
                                     path.contains("/register") || 
                                     path.contains("/app_reauth")
                
                // Detect 401 Unauthorized → invoke callback to clear session & redirect
                if (response.code == 401 && !isAuthEndpoint) {
                    val now = System.currentTimeMillis()
                    val shouldTrigger = synchronized(this) {
                        if (now - lastUnauthorizedTime > 5000L) {
                            lastUnauthorizedTime = now
                            true
                        } else {
                            false
                        }
                    }
                    if (shouldTrigger) {
                        onUnauthorized?.invoke()
                    }
                }
                
                response
            }
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: EchoWithinApiService by lazy {
        retrofit.create(EchoWithinApiService::class.java)
    }
}
