package com.smartvision.svplayer.ui.appconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.appconfig.AppConfigRepository
import com.smartvision.svplayer.data.appconfig.AppRuntimeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppConfigViewModel(
    private val repository: AppConfigRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppConfigUiState())
    val uiState: StateFlow<AppConfigUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val config = repository.loadConfig()
            val acceptedVersion = repository.acceptedConsentVersion.first()
            _uiState.update {
                it.copy(
                    loading = false,
                    config = config,
                    acceptedConsentVersion = config.acceptedConsentVersion ?: acceptedVersion,
                )
            }
        }
    }

    fun acceptConsent() {
        val version = _uiState.value.config.consent.version
        viewModelScope.launch {
            repository.acceptConsent(version)
            _uiState.update { it.copy(acceptedConsentVersion = version) }
        }
    }
}

data class AppConfigUiState(
    val loading: Boolean = true,
    val config: AppRuntimeConfig = AppRuntimeConfig(),
    val acceptedConsentVersion: String = "",
) {
    val consentRequired: Boolean
        get() = !loading && acceptedConsentVersion != config.consent.version
}
