package com.multidrive.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multidrive.app.data.local.dao.FileMetaDao
import com.multidrive.app.domain.model.DriveFile
import com.multidrive.app.domain.usecase.ListFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val files: List<DriveFile>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val listFilesUseCase: ListFilesUseCase,
    private val fileMetaDao: FileMetaDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles(parentId: String? = null) {
        viewModelScope.launch {
            try {
                listFilesUseCase(parentId).collect { files ->
                    _uiState.value = HomeUiState.Success(files)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load files")
            }
        }
    }

    fun trashFile(fileId: Int) {
        viewModelScope.launch {
            fileMetaDao.trashFile(fileId)
        }
    }
}
