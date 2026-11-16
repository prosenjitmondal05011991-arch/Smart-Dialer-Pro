package com.example.smartdialer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteContactEntity::class,
        ContactNoteEntity::class,
        BlockedNumberEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartDialerDatabase : RoomDatabase() {
    abstract fun favoriteContactDao(): FavoriteContactDao
    abstract fun contactNoteDao(): ContactNoteDao
    abstract fun blockedNumberDao(): BlockedNumberDao

    companion object {
        @Volatile
        private var instance: SmartDialerDatabase? = null

        fun getInstance(context: Context): SmartDialerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartDialerDatabase::class.java,
                    "smart_dialer_pro.db"
                ).build().also { instance = it }
            }
        }
    }
}
