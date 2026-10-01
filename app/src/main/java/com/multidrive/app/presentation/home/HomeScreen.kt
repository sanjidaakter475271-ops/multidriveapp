package com.multidrive.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multidrive.app.domain.model.DriveFile
import com.multidrive.app.presentation.components.FileRow
import com.multidrive.app.presentation.viewmodel.HomeUiState
import com.multidrive.app.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onFileClick: (DriveFile) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MultiDrive - Unified Files") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HomeUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is HomeUiState.Success -> {
                    if (state.files.isEmpty()) {
                        Text(
                            text = "No files found. Add Google accounts to get started.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.files) { file ->
                                FileRow(
                                    file = file,
                                    accountEmail = file.accountEmail,
                                    onFileClick = onFileClick,
                                    onRename = { /* Rename Dialog */ },
                                    onDelete = { viewModel.trashFile(file.id) },
                                    onShare = { /* Share Link */ },
                                    onDownload = { /* Trigger Download */ }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
