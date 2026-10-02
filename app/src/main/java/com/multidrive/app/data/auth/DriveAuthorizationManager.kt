package com.multidrive.app.data.auth

import android.content.Context
import com.multidrive.app.data.local.dao.AccountDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveAuthorizationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureTokenStore: SecureTokenStore,
    private val accountDao: AccountDao,
    private val okHttpClient: OkHttpClient
) {
    /**
     * Gets a valid OAuth access token for an account email.
     * If the current access token is missing or expired, attempts automatic refresh
     * using the saved refresh_token.
     */
    suspend fun getAccessToken(accountEmail: String): String? = withContext(Dispatchers.IO) {
        val account = accountDao.getAccountByEmail(accountEmail)
        val tokenFromStore = secureTokenStore.getToken("token_$accountEmail")

        if (account != null && account.refreshToken != null && (tokenFromStore.isNullOrEmpty() || account.isTokenExpired)) {
            // Attempt automatic token refresh using refreshToken
            val refreshedToken = refreshAccessTokenWithRefreshToken(account.email, account.refreshToken)
            if (!refreshedToken.isNullOrEmpty()) {
                return@withContext refreshedToken
            }
        }

        tokenFromStore ?: account?.accessToken
    }

    /**
     * Calls Google OAuth token endpoint (https://oauth2.googleapis.com/token) with grant_type=refresh_token
     * to obtain a new access_token.
     */
    suspend fun refreshAccessTokenWithRefreshToken(
        accountEmail: String,
        refreshToken: String,
        clientId: String = GoogleOAuthHelper.DEFAULT_CLIENT_ID,
        clientSecret: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val formBodyBuilder = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)

            if (clientSecret.isNotBlank()) {
                formBodyBuilder.add("client_secret", clientSecret)
            }

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBodyBuilder.build())
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                val newAccessToken = json.getString("access_token")
                val expiresInSeconds = json.optLong("expires_in", 3600L)
                val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)

                // Save new token in secure store and DB
                secureTokenStore.saveToken("token_$accountEmail", newAccessToken)
                accountDao.getAccountByEmail(accountEmail)?.let { acc ->
                    accountDao.updateAccount(acc.copy(
                        accessToken = newAccessToken,
                        tokenExpiresAt = expiresAt,
                        status = "CONNECTED",
                        lastError = null
                    ))
                }
                response.close()
                newAccessToken
            } else {
                response.close()
                // Mark account status as REAUTH_REQUIRED
                accountDao.getAccountByEmail(accountEmail)?.let { acc ->
                    accountDao.updateAccount(acc.copy(
                        status = "REAUTH_REQUIRED",
                        lastError = "Refresh token expired or revoked"
                    ))
                }
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveToken(accountName: String, token: String, refreshToken: String? = null) {
        secureTokenStore.saveToken("token_$accountName", token)
        if (refreshToken != null) {
            secureTokenStore.saveToken("refresh_$accountName", refreshToken)
        }
    }

    fun getRefreshToken(accountName: String): String? {
        return secureTokenStore.getToken("refresh_$accountName")
    }

    fun clearToken(accountName: String) {
        secureTokenStore.removeToken("token_$accountName")
        secureTokenStore.removeToken("refresh_$accountName")
    }
}
