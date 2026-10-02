package com.multidrive.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri

object GoogleOAuthHelper {
    private const val OAUTH_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    const val DEFAULT_CLIENT_ID = "1053073934306-multidrive.apps.googleusercontent.com"
    private const val SCOPE = "https://www.googleapis.com/auth/drive"
    const val DEFAULT_REDIRECT_URI = "https://developers.google.com/oauthplayground"

    /**
     * Builds the Google OAuth 2.0 authorization URL using response_type=code and access_type=offline.
     * This allows obtaining an Authorization Code that exchanges for both Access Token and Refresh Token.
     */
    fun buildAuthorizationUrl(
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = DEFAULT_REDIRECT_URI
    ): String {
        return Uri.parse(OAUTH_AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", clientId.ifBlank { DEFAULT_CLIENT_ID })
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code") // Correct response_type for offline refresh tokens!
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent select_account")
            .build()
            .toString()
    }

    /**
     * Launches the Google OAuth Web Consent Flow in the user's browser.
     */
    fun launchGoogleOAuthBrowser(
        context: Context,
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = DEFAULT_REDIRECT_URI
    ) {
        val url = buildAuthorizationUrl(clientId, redirectUri)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
