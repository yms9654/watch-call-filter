package com.yms.watchcallfilter

import android.net.Uri
import android.telecom.CallScreeningService.CallResponse
import com.google.common.truth.Truth.assertThat
import com.yms.watchcallfilter.CallFilterService.Companion.toResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * End-to-end test of the screening pipeline:
 *   tel: URI  ->  NumberUtil  ->  ScreeningEngine  ->  CallResponse
 *
 * Simulates real incoming-call scenarios by feeding Uri handles through
 * the same path the CallScreeningService uses at runtime.
 */
@RunWith(RobolectricTestRunner::class)
class CallFilterE2ETest {

    private class InMemoryContacts(numbers: Collection<String>) : ContactRepository {
        private val set = numbers.flatMap { PhoneNumberVariants.expand(it) }.toSet()
        override fun isKnown(phoneNumber: String): Boolean =
            PhoneNumberVariants.expand(phoneNumber).any { it in set }
    }

    private data class InMemorySettings(
        override val enabled: Boolean = true,
        override val blockPrivate: Boolean = true
    ) : ScreeningSettings

    private fun pipeline(
        handle: Uri?,
        contacts: Collection<String>,
        enabled: Boolean = true,
        blockPrivate: Boolean = true
    ): CallResponse {
        val engine = ScreeningEngine(
            contacts = InMemoryContacts(contacts),
            settings = InMemorySettings(enabled = enabled, blockPrivate = blockPrivate)
        )
        val number = NumberUtil.fromHandle(handle)
        return engine.decide(number).toResponse()
    }

    // --- Scenario 1: incoming call from a saved contact --------------------

    @Test
    fun `incoming call from contact is allowed`() {
        val response = pipeline(
            handle = Uri.parse("tel:010-1234-5678"),
            contacts = listOf("01012345678")
        )
        assertThat(response.disallowCall).isFalse()
        assertThat(response.rejectCall).isFalse()
    }

    @Test
    fun `incoming call from contact with international format is allowed`() {
        val response = pipeline(
            handle = Uri.parse("tel:+82-10-1234-5678"),
            contacts = listOf("+821012345678")
        )
        assertThat(response.disallowCall).isFalse()
        assertThat(response.rejectCall).isFalse()
    }

    // --- Scenario 2: incoming call from an unknown number ------------------

    @Test
    fun `incoming call from unknown number is blocked and rejected`() {
        val response = pipeline(
            handle = Uri.parse("tel:010-9999-8888"),
            contacts = listOf("01012345678")
        )
        assertThat(response.disallowCall).isTrue()
        assertThat(response.rejectCall).isTrue()
        assertThat(response.skipCallLog).isFalse()
        assertThat(response.skipNotification).isFalse()
    }

    @Test
    fun `incoming call from unknown number when contacts empty is blocked`() {
        val response = pipeline(
            handle = Uri.parse("tel:010-5555-5555"),
            contacts = emptyList()
        )
        assertThat(response.disallowCall).isTrue()
        assertThat(response.rejectCall).isTrue()
    }

    // --- Scenario 3: private / number-withheld caller ----------------------

    @Test
    fun `private number is blocked when blockPrivate is enabled`() {
        val response = pipeline(
            handle = Uri.parse("tel:"),
            contacts = listOf("01012345678"),
            blockPrivate = true
        )
        assertThat(response.disallowCall).isTrue()
        assertThat(response.rejectCall).isTrue()
    }

    @Test
    fun `null handle is blocked when blockPrivate is enabled`() {
        val response = pipeline(
            handle = null,
            contacts = listOf("01012345678"),
            blockPrivate = true
        )
        assertThat(response.disallowCall).isTrue()
        assertThat(response.rejectCall).isTrue()
    }

    @Test
    fun `private number is allowed when blockPrivate disabled`() {
        val response = pipeline(
            handle = Uri.parse("tel:"),
            contacts = listOf("01012345678"),
            blockPrivate = false
        )
        assertThat(response.disallowCall).isFalse()
        assertThat(response.rejectCall).isFalse()
    }

    // --- Scenario 4: engine disabled (kill switch) -------------------------

    @Test
    fun `when disabled every call is allowed`() {
        val unknown = pipeline(
            handle = Uri.parse("tel:010-9999-8888"),
            contacts = emptyList(),
            enabled = false
        )
        val private = pipeline(
            handle = null,
            contacts = emptyList(),
            enabled = false
        )
        assertThat(unknown.disallowCall).isFalse()
        assertThat(private.disallowCall).isFalse()
    }

    // --- Scenario 5: non-telephony handles (SIP etc.) ----------------------

    @Test
    fun `sip handle is treated as private`() {
        val response = pipeline(
            handle = Uri.parse("sip:bob@example.com"),
            contacts = listOf("01012345678"),
            blockPrivate = true
        )
        assertThat(response.disallowCall).isTrue()
    }

    // --- Scenario 6: kr cross-format (saved local, call international) ----

    @Test
    fun `kr contact saved as local is allowed when called from international`() {
        val response = pipeline(
            handle = Uri.parse("tel:+821012345678"),
            contacts = listOf("01012345678")
        )
        assertThat(response.disallowCall).isFalse()
    }

    @Test
    fun `kr contact saved as international is allowed when called from local`() {
        val response = pipeline(
            handle = Uri.parse("tel:010-1234-5678"),
            contacts = listOf("+821012345678")
        )
        assertThat(response.disallowCall).isFalse()
    }

    // --- Scenario 7: contact saved with spacing variants -------------------

    @Test
    fun `contact lookup is format-insensitive`() {
        val a = pipeline(Uri.parse("tel:010 1234 5678"), listOf("01012345678"))
        val b = pipeline(Uri.parse("tel:(010) 1234-5678"), listOf("01012345678"))
        val c = pipeline(Uri.parse("tel:010.1234.5678"), listOf("01012345678"))
        assertThat(a.disallowCall).isFalse()
        assertThat(b.disallowCall).isFalse()
        assertThat(c.disallowCall).isFalse()
    }
}
