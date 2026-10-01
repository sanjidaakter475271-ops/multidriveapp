package com.multidrive.app.domain.usecase

import com.multidrive.app.domain.model.DriveFile
import com.multidrive.app.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(query: String): Flow<List<DriveFile>> {
        return fileRepository.searchFiles(query)
    }
}
