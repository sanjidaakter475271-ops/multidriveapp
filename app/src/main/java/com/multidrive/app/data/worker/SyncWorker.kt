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
        val accounts = accountRepository.getAllAccounts().first()
        var hasErrors = false

        for (account in accounts) {
            if (!account.isActive) continue
            try {
                val syncResult = syncAccountUseCase(account.id)
                val quotaResult = refreshQuotaUseCase(account.id)

                if (syncResult.isFailure || quotaResult.isFailure) {
                    val err = syncResult.exceptionOrNull()?.message ?: quotaResult.exceptionOrNull()?.message
                    accountRepository.updateAccount(account.copy(
                        status = "REAUTH_REQUIRED",
                        lastError = err
                    ))
                    hasErrors = true
                } else {
                    accountRepository.updateAccount(account.copy(
                        status = "CONNECTED",
                        lastError = null,
                        lastSynced = System.currentTimeMillis()
                    ))
                }
            } catch (e: Exception) {
                hasErrors = true
                accountRepository.updateAccount(account.copy(
                    status = "ERROR",
                    lastError = e.localizedMessage
                ))
            }
        }
        return if (hasErrors) Result.retry() else Result.success()
    }
}
