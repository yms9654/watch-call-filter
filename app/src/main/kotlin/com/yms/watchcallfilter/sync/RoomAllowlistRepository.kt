package com.yms.watchcallfilter.sync

import com.yms.watchcallfilter.ContactRepository
import com.yms.watchcallfilter.PhoneNumberVariants

/**
 * Reads from Room cache populated by AllowlistSyncWorker. Tries every
 * PhoneNumberVariants candidate against the table.
 */
class RoomAllowlistRepository(private val dao: AllowlistDao) : ContactRepository {

    override fun isKnown(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return PhoneNumberVariants.expand(phoneNumber).any { dao.contains(it) }
    }
}
