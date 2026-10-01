package com.multidrive.app.domain.usecase

import com.multidrive.app.domain.repository.FileRepository
import javax.inject.Inject

class DeleteFileUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(accountId: Int, driveFileId: String): Result<Unit> {
        return fileRepository.deleteRemoteFile(accountId, driveFileId)
    }
}
