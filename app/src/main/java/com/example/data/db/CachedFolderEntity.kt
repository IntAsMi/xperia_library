package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.FolderItem

@Entity(tableName = "cached_folders")
data class CachedFolderEntity(
  @PrimaryKey val uriString: String,
  val parentFolderUri: String,
  val name: String,
  val path: String,
  val fileCount: Int = 0,
  val subfolderCount: Int = 0,
  val totalDurationMs: Long = 0L,
  val lastScanned: Long = System.currentTimeMillis()
) {
  fun toFolderItem(): FolderItem {
    return FolderItem(
      uriString = uriString,
      name = name,
      path = path,
      fileCount = fileCount,
      subfolderCount = subfolderCount,
      totalDurationMs = totalDurationMs,
      isVirtual = false
    )
  }

  companion object {
    fun fromFolderItem(item: FolderItem, parentFolderUri: String): CachedFolderEntity {
      return CachedFolderEntity(
        uriString = item.uriString,
        parentFolderUri = parentFolderUri,
        name = item.name,
        path = item.path,
        fileCount = item.fileCount,
        subfolderCount = item.subfolderCount,
        totalDurationMs = item.totalDurationMs
      )
    }
  }
}
