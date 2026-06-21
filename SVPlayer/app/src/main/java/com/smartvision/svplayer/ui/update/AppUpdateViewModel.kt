package com.smartvision.svplayer.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.update.AppUpdateException
import com.smartvision.svplayer.data.update.AppUpdateInfo
import com.smartvision.svplayer.data.update.AppUpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppUpdateViewModel(
    private val repository: AppUpdateRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate() {
        if (_uiState.value.checking) return
        viewModelScope.launch {
            _uiState.update { it.copy(checking = true, errorMessage = null) }
            runCatching { repository.checkForUpdate() }
                .onSuccess { update ->
                    _uiState.update {
                        it.copy(
                            checking = false,
                            update = update,
                            dismissedVersionCode = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            checking = false,
                            errorMessage = error.toUserMessage("Verification de mise a jour impossible."),
                        )
                    }
                }
        }
    }

    fun installUpdate(context: Context) {
        val update = _uiState.value.update ?: return
        if (_uiState.value.installing) return
        viewModelScope.launch {
            _uiState.update { it.copy(installing = true, errorMessage = null) }
            runCatching {
                val apk = repository.downloadApk(update)
                repository.openInstaller(apk)
            }.onSuccess {
                _uiState.update { it.copy(installing = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        installing = false,
                        errorMessage = error.toUserMessage("Mise a jour impossible."),
                    )
                }
            }
        }
    }

    fun dismiss() {
        val update = _uiState.value.update ?: return
        if (update.mandatory) return
        _uiState.update { it.copy(dismissedVersionCode = update.versionCode) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class AppUpdateUiState(
    val checking: Boolean = false,
    val installing: Boolean = false,
    val update: AppUpdateInfo? = null,
    val dismissedVersionCode: Int? = null,
    val errorMessage: String? = null,
) {
    val shouldShowDialog: Boolean
        get() = update != null && dismissedVersionCode != update.versionCode
}

private fun Throwable.toUserMessage(default: String): String =
    when (this) {
        is AppUpdateException -> message ?: default
        else -> default
    }

