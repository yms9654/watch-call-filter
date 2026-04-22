package com.yms.watchcallfilter

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

interface ContactRepository {
    fun isKnown(phoneNumber: String): Boolean
}

class PhoneLookupContactRepository(private val context: Context) : ContactRepository {

    override fun isKnown(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        return context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.count > 0
        } ?: false
    }
}
