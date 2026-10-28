package com.multidrive.app.di

import com.multidrive.app.data.auth.TokenProvider
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.data.remote.DriveAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenProvider: TokenProvider): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BUG-007 FIX: Use BASIC level for release builds to avoid leaking request/response bodies
            // In production, BODY level exposes file names, sizes, metadata in logcat
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            // BUG-013 FIX: Add DriveAuthInterceptor to automatically attach tokens to requests
            .addInterceptor(DriveAuthInterceptor { tokenProvider.getCurrentToken() })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/drive/v3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDriveApiService(retrofit: Retrofit): DriveApiService {
        return retrofit.create(DriveApiService::class.java)
    }
}
