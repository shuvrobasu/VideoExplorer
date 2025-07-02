package com.videoexplorer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.videoexplorer.data.repository.VideoRepository
import com.videoexplorer.data.models.VideoFile
import com.videoexplorer.data.models.PlayHistoryWithFileName
import com.videoexplorer.data.models.CustomTag

class VideoViewModel(
    private val repository: VideoRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _videoFolders = MutableStateFlow<List<String>>(emptyList())
    val videoFolders: StateFlow<List<String>> = _videoFolders.asStateFlow()
    
    private val _videosInFolder = MutableStateFlow<List<VideoFile>>(emptyList())
    val videosInFolder: StateFlow<List<VideoFile>> = _videosInFolder.asStateFlow()
    
    val playHistory: StateFlow<List<PlayHistoryWithFileName>> = repository.getPlayHistoryFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val customTags: StateFlow<List<CustomTag>> = repository.getCustomTagsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun loadVideoFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val folders = repository.getAllVideoFolders()
                _videoFolders.value = folders
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadVideosInFolder(folderPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.scanAndUpdateFolder(folderPath)
                repository.getVideosInFolderFlow(folderPath).collect { videos ->
                    _videosInFolder.value = videos
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateVideoRating(video: VideoFile, rating: Float) {
        viewModelScope.launch {
            repository.updateVideoRating(video, rating)
        }
    }
    
    fun updateVideoTags(video: VideoFile, tags: List<String>) {
        viewModelScope.launch {
            repository.updateVideoTags(video, tags)
        }
    }
    
    fun toggleVideoFavorite(video: VideoFile) {
        viewModelScope.launch {
            repository.toggleVideoFavorite(video)
        }
    }
    
    fun recordVideoPlay(video: VideoFile) {
        viewModelScope.launch {
            repository.recordVideoPlay(video)
        }
    }
    
    fun addCustomTag(tagName: String, color: String) {
        viewModelScope.launch {
            repository.addCustomTag(tagName, color)
        }
    }
    
    fun deleteCustomTag(tag: CustomTag) {
        viewModelScope.launch {
            repository.deleteCustomTag(tag)
        }
    }
    
    fun parseVideoTags(tagsJson: String): List<String> {
        return repository.parseVideoTags(tagsJson)
    }
}

class VideoViewModelFactory(
    private val repository: VideoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}