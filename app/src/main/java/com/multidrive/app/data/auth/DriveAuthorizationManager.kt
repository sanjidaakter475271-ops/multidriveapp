package com.multidrive.app.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides OAuth access tokens for Drive API calls.
 *
 * Tokens are stored by [SecureTokenStore] after being obtained during sign-in
 * (via Credential Manager in MainActivity). This class retrieves cached tokens;
 * token refresh must be triggered externally via the sign-in flow when a 401 is received.
 */
@Singleton
class DriveAuthorizationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureTokenStore: SecureTokenStore
) {
    suspend fun getAccessToken(accountName: String): String? = withContext(Dispatchers.IO) {
        secureTokenStore.getToken("token_$accountName")
    }

    suspend fun refreshAccessToken(accountName: String): String? = withContext(Dispatchers.IO) {
        // Token refresh requires a new sign-in flow via Credential Manager.
        // Clear stale token so the next getAccessToken call returns null,
        // prompting the caller to trigger re-authentication.
        secureTokenStore.removeToken("token_$accountName")
        null
    }

    fun saveToken(accountName: String, token: String) {
        secureTokenStore.saveToken("token_$accountName", token)
    }

    fun clearToken(accountName: String) {
        secureTokenStore.removeToken("token_$accountName")
    }
}
