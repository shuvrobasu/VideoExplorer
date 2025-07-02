package com.videoexplorer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "video_files")
data class VideoFile(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val folderPath: String,
    val fileSize: Long,
    val dateModified: Long,
    val dateAdded: Long,
    val duration: Long,
    val rating: Float = 0f,
    val tags: String = "", // JSON string of tags
    val thumbnailBlob: ByteArray? = null,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val isFavorite: Boolean = false,
    val isNew: Boolean = true
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VideoFile
        return filePath == other.filePath
    }

    override fun hashCode(): Int {
        return filePath.hashCode()
    }
}

@Entity(tableName = "play_history")
data class PlayHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val playedAt: Long,
    val duration: Long
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "custom_tags")
data class CustomTag(
    @PrimaryKey val tagName: String,
    val color: String,
    val createdAt: Long
)