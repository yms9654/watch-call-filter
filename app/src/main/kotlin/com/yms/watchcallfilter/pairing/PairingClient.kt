package com.yms.watchcallfilter.pairing

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Manages the Firestore handshake that lets a parent claim this watch
 * from the web admin.
 */
class PairingClient(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Writes a pairingCodes/{code} doc that the parent web app can claim.
     * Returns the 6-digit code that should be displayed to the user.
     */
    suspend fun publishPairingCode(
        watchId: String,
        watchAuthUid: String,
        ttlMillis: Long = TimeUnit_TEN_MIN,
    ): String {
        val code = PairingCode.generate()
        val now = System.currentTimeMillis()
        firestore.collection("pairingCodes").document(code).set(
            mapOf(
                "watchId" to watchId,
                "watchAuthUid" to watchAuthUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to com.google.firebase.Timestamp(
                    (now + ttlMillis) / 1000,
                    (((now + ttlMillis) % 1000) * 1_000_000).toInt(),
                ),
            )
        ).await()
        return code
    }

    /**
     * True once the parent has claimed the code and the watches/{watchId}
     * doc exists.
     */
    suspend fun isPaired(watchId: String): Boolean {
        val snap = firestore.collection("watches").document(watchId).get().await()
        return snap.exists()
    }

    companion object {
        private const val TimeUnit_TEN_MIN: Long = 10 * 60 * 1000L
    }
}
