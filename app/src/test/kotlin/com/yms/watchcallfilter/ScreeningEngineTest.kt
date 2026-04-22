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
        assertThat(engine.decide("010-1234-5678")).isEqualTo(Decision.ALLOW)
    }

    @Test
    fun `unknown number is blocked`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(setOf("01012345678")),
            settings = FakeSettings()
        )
        assertThat(engine.decide("010-9999-8888")).isEqualTo(Decision.BLOCK)
    }

    @Test
    fun `private number is blocked when blockPrivate is true`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(emptySet()),
            settings = FakeSettings(blockPrivate = true)
        )
        assertThat(engine.decide(null)).isEqualTo(Decision.BLOCK)
        assertThat(engine.decide("")).isEqualTo(Decision.BLOCK)
    }

    @Test
    fun `private number is allowed when blockPrivate is false`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(emptySet()),
            settings = FakeSettings(blockPrivate = false)
        )
        assertThat(engine.decide(null)).isEqualTo(Decision.ALLOW)
        assertThat(engine.decide("")).isEqualTo(Decision.ALLOW)
    }

    @Test
    fun `disabled engine allows everything`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(emptySet()),
            settings = FakeSettings(enabled = false, blockPrivate = true)
        )
        assertThat(engine.decide("010-9999-8888")).isEqualTo(Decision.ALLOW)
        assertThat(engine.decide(null)).isEqualTo(Decision.ALLOW)
    }

    @Test
    fun `normalization is applied before contact lookup`() {
        val engine = ScreeningEngine(
            contacts = FakeContacts(setOf("+821012345678")),
            settings = FakeSettings()
        )
        assertThat(engine.decide("+82 10-1234-5678")).isEqualTo(Decision.ALLOW)
        assertThat(engine.decide("+82  (10) 1234 5678")).isEqualTo(Decision.ALLOW)
    }
}
