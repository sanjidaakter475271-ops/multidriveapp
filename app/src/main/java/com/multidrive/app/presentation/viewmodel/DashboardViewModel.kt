package com.multidrive.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val accounts: List<Account>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = accountRepository.getAllAccounts()
        .catch { e ->
            // BUG-014 FIX: Catch errors BEFORE map so upstream errors are handled
            emit(DashboardUiState.Error(e.message ?: "Failed to load dashboard"))
        }
        .map { accounts ->
            DashboardUiState.Success(accounts)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}
