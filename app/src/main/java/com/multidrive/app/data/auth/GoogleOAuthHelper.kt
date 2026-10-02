package com.multidrive.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

/**
 * Launches Google OAuth via the backend relay using Chrome Custom Tabs.
 *
 * WHY NOT PLAIN WEBVIEW:
 *   Google blocks OAuth login inside WebView. Chrome Custom Tabs uses
 *   the device's real Chrome browser session, so Google login works correctly.
 *
 * WHY BACKEND + PROXY:
 *   The OAuth client_secret must never be embedded in the Android app.
 *   The backend holds the secret and performs the authorization_code exchange,
 *   storing tokens server-side before redirecting the app via deep link.
 *
 * FLOW:
 *   1. App calls launchOAuthViaBrowser(context)
 *   2. Chrome Custom Tab opens BACKEND /auth/start
 *   3. Backend redirects to Google OAuth (response_type=code, access_type=offline)
 *   4. Google shows login + consent screen
 *   5. Google redirects to backend callback with ?code=...&state=...
 *   6. Backend exchanges code for access_token + refresh_token at oauth2.googleapis.com/token
 *   7. Backend stores tokens per account, then redirects to multidrive://auth-done?account=email
 *   8. Android deep link opens app; account list refreshes
 */
object GoogleOAuthHelper {
    const val DEFAULT_CLIENT_ID = "1053073934306-multidrive.apps.googleusercontent.com"
    const val DEFAULT_REDIRECT_URI = "https://developers.google.com/oauthplayground"

    /**
     * Opens Chrome Custom Tabs to the backend OAuth start endpoint.
     * The backend initiates the Google OAuth code flow securely.
     */
    fun launchOAuthViaBrowser(context: Context, backendStartUrl: String = OAuthConfig.AUTH_START_URL) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()

            customTabsIntent.launchUrl(context, Uri.parse(backendStartUrl))
        } catch (e: Exception) {
            // Fallback: plain browser intent if Chrome Custom Tabs not available
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(backendStartUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    /**
     * Builds a direct Google OAuth URL (authorization-code flow) for use
     * if you want the app to start OAuth directly without a backend relay.
     * NOTE: Token exchange still requires a backend to keep client_secret secure.
     */
    fun buildAuthorizationUrl(
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = DEFAULT_REDIRECT_URI
    ): String {
        return Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id", clientId.ifBlank { DEFAULT_CLIENT_ID })
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "https://www.googleapis.com/auth/drive")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent select_account")
            .build()
            .toString()
    }
}
