package com.multidrive.app.domain.usecase

import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.repository.AccountRepository
import javax.inject.Inject

class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authManager: DriveAuthorizationManager,
    private val driveApiService: DriveApiService
) {
    suspend operator fun invoke(
        googleAccountId: String,
        email: String,
        displayName: String,
        photoUrl: String?
    ): Result<Account> {
        return try {
            val existing = accountRepository.getAccountByEmail(email)
            if (existing != null) {
                return Result.failure(Exception("Account already added"))
            }

            val token = authManager.getAccessToken(email)
                ?: return Result.failure(Exception("Failed to obtain OAuth access token"))

            val aboutResponse = driveApiService.getAbout("Bearer $token")
            val quota = aboutResponse.storageQuota?.limit ?: 15_000_000_000L
            val used = aboutResponse.storageQuota?.usage ?: 0L

            val account = Account(
                id = 0,
                googleAccountId = googleAccountId,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                storageQuota = quota,
                storageUsed = used,
                lastSynced = System.currentTimeMillis(),
                isActive = true,
                sortOrder = 0
            )

            val id = accountRepository.insertAccount(account)
            Result.success(account.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
