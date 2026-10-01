package com.multidrive.app.data.repository

import com.multidrive.app.data.local.dao.UploadTaskDao
import com.multidrive.app.data.local.entity.UploadTaskEntity
import com.multidrive.app.domain.repository.UploadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepositoryImpl @Inject constructor(
    private val uploadTaskDao: UploadTaskDao
) : UploadRepository {

    override fun getAllTasks(): Flow<List<UploadTaskEntity>> {
        return uploadTaskDao.getAllTasks()
    }

    override fun getPendingTasks(): Flow<List<UploadTaskEntity>> {
        return uploadTaskDao.getPendingTasks()
    }

    override suspend fun getTaskById(id: Int): UploadTaskEntity? {
        return uploadTaskDao.getTaskById(id)
    }

    override suspend fun insertTask(task: UploadTaskEntity): Long {
        return uploadTaskDao.insertTask(task)
    }

    override suspend fun updateTask(task: UploadTaskEntity) {
        uploadTaskDao.updateTask(task)
    }

    override suspend fun deleteTask(id: Int) {
        uploadTaskDao.deleteTaskById(id)
    }
}
