package com.multidrive.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.multidrive.app.domain.model.Account
import com.multidrive.app.presentation.accounts.AccountsScreen
import com.multidrive.app.presentation.dashboard.DashboardScreen
import com.multidrive.app.presentation.home.HomeScreen
import com.multidrive.app.presentation.preview.PreviewScreen
import com.multidrive.app.presentation.settings.SettingsScreen
import com.multidrive.app.presentation.upload.UploadCenterScreen
import com.multidrive.app.presentation.viewmodel.AccountsViewModel
import kotlinx.coroutines.launch

private const val ANIM_DURATION = 300

data class DrawerMenuItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    accountsViewModel: AccountsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val accounts by accountsViewModel.accounts.collectAsState(initial = emptyList())

    // Aggregated Storage Calculation across accounts
    val totalUsedBytes = remember(accounts) { accounts.sumOf { it.storageUsed } }
    val totalQuotaBytes = remember(accounts) {
        val sum = accounts.sumOf { it.storageQuota }
        if (sum > 0) sum else 15L * 1024 * 1024 * 1024
    }
    val usedGB = totalUsedBytes.toDouble() / (1024.0 * 1024 * 1024)
    val totalGB = totalQuotaBytes.toDouble() / (1024.0 * 1024 * 1024)
    val storageProgress = (usedGB / totalGB).toFloat().coerceIn(0f, 1f)

    // Sidebar items matching Screenshot 2
    val drawerItems = listOf(
        DrawerMenuItem("Recent", Screen.Home.route, Icons.Outlined.Schedule),
        DrawerMenuItem("Uploads", Screen.UploadCenter.route, Icons.Outlined.Upload),
        DrawerMenuItem("Offline", Screen.Home.route, Icons.Outlined.CheckCircle),
        DrawerMenuItem("Trash", Screen.Home.route, Icons.Outlined.Delete),
        DrawerMenuItem("Accounts", Screen.Accounts.route, Icons.Outlined.AccountCircle),
        DrawerMenuItem("Settings", Screen.Settings.route, Icons.Outlined.Settings),
        DrawerMenuItem("Help & feedback", Screen.Settings.route, Icons.Outlined.HelpOutline)
    )

    // Bottom Navigation Bar items matching Screenshot 1 (Home, Starred, Shared, Files + Dashboard)
    val bottomNavItems = listOf(
        Triple("Home", Screen.Home.route, Icons.Default.Home),
        Triple("Starred", Screen.Home.route, Icons.Default.Star),
        Triple("Shared", Screen.Home.route, Icons.Default.People),
        Triple("Files", Screen.Home.route, Icons.Default.Folder),
        Triple("Dashboard", Screen.Dashboard.route, Icons.Default.Storage)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp)
            ) {
                // Header (Screenshot 2: Google Drive / MultiDrive title)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "MultiDrive",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                val navBackStackEntry = navController.currentBackStackEntryAsState().value
                val currentRoute = navBackStackEntry?.destination?.route

                // Drawer items
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(HorizontalPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider()

                // Storage usage section (Screenshot 2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (storageProgress > 0.9f) Icons.Default.Warning else Icons.Outlined.Cloud,
                            contentDescription = null,
                            tint = if (storageProgress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (storageProgress > 0.9f) "Storage Full" else "Storage Usage",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (storageProgress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { storageProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (storageProgress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${"%.2f".format(usedGB)} GB of ${"%.1f".format(totalGB)} GB used (${accounts.size} Drive accounts)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Dashboard.route)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Get more storage / Dashboard")
                    }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry = navController.currentBackStackEntryAsState().value
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = bottomNavItems.any { it.second == currentRoute }

                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { (label, route, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                selected = currentRoute == route,
                                onClick = {
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(ANIM_DURATION)
                    ) + fadeIn(animationSpec = tween(ANIM_DURATION))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(ANIM_DURATION)
                    ) + fadeOut(animationSpec = tween(ANIM_DURATION))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(ANIM_DURATION)
                    ) + fadeIn(animationSpec = tween(ANIM_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(ANIM_DURATION)
                    ) + fadeOut(animationSpec = tween(ANIM_DURATION))
                }
            ) {
                composable(
                    Screen.Home.route,
                    enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                    exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
                ) {
                    HomeScreen(
                        viewModel = hiltViewModel(),
                        onFileClick = { file ->
                            navController.navigate(Screen.Preview.createRoute(file.id))
                        },
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        },
                        onNavigateToAccounts = {
                            navController.navigate(Screen.Accounts.route)
                        },
                        onNavigateToUpload = {
                            navController.navigate(Screen.UploadCenter.route)
                        }
                    )
                }

                composable(
                    Screen.Dashboard.route,
                    enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                    exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
                ) {
                    DashboardScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) }
                    )
                }

                composable(
                    Screen.Settings.route,
                    enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                    exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
                ) {
                    SettingsScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                        onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                        onNavigateToUploadCenter = { navController.navigate(Screen.UploadCenter.route) }
                    )
                }

                composable(Screen.Accounts.route) {
                    AccountsScreen(
                        viewModel = hiltViewModel(),
                        onAddAccountClick = { /* Launch Google Sign-In */ },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.UploadCenter.route) {
                    UploadCenterScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Preview.route,
                    arguments = listOf(navArgument("fileId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val fileId = backStackEntry.arguments?.getInt("fileId") ?: 0
                    PreviewScreen(
                        fileName = "File #$fileId",
                        mimeType = "application/octet-stream",
                        webContentLink = null,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private val HorizontalPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
