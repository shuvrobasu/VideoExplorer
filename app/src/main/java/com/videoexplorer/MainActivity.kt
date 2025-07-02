package com.videoexplorer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.videoexplorer.data.database.VideoDatabase
import com.videoexplorer.data.repository.VideoRepository
import com.videoexplorer.ui.screens.*
import com.videoexplorer.ui.theme.VideoExplorerTheme
import com.videoexplorer.ui.viewmodels.VideoViewModel
import com.videoexplorer.ui.viewmodels.VideoViewModelFactory

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            // Permissions granted, initialize the app
        } else {
            // Handle permission denial
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request permissions
        checkPermissions()
        
        val database = VideoDatabase.getDatabase(this)
        val repository = VideoRepository(
            database.videoDao(),
            database.settingsDao(),
            this
        )
        val viewModelFactory = VideoViewModelFactory(repository)
        
        setContent {
            VideoExplorerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoExplorerApp(viewModelFactory)
                }
            }
        }
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun VideoExplorerApp(viewModelFactory: VideoViewModelFactory) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "folder_list"
    ) {
        composable("folder_list") {
            FolderListScreen(
                onFolderClick = { folderPath ->
                    navController.navigate("video_list/$folderPath")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onHistoryClick = {
                    navController.navigate("history")
                },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable("video_list/{folderPath}") { backStackEntry ->
            val folderPath = backStackEntry.arguments?.getString("folderPath") ?: ""
            VideoListScreen(
                folderPath = folderPath,
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable("history") {
            HistoryScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
    }
}