package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CachedTrackEntity::class], version = 1, exportSchema = false)
abstract class DapDatabase : RoomDatabase() {
  abstract fun audioTrackDao(): AudioTrackDao

  companion object {
    @Volatile
    private var INSTANCE: DapDatabase? = null

    fun getInstance(context: Context): DapDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          DapDatabase::class.java,
          "dap_audiophile_cache.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
