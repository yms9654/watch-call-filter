package com.yms.watchcallfilter.identity

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Stable, locally-generated UUID identifying this watch instance.
 * Survives uninstalls only if SharedPreferences are backed up.
 */
class WatchIdentity(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    val watchId: String
        get() = prefs.getString(KEY, null) ?: createAndStore()

    private fun createAndStore(): String {
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, id).apply()
        return id
    }

    companion object {
        private const val PREF = "watch_identity"
        private const val KEY = "watch_id"
    }
}
