package com.multidrive.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentFolderId: String?,
    val accountId: Int?,
    val driveFolderId: String?,
    val color: String = "#3b82f6",
    val createdAt: Long,
    val modifiedAt: Long
)
