package com.yms.watchcallfilter.sync

import com.yms.watchcallfilter.ContactRepository

/**
 * Allow if ANY upstream repository knows the number.
 */
class UnionContactRepository(
    private val repositories: List<ContactRepository>,
) : ContactRepository {

    override fun isKnown(phoneNumber: String): Boolean =
        repositories.any { it.isKnown(phoneNumber) }
}
