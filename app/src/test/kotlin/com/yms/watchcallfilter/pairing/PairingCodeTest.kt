package com.yms.watchcallfilter.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PairingCodeTest {

    @Test
    fun `generate produces six digit numeric strings`() {
        repeat(50) {
            val code = PairingCode.generate()
            assertThat(code).hasLength(6)
            assertThat(code.all { it.isDigit() }).isTrue()
        }
    }

    @Test
    fun `isValid accepts six-digit numeric strings`() {
        assertThat(PairingCode.isValid("000000")).isTrue()
        assertThat(PairingCode.isValid("123456")).isTrue()
    }

    @Test
    fun `isValid rejects wrong length or non-digits`() {
        assertThat(PairingCode.isValid("12345")).isFalse()
        assertThat(PairingCode.isValid("1234567")).isFalse()
        assertThat(PairingCode.isValid("abcdef")).isFalse()
        assertThat(PairingCode.isValid("12345a")).isFalse()
        assertThat(PairingCode.isValid("")).isFalse()
    }
}
