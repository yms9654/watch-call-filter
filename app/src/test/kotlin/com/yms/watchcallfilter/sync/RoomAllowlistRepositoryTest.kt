package com.yms.watchcallfilter.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomAllowlistRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoomAllowlistRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = RoomAllowlistRepository(db.allowlistDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `unknown number returns false`() {
        assertThat(repo.isKnown("01012345678")).isFalse()
    }

    @Test
    fun `seeded number is found by exact e164`() {
        db.allowlistDao().upsertAll(listOf(
            AllowlistRow(e164 = "+821012345678", name = "엄마", updatedAt = 0)
        ))
        assertThat(repo.isKnown("+821012345678")).isTrue()
    }

    @Test
    fun `seeded international form matches local-format query`() {
        db.allowlistDao().upsertAll(listOf(
            AllowlistRow(e164 = "+821012345678", name = "엄마", updatedAt = 0)
        ))
        assertThat(repo.isKnown("01012345678")).isTrue()
    }

    @Test
    fun `seeded local form matches international-format query`() {
        db.allowlistDao().upsertAll(listOf(
            AllowlistRow(e164 = "01012345678", name = "엄마", updatedAt = 0)
        ))
        assertThat(repo.isKnown("+821012345678")).isTrue()
    }

    @Test
    fun `deleteNotIn removes stale rows`() {
        val dao = db.allowlistDao()
        dao.upsertAll(listOf(
            AllowlistRow(e164 = "01011111111", name = "A", updatedAt = 0),
            AllowlistRow(e164 = "01022222222", name = "B", updatedAt = 0),
        ))
        dao.deleteNotIn(listOf("01011111111"))
        assertThat(dao.contains("01011111111")).isTrue()
        assertThat(dao.contains("01022222222")).isFalse()
    }

    @Test
    fun `blank query returns false without lookup`() {
        assertThat(repo.isKnown("")).isFalse()
    }
}
