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

    private const val KEY_DISMISSED_UPDATE_CODE = "dismissed_update_code"
    private const val KEY_OFFLINE_PRIVACY_SHOWN = "offline_privacy_shown"

    var dismissedUpdateCode: Int
        get() = prefs?.getInt(KEY_DISMISSED_UPDATE_CODE, -1) ?: -1
        set(value) {
            prefs?.edit()?.putInt(KEY_DISMISSED_UPDATE_CODE, value)?.apply()
        }

    var offlinePrivacyShown: Boolean
        get() = prefs?.getBoolean(KEY_OFFLINE_PRIVACY_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_OFFLINE_PRIVACY_SHOWN, value)?.apply()
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

    /**
     * Clears SESSION-level data (token, username, account tier, trial state)
     * while preserving DEVICE-LOCAL data: the app-lock PIN hash/flags,
     * sync-mode preference, dismissed-update code, and the offline-privacy
     * "shown once" flag.
     *
     * Use this on logout / 401 session expiry instead of [clear]. The PIN
     * hash is a device-local security secret — it must survive logout so
     * the user can still unlock locked notes while offline after signing
     * out. Wiping it (the old behaviour) made the correct PIN silently
     * fail offline, because the offline verify path has no hash to
     * compare against. Sync-mode and the dismissed-update / privacy flags
     * are user preferences that should also persist across sign-out.
     *
     * [clear] (full wipe) remains for true account deletion / factory reset.
     */
    fun clearSession() {
        val savedPinHash = localPinHash
        val savedHasPin = localHasPin
        val savedPinConfigured = localPinConfigured
        val savedSyncMode = syncMode
        val savedDismissedUpdate = dismissedUpdateCode
        val savedOfflinePrivacyShown = offlinePrivacyShown
        prefs?.edit()?.clear()?.apply()
        prefs?.edit()?.apply {
            putString(KEY_LOCAL_PIN_HASH, savedPinHash)
            putBoolean(KEY_LOCAL_HAS_PIN, savedHasPin)
            putBoolean(KEY_LOCAL_PIN_CONFIGURED, savedPinConfigured)
            putString(KEY_SYNC_MODE, savedSyncMode)
            putInt(KEY_DISMISSED_UPDATE_CODE, savedDismissedUpdate)
            putBoolean(KEY_OFFLINE_PRIVACY_SHOWN, savedOfflinePrivacyShown)
        }
        try {
            ApiClient.clearCookies()
        } catch (_: Exception) {}
    }
}

