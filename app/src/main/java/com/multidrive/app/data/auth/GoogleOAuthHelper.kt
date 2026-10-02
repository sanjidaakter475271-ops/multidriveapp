package com.multidrive.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Handles Google OAuth 2.0 Authorization Code Flow.
 *
 * Direct Flow:
 * 1. Launches Chrome Custom Tabs to Google's real OAuth endpoint:
 *    https://accounts.google.com/o/oauth2/v2/auth
 *    Params: client_id, redirect_uri=https://developers.google.com/oauthplayground,
 *            response_type=code, access_type=offline, prompt=consent select_account
 * 2. User signs into Google and grants Drive permission.
 * 3. Google displays the Authorization Code (4/0A...).
 * 4. User pastes the code into the app.
 * 5. App exchanges code at https://oauth2.googleapis.com/token for access_token + refresh_token.
 */
object GoogleOAuthHelper {
    const val DEFAULT_CLIENT_ID = "1053073934306-multidrive.apps.googleusercontent.com"
    const val DEFAULT_REDIRECT_URI = "https://developers.google.com/oauthplayground"
    const val SCOPE = "https://www.googleapis.com/auth/drive"

    /**
     * Builds the REAL Google OAuth 2.0 URL (response_type=code, access_type=offline).
     */
    fun buildAuthorizationUrl(
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = DEFAULT_REDIRECT_URI
    ): String {
        return Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id", clientId.ifBlank { DEFAULT_CLIENT_ID })
            .appendQueryParameter("redirect_uri", redirectUri.ifBlank { DEFAULT_REDIRECT_URI })
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent select_account")
            .build()
            .toString()
    }

    /**
     * Opens Chrome Custom Tabs directly to Google's real OAuth consent page.
     */
    fun launchGoogleOAuthBrowser(
        context: Context,
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = DEFAULT_REDIRECT_URI
    ) {
        val url = buildAuthorizationUrl(clientId, redirectUri)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }
}
