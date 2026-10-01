package com.multidrive.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [Index(value = ["accountId", "driveFileId"], unique = true)]
)
data class FileMetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val driveFileId: String,
    val name: String,
    val parentId: String?,
    val mimeType: String,
    val size: Long,
    val accountId: Int,
    val createdTime: Long,
    val modifiedTime: Long,
    val isFolder: Boolean,
    val isStarred: Boolean = false,
    val trashed: Boolean = false,
    val webViewLink: String?,
    val webContentLink: String?
)
