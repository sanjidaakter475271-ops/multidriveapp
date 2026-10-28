package com.multidrive.app.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the current OAuth access token for the DriveAuthInterceptor.
 * 
 * BUG-013 FIX: This class enables the DriveAuthInterceptor to automatically
 * attach access tokens to all HTTP requests made by OkHttpClient.
 */
@Singleton
class TokenProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var currentToken: String? = null

    /**
     * Update the current token (called after successful authentication).
     */
    fun updateToken(token: String) {
        currentToken = token
    }

    /**
     * Get the current token for the interceptor.
     */
    fun getCurrentToken(): String? = currentToken

    /**
     * Clear the current token (called on logout).
     */
    fun clearToken() {
        currentToken = null
    }
}
