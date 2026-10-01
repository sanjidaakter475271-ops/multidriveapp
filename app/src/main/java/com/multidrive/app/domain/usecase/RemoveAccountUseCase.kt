package com.multidrive.app.domain.usecase

import com.multidrive.app.data.auth.GoogleAuthManager
import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.repository.AccountRepository
import javax.inject.Inject

class RemoveAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val googleAuthManager: GoogleAuthManager
) {
    suspend operator fun invoke(account: Account) {
        googleAuthManager.removeAccountToken(account.email)
        accountRepository.deleteAccount(account)
    }
}
