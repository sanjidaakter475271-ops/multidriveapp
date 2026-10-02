package com.multidrive.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.repository.AccountRepository
import com.multidrive.app.domain.usecase.AddAccountUseCase
import com.multidrive.app.domain.usecase.RefreshQuotaUseCase
import com.multidrive.app.domain.usecase.RemoveAccountUseCase
import com.multidrive.app.domain.usecase.SyncAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AddAccountState {
    data object Idle : AddAccountState
    data object Loading : AddAccountState
    data class Success(val account: Account) : AddAccountState
    data class Error(val message: String) : AddAccountState
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val addAccountUseCase: AddAccountUseCase,
    private val removeAccountUseCase: RemoveAccountUseCase,
    private val syncAccountUseCase: SyncAccountUseCase,
    private val refreshQuotaUseCase: RefreshQuotaUseCase,
    private val authManager: DriveAuthorizationManager
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addAccountState = MutableStateFlow<AddAccountState>(AddAccountState.Idle)
    val addAccountState: StateFlow<AddAccountState> = _addAccountState.asStateFlow()

    private val _testConnectionState = MutableStateFlow<String?>(null)
    val testConnectionState: StateFlow<String?> = _testConnectionState.asStateFlow()

    fun addAccount(
        email: String,
        displayName: String,
        accessToken: String,
        refreshToken: String? = null
    ) {
        viewModelScope.launch {
            _addAccountState.value = AddAccountState.Loading
            try {
                authManager.saveToken(email, accessToken, refreshToken)

                val result = addAccountUseCase(
                    googleAccountId = email,
                    email = email,
                    displayName = displayName.ifBlank { email },
                    photoUrl = null,
                    refreshToken = refreshToken
                )

                result.fold(
                    onSuccess = { account ->
                        syncAccountUseCase(account.id)
                        _addAccountState.value = AddAccountState.Success(account)
                    },
                    onFailure = { error ->
                        _addAccountState.value = AddAccountState.Error(error.message ?: "Failed to add account")
                    }
                )
            } catch (e: Exception) {
                _addAccountState.value = AddAccountState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun testAccountConnection(account: Account) {
        viewModelScope.launch {
            _testConnectionState.value = "Testing connection for ${account.email}..."
            val result = refreshQuotaUseCase(account.id)
            result.fold(
                onSuccess = {
                    accountRepository.updateAccount(account.copy(status = "CONNECTED", lastError = null))
                    _testConnectionState.value = "Connection OK for ${account.email}! Quota & Drive API verified."
                },
                onFailure = { error ->
                    accountRepository.updateAccount(account.copy(status = "REAUTH_REQUIRED", lastError = error.message))
                    _testConnectionState.value = "Connection failed for ${account.email}: ${error.message}"
                }
            )
        }
    }

    fun clearTestMessage() {
        _testConnectionState.value = null
    }

    fun resetAddAccountState() {
        _addAccountState.value = AddAccountState.Idle
    }

    fun removeAccount(account: Account) {
        viewModelScope.launch {
            removeAccountUseCase(account)
        }
    }
}
