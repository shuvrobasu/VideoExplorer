package com.videoexplorer.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.videoexplorer.data.models.*

@Dao
interface VideoDao {
    @Query("SELECT * FROM video_files WHERE folderPath = :folderPath")
    suspend fun getVideosInFolder(folderPath: String): List<VideoFile>
    
    @Query("SELECT * FROM video_files WHERE folderPath = :folderPath")
    fun getVideosInFolderFlow(folderPath: String): Flow<List<VideoFile>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoFile)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoFile>)
    
    @Update
    suspend fun updateVideo(video: VideoFile)
    
    @Delete
    suspend fun deleteVideo(video: VideoFile)
    
    @Query("DELETE FROM video_files WHERE filePath IN (:filePaths)")
    suspend fun deleteVideosByPaths(filePaths: List<String>)
    
    @Query("SELECT DISTINCT folderPath FROM video_files ORDER BY folderPath")
    suspend fun getAllFolders(): List<String>
    
    @Query("SELECT DISTINCT folderPath FROM video_files ORDER BY folderPath")
    fun getAllFoldersFlow(): Flow<List<String>>
    
    @Query("UPDATE video_files SET isNew = 0 WHERE dateAdded < :cutoffTime")
    suspend fun markOldVideosAsNotNew(cutoffTime: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(playHistory: PlayHistory)
    
    @Query("SELECT ph.*, vf.fileName FROM play_history ph LEFT JOIN video_files vf ON ph.filePath = vf.filePath ORDER BY ph.playedAt DESC LIMIT :limit")
    suspend fun getPlayHistory(limit: Int = 100): List<PlayHistoryWithFileName>
    
    @Query("SELECT ph.*, vf.fileName FROM play_history ph LEFT JOIN video_files vf ON ph.filePath = vf.filePath ORDER BY ph.playedAt DESC LIMIT :limit")
    fun getPlayHistoryFlow(limit: Int = 100): Flow<List<PlayHistoryWithFileName>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTag(tag: CustomTag)
    
    @Query("SELECT * FROM custom_tags ORDER BY tagName")
    suspend fun getAllCustomTags(): List<CustomTag>
    
    @Query("SELECT * FROM custom_tags ORDER BY tagName")
    fun getAllCustomTagsFlow(): Flow<List<CustomTag>>
    
    @Delete
    suspend fun deleteCustomTag(tag: CustomTag)
}

data class PlayHistoryWithFileName(
    val id: Long,
    val filePath: String,
    val playedAt: Long,
    val duration: Long,
    val fileName: String?
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE key = :key")
    suspend fun getSetting(key: String): AppSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettings)
    
    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun getSettingValue(key: String): String?
}