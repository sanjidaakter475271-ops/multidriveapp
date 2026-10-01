package com.multidrive.app.data.repository

import com.multidrive.app.data.local.dao.AccountDao
import com.multidrive.app.data.local.entity.AccountEntity
import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(id: Int): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun getAccountByEmail(email: String): Account? {
        return accountDao.getAccountByEmail(email)?.toDomain()
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override suspend fun updateQuota(id: Int, used: Long, quota: Long) {
        accountDao.updateQuota(id, used, quota)
    }

    private fun AccountEntity.toDomain() = Account(
        id = id,
        googleAccountId = googleAccountId,
        email = email,
        displayName = accountName,
        photoUrl = avatarUrl,
        storageQuota = storageQuota,
        storageUsed = storageUsed,
        lastSynced = lastSynced,
        isActive = isActive,
        sortOrder = sortOrder
    )

    private fun Account.toEntity() = AccountEntity(
        id = id,
        googleAccountId = googleAccountId,
        email = email,
        accountName = displayName,
        avatarUrl = photoUrl,
        storageQuota = storageQuota,
        storageUsed = storageUsed,
        lastSynced = lastSynced,
        isActive = isActive,
        sortOrder = sortOrder
    )
}
