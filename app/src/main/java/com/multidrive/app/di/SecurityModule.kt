package com.multidrive.app.di

import android.content.Context
import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.auth.GoogleAuthManager
import com.multidrive.app.data.auth.SecureTokenStore
import com.multidrive.app.data.local.dao.AccountDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecureTokenStore(@ApplicationContext context: Context): SecureTokenStore {
        return SecureTokenStore(context)
    }

    @Provides
    @Singleton
    fun provideGoogleAuthManager(
        @ApplicationContext context: Context,
        secureTokenStore: SecureTokenStore
    ): GoogleAuthManager {
        return GoogleAuthManager(context, secureTokenStore)
    }

    @Provides
    @Singleton
    fun provideDriveAuthorizationManager(
        @ApplicationContext context: Context,
        secureTokenStore: SecureTokenStore,
        accountDao: AccountDao,
        okHttpClient: OkHttpClient
    ): DriveAuthorizationManager {
        return DriveAuthorizationManager(context, secureTokenStore, accountDao, okHttpClient)
    }
}
