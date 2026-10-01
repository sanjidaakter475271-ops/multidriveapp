package com.multidrive.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Starred : Screen("starred")
    data object Shared : Screen("shared")
    data object Files : Screen("files")
    data object Dashboard : Screen("dashboard")
    data object Accounts : Screen("accounts")
    data object UploadCenter : Screen("upload_center")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")
    data object Preview : Screen("preview/{fileId}") {
        fun createRoute(fileId: Int) = "preview/$fileId"
    }
}
