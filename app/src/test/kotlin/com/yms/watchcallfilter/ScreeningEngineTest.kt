package com.yms.watchcallfilter

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScreeningEngineTest {

    private class FakeContacts(private val known: Set<String>) : ContactRepository {
        override fun isKnown(phoneNumber: String): Boolean = phoneNumber in known
    }

    private data class FakeSettings(
        override val enabled: Boolean = true,
        override val blockPrivate: Boolean = true
    ) : ScreeningSettings

    @Test
    fun `known contact is allowed`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(setOf("01012345678")),
            settings = FakeSettings()
        )
        assertThat(engine.decide("010-1234-5678")).isEqualTo(Decision.Allow)
    }

    @Test
    fun `unknown number is blocked with UNKNOWN_NUMBER reason`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(setOf("01012345678")),
            settings = FakeSettings()
        )
        assertThat(engine.decide("010-9999-8888"))
            .isEqualTo(Decision.Block(BlockReason.UNKNOWN_NUMBER))
    }

    @Test
    fun `private number is blocked with PRIVATE_NUMBER reason when blockPrivate is true`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(emptySet()),
            settings = FakeSettings(blockPrivate = true)
        )
        assertThat(engine.decide(null))
            .isEqualTo(Decision.Block(BlockReason.PRIVATE_NUMBER))
        assertThat(engine.decide(""))
            .isEqualTo(Decision.Block(BlockReason.PRIVATE_NUMBER))
    }

    @Test
    fun `private number is allowed when blockPrivate is false`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(emptySet()),
            settings = FakeSettings(blockPrivate = false)
        )
        assertThat(engine.decide(null)).isEqualTo(Decision.Allow)
        assertThat(engine.decide("")).isEqualTo(Decision.Allow)
    }

    @Test
    fun `disabled engine allows everything`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(emptySet()),
            settings = FakeSettings(enabled = false, blockPrivate = true)
        )
        assertThat(engine.decide("010-9999-8888")).isEqualTo(Decision.Allow)
        assertThat(engine.decide(null)).isEqualTo(Decision.Allow)
    }

    @Test
    fun `normalization is applied before contact lookup`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(setOf("+821012345678")),
            settings = FakeSettings()
        )
        assertThat(engine.decide("+82 10-1234-5678")).isEqualTo(Decision.Allow)
        assertThat(engine.decide("+82  (10) 1234 5678")).isEqualTo(Decision.Allow)
    }
}
