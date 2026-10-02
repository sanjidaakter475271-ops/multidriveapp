package com.multidrive.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.multidrive.app.data.auth.OAuthConfig
import com.multidrive.app.presentation.navigation.AppNavGraph
import com.multidrive.app.presentation.splash.SnapchatSplashScreen
import com.multidrive.app.presentation.theme.MultiDriveTheme
import com.multidrive.app.presentation.viewmodel.AccountsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val accountsViewModel: AccountsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted callback */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRequiredPermissions()

        // Handle deep link that launched this Activity from cold start
        handleOAuthDeepLink(intent)

        setContent {
            MultiDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isSplashFinished by remember { mutableStateOf(false) }

                    Crossfade(
                        targetState = isSplashFinished,
                        animationSpec = tween(500),
                        label = "splashCrossfade"
                    ) { finished ->
                        if (!finished) {
                            SnapchatSplashScreen(onSplashFinished = { isSplashFinished = true })
                        } else {
                            AppNavGraph()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running (launchMode=singleTask)
        handleOAuthDeepLink(intent)
    }

    /**
     * Handles the OAuth deep link: multidrive://auth-done?account=<email>&status=ok
     * Sent by the backend after successful token exchange.
     */
    private fun handleOAuthDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == OAuthConfig.DEEP_LINK_SCHEME && data.host == OAuthConfig.DEEP_LINK_HOST) {
            val accountEmail = data.getQueryParameter(OAuthConfig.PARAM_ACCOUNT)
            val status = data.getQueryParameter(OAuthConfig.PARAM_STATUS)
            val error = data.getQueryParameter(OAuthConfig.PARAM_ERROR)

            if (!accountEmail.isNullOrBlank() && status == "ok") {
                // Notify AccountsViewModel to sync the newly added account from backend
                accountsViewModel.syncNewAccountFromBackend(accountEmail)
            } else if (!error.isNullOrBlank()) {
                accountsViewModel.notifyOAuthError("OAuth failed: $error")
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
