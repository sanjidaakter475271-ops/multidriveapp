package com.multidrive.app.data.auth

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveAuthorizationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureTokenStore: SecureTokenStore
) {
    private val scope = "oauth2:https://www.googleapis.com/auth/drive"

    suspend fun getAccessToken(accountName: String): String? = withContext(Dispatchers.IO) {
        try {
            val cachedToken = secureTokenStore.getToken("token_$accountName")
            if (!cachedToken.isNullOrEmpty()) {
                return@withContext cachedToken
            }
            val account = android.accounts.Account(accountName, "com.google")
            val token = GoogleAuthUtil.getToken(context, account, scope)
            if (token != null) {
                secureTokenStore.saveToken("token_$accountName", token)
            }
            token
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun refreshAccessToken(accountName: String): String? = withContext(Dispatchers.IO) {
        try {
            val oldToken = secureTokenStore.getToken("token_$accountName")
            if (oldToken != null) {
                GoogleAuthUtil.clearToken(context, oldToken)
            }
            secureTokenStore.removeToken("token_$accountName")
            val account = android.accounts.Account(accountName, "com.google")
            val newToken = GoogleAuthUtil.getToken(context, account, scope)
            if (newToken != null) {
                secureTokenStore.saveToken("token_$accountName", newToken)
            }
            newToken
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
