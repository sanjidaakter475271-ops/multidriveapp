package com.multidrive.app.domain.repository

import com.multidrive.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFolders(): Flow<List<FolderEntity>>
    suspend fun createFolder(name: String, parentFolderId: String?, accountId: Int?): FolderEntity
    suspend fun deleteFolder(id: Int)
}
