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
        // Explicit generics: without them `map` infers Flow<DashboardUiState.Success>,
        // which makes `catch { emit(DashboardUiState.Error(...)) }` a type mismatch.
        .map<List<Account>, DashboardUiState> { accounts ->
            DashboardUiState.Success(accounts)
        }
        .catch { e ->
            emit(DashboardUiState.Error(e.message ?: "Failed to load dashboard"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}
