package com.multidrive.app.data.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthorizationOutcome {
    data class Authorized(val authCode: String) : AuthorizationOutcome
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthorizationOutcome
    data class Failed(val message: String) : AuthorizationOutcome
}

@Singleton
class AuthorizationClientHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: AuthorizationClient = Identity.getAuthorizationClient(context)

    /**
     * Builds an authorization request for the Drive scope with offline access.
     * Offline access (refresh token) requires a Web OAuth client ID; throws
     * IllegalStateException when GOOGLE_WEB_CLIENT_ID is not configured.
     */
    fun buildRequest(): AuthorizationRequest {
        val webClientId = OAuthConfig.WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            throw IllegalStateException(
                "GOOGLE_WEB_CLIENT_ID is not configured. Offline access requires a Web OAuth client ID."
            )
        }
        return AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GoogleOAuthHelper.SCOPE)))
            .requestOfflineAccess(webClientId, true)
            .build()
    }

    suspend fun authorize(request: AuthorizationRequest): AuthorizationOutcome = withContext(Dispatchers.IO) {
        val result = try {
            client.authorize(request).await()
        } catch (e: Exception) {
            return@withContext AuthorizationOutcome.Failed(e.message ?: "Authorization failed")
        }
        outcomeFrom(result)
    }

    /**
     * Parses the Google consent popup result delivered via the Activity result registry.
     */
    suspend fun onActivityResult(resultCode: Int, data: Intent?): AuthorizationOutcome = withContext(Dispatchers.IO) {
        if (resultCode != Activity.RESULT_OK) {
            return@withContext AuthorizationOutcome.Failed("Authorization cancelled")
        }
        if (data == null) {
            return@withContext AuthorizationOutcome.Failed("No consent result data")
        }
        val result = try {
            client.getAuthorizationResultFromIntent(data)
        } catch (e: Exception) {
            return@withContext AuthorizationOutcome.Failed(e.message ?: "Consent result handling failed")
        }
        outcomeFrom(result)
    }

    private fun outcomeFrom(result: AuthorizationResult): AuthorizationOutcome {
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
            return if (pendingIntent != null) {
                AuthorizationOutcome.NeedsConsent(pendingIntent)
            } else {
                AuthorizationOutcome.Failed("Resolution required but no pending intent returned")
            }
        }
        val authCode = result.serverAuthCode
        return if (authCode.isNullOrEmpty()) {
            AuthorizationOutcome.Failed("Authorization returned no auth code")
        } else {
            AuthorizationOutcome.Authorized(authCode)
        }
    }

    fun isGooglePlayServicesAvailable(): Boolean {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == CommonStatusCodes.SUCCESS
    }
}
