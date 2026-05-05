package com.yms.watchcallfilter.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.yms.watchcallfilter.identity.WatchIdentity
import kotlinx.coroutines.tasks.await

/**
 * Periodically pulls watches/{watchId}/allowlist from Firestore and
 * mirrors it into the Room cache that the screening service queries.
 *
 * Quietly succeeds (returns Result.success) if:
 *   - the watch hasn't been paired yet
 *   - the user is not signed in
 * since these are normal pre-pairing states. Network/permission
 * failures retry.
 */
class AllowlistSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            Log.i(TAG, "no auth user yet — skipping sync")
            return Result.success()
        }

        val watchId = WatchIdentity(applicationContext).watchId
        val firestore = FirebaseFirestore.getInstance()

        val watchSnap = try {
            firestore.collection("watches").document(watchId).get().await()
        } catch (t: Throwable) {
            Log.w(TAG, "watch doc fetch failed", t)
            return Result.retry()
        }
        if (!watchSnap.exists()) {
            Log.i(TAG, "watch $watchId not paired yet — skipping sync")
            return Result.success()
        }

        val entries = try {
            firestore.collection("watches").document(watchId)
                .collection("allowlist").get().await()
        } catch (t: Throwable) {
            Log.w(TAG, "allowlist fetch failed", t)
            return Result.retry()
        }

        val rows = entries.documents.mapNotNull { d ->
            val e164 = d.getString("e164") ?: return@mapNotNull null
            val name = d.getString("name") ?: ""
            AllowlistRow(e164 = e164, name = name, updatedAt = System.currentTimeMillis())
        }

        val dao = AppDatabase.get(applicationContext).allowlistDao()
        dao.upsertAll(rows)
        dao.deleteNotIn(rows.map { it.e164 })

        Log.i(TAG, "synced ${rows.size} allowlist entries for watch $watchId")
        return Result.success()
    }

    companion object {
        private const val TAG = "AllowlistSyncWorker"
    }
}
