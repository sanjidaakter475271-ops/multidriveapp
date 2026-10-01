package com.multidrive.app.domain.repository

import com.multidrive.app.domain.model.DriveFile
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getAllFiles(): Flow<List<DriveFile>>
    fun getFilesByAccount(accountId: Int): Flow<List<DriveFile>>
    fun getFilesByParent(parentId: String?): Flow<List<DriveFile>>
    suspend fun getFileById(id: Int): DriveFile?
    suspend fun getFileByDriveId(driveFileId: String, accountId: Int): DriveFile?
    suspend fun insertFiles(files: List<DriveFile>)
    suspend fun updateFile(file: DriveFile)
    suspend fun deleteFileLocally(id: Int)
    suspend fun searchFiles(query: String): Flow<List<DriveFile>>
    suspend fun fetchRemoteFiles(accountId: Int, folderId: String? = null): Result<List<DriveFile>>
    suspend fun deleteRemoteFile(accountId: Int, driveFileId: String): Result<Unit>
    suspend fun renameRemoteFile(accountId: Int, driveFileId: String, newName: String): Result<Unit>
}
