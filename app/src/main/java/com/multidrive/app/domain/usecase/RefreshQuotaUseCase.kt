package com.multidrive.app.domain.usecase

import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.domain.repository.AccountRepository
import javax.inject.Inject

class RefreshQuotaUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authManager: DriveAuthorizationManager,
    private val driveApiService: DriveApiService
) {
    suspend operator fun invoke(accountId: Int): Result<Unit> {
        return try {
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found"))

            val token = authManager.getAccessToken(account.email)
                ?: return Result.failure(Exception("Failed to get token"))

            val about = driveApiService.getAbout("Bearer $token")
            val quota = about.storageQuota?.limit ?: account.storageQuota
            val used = about.storageQuota?.usage ?: account.storageUsed

            accountRepository.updateQuota(accountId, used, quota)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
