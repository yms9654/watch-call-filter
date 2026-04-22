package com.yms.watchcallfilter

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class CallFilterService : CallScreeningService() {

    private lateinit var engine: ScreeningEngine

    override fun onCreate() {
        super.onCreate()
        engine = ScreeningEngine(
            contacts = PhoneLookupContactRepository(applicationContext),
            settings = SharedPrefScreeningSettings(applicationContext)
        )
    }

    override fun onScreenCall(details: Call.Details) {
        val number = NumberUtil.fromHandle(details.handle)
        val decision = engine.decide(number)
        Log.i(TAG, "screen call number=${number ?: "<private>"} -> $decision")
        respondToCall(details, decision.toResponse())
    }

    companion object {
        private const val TAG = "CallFilterService"

        fun Decision.toResponse(): CallResponse =
            when (this) {
                Decision.ALLOW -> CallResponse.Builder().build()
                Decision.BLOCK -> CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            }
    }
}
