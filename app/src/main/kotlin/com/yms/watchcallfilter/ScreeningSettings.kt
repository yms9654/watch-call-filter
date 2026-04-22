package com.yms.watchcallfilter

import android.content.Context
import android.content.SharedPreferences

interface ScreeningSettings {
    val enabled: Boolean
    val blockPrivate: Boolean
}

class SharedPrefScreeningSettings(context: Context) : ScreeningSettings {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override val enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)

    override val blockPrivate: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_PRIVATE, true)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun setBlockPrivate(value: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_PRIVATE, value).apply()
    }

    companion object {
        private const val PREF_NAME = "screening_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BLOCK_PRIVATE = "block_private"
    }
}
