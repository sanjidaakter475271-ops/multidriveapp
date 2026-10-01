package com.multidrive.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Dashboard : Screen("dashboard")
    data object Accounts : Screen("accounts")
    data object UploadCenter : Screen("upload_center")
    data object Settings : Screen("settings")
    data object Preview : Screen("preview/{fileId}") {
        fun createRoute(fileId: Int) = "preview/$fileId"
    }
}
