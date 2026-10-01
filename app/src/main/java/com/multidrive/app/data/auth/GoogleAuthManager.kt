package com.multidrive.app.data.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureTokenStore: SecureTokenStore
) {
    private val driveScope = Scope("https://www.googleapis.com/auth/drive")

    val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(driveScope)
            .build()
    }

    fun getSignInClient() = GoogleSignIn.getClient(context, gso)

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

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
