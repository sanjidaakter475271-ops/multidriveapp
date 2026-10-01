package com.multidrive.app.data.repository

import com.multidrive.app.data.local.dao.FolderDao
import com.multidrive.app.data.local.entity.FolderEntity
import com.multidrive.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<FolderEntity>> {
        return folderDao.getAllFolders()
    }

    override suspend fun createFolder(
        name: String,
        parentFolderId: String?,
        accountId: Int?
    ): FolderEntity {
        val now = System.currentTimeMillis()
        val entity = FolderEntity(
            name = name,
            parentFolderId = parentFolderId,
            accountId = accountId,
            driveFolderId = null,
            createdAt = now,
            modifiedAt = now
        )
        val id = folderDao.insertFolder(entity)
        return entity.copy(id = id.toInt())
    }

    override suspend fun deleteFolder(id: Int) {
        folderDao.deleteFolderById(id)
    }
}
