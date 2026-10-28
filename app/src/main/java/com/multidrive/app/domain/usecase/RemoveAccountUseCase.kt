package com.multidrive.app.domain.usecase

import android.util.Log
import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.auth.GoogleAuthManager
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class RemoveAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val googleAuthManager: GoogleAuthManager,
    private val authManager: DriveAuthorizationManager,
    private val okHttpClient: OkHttpClient
) {
    suspend operator fun invoke(account: Account) = withContext(Dispatchers.IO) {
        // BUG-025 FIX: Revoke token at Google before removing locally
        try {
            val token = authManager.getAccessToken(account.email)
            if (!token.isNullOrEmpty()) {
                val revokeRequest = Request.Builder()
                    .url("https://oauth2.googleapis.com/revoke?token=${token}")
                    .post(okhttp3.RequestBody.create(null, ""))
                    .build()
                okHttpClient.newCall(revokeRequest).execute().close()
                Log.d("RemoveAccount", "Token revoked at Google for ${account.email}")
            }
        } catch (e: Exception) {
            Log.w("RemoveAccount", "Failed to revoke token at Google (non-critical): ${e.message}")
        }

        googleAuthManager.removeAccountToken(account.email)
        authManager.clearToken(account.email)
        accountRepository.deleteAccount(account)
    }
}
