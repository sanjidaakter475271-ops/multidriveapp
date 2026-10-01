package com.multidrive.app.data.local.dao

import androidx.room.*
import com.multidrive.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    // Suspend version for DocumentsProvider (non-Flow context)
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC")
    suspend fun getAllAccountsSuspend(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Int): AccountEntity?

    @Query("SELECT * FROM accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: Int)

    @Query("UPDATE accounts SET storageUsed = :used, storageQuota = :quota WHERE id = :accountId")
    suspend fun updateQuota(accountId: Int, used: Long, quota: Long)
}
