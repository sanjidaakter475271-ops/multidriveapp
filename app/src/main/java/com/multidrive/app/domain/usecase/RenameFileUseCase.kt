package com.multidrive.app.domain.usecase

import com.multidrive.app.domain.repository.FileRepository
import javax.inject.Inject

class RenameFileUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(accountId: Int, driveFileId: String, newName: String): Result<Unit> {
        return fileRepository.renameRemoteFile(accountId, driveFileId, newName)
    }
}
