package com.multidrive.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multidrive.app.data.local.entity.UploadTaskEntity
import com.multidrive.app.domain.model.DriveFile
import com.multidrive.app.domain.repository.AccountRepository
import com.multidrive.app.domain.repository.SettingsRepository
import com.multidrive.app.domain.repository.UploadRepository
import com.multidrive.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val files: List<DriveFile>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

sealed interface ActionState {
    data object Idle : ActionState
    data class Success(val message: String) : ActionState
    data class Error(val message: String) : ActionState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listFilesUseCase: ListFilesUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val shareFileUseCase: ShareFileUseCase,
    private val selectTargetAccountUseCase: SelectTargetAccountUseCase,
    private val accountRepository: AccountRepository,
    private val uploadRepository: UploadRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

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

    fun createFolder(folderName: String, parentId: String? = null) {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().first()
            if (accounts.isEmpty()) {
                _actionState.value = ActionState.Error("No connected Google Drive account")
                return@launch
            }

            val mode = settingsRepository.getRoutingMode().first()
            val targetAccount = selectTargetAccountUseCase.execute(accounts, mode, 0L)
                ?: accounts.first()

            val result = createFolderUseCase(
                accountId = targetAccount.id,
                folderName = folderName,
                parentId = parentId
            )

            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Folder '$folderName' created successfully on ${targetAccount.email}")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to create folder")
                }
            )
        }
    }

    fun renameFile(file: DriveFile, newName: String) {
        viewModelScope.launch {
            val result = renameFileUseCase(file.accountId, file.driveFileId, newName)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Renamed to '$newName'")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to rename file")
                }
            )
        }
    }

    fun deleteFile(file: DriveFile) {
        viewModelScope.launch {
            val result = deleteFileUseCase(file.accountId, file.driveFileId)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Deleted '${file.name}' from Google Drive")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete file")
                }
            )
        }
    }

    fun shareFile(file: DriveFile, onLinkReady: (String) -> Unit) {
        viewModelScope.launch {
            val result = shareFileUseCase(file.accountId, file.driveFileId)
            result.fold(
                onSuccess = { link ->
                    onLinkReady(link)
                    _actionState.value = ActionState.Success("Share link created")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to share file")
                }
            )
        }
    }

    fun uploadFileFromUri(uri: Uri, parentId: String? = null) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                var fileName = "uploaded_file"
                var fileSize = 0L

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                // Copy Uri content to temp file for worker
                val tempFile = File(context.cacheDir, "upload_$fileName")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val accounts = accountRepository.getAllAccounts().first()
                if (accounts.isEmpty()) {
                    _actionState.value = ActionState.Error("No connected Google Drive accounts to upload to")
                    return@launch
                }

                val mode = settingsRepository.getRoutingMode().first()
                val targetAccount = selectTargetAccountUseCase.execute(accounts, mode, fileSize)
                    ?: accounts.first()

                val task = UploadTaskEntity(
                    localPath = tempFile.absolutePath,
                    fileName = fileName,
                    mimeType = mimeType,
                    size = fileSize,
                    accountId = targetAccount.id,
                    parentDriveFolderId = parentId,
                    status = "pending",
                    progress = 0f,
                    createdAt = System.currentTimeMillis()
                )

                uploadRepository.insertTask(task)
                _actionState.value = ActionState.Success("Upload queued for '${targetAccount.email}'")
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Failed to queue upload")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}
