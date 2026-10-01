package com.multidrive.app.domain.usecase

import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.domain.repository.AccountRepository
import com.multidrive.app.domain.repository.FileRepository
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authManager: DriveAuthorizationManager,
    private val driveApiService: DriveApiService,
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(
        accountId: Int,
        folderName: String,
        parentId: String? = null
    ): Result<Unit> {
        return try {
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found"))
            val token = authManager.getAccessToken(account.email)
                ?: return Result.failure(Exception("Auth failed"))

            val body = buildMap<String, Any> {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
                if (parentId != null) put("parents", listOf(parentId))
            }

            driveApiService.createFile("Bearer $token", body)
            fileRepository.fetchRemoteFiles(accountId, parentId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
