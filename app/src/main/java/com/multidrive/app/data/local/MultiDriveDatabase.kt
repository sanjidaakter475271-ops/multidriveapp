package com.multidrive.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.multidrive.app.data.local.dao.*
import com.multidrive.app.data.local.entity.*

@Database(
    entities = [
        AccountEntity::class,
        FileMetaEntity::class,
        FolderEntity::class,
        UploadTaskEntity::class,
        AppSettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MultiDriveDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun fileMetaDao(): FileMetaDao
    abstract fun folderDao(): FolderDao
    abstract fun uploadTaskDao(): UploadTaskDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "multidrive.db"

        /**
         * Migration v1 → v2:
         * UploadTaskEntity: renamed sizeBytes→size, added parentDriveFolderId,
         * speedBytesPerSecond, retryCount, updatedAt.
         * Safest approach: drop and recreate (no user-critical data in tasks).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `upload_tasks`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `upload_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `size` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `parentDriveFolderId` TEXT,
                        `status` TEXT NOT NULL,
                        `progress` REAL NOT NULL DEFAULT 0,
                        `uploadedBytes` INTEGER NOT NULL DEFAULT 0,
                        `speedBytesPerSecond` INTEGER NOT NULL DEFAULT 0,
                        `driveFileId` TEXT,
                        `resumableSessionUri` TEXT,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `lastError` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Used by DocumentsProvider which starts before the Hilt graph is ready.
         */
        fun buildManually(context: Context): MultiDriveDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MultiDriveDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
