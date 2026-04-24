package com.roboblocker.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("roboblocker_prefs", Context.MODE_PRIVATE)

    // ─── Blocking settings ────────────────────────────────────────────────────

    var isBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_BLOCKING_ENABLED, value) }

    var blockUnknownNumbers: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_UNKNOWN, false)
        set(value) = prefs.edit { putBoolean(KEY_BLOCK_UNKNOWN, value) }

    var blockInternational: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_INTERNATIONAL, false)
        set(value) = prefs.edit { putBoolean(KEY_BLOCK_INTERNATIONAL, value) }

    var blockSpamPatterns: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_PATTERNS, true)
        set(value) = prefs.edit { putBoolean(KEY_BLOCK_PATTERNS, value) }

    var aiDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_AI_ENABLED, value) }

    var aiApiKey: String
        get() = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_AI_API_KEY, value) }

    var aiSensitivity: Int   // 0=Low, 1=Medium, 2=High
        get() = prefs.getInt(KEY_AI_SENSITIVITY, 1)
        set(value) = prefs.edit { putInt(KEY_AI_SENSITIVITY, value) }

    // ─── Frequency abuse settings ─────────────────────────────────────────────

    var frequencyBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_FREQ_BLOCK, true)
        set(value) = prefs.edit { putBoolean(KEY_FREQ_BLOCK, value) }

    var frequencyThreshold: Int   // calls in 1 hour to trigger block
        get() = prefs.getInt(KEY_FREQ_THRESHOLD, 5)
        set(value) = prefs.edit { putInt(KEY_FREQ_THRESHOLD, value) }

    // ─── Notification settings ────────────────────────────────────────────────

    var showBlockedNotification: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_BLOCKED, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_BLOCKED, value) }

    var silentMode: Boolean
        get() = prefs.getBoolean(KEY_SILENT_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_SILENT_MODE, value) }

    // ─── Schedule settings ────────────────────────────────────────────────────

    var scheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SCHEDULE_ENABLED, value) }

    var scheduleStartHour: Int
        get() = prefs.getInt(KEY_SCHEDULE_START, 22)
        set(value) = prefs.edit { putInt(KEY_SCHEDULE_START, value) }

    var scheduleEndHour: Int
        get() = prefs.getInt(KEY_SCHEDULE_END, 8)
        set(value) = prefs.edit { putInt(KEY_SCHEDULE_END, value) }

    // ─── Whitelist contacts ───────────────────────────────────────────────────

    var neverBlockContacts: Boolean
        get() = prefs.getBoolean(KEY_WHITELIST_CONTACTS, true)
        set(value) = prefs.edit { putBoolean(KEY_WHITELIST_CONTACTS, value) }

    companion object {
        private const val KEY_BLOCKING_ENABLED  = "blocking_enabled"
        private const val KEY_BLOCK_UNKNOWN     = "block_unknown"
        private const val KEY_BLOCK_INTERNATIONAL = "block_international"
        private const val KEY_BLOCK_PATTERNS    = "block_patterns"
        private const val KEY_AI_ENABLED        = "ai_enabled"
        private const val KEY_AI_API_KEY        = "ai_api_key"
        private const val KEY_AI_SENSITIVITY    = "ai_sensitivity"
        private const val KEY_FREQ_BLOCK        = "freq_block"
        private const val KEY_FREQ_THRESHOLD    = "freq_threshold"
        private const val KEY_NOTIFY_BLOCKED    = "notify_blocked"
        private const val KEY_SILENT_MODE       = "silent_mode"
        private const val KEY_SCHEDULE_ENABLED  = "schedule_enabled"
        private const val KEY_SCHEDULE_START    = "schedule_start"
        private const val KEY_SCHEDULE_END      = "schedule_end"
        private const val KEY_WHITELIST_CONTACTS = "whitelist_contacts"
    }
}
