package com.multidrive.app.domain.repository

import com.multidrive.app.data.local.entity.UploadTaskEntity
import kotlinx.coroutines.flow.Flow

interface UploadRepository {
    fun getAllTasks(): Flow<List<UploadTaskEntity>>
    fun getPendingTasks(): Flow<List<UploadTaskEntity>>
    suspend fun getTaskById(id: Int): UploadTaskEntity?
    suspend fun insertTask(task: UploadTaskEntity): Long
    suspend fun updateTask(task: UploadTaskEntity)
    suspend fun deleteTask(id: Int)
}
