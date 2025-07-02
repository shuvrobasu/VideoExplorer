package com.videoexplorer.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import com.videoexplorer.data.database.VideoDao
import com.videoexplorer.data.database.SettingsDao
import com.videoexplorer.data.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.io.File

class VideoRepository(
    private val videoDao: VideoDao,
    private val settingsDao: SettingsDao,
    private val context: Context
) {
    private val gson = Gson()
    
    suspend fun scanAndUpdateFolder(folderPath: String): List<VideoFile> {
        return withContext(Dispatchers.IO) {
            val existingVideos = videoDao.getVideosInFolder(folderPath).associateBy { it.filePath }
            val currentFiles = scanVideoFilesInFolder(folderPath)
            
            // Remove deleted files from database
            val currentFilePaths = currentFiles.map { it.filePath }.toSet()
            val deletedFiles = existingVideos.keys - currentFilePaths
            if (deletedFiles.isNotEmpty()) {
                videoDao.deleteVideosByPaths(deletedFiles.toList())
            }
            
            // Update or insert current files
            val videosToInsert = mutableListOf<VideoFile>()
            currentFiles.forEach { scannedVideo ->
                val existingVideo = existingVideos[scannedVideo.filePath]
                if (existingVideo == null) {
                    // New file
                    val videoWithThumbnail = generateThumbnail(scannedVideo)
                    videosToInsert.add(videoWithThumbnail.copy(isNew = true))
                } else {
                    // Existing file - check if modified
                    if (existingVideo.dateModified != scannedVideo.dateModified) {
                        val updatedVideo = existingVideo.copy(
                            fileName = scannedVideo.fileName,
                            fileSize = scannedVideo.fileSize,
                            dateModified = scannedVideo.dateModified,
                            duration = scannedVideo.duration
                        )
                        val videoWithThumbnail = generateThumbnail(updatedVideo)
                        videosToInsert.add(videoWithThumbnail)
                    }
                }
            }
            
            if (videosToInsert.isNotEmpty()) {
                videoDao.insertVideos(videosToInsert)
            }
            
            // Mark old videos as not new based on settings
            val newFileDays = getNewFileDurationDays()
            val cutoffTime = System.currentTimeMillis() - (newFileDays * 24 * 60 * 60 * 1000L)
            videoDao.markOldVideosAsNotNew(cutoffTime)
            
            videoDao.getVideosInFolder(folderPath)
        }
    }
    
    private suspend fun scanVideoFilesInFolder(folderPath: String): List<VideoFile> {
        return withContext(Dispatchers.IO) {
            val videoFiles = mutableListOf<VideoFile>()
            val folder = File(folderPath)
            
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles { file ->
                    file.isFile && isVideoFile(file.extension.lowercase())
                }?.forEach { file ->
                    val duration = getVideoDuration(file.absolutePath)
                    videoFiles.add(
                        VideoFile(
                            filePath = file.absolutePath,
                            fileName = file.name,
                            folderPath = folderPath,
                            fileSize = file.length(),
                            dateModified = file.lastModified(),
                            dateAdded = System.currentTimeMillis(),
                            duration = duration
                        )
                    )
                }
            }
            videoFiles
        }
    }
    
    private fun isVideoFile(extension: String): Boolean {
        val videoExtensions = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
        return extension in videoExtensions
    }
    
    private suspend fun getVideoDuration(filePath: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()
                duration?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    private suspend fun generateThumbnail(video: VideoFile): VideoFile {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(video.filePath)
                val bitmap = retriever.getFrameAtTime(1000000) // 1 second
                retriever.release()
                
                bitmap?.let {
                    val stream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val thumbnailBytes = stream.toByteArray()
                    video.copy(thumbnailBlob = thumbnailBytes)
                } ?: video
            } catch (e: Exception) {
                video
            }
        }
    }
    
    suspend fun getAllVideoFolders(): List<String> {
        return withContext(Dispatchers.IO) {
            val folders = mutableSetOf<String>()
            
            // Scan common video directories
            val commonPaths = listOf(
                "/storage/emulated/0/DCIM",
                "/storage/emulated/0/Movies",
                "/storage/emulated/0/Download",
                "/storage/emulated/0/Pictures"
            )
            
            commonPaths.forEach { path ->
                scanForVideoFolders(File(path), folders)
            }
            
            // Also scan external storage
            context.getExternalFilesDirs(null)?.forEach { dir ->
                dir?.let { scanForVideoFolders(it.parentFile?.parentFile, folders) }
            }
            
            folders.toList().sorted()
        }
    }
    
    private fun scanForVideoFolders(directory: File?, folders: MutableSet<String>) {
        try {
            directory?.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    // Check if directory contains video files
                    val hasVideos = file.listFiles()?.any { child ->
                        child.isFile && isVideoFile(child.extension.lowercase())
                    } ?: false
                    
                    if (hasVideos) {
                        folders.add(file.absolutePath)
                    }
                    
                    // Recursively scan subdirectories (limit depth to avoid infinite loops)
                    if (file.absolutePath.count { it == '/' } < 10) {
                        scanForVideoFolders(file, folders)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission denied - skip this directory
        }
    }
    
    fun getVideosInFolderFlow(folderPath: String): Flow<List<VideoFile>> {
        return videoDao.getVideosInFolderFlow(folderPath)
    }
    
    suspend fun updateVideoRating(video: VideoFile, rating: Float) {
        videoDao.updateVideo(video.copy(rating = rating))
    }
    
    suspend fun updateVideoTags(video: VideoFile, tags: List<String>) {
        val tagsJson = gson.toJson(tags)
        videoDao.updateVideo(video.copy(tags = tagsJson))
    }
    
    suspend fun toggleVideoFavorite(video: VideoFile) {
        videoDao.updateVideo(video.copy(isFavorite = !video.isFavorite))
    }
    
    suspend fun recordVideoPlay(video: VideoFile) {
        val playHistory = PlayHistory(
            filePath = video.filePath,
            playedAt = System.currentTimeMillis(),
            duration = video.duration
        )
        videoDao.insertPlayHistory(playHistory)
        
        // Update play count
        videoDao.updateVideo(
            video.copy(
                playCount = video.playCount + 1,
                lastPlayed = System.currentTimeMillis()
            )
        )
    }
    
    fun getPlayHistoryFlow(): Flow<List<PlayHistoryWithFileName>> {
        return videoDao.getPlayHistoryFlow()
    }
    
    suspend fun addCustomTag(tagName: String, color: String) {
        val tag = CustomTag(
            tagName = tagName,
            color = color,
            createdAt = System.currentTimeMillis()
        )
        videoDao.insertCustomTag(tag)
    }
    
    fun getCustomTagsFlow(): Flow<List<CustomTag>> {
        return videoDao.getAllCustomTagsFlow()
    }
    
    suspend fun deleteCustomTag(tag: CustomTag) {
        videoDao.deleteCustomTag(tag)
    }
    
    fun parseVideoTags(tagsJson: String): List<String> {
        return try {
            if (tagsJson.isEmpty()) return emptyList()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(tagsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getNewFileDurationDays(): Int {
        val setting = settingsDao.getSetting("new_file_duration_days")
        return setting?.value?.toIntOrNull() ?: 7
    }
    
    suspend fun setNewFileDurationDays(days: Int) {
        settingsDao.setSetting(AppSettings("new_file_duration_days", days.toString()))
    }
}