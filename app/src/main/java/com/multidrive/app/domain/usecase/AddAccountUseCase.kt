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
        photoUrl: String?,
        refreshToken: String? = null,
        expiresInSeconds: Long = 3600L
    ): Result<Account> {
        return try {
            val token = authManager.getAccessToken(email)
                ?: return Result.failure(Exception("Failed to obtain OAuth access token"))

            val aboutResponse = driveApiService.getAbout("Bearer $token")
            val quota = aboutResponse.storageQuota?.limit ?: 15_000_000_000L
            val used = aboutResponse.storageQuota?.usage ?: 0L
            val userEmail = aboutResponse.user?.emailAddress ?: email
            val userDisplayName = aboutResponse.user?.displayName ?: displayName
            val photo = aboutResponse.user?.photoLink ?: photoUrl
            val subjectId = aboutResponse.user?.permissionId ?: googleAccountId

            val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)

            val existing = accountRepository.getAccountByEmail(userEmail)
            val accountToSave = Account(
                id = existing?.id ?: 0,
                googleAccountId = subjectId,
                email = userEmail,
                displayName = userDisplayName.ifBlank { userEmail },
                photoUrl = photo,
                storageQuota = quota,
                storageUsed = used,
                lastSynced = System.currentTimeMillis(),
                isActive = true,
                sortOrder = existing?.sortOrder ?: 0,
                refreshToken = refreshToken ?: existing?.refreshToken,
                accessToken = token,
                tokenExpiresAt = expiresAt,
                googleSubjectId = subjectId,
                status = "CONNECTED",
                lastError = null
            )

            val id = accountRepository.insertAccount(accountToSave)
            Result.success(accountToSave.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
