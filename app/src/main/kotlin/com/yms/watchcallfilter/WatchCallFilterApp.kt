package com.yms.watchcallfilter

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yms.watchcallfilter.sync.AllowlistSyncWorker
import java.util.concurrent.TimeUnit

class WatchCallFilterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) return
        signInAnonymouslyIfNeeded()
        scheduleAllowlistSync()
    }

    private fun signInAnonymouslyIfNeeded() {
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnFailureListener {
                android.util.Log.w(TAG, "anonymous sign-in failed", it)
            }
        }
    }

    private fun scheduleAllowlistSync() {
        val request = PeriodicWorkRequestBuilder<AllowlistSyncWorker>(
            30, TimeUnit.MINUTES,
            10, TimeUnit.MINUTES,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "allowlist-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        private const val TAG = "WatchCallFilterApp"
    }
}
