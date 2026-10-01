package com.multidrive.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.multidrive.app.domain.repository.AccountRepository
import com.multidrive.app.domain.usecase.RefreshQuotaUseCase
import com.multidrive.app.domain.usecase.SyncAccountUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val syncAccountUseCase: SyncAccountUseCase,
    private val refreshQuotaUseCase: RefreshQuotaUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val accounts = accountRepository.getAllAccounts().first()
            for (account in accounts) {
                syncAccountUseCase(account.id)
                refreshQuotaUseCase(account.id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
