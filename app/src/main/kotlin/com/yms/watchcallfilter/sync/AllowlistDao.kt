package com.yms.watchcallfilter.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "allowlist")
data class AllowlistRow(
    @PrimaryKey val e164: String,
    val name: String,
    val updatedAt: Long,
)

@Dao
interface AllowlistDao {

    @Query("SELECT EXISTS(SELECT 1 FROM allowlist WHERE e164 = :number LIMIT 1)")
    fun contains(number: String): Boolean

    @Query("SELECT * FROM allowlist")
    fun all(): List<AllowlistRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(rows: List<AllowlistRow>)

    @Query("DELETE FROM allowlist WHERE e164 NOT IN (:keep)")
    fun deleteNotIn(keep: List<String>)

    @Query("DELETE FROM allowlist")
    fun clear()
}
