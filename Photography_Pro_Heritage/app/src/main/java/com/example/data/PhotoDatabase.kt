package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
  @Query("SELECT * FROM captured_photos ORDER BY timestamp DESC")
  fun getAllPhotos(): Flow<List<PhotoEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPhoto(photo: PhotoEntity): Long

  @Query("DELETE FROM captured_photos WHERE id = :photoId")
  suspend fun deletePhoto(photoId: Long)

  @Query("SELECT COUNT(*) FROM captured_photos")
  suspend fun getPhotoCount(): Int
}

@Dao
interface PresetDao {
  @Query("SELECT * FROM shooting_presets")
  fun getAllPresets(): Flow<List<PresetEntity>>

  @Query("SELECT * FROM shooting_presets WHERE slotKey = :slotKey")
  suspend fun getPreset(slotKey: String): PresetEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun savePreset(preset: PresetEntity)
}

@Database(entities = [PhotoEntity::class, PresetEntity::class], version = 1, exportSchema = false)
abstract class PhotoDatabase : RoomDatabase() {
  abstract fun photoDao(): PhotoDao
  abstract fun presetDao(): PresetDao

  companion object {
    @Volatile
    private var INSTANCE: PhotoDatabase? = null

    fun getDatabase(context: Context): PhotoDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          PhotoDatabase::class.java,
          "photo_pro_database"
        ).build()
        INSTANCE = instance
        instance
      }
    }
  }
}
