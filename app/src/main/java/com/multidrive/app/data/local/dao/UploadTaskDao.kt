package com.multidrive.app.data.local.dao

import androidx.room.*
import com.multidrive.app.data.local.entity.UploadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadTaskDao {
    @Query("SELECT * FROM upload_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<UploadTaskEntity>>

    @Query("SELECT * FROM upload_tasks WHERE status IN ('pending', 'uploading') ORDER BY createdAt ASC")
    fun getPendingTasks(): Flow<List<UploadTaskEntity>>

    @Query("SELECT * FROM upload_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): UploadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: UploadTaskEntity): Long

    @Update
    suspend fun updateTask(task: UploadTaskEntity)

    @Query("DELETE FROM upload_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)
}
