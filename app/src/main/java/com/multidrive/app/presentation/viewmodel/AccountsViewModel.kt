package com.multidrive.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multidrive.app.data.auth.AuthorizationClientHelper
import com.multidrive.app.data.auth.AuthorizationOutcome
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

sealed interface AuthorizationEvent {
    data class Consent(val pendingIntent: android.app.PendingIntent) : AuthorizationEvent
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val addAccountUseCase: AddAccountUseCase,
    private val removeAccountUseCase: RemoveAccountUseCase,
    private val syncAccountUseCase: SyncAccountUseCase,
    private val refreshQuotaUseCase: RefreshQuotaUseCase,
    private val authManager: DriveAuthorizationManager,
    private val authClientHelper: AuthorizationClientHelper
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addAccountState = MutableStateFlow<AddAccountState>(AddAccountState.Idle)
    val addAccountState: StateFlow<AddAccountState> = _addAccountState.asStateFlow()

    private val _testConnectionState = MutableStateFlow<String?>(null)
    val testConnectionState: StateFlow<String?> = _testConnectionState.asStateFlow()

    private val _authorizationEvent = MutableStateFlow<AuthorizationEvent?>(null)
    val authorizationEvent: StateFlow<AuthorizationEvent?> = _authorizationEvent.asStateFlow()

    fun requestAuthorization() {
        viewModelScope.launch {
            _addAccountState.value = AddAccountState.Loading
            val outcome = try {
                authClientHelper.authorize(authClientHelper.buildRequest())
            } catch (e: Exception) {
                AuthorizationOutcome.Failed(e.message ?: "Authorization failed")
            }
            when (outcome) {
                is AuthorizationOutcome.Authorized -> {
                    _authorizationEvent.value = null
                    completeFromAuthCode(outcome.authCode)
                }
                is AuthorizationOutcome.NeedsConsent -> {
                    _authorizationEvent.value = AuthorizationEvent.Consent(outcome.pendingIntent)
                }
                is AuthorizationOutcome.Failed -> {
                    _authorizationEvent.value = null
                    _addAccountState.value = AddAccountState.Error(outcome.message)
                }
            }
        }
    }

    fun handleConsentResult(resultCode: Int, data: android.content.Intent?) {
        viewModelScope.launch {
            val outcome = authClientHelper.onActivityResult(resultCode, data)
            when (outcome) {
                is AuthorizationOutcome.Authorized -> {
                    _authorizationEvent.value = null
                    completeFromAuthCode(outcome.authCode)
                }
                is AuthorizationOutcome.Failed -> {
                    _authorizationEvent.value = null
                    _addAccountState.value = AddAccountState.Error(outcome.message)
                }
                is AuthorizationOutcome.NeedsConsent -> {
                    _authorizationEvent.value = AuthorizationEvent.Consent(outcome.pendingIntent)
                }
            }
        }
    }

    fun isGooglePlayServicesAvailable(): Boolean = authClientHelper.isGooglePlayServicesAvailable()

    fun consumeAuthorizationEvent() {
        _authorizationEvent.value = null
    }

    private suspend fun completeFromAuthCode(authCode: String) {
        val exchangeResult = authManager.exchangeAuthorizationCodeWithoutRedirect(authCode.trim())
        exchangeResult.fold(
            onSuccess = { tokenResp ->
                val tempEmail = "pending_${System.currentTimeMillis()}@gmail.com"
                authManager.saveToken(tempEmail, tokenResp.accessToken, tokenResp.refreshToken)
                val addResult = addAccountUseCase(
                    googleAccountId = tempEmail,
                    email = tempEmail,
                    displayName = "",
                    photoUrl = null,
                    refreshToken = tokenResp.refreshToken,
                    expiresInSeconds = tokenResp.expiresInSeconds
                )
                addResult.fold(
                    onSuccess = { account ->
                        syncAccountUseCase(account.id)
                        _addAccountState.value = AddAccountState.Success(account)
                    },
                    onFailure = { error ->
                        _addAccountState.value = AddAccountState.Error("Failed to add account: ${error.message}")
                    }
                )
            },
            onFailure = { error ->
                _addAccountState.value = AddAccountState.Error(error.message ?: "OAuth Code Exchange failed")
            }
        )
    }

    fun syncNewAccountFromBackend(email: String) {
        viewModelScope.launch {
            _addAccountState.value = AddAccountState.Loading
            val token = authManager.getAccessToken(email)
            if (token.isNullOrBlank()) {
                _addAccountState.value = AddAccountState.Error(
                    "Account '$email' authorized! Enter the access token below to complete setup."
                )
                return@launch
            }
            val result = addAccountUseCase(
                googleAccountId = email,
                email = email,
                displayName = "",
                photoUrl = null
            )
            result.fold(
                onSuccess = { account ->
                    syncAccountUseCase(account.id)
                    _addAccountState.value = AddAccountState.Success(account)
                },
                onFailure = { error ->
                    _addAccountState.value = AddAccountState.Error(error.message ?: "Failed to sync account from backend")
                }
            )
        }
    }

    fun notifyOAuthError(message: String) {
        _addAccountState.value = AddAccountState.Error(message)
    }

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

    fun addAccountFromAuthCode(authCode: String, displayName: String = "") {
        viewModelScope.launch {
            _addAccountState.value = AddAccountState.Loading
            try {
                val exchangeResult = authManager.exchangeAuthorizationCode(authCode = authCode.trim())
                exchangeResult.fold(
                    onSuccess = { tokenResp ->
                        val tempEmail = "pending_${System.currentTimeMillis()}@gmail.com"
                        authManager.saveToken(tempEmail, tokenResp.accessToken, tokenResp.refreshToken)
                        val addResult = addAccountUseCase(
                            googleAccountId = tempEmail,
                            email = tempEmail,
                            displayName = displayName,
                            photoUrl = null,
                            refreshToken = tokenResp.refreshToken,
                            expiresInSeconds = tokenResp.expiresInSeconds
                        )
                        addResult.fold(
                            onSuccess = { account ->
                                syncAccountUseCase(account.id)
                                _addAccountState.value = AddAccountState.Success(account)
                            },
                            onFailure = { error ->
                                _addAccountState.value = AddAccountState.Error("Failed to fetch Google Account info: ${error.message}")
                            }
                        )
                    },
                    onFailure = { error ->
                        _addAccountState.value = AddAccountState.Error(error.message ?: "OAuth Code Exchange failed")
                    }
                )
            } catch (e: Exception) {
                _addAccountState.value = AddAccountState.Error(e.message ?: "Unexpected error during OAuth exchange")
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
