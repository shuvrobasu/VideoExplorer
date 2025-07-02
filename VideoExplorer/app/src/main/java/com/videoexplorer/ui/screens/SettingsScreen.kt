package com.videoexplorer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.videoexplorer.ui.viewmodels.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: VideoViewModel = viewModel()
) {
    var newFileDays by remember { mutableStateOf(7) }
    
    LaunchedEffect(Unit) {
        // Load current settings
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "New File Duration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "How long to show 'NEW' indicator on files (days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${newFileDays} days")
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Slider(
                            value = newFileDays.toFloat(),
                            onValueChange = { newFileDays = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 29,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Add more settings as needed
        }
    }
}