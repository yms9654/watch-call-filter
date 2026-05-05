package com.yms.watchcallfilter.sync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AllowlistRow::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun allowlistDao(): AllowlistDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "watch-call-filter.db",
            )
                .fallbackToDestructiveMigration()
                // CallScreeningService.onScreenCall runs on the main
                // thread and must respond synchronously. The lookup is
                // a single indexed query so blocking is acceptable.
                .allowMainThreadQueries()
                .build()
                .also { INSTANCE = it }
        }
    }
}
