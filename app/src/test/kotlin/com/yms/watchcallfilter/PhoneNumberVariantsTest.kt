package com.yms.watchcallfilter

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhoneNumberVariantsTest {

    @Test
    fun `kr international expands to local form`() {
        assertThat(PhoneNumberVariants.expand("+821012345678"))
            .containsExactly("+821012345678", "01012345678")
            .inOrder()
    }

    @Test
    fun `kr local expands to international form`() {
        assertThat(PhoneNumberVariants.expand("01012345678"))
            .containsExactly("01012345678", "+821012345678")
            .inOrder()
    }

    @Test
    fun `non-kr international number is not expanded`() {
        assertThat(PhoneNumberVariants.expand("+12025551234"))
            .containsExactly("+12025551234")
    }

    @Test
    fun `too-short local number is not expanded`() {
        assertThat(PhoneNumberVariants.expand("0212345"))
            .containsExactly("0212345")
    }

    @Test
    fun `blank input yields empty list`() {
        assertThat(PhoneNumberVariants.expand("")).isEmpty()
    }

    @Test
    fun `landline with leading zero expands too`() {
        assertThat(PhoneNumberVariants.expand("0212345678"))
            .containsExactly("0212345678", "+82212345678")
            .inOrder()
    }
}
