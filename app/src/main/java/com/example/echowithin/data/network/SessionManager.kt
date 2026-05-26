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

    private const val KEY_LOCAL_PIN_HASH = "local_pin_hash"
    private const val KEY_LOCAL_HAS_PIN = "local_has_pin"
    private const val KEY_LOCAL_PIN_CONFIGURED = "local_pin_configured"

    var localPinHash: String?
        get() = prefs?.getString(KEY_LOCAL_PIN_HASH, null)
        set(value) {
            prefs?.edit()?.putString(KEY_LOCAL_PIN_HASH, value)?.apply()
        }

    var localHasPin: Boolean
        get() = prefs?.getBoolean(KEY_LOCAL_HAS_PIN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_LOCAL_HAS_PIN, value)?.apply()
        }

    /**
     * Tracks whether a PIN has EVER been configured (online or offline).
     * Only cleared on successful online server PIN removal.
     * Prevents the offline PIN bypass vulnerability.
     */
    var localPinConfigured: Boolean
        get() = prefs?.getBoolean(KEY_LOCAL_PIN_CONFIGURED, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_LOCAL_PIN_CONFIGURED, value)?.apply()
        }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
        try {
            ApiClient.clearCookies()
        } catch (_: Exception) {}
    }
}

