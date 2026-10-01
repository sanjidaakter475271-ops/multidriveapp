package com.multidrive.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val googleAccountId: String,
    val email: String,
    val accountName: String,
    val avatarUrl: String?,
    val storageQuota: Long,
    val storageUsed: Long,
    val lastSynced: Long,
    val isActive: Boolean = true,
    val sortOrder: Int
)
