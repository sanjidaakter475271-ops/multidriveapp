package com.multidrive.app.presentation.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multidrive.app.domain.model.DriveFile
import com.multidrive.app.presentation.components.FileGridCard
import com.multidrive.app.presentation.components.FileRow
import com.multidrive.app.presentation.components.SkeletonFileGridCard
import com.multidrive.app.presentation.components.SkeletonFileRow
import com.multidrive.app.presentation.viewmodel.ActionState
import com.multidrive.app.presentation.viewmodel.HomeUiState
import com.multidrive.app.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onFileClick: (DriveFile) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Drive, 1: Computers
    var isGridView by remember { mutableStateOf(true) }
    var sortAscending by remember { mutableStateOf(true) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Dialog States
    var selectedFileForRename by remember { mutableStateOf<DriveFile?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var selectedFileForDelete by remember { mutableStateOf<DriveFile?>(null) }
    var sharedLinkToDisplay by remember { mutableStateOf<String?>(null) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // System File Picker for uploading real files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadFileFromUri(it)
        }
    }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetActionState()
            }
            is ActionState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Google Drive-style Top Floating Search Bar (Screenshot 1)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search in Drive", style = MaterialTheme.typography.bodyLarge) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { /* AI Assistant */ }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onNavigateToAccounts) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Accounts",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Sub-header Tabs: My Drive | Computers (Screenshot 1)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Drive", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Computers", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                // Control Bar: Name ↑ Sort & View Toggle Buttons (Screenshot 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { sortAscending = !sortAscending },
                        label = { Text("Name") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Sort",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = { isGridView = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "List View",
                                tint = if (!isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { isGridView = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid View",
                                tint = if (isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Scan / Upload")
                }

                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(6) {
                                SkeletonFileGridCard()
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(6) {
                                SkeletonFileRow()
                                HorizontalDivider()
                            }
                        }
                    }
                }
                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadFiles() }) {
                            Text("Retry")
                        }
                    }
                }
                is HomeUiState.Success -> {
                    val filteredFiles = remember(state.files, searchQuery, sortAscending) {
                        var list = if (searchQuery.isBlank()) state.files
                        else state.files.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        list = if (sortAscending) list.sortedBy { it.name } else list.sortedByDescending { it.name }
                        list
                    }

                    if (filteredFiles.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching files" else "No files found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try a different search query." else "Add a Google Drive account to sync and manage your files.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isBlank()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(onClick = onNavigateToAccounts) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Google Account")
                                }
                            }
                        }
                    } else {
                        if (isGridView) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredFiles) { file ->
                                    FileGridCard(
                                        file = file,
                                        accountEmail = file.accountEmail,
                                        onFileClick = onFileClick,
                                        onRename = {
                                            selectedFileForRename = file
                                            renameInput = file.name
                                        },
                                        onDelete = { selectedFileForDelete = file },
                                        onShare = {
                                            viewModel.shareFile(file) { link ->
                                                sharedLinkToDisplay = link
                                            }
                                        },
                                        onDownload = { /* Download action */ }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredFiles) { file ->
                                    FileRow(
                                        file = file,
                                        accountEmail = file.accountEmail,
                                        onFileClick = onFileClick,
                                        onRename = {
                                            selectedFileForRename = file
                                            renameInput = file.name
                                        },
                                        onDelete = { selectedFileForDelete = file },
                                        onShare = {
                                            viewModel.shareFile(file) { link ->
                                                sharedLinkToDisplay = link
                                            }
                                        },
                                        onDownload = { /* Download action */ }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB Speed Dial Options Dialog
        if (showFabMenu) {
            AlertDialog(
                onDismissRequest = { showFabMenu = false },
                title = { Text("Create / Add") },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("Upload File") },
                            leadingContent = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showFabMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Create Folder") },
                            leadingContent = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showFabMenu = false
                                showCreateFolderDialog = true
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Add Google Account") },
                            leadingContent = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showFabMenu = false
                                onNavigateToAccounts()
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFabMenu = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Create Folder Dialog
        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createFolder(folderNameInput.trim())
                            showCreateFolderDialog = false
                            folderNameInput = ""
                        },
                        enabled = folderNameInput.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Rename File Dialog
        selectedFileForRename?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedFileForRename = null },
                title = { Text("Rename") },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.renameFile(file, renameInput.trim())
                            selectedFileForRename = null
                        },
                        enabled = renameInput.isNotBlank()
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedFileForRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        selectedFileForDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedFileForDelete = null },
                title = { Text("Delete '${file.name}'?") },
                text = { Text("This will permanently delete the item from Google Drive.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFile(file)
                            selectedFileForDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedFileForDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Share Link Dialog
        sharedLinkToDisplay?.let { link ->
            AlertDialog(
                onDismissRequest = { sharedLinkToDisplay = null },
                title = { Text("Share Link") },
                text = {
                    Column {
                        Text("Link generated for Google Drive file:")
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = link,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Google Drive Link", link))
                            sharedLinkToDisplay = null
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Link")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sharedLinkToDisplay = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
