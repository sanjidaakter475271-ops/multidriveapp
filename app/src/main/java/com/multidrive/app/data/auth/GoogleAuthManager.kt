package com.multidrive.app.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google account token storage.
 * Authentication itself is handled by Credential Manager (androidx.credentials)
 * in MainActivity; this class only provides token persistence helpers.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureTokenStore: SecureTokenStore
) {
    fun saveAccountToken(email: String, token: String) {
        secureTokenStore.saveToken("token_$email", token)
    }

    fun getAccountToken(email: String): String? {
        return secureTokenStore.getToken("token_$email")
    }

    fun removeAccountToken(email: String) {
        secureTokenStore.removeToken("token_$email")
    }
}
