package com.multidrive.app.domain.usecase

import com.multidrive.app.data.local.dao.AccountDao
import com.multidrive.app.data.local.dao.FileMetaDao
import com.multidrive.app.domain.model.DriveFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ListFilesUseCase @Inject constructor(
    private val fileMetaDao: FileMetaDao,
    private val accountDao: AccountDao
) {
    operator fun invoke(parentId: String? = null): Flow<List<DriveFile>> {
        val filesFlow = if (parentId == null) fileMetaDao.getRootFiles() else fileMetaDao.getFilesByParent(parentId)
        val accountsFlow = accountDao.getAllAccounts()

        return combine(filesFlow, accountsFlow) { files, accounts ->
            val accountMap = accounts.associateBy { it.id }
            files.map { entity ->
                val account = accountMap[entity.accountId]
                DriveFile(
                    id = entity.id,
                    driveFileId = entity.driveFileId,
                    name = entity.name,
                    parentId = entity.parentId,
                    mimeType = entity.mimeType,
                    size = entity.size,
                    accountId = entity.accountId,
                    createdTime = entity.createdTime,
                    modifiedTime = entity.modifiedTime,
                    isFolder = entity.isFolder,
                    isStarred = entity.isStarred,
                    trashed = entity.trashed,
                    webViewLink = entity.webViewLink,
                    webContentLink = entity.webContentLink,
                    accountEmail = account?.email
                )
            }
        }
    }
}
