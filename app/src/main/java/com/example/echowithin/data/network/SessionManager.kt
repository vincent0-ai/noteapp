package com.example.echowithin.data.network

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "echo_within_session"
    private const val KEY_TOKEN = "x_app_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_SYNC_MODE = "sync_mode"
    private const val KEY_ACCOUNT_TIER = "account_tier"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs?.getString(KEY_TOKEN, null)
        set(value) {
            prefs?.edit()?.putString(KEY_TOKEN, value)?.apply()
        }

    var username: String?
        get() = prefs?.getString(KEY_USERNAME, null)
        set(value) {
            prefs?.edit()?.putString(KEY_USERNAME, value)?.apply()
        }

    var syncMode: String
        get() = prefs?.getString(KEY_SYNC_MODE, "automatic") ?: "automatic"
        set(value) {
            prefs?.edit()?.putString(KEY_SYNC_MODE, value)?.apply()
        }

    var accountTier: String
        get() = prefs?.getString(KEY_ACCOUNT_TIER, "free") ?: "free"
        set(value) {
            prefs?.edit()?.putString(KEY_ACCOUNT_TIER, value)?.apply()
        }



    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}

