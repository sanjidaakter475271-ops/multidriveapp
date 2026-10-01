package com.multidrive.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multidrive.app.data.auth.DriveAuthorizationManager
import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.repository.AccountRepository
import com.multidrive.app.domain.usecase.AddAccountUseCase
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
    private val authManager: DriveAuthorizationManager
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addAccountState = MutableStateFlow<AddAccountState>(AddAccountState.Idle)
    val addAccountState: StateFlow<AddAccountState> = _addAccountState.asStateFlow()

    fun addAccount(
        email: String,
        displayName: String,
        accessToken: String
    ) {
        viewModelScope.launch {
            _addAccountState.value = AddAccountState.Loading
            try {
                // Save access token for this account email
                authManager.saveToken(email, accessToken)

                // Call AddAccountUseCase to fetch about info from Google Drive API
                val result = addAccountUseCase(
                    googleAccountId = email,
                    email = email,
                    displayName = displayName.ifBlank { email },
                    photoUrl = null
                )

                result.fold(
                    onSuccess = { account ->
                        // Sync remote files immediately
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

    fun resetAddAccountState() {
        _addAccountState.value = AddAccountState.Idle
    }

    fun removeAccount(account: Account) {
        viewModelScope.launch {
            removeAccountUseCase(account)
        }
    }
}
