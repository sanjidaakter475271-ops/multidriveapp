package com.multidrive.app.domain.usecase

import com.multidrive.app.domain.repository.FileRepository
import javax.inject.Inject

class SyncAccountUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(accountId: Int, folderId: String? = null): Result<Unit> {
        return fileRepository.fetchRemoteFiles(accountId, folderId).map { }
    }
}
