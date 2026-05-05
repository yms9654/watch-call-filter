package com.yms.watchcallfilter.sync

import com.google.common.truth.Truth.assertThat
import com.yms.watchcallfilter.ContactRepository
import org.junit.Test

class UnionContactRepositoryTest {

    private class FixedRepo(private val known: Set<String>) : ContactRepository {
        override fun isKnown(phoneNumber: String): Boolean = phoneNumber in known
    }

    @Test
    fun `returns true when any upstream knows the number`() {
        val a = FixedRepo(setOf("01012345678"))
        val b = FixedRepo(setOf("01099998888"))
        val u = UnionContactRepository(listOf(a, b))
        assertThat(u.isKnown("01012345678")).isTrue()
        assertThat(u.isKnown("01099998888")).isTrue()
    }

    @Test
    fun `returns false when no upstream knows the number`() {
        val u = UnionContactRepository(listOf(
            FixedRepo(setOf("01012345678")),
            FixedRepo(setOf("01099998888")),
        ))
        assertThat(u.isKnown("01055554444")).isFalse()
    }

    @Test
    fun `empty repository list never matches`() {
        val u = UnionContactRepository(emptyList())
        assertThat(u.isKnown("01012345678")).isFalse()
    }
}
