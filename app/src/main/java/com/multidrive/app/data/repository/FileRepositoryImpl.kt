package com.multidrive.app.data.repository

import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.local.dao.AccountDao
import com.multidrive.app.data.local.dao.FileMetaDao
import com.multidrive.app.data.local.entity.FileMetaEntity
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.data.remote.DriveErrorMapper
import com.multidrive.app.domain.model.DriveFile
import com.multidrive.app.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileMetaDao: FileMetaDao,
    private val accountDao: AccountDao,
    private val driveApiService: DriveApiService,
    private val authManager: DriveAuthorizationManager
) : FileRepository {

    override fun getAllFiles(): Flow<List<DriveFile>> {
        return fileMetaDao.getAllFiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFilesByAccount(accountId: Int): Flow<List<DriveFile>> {
        return fileMetaDao.getFilesByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFilesByParent(parentId: String?): Flow<List<DriveFile>> {
        val flow = if (parentId == null) fileMetaDao.getRootFiles() else fileMetaDao.getFilesByParent(parentId)
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getFileById(id: Int): DriveFile? {
        return fileMetaDao.getFileById(id)?.toDomain()
    }

    override suspend fun getFileByDriveId(driveFileId: String, accountId: Int): DriveFile? {
        return fileMetaDao.getFileByDriveId(driveFileId, accountId)?.toDomain()
    }

    override suspend fun insertFiles(files: List<DriveFile>) {
        fileMetaDao.insertFiles(files.map { it.toEntity() })
    }

    override suspend fun updateFile(file: DriveFile) {
        fileMetaDao.updateFile(file.toEntity())
    }

    override suspend fun deleteFileLocally(id: Int) {
        fileMetaDao.deleteFileById(id)
    }

    override suspend fun searchFiles(query: String): Flow<List<DriveFile>> {
        // BUG-018 FIX: Use case-insensitive search
        val formattedQuery = "%$query%"
        return fileMetaDao.searchFilesCaseInsensitive(formattedQuery).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchRemoteFiles(accountId: Int, folderId: String?): Result<List<DriveFile>> {
        return try {
            val account = accountDao.getAccountById(accountId) ?: return Result.failure(Exception("Account not found"))
            val token = authManager.getAccessToken(account.email) ?: return Result.failure(Exception("Auth failed"))
            val authHeader = "Bearer $token"

            var query = "trashed = false"
            if (folderId != null) {
                query += " and '$folderId' in parents"
            }

            var pageToken: String? = null
            val allRemoteFiles = mutableListOf<FileMetaEntity>()

            // BUG-026 FIX: Implement pagination with nextPageToken for large folders
            do {
                val response = driveApiService.listFiles(
                    authorization = authHeader,
                    query = query,
                    pageSize = 100,
                    pageToken = pageToken,
                    fields = "nextPageToken,files(id,name,mimeType,size,parents,createdTime,modifiedTime,trashed,webViewLink,webContentLink)"
                )

                val remoteFiles = response.files.map { remote ->
                    FileMetaEntity(
                        driveFileId = remote.id,
                        name = remote.name,
                        parentId = remote.parents?.firstOrNull(),
                        mimeType = remote.mimeType,
                        size = remote.size ?: 0L,
                        accountId = accountId,
                        createdTime = System.currentTimeMillis(),
                        modifiedTime = System.currentTimeMillis(),
                        isFolder = remote.mimeType == "application/vnd.google-apps.folder",
                        webViewLink = remote.webViewLink,
                        webContentLink = remote.webContentLink
                    )
                }

                allRemoteFiles.addAll(remoteFiles)
                pageToken = response.nextPageToken
            } while (pageToken != null)

            // BUG-019 FIX: Use REPLACE strategy to merge remote data with local
            // This ensures local metadata is updated with remote state
            fileMetaDao.insertFiles(allRemoteFiles)
            Result.success(allRemoteFiles.map { it.toDomain() })
        } catch (e: Exception) {
            // BUG-029 FIX: Map error using DriveErrorMapper
            Result.failure(DriveErrorMapper.mapException(e))
        }
    }

    override suspend fun deleteRemoteFile(accountId: Int, driveFileId: String): Result<Unit> {
        return try {
            val account = accountDao.getAccountById(accountId) ?: return Result.failure(Exception("Account not found"))
            val token = authManager.getAccessToken(account.email) ?: return Result.failure(Exception("Auth failed"))
            driveApiService.deleteFile("Bearer $token", driveFileId)
            fileMetaDao.getFileByDriveId(driveFileId, accountId)?.let {
                fileMetaDao.deleteFileById(it.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // BUG-029 FIX: Map error using DriveErrorMapper
            Result.failure(DriveErrorMapper.mapException(e))
        }
    }

    override suspend fun renameRemoteFile(accountId: Int, driveFileId: String, newName: String): Result<Unit> {
        return try {
            val account = accountDao.getAccountById(accountId) ?: return Result.failure(Exception("Account not found"))
            val token = authManager.getAccessToken(account.email) ?: return Result.failure(Exception("Auth failed"))
            val body = mapOf("name" to newName)
            driveApiService.updateFile("Bearer $token", driveFileId, body)
            fileMetaDao.getFileByDriveId(driveFileId, accountId)?.let {
                fileMetaDao.updateFile(it.copy(name = newName))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // BUG-029 FIX: Map error using DriveErrorMapper
            Result.failure(DriveErrorMapper.mapException(e))
        }
    }

    private fun FileMetaEntity.toDomain() = DriveFile(
        id = id,
        driveFileId = driveFileId,
        name = name,
        parentId = parentId,
        mimeType = mimeType,
        size = size,
        accountId = accountId,
        createdTime = createdTime,
        modifiedTime = modifiedTime,
        isFolder = isFolder,
        isStarred = isStarred,
        trashed = trashed,
        webViewLink = webViewLink,
        webContentLink = webContentLink
    )

    private fun DriveFile.toEntity() = FileMetaEntity(
        id = id,
        driveFileId = driveFileId,
        name = name,
        parentId = parentId,
        mimeType = mimeType,
        size = size,
        accountId = accountId,
        createdTime = createdTime,
        modifiedTime = modifiedTime,
        isFolder = isFolder,
        isStarred = isStarred,
        trashed = trashed,
        webViewLink = webViewLink,
        webContentLink = webContentLink
    )
}
