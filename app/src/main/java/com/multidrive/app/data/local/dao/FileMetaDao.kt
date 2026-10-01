package com.multidrive.app.data.local.dao

import androidx.room.*
import com.multidrive.app.data.local.entity.FileMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileMetaDao {
    @Query("SELECT * FROM files WHERE trashed = 0 ORDER BY modifiedTime DESC")
    fun getAllFiles(): Flow<List<FileMetaEntity>>

    @Query("SELECT * FROM files WHERE parentId IS NULL AND trashed = 0 ORDER BY isFolder DESC, name ASC")
    fun getRootFiles(): Flow<List<FileMetaEntity>>

    @Query("SELECT * FROM files WHERE parentId IS NULL AND accountId = :accountId AND trashed = 0 ORDER BY isFolder DESC, name ASC")
    suspend fun getRootFilesSuspend(accountId: Int): List<FileMetaEntity>

    @Query("SELECT * FROM files WHERE parentId = :parentId AND trashed = 0 ORDER BY isFolder DESC, name ASC")
    fun getFilesByParent(parentId: String): Flow<List<FileMetaEntity>>

    @Query("SELECT * FROM files WHERE parentId = :parentId AND accountId = :accountId AND trashed = 0 ORDER BY isFolder DESC, name ASC")
    suspend fun getFilesByParentSuspend(parentId: String, accountId: Int): List<FileMetaEntity>

    @Query("SELECT * FROM files WHERE accountId = :accountId AND trashed = 0")
    fun getFilesByAccount(accountId: Int): Flow<List<FileMetaEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' AND trashed = 0")
    fun searchFiles(query: String): Flow<List<FileMetaEntity>>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): FileMetaEntity?

    @Query("SELECT * FROM files WHERE driveFileId = :driveFileId AND accountId = :accountId LIMIT 1")
    suspend fun getFileByDriveId(driveFileId: String, accountId: Int): FileMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileMetaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileMetaEntity>)

    @Update
    suspend fun updateFile(file: FileMetaEntity)

    @Query("UPDATE files SET trashed = 1 WHERE id = :fileId")
    suspend fun trashFile(fileId: Int)

    @Query("DELETE FROM files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: Int)

    @Query("DELETE FROM files WHERE accountId = :accountId")
    suspend fun deleteFilesByAccount(accountId: Int)
}
