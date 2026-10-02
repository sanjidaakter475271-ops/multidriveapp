package com.multidrive.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri

object GoogleOAuthHelper {
    private const val OAUTH_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    const val DEFAULT_CLIENT_ID = "1053073934306-multidrive.apps.googleusercontent.com"
    private const val SCOPE = "https://www.googleapis.com/auth/drive"
    private const val REDIRECT_URI = "https://developers.google.com/oauthplayground"

    /**
     * Launches the Google OAuth Web Consent Flow in the user's browser with prompt=consent select_account
     * to support adding multiple Google accounts seamlessly.
     */
    fun launchGoogleOAuthBrowser(
        context: Context,
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = REDIRECT_URI
    ) {
        val authUri = Uri.parse(OAUTH_AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", clientId.ifBlank { DEFAULT_CLIENT_ID })
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("access_type", "offline") // Request refresh_token for offline long-term access
            .appendQueryParameter("prompt", "consent select_account")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
