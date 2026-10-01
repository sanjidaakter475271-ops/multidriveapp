package com.multidrive.app.data.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.remote.ResumableUploadClient
import com.multidrive.app.domain.repository.AccountRepository
import com.multidrive.app.domain.repository.FileRepository
import com.multidrive.app.domain.repository.UploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository,
    private val accountRepository: AccountRepository,
    private val fileRepository: FileRepository,
    private val authManager: DriveAuthorizationManager,
    private val uploadClient: ResumableUploadClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("TASK_ID", -1)
        if (taskId == -1) return Result.failure()

        val task = uploadRepository.getTaskById(taskId) ?: return Result.failure()
        val account = accountRepository.getAccountById(task.accountId) ?: return Result.failure()

        try {
            uploadRepository.updateTask(task.copy(status = "uploading"))
            val token = authManager.getAccessToken(account.email) ?: return Result.failure()

            val file = File(task.localPath)
            if (!file.exists()) return Result.failure()

            val driveFileId = uploadClient.uploadFile(
                authorization = "Bearer $token",
                file = file,
                fileName = task.fileName,
                mimeType = task.mimeType,
                parentFolderId = task.parentDriveFolderId,
                onProgress = { progress, speed ->
                    // Progress callback
                }
            )

            if (driveFileId != null) {
                uploadRepository.updateTask(task.copy(status = "completed", progress = 1.0f, driveFileId = driveFileId))
                fileRepository.fetchRemoteFiles(account.id, task.parentDriveFolderId)
                return Result.success(workDataOf("DRIVE_FILE_ID" to driveFileId))
            } else {
                uploadRepository.updateTask(task.copy(status = "failed", lastError = "Upload failed"))
                return Result.failure()
            }
        } catch (e: Exception) {
            uploadRepository.updateTask(task.copy(status = "failed", lastError = e.localizedMessage))
            return Result.failure()
        }
    }
}
