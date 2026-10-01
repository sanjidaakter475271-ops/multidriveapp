package com.multidrive.app.di

import android.content.Context
import androidx.room.Room
import com.multidrive.app.data.local.MultiDriveDatabase
import com.multidrive.app.data.local.MultiDriveDatabase.Companion.MIGRATION_1_2
import com.multidrive.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MultiDriveDatabase {
        return Room.databaseBuilder(
            context,
            MultiDriveDatabase::class.java,
            MultiDriveDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides fun provideAccountDao(db: MultiDriveDatabase): AccountDao = db.accountDao()
    @Provides fun provideFileMetaDao(db: MultiDriveDatabase): FileMetaDao = db.fileMetaDao()
    @Provides fun provideFolderDao(db: MultiDriveDatabase): FolderDao = db.folderDao()
    @Provides fun provideUploadTaskDao(db: MultiDriveDatabase): UploadTaskDao = db.uploadTaskDao()
    @Provides fun provideSettingsDao(db: MultiDriveDatabase): SettingsDao = db.settingsDao()
}
