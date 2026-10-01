package com.multidrive.app.domain.model

data class DriveFile(
    val id: Int,
    val driveFileId: String,
    val name: String,
    val parentId: String?,
    val mimeType: String,
    val size: Long,
    val accountId: Int,
    val createdTime: Long,
    val modifiedTime: Long,
    val isFolder: Boolean,
    val isStarred: Boolean,
    val trashed: Boolean,
    val webViewLink: String?,
    val webContentLink: String?,
    val accountEmail: String? = null
)
