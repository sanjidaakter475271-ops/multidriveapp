package com.multidrive.app.data.auth

/**
 * Central configuration for Google OAuth via Backend + Reverse Proxy.
 *
 * Architecture:
 *   App → Chrome Custom Tabs → BACKEND_BASE_URL/auth/start
 *        → Google OAuth consent (response_type=code, access_type=offline)
 *        → Google redirects to BACKEND_CALLBACK_URL
 *        → Backend exchanges code for access_token + refresh_token
 *        → Backend redirects to multidrive://auth-done?account=<email>
 *        → App receives deep link, refreshes account list
 *
 * Client Secret is ONLY in the backend — never in the app.
 */
object OAuthConfig {
    /**
     * Base URL of your backend server.
     * Replace with your real domain (Cloudflare Tunnel, DuckDNS, etc.)
     */
    const val BACKEND_BASE_URL = "https://multidrive-auth.example.com"

    /** Backend endpoint that starts the Google OAuth flow */
    const val AUTH_START_URL = "$BACKEND_BASE_URL/auth/start"

    /**
     * Deep link scheme/host that the backend redirects to after OAuth completes.
     * AndroidManifest must declare intent-filter for this scheme+host.
     */
    const val DEEP_LINK_SCHEME = "multidrive"
    const val DEEP_LINK_HOST = "auth-done"
    const val DEEP_LINK_URI = "multidrive://auth-done"

    /**
     * Query params returned in the deep link by the backend.
     * e.g. multidrive://auth-done?account=user@gmail.com&status=ok
     */
    const val PARAM_ACCOUNT = "account"
    const val PARAM_STATUS = "status"
    const val PARAM_ERROR = "error"
}
