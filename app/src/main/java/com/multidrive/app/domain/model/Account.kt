package com.multidrive.app.domain.model

data class Account(
    val id: Int = 0,
    val googleAccountId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val storageQuota: Long,
    val storageUsed: Long,
    val lastSynced: Long,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
) {
    val storageAvailable: Long
        get() = if (storageQuota > 0) storageQuota - storageUsed else 0L
}
