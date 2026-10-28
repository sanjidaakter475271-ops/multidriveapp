package com.multidrive.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["email"], unique = true)] // BUG-028 FIX: Unique constraint on email
)
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
    val sortOrder: Int,
    val refreshToken: String? = null,
    // BUG-010 FIX: Removed accessToken — only store in SecureTokenStore (encrypted)
    // val accessToken: String? = null,  // REMOVED: Was stored in plain text in Room DB
    val tokenExpiresAt: Long = 0L,
    val googleSubjectId: String? = null,
    val status: String = "CONNECTED",
    val lastError: String? = null
) {
    val isTokenExpired: Boolean
        get() = tokenExpiresAt > 0 && System.currentTimeMillis() >= tokenExpiresAt
}
