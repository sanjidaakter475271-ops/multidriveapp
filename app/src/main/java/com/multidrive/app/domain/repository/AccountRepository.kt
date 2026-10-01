package com.multidrive.app.domain.repository

import com.multidrive.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Int): Account?
    suspend fun getAccountByEmail(email: String): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun updateQuota(id: Int, used: Long, quota: Long)
}
