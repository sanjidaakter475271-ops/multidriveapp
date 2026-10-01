package com.multidrive.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_tasks")
data class UploadTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val accountId: Int,
    val parentDriveFolderId: String? = null,
    val status: String, // pending, uploading, completed, failed
    val progress: Float = 0f,
    val uploadedBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val driveFileId: String? = null,
    val resumableSessionUri: String? = null,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
