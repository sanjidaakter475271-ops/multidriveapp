package com.multidrive.app.domain.usecase

import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.domain.repository.AccountRepository
import javax.inject.Inject

class ShareFileUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authManager: DriveAuthorizationManager,
    private val driveApiService: DriveApiService
) {
    suspend operator fun invoke(
        accountId: Int,
        driveFileId: String,
        role: String = "reader"
    ): Result<String> {
        return try {
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found"))
            val token = authManager.getAccessToken(account.email)
                ?: return Result.failure(Exception("Auth failed"))
            val auth = "Bearer $token"

            val permBody = mapOf("role" to role, "type" to "anyone")
            driveApiService.createPermission(auth, driveFileId, permBody)

            val fileMeta = driveApiService.getFileMetadata(auth, driveFileId)
            val link = fileMeta.webViewLink
                ?: "https://drive.google.com/file/d/$driveFileId/view"
            Result.success(link)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
