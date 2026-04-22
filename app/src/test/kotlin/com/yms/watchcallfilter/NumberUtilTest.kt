package com.yms.watchcallfilter

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NumberUtilTest {

    @Test
    fun `normalize strips formatting characters`() {
        assertThat(NumberUtil.normalize("010-1234-5678")).isEqualTo("01012345678")
        assertThat(NumberUtil.normalize("+82 10 1234 5678")).isEqualTo("+821012345678")
        assertThat(NumberUtil.normalize("(02) 555-1234")).isEqualTo("025551234")
    }

    @Test
    fun `normalize returns null for blank or empty`() {
        assertThat(NumberUtil.normalize(null)).isNull()
        assertThat(NumberUtil.normalize("")).isNull()
        assertThat(NumberUtil.normalize("   ")).isNull()
    }

    @Test
    fun `normalize returns null when only formatting chars`() {
        assertThat(NumberUtil.normalize("---")).isNull()
        assertThat(NumberUtil.normalize("()")).isNull()
    }

    @Test
    fun `fromHandle parses tel URI`() {
        val uri = Uri.parse("tel:010-1234-5678")
        assertThat(NumberUtil.fromHandle(uri)).isEqualTo("01012345678")
    }

    @Test
    fun `fromHandle returns null for null or non-tel scheme`() {
        assertThat(NumberUtil.fromHandle(null)).isNull()
        assertThat(NumberUtil.fromHandle(Uri.parse("sip:alice@example.com"))).isNull()
    }

    @Test
    fun `fromHandle returns null for empty tel URI (private number)`() {
        assertThat(NumberUtil.fromHandle(Uri.parse("tel:"))).isNull()
    }
}
