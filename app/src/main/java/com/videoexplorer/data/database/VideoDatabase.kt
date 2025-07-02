package com.videoexplorer.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.videoexplorer.data.models.*

@Database(
    entities = [VideoFile::class, PlayHistory::class, AppSettings::class, CustomTag::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null
        
        fun getDatabase(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "video_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    // Add any type converters if needed
}