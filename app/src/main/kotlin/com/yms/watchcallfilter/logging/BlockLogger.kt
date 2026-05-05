package com.yms.watchcallfilter.logging

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.yms.watchcallfilter.BlockReason

/**
 * Records a blocked call to Firestore so the parent web sees it.
 * Fire-and-forget: writes are queued by the Firestore offline cache
 * if the watch has no network at the moment.
 */
class BlockLogger(private val watchId: String) {

    fun log(e164: String?, reason: BlockReason) {
        try {
            FirebaseFirestore.getInstance()
                .collection("watches").document(watchId)
                .collection("blockLog")
                .add(mapOf(
                    "e164" to e164,
                    "reason" to reason.name.lowercase(),
                    "blockedAt" to FieldValue.serverTimestamp(),
                ))
                .addOnFailureListener { Log.w(TAG, "block log write failed", it) }
        } catch (t: Throwable) {
            // Firebase not yet initialized, etc. — never crash a screening decision.
            Log.w(TAG, "block log skipped: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "BlockLogger"
    }
}
