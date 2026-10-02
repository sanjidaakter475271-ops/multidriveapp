package com.multidrive.app.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.multidrive.app.data.auth.GoogleOAuthHelper
import com.multidrive.app.data.auth.OAuthConfig
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
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val addState by viewModel.addAccountState.collectAsState()
    val testMessage by viewModel.testConnectionState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Auth Code (Recommended), 1: Manual Access Token

    // Form inputs
    var authCodeInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var refreshTokenInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(addState) {
        when (val state = addState) {
            is AddAccountState.Success -> {
                showAddDialog = false
                authCodeInput = ""
                emailInput = ""
                nameInput = ""
                tokenInput = ""
                refreshTokenInput = ""
                snackbarHostState.showSnackbar("Google Account '${state.account.email}' connected successfully!")
                viewModel.resetAddAccountState()
            }
            is AddAccountState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
            }
            else -> {}
        }
    }

    LaunchedEffect(testMessage) {
        testMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearTestMessage()
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
                text = { Text("Add Google Drive") },
                modifier = Modifier.navigationBarsPadding()
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
                            onTestConnection = { viewModel.testAccountConnection(account) },
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
                title = { Text("Connect Google Drive Account") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Auth Code") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Access Token") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedTab == 0) {
                            // Tab 0: OAuth Code Flow (response_type=code + access_type=offline)
                        OutlinedButton(
                            onClick = {
                                GoogleOAuthHelper.launchOAuthViaBrowser(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google (Secure Browser)")
                        }

                        Text(
                            text = "Opens Google sign-in via your browser. After approving, you'll be returned automatically. Backend URL: ${OAuthConfig.AUTH_START_URL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                            OutlinedTextField(
                                value = authCodeInput,
                                onValueChange = { authCodeInput = it },
                                label = { Text("Google Authorization Code") },
                                placeholder = { Text("4/0A...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Tab 1: Direct Access Token Entry
                            Text(
                                text = "Enter account details & OAuth Access Token directly:",
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

                            OutlinedTextField(
                                value = refreshTokenInput,
                                onValueChange = { refreshTokenInput = it },
                                label = { Text("OAuth Refresh Token (Optional)") },
                                placeholder = { Text("1//04...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (addState is AddAccountState.Loading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Exchanging token & verifying with Google Drive API...")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                viewModel.addAccountFromAuthCode(
                                    authCode = authCodeInput.trim()
                                )
                            } else {
                                viewModel.addAccount(
                                    email = emailInput.trim(),
                                    displayName = nameInput.trim(),
                                    accessToken = tokenInput.trim(),
                                    refreshToken = refreshTokenInput.trim().ifBlank { null }
                                )
                            }
                        },
                        enabled = (if (selectedTab == 0) authCodeInput.isNotBlank() else (emailInput.isNotBlank() && tokenInput.isNotBlank())) && addState !is AddAccountState.Loading
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
    onTestConnection: () -> Unit,
    onDelete: () -> Unit
) {
    val isConnected = account.status == "CONNECTED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = account.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(text = account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Account", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Indicator (● Connected vs ● Reauth Required)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "Connected" else "Reauth Required",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }

                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val usedGB = account.storageUsed.toDouble() / (1024.0 * 1024 * 1024)
            val totalGB = if (account.storageQuota > 0) account.storageQuota.toDouble() / (1024.0 * 1024 * 1024) else 15.0
            Text(
                text = "Storage: ${"%.2f".format(usedGB)} GB of ${"%.1f".format(totalGB)} GB used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!account.lastError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Error: ${account.lastError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
