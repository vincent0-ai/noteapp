package com.example.echowithin.data.network

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "echo_within_session"
    private const val KEY_TOKEN = "x_app_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_SYNC_MODE = "sync_mode"
    private const val KEY_ACCOUNT_TIER = "account_tier"
    private const val KEY_IS_TRIAL = "is_trial"
    private const val KEY_TRIAL_DAYS_REMAINING = "trial_days_remaining"

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

    var isTrial: Boolean
        get() = prefs?.getBoolean(KEY_IS_TRIAL, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_IS_TRIAL, value)?.apply()
        }

    var trialDaysRemaining: Int
        get() = prefs?.getInt(KEY_TRIAL_DAYS_REMAINING, 0) ?: 0
        set(value) {
            prefs?.edit()?.putInt(KEY_TRIAL_DAYS_REMAINING, value)?.apply()
        }



    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}

