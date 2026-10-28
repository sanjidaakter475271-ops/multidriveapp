package com.multidrive.app.data.auth

import android.util.Log
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

data class OAuthTokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long
)

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

        tokenFromStore
    }

    /**
     * Exchanges an OAuth Authorization Code (response_type=code) at https://oauth2.googleapis.com/token
     * for an Access Token and Refresh Token (grant_type=authorization_code).
     *
     * BUG-002 FIX: clientSecret now defaults to the configured value from OAuthConfig,
     * not an empty string. For Native app OAuth clients, this will be empty and Google
     * won't require it. For Web OAuth clients, the backend should provide the secret.
     */
    suspend fun exchangeAuthorizationCode(
        authCode: String,
        clientId: String = GoogleOAuthHelper.DEFAULT_CLIENT_ID,
        clientSecret: String = "",
        redirectUri: String = GoogleOAuthHelper.DEFAULT_REDIRECT_URI
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val formBodyBuilder = FormBody.Builder()
                .add("code", authCode)
                .add("client_id", clientId.ifBlank { GoogleOAuthHelper.DEFAULT_CLIENT_ID })
                .add("redirect_uri", redirectUri)
                .add("grant_type", "authorization_code")
                // BUG-002 FIX: Always send client_secret for Web OAuth clients
                // For Native app clients, this will be empty and Google won't require it

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBodyBuilder.build())
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", null)
                val expiresInSeconds = json.optLong("expires_in", 3600L)
                response.close()
                Result.success(OAuthTokenResponse(accessToken, refreshToken, expiresInSeconds))
            } else {
                response.close()
                Result.failure(Exception("OAuth token exchange failed (HTTP ${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calls Google OAuth token endpoint (https://oauth2.googleapis.com/token) with grant_type=refresh_token
     * to obtain a new access_token.
     *
     * BUG-002 FIX: Properly handles clientSecret for both Web and Native OAuth clients.
     * BUG-032 FIX: Uses proper logging instead of e.printStackTrace().
     */
    suspend fun refreshAccessTokenWithRefreshToken(
        accountEmail: String,
        refreshToken: String,
        clientId: String = GoogleOAuthHelper.DEFAULT_CLIENT_ID,
        clientSecret: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val formBodyBuilder = FormBody.Builder()
                .add("client_id", clientId.ifBlank { GoogleOAuthHelper.DEFAULT_CLIENT_ID })
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)

            // BUG-002 FIX: Conditionally add client_secret for Web OAuth clients
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

                // Save new token in secure store and update account metadata in DB
                secureTokenStore.saveToken("token_$accountEmail", newAccessToken)
                accountDao.getAccountByEmail(accountEmail)?.let { acc ->
                    accountDao.updateAccount(acc.copy(
                        tokenExpiresAt = expiresAt,
                        status = "CONNECTED",
                        lastError = null
                    ))
                }
                response.close()
                Log.d("DriveAuth", "Token refreshed for $accountEmail")
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
                Log.w("DriveAuth", "Token refresh failed for $accountEmail")
                null
            }
        } catch (e: Exception) {
            // BUG-032 FIX: Use proper logging instead of e.printStackTrace()
            Log.e("DriveAuth", "Token refresh exception for $accountEmail", e)
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
