package com.yms.watchcallfilter

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PhoneLookupContactRepositoryTest {

    private lateinit var context: Context
    private lateinit var provider: FakePhoneLookupProvider
    private lateinit var repository: PhoneLookupContactRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val authority = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.authority!!
        provider = Robolectric.setupContentProvider(
            FakePhoneLookupProvider::class.java,
            authority
        ) as FakePhoneLookupProvider
        repository = PhoneLookupContactRepository(context)
    }

    @Test
    fun `returns true when contact exists`() {
        provider.addContact("01012345678")
        assertThat(repository.isKnown("01012345678")).isTrue()
    }

    @Test
    fun `returns false when contact not present`() {
        provider.addContact("01012345678")
        assertThat(repository.isKnown("01099998888")).isFalse()
    }

    @Test
    fun `returns false for blank number`() {
        assertThat(repository.isKnown("")).isFalse()
    }

    @Test
    fun `encodes special characters safely`() {
        provider.addContact("+821012345678")
        assertThat(repository.isKnown("+821012345678")).isTrue()
    }

    @Test
    fun `matches kr local saved contact when called with international format`() {
        provider.addContact("01012345678")
        assertThat(repository.isKnown("+821012345678")).isTrue()
    }

    @Test
    fun `matches kr international saved contact when called with local format`() {
        provider.addContact("+821012345678")
        assertThat(repository.isKnown("01012345678")).isTrue()
    }

    class FakePhoneLookupProvider : ContentProvider() {

        private val numbers = mutableSetOf<String>()

        fun addContact(number: String) {
            numbers.add(number)
        }

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val queried = Uri.decode(uri.lastPathSegment ?: "")
            val cursor = MatrixCursor(projection ?: arrayOf(ContactsContract.PhoneLookup._ID))
            if (queried in numbers) {
                cursor.addRow(arrayOf<Any>(1L))
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int = 0
    }
}
