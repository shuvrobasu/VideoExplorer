package com.videoexplorer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.videoexplorer.ui.components.VideoListItem
import com.videoexplorer.ui.viewmodels.VideoViewModel
import com.videoexplorer.data.models.VideoFile
import com.videoexplorer.utils.playVideo

enum class SortOption(val displayName: String) {
    NAME("Name"),
    DATE("Date"),
    SIZE("Size"),
    RATING("Rating"),
    DURATION("Duration")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    folderPath: String,
    onBackClick: () -> Unit,
    viewModel: VideoViewModel = viewModel()
) {
    val context = LocalContext.current
    val videos by viewModel.videosInFolder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var sortOption by remember { mutableStateOf(SortOption.NAME) }
    var sortAscending by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoFile?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(folderPath) {
        viewModel.loadVideosInFolder(folderPath)
    }
    
    // Sort videos
    val sortedVideos = remember(videos, sortOption, sortAscending) {
        val sorted = when (sortOption) {
            SortOption.NAME -> videos.sortedBy { it.fileName }
            SortOption.DATE -> videos.sortedBy { it.dateModified }
            SortOption.SIZE -> videos.sortedBy { it.fileSize }
            SortOption.RATING -> videos.sortedBy { it.rating }
            SortOption.DURATION -> videos.sortedBy { it.duration }
        }
        if (sortAscending) sorted else sorted.reversed()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = folderPath.substringAfterLast("/"),
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(option.displayName)
                                            if (sortOption == option) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (sortOption == option) {
                                            sortAscending = !sortAscending
                                        } else {
                                            sortOption = option
                                            sortAscending = true
                                        }
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedVideos) { video ->
                        VideoListItem(
                            video = video,
                            onVideoClick = { 
                                viewModel.recordVideoPlay(it)
                            },
                            onVideoLongClick = { 
                                playVideo(context, it.filePath)
                                viewModel.recordVideoPlay(it)
                            },
                            onRatingChange = { videoFile, rating ->
                                viewModel.updateVideoRating(videoFile, rating)
                            },
                            onTagsClick = { 
                                selectedVideo = it
                                showTagDialog = true
                            },
                            onFavoriteClick = { 
                                viewModel.toggleVideoFavorite(it)
                            },
                            tags = viewModel.parseVideoTags(video.tags),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    
    // Tag editing dialog
    if (showTagDialog && selectedVideo != null) {
        TagEditDialog(
            video = selectedVideo!!,
            onDismiss = { 
                showTagDialog = false
                selectedVideo = null
            },
            onTagsUpdate = { video, tags ->
                viewModel.updateVideoTags(video, tags)
                showTagDialog = false
                selectedVideo = null
            },
            viewModel = viewModel
        )
    }
}