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
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `refreshToken` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `accessToken` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `tokenExpiresAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `googleSubjectId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'CONNECTED'")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `lastError` TEXT DEFAULT NULL")
            }
        }

        fun buildManually(context: Context): MultiDriveDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MultiDriveDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
