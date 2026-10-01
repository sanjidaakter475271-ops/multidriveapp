package com.multidrive.app.data.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.data.remote.DriveApiService
import com.multidrive.app.domain.repository.AccountRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val authManager: DriveAuthorizationManager,
    private val driveApiService: DriveApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val accountId = inputData.getInt("ACCOUNT_ID", -1)
        val driveFileId = inputData.getString("DRIVE_FILE_ID") ?: return@withContext Result.failure()
        val destinationUriString = inputData.getString("DESTINATION_URI") ?: return@withContext Result.failure()

        val account = accountRepository.getAccountById(accountId) ?: return@withContext Result.failure()
        val token = authManager.getAccessToken(account.email) ?: return@withContext Result.failure()

        try {
            val response = driveApiService.downloadFile("Bearer $token", driveFileId)
            val destUri = Uri.parse(destinationUriString)
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                response.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
