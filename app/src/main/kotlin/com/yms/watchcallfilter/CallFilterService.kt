package com.yms.watchcallfilter

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.yms.watchcallfilter.identity.WatchIdentity
import com.yms.watchcallfilter.logging.BlockLogger
import com.yms.watchcallfilter.sync.AppDatabase
import com.yms.watchcallfilter.sync.RoomAllowlistRepository
import com.yms.watchcallfilter.sync.UnionContactRepository

class CallFilterService : CallScreeningService() {

    private lateinit var engine: ScreeningEngine
    private lateinit var blockLogger: BlockLogger

    override fun onCreate() {
        super.onCreate()
        val deviceContacts = PhoneLookupContactRepository(applicationContext)
        val remoteContacts = RoomAllowlistRepository(
            AppDatabase.get(applicationContext).allowlistDao()
        )
        engine = ScreeningEngine(
            contacts = UnionContactRepository(listOf(deviceContacts, remoteContacts)),
            settings = SharedPrefScreeningSettings(applicationContext)
        )
        blockLogger = BlockLogger(WatchIdentity(applicationContext).watchId)
    }

    override fun onScreenCall(details: Call.Details) {
        val number = NumberUtil.fromHandle(details.handle)
        val decision = engine.decide(number)
        Log.i(TAG, "screen call number=${number ?: "<private>"} -> $decision")
        respondToCall(details, decision.toResponse())
        if (decision is Decision.Block) {
            blockLogger.log(number, decision.reason)
        }
    }

    companion object {
        private const val TAG = "CallFilterService"

        fun Decision.toResponse(): CallResponse = when (this) {
            is Decision.Allow -> CallResponse.Builder().build()
            is Decision.Block -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }
    }
}
