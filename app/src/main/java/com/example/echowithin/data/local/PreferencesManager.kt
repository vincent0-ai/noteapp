package com.example.echowithin.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.echowithin.EchoWithinApplication

object PreferencesManager {
    private const val PREFS_NAME = "echowithin_preferences"

    private val prefs: SharedPreferences by lazy {
        EchoWithinApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Sort order: "date_modified" (default), "date_created", "title_az", "title_za"
    var sortOrder: String
        get() = prefs.getString("sort_order", "date_modified") ?: "date_modified"
        set(value) = prefs.edit().putString("sort_order", value).apply()

    // Trash auto-purge days: 7, 14, 30 (default)
    var trashPurgeDays: Int
        get() = prefs.getInt("trash_purge_days", 30)
        set(value) = prefs.edit().putInt("trash_purge_days", value).apply()

    // Biometric unlock enabled
    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    // Auto-purge trash: delete trashed notes after trashPurgeDays
    var autoPurgeTrash: Boolean
        get() = prefs.getBoolean("auto_purge_trash", true)
        set(value) = prefs.edit().putBoolean("auto_purge_trash", value).apply()
}
