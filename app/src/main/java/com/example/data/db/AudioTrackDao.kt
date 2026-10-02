package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AudioTrackDao {
  @Query("SELECT * FROM cached_tracks WHERE parentFolderUri = :folderUri ORDER BY diskNumber ASC, trackNumber ASC, fileName ASC")
  suspend fun getTracksForFolder(folderUri: String): List<CachedTrackEntity>

  @Query("SELECT * FROM cached_tracks WHERE uriString = :uri LIMIT 1")
  suspend fun getTrackByUri(uri: String): CachedTrackEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTracks(tracks: List<CachedTrackEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrack(track: CachedTrackEntity)

  @Query("DELETE FROM cached_tracks WHERE parentFolderUri = :folderUri")
  suspend fun clearFolder(folderUri: String)

  @Query("SELECT COUNT(*) FROM cached_tracks")
  suspend fun getTotalTrackCount(): Int

  @Query("SELECT * FROM cached_folders WHERE parentFolderUri = :parentFolderUri ORDER BY name ASC")
  suspend fun getSubfoldersForFolder(parentFolderUri: String): List<CachedFolderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolders(folders: List<CachedFolderEntity>)

  @Query("DELETE FROM cached_folders WHERE parentFolderUri = :parentFolderUri")
  suspend fun clearFolders(parentFolderUri: String)

  @Query("DELETE FROM cached_folders")
  suspend fun clearAllFolders()

  @Query("DELETE FROM cached_tracks")
  suspend fun clearAllTracks()
}
