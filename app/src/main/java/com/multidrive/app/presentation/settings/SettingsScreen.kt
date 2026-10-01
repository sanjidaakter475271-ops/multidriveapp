package com.multidrive.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.multidrive.app.domain.model.RoutingMode
import com.multidrive.app.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToUploadCenter: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val currentMode by viewModel.routingMode.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MultiDrive Settings") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account & Storage Section
            Text(
                text = "Accounts & Storage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            SettingsCategoryItem(
                icon = Icons.Default.AccountCircle,
                title = "Manage Google Accounts",
                subtitle = "Add, remove, or switch Google Drive accounts",
                onClick = onNavigateToAccounts
            )

            SettingsCategoryItem(
                icon = Icons.Default.Storage,
                title = "Storage Dashboard",
                subtitle = "View storage quota across all connected drives",
                onClick = onNavigateToDashboard
            )

            HorizontalDivider()

            // Upload & Sync Section
            Text(
                text = "Upload & Routing Policy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SettingsCategoryItem(
                icon = Icons.Default.AltRoute,
                title = "Upload Routing Mode",
                subtitle = "Current: ${currentMode.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                onClick = { showModeDialog = true }
            )

            SettingsCategoryItem(
                icon = Icons.Default.CloudUpload,
                title = "Upload Center",
                subtitle = "View active and completed uploads",
                onClick = onNavigateToUploadCenter
            )

            HorizontalDivider()

            // App Info Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SettingsCategoryItem(
                icon = Icons.Default.Info,
                title = "MultiDrive Version",
                subtitle = "1.0.0 (Unified Google Drive Manager)",
                onClick = {}
            )
        }

        if (showModeDialog) {
            AlertDialog(
                onDismissRequest = { showModeDialog = false },
                title = { Text("Select Upload Routing Policy") },
                text = {
                    Column {
                        RoutingMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setRoutingMode(mode)
                                        showModeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentMode == mode,
                                    onClick = {
                                        viewModel.setRoutingMode(mode)
                                        showModeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mode.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = when (mode) {
                                            RoutingMode.MOST_AVAILABLE -> "Routes uploads to account with most free space"
                                            RoutingMode.ROUND_ROBIN -> "Distributes uploads evenly across accounts"
                                            RoutingMode.PRIORITY -> "Fills accounts in order of sort priority"
                                            RoutingMode.MANUAL -> "Always asks which account to upload to"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCategoryItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
