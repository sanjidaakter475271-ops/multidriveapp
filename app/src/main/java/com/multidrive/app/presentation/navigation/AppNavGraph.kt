package com.multidrive.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.multidrive.app.presentation.accounts.AccountsScreen
import com.multidrive.app.presentation.dashboard.DashboardScreen
import com.multidrive.app.presentation.home.HomeScreen
import com.multidrive.app.presentation.preview.PreviewScreen
import com.multidrive.app.presentation.settings.SettingsScreen
import com.multidrive.app.presentation.upload.UploadCenterScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

private const val ANIM_DURATION = 300

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        Triple("Files", Screen.Home.route, Icons.Default.Folder),
        Triple("Dashboard", Screen.Dashboard.route, Icons.Default.Dashboard),
        Triple("Settings", Screen.Settings.route, Icons.Default.Settings)
    )

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
            // Bottom nav tabs — use crossfade instead of slide for tab switches
            composable(
                Screen.Home.route,
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
            ) {
                HomeScreen(
                    viewModel = hiltViewModel(),
                    onFileClick = { file ->
                        navController.navigate(Screen.Preview.createRoute(file.id))
                    }
                )
            }

            composable(
                Screen.Dashboard.route,
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
            ) {
                DashboardScreen(viewModel = hiltViewModel())
            }

            composable(
                Screen.Settings.route,
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
            ) {
                SettingsScreen(viewModel = hiltViewModel())
            }

            // Detail screens — slide in from right
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    viewModel = hiltViewModel(),
                    onAddAccountClick = { /* Launch Google Sign-In from MainActivity */ },
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
