package com.multidrive.app.presentation.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.multidrive.app.domain.model.Account
import com.multidrive.app.presentation.viewmodel.AddAccountState
import com.multidrive.app.presentation.viewmodel.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = hiltViewModel(),
    onAddAccountClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val addState by viewModel.addAccountState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(addState) {
        when (val state = addState) {
            is AddAccountState.Success -> {
                showAddDialog = false
                emailInput = ""
                nameInput = ""
                tokenInput = ""
                snackbarHostState.showSnackbar("Google Account '${state.account.email}' added successfully!")
                viewModel.resetAddAccountState()
            }
            is AddAccountState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Google Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Google Drive") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (accounts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Google Accounts Connected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect one or more Google Drive accounts to manage all your files in one place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Google Drive Account")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(accounts) { account ->
                        AccountItemRow(
                            account = account,
                            onDelete = { viewModel.removeAccount(account) }
                        )
                    }
                }
            }
        }

        // Add Account Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (addState !is AddAccountState.Loading) {
                        showAddDialog = false
                    }
                },
                title = { Text("Connect Google Drive") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Enter your Google account email and OAuth Access Token to connect to Google Drive REST API.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Google Account Email") },
                            placeholder = { Text("user@gmail.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Label (Optional)") },
                            placeholder = { Text("Work Drive / Personal Drive") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("Google Drive OAuth Access Token") },
                            placeholder = { Text("ya29.a0...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (addState is AddAccountState.Loading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Verifying with Google Drive API...")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addAccount(
                                email = emailInput.trim(),
                                displayName = nameInput.trim(),
                                accessToken = tokenInput.trim()
                            )
                        },
                        enabled = emailInput.isNotBlank() && tokenInput.isNotBlank() && addState !is AddAccountState.Loading
                    ) {
                        Text("Connect Account")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddDialog = false },
                        enabled = addState !is AddAccountState.Loading
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AccountItemRow(
    account: Account,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.displayName, style = MaterialTheme.typography.titleMedium)
                Text(text = account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                val usedGB = account.storageUsed.toDouble() / (1024.0 * 1024 * 1024)
                val totalGB = if (account.storageQuota > 0) account.storageQuota.toDouble() / (1024.0 * 1024 * 1024) else 15.0
                Text(
                    text = "${"%.2f".format(usedGB)} GB / ${"%.1f".format(totalGB)} GB used",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Account", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
