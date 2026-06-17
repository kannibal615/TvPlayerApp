package com.smartvision.svplayer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val settings = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerSettings())

    fun setDisplaySize(value: String) = viewModelScope.launch { repository.setDisplaySize(value) }
    fun setAnimationsEnabled(value: Boolean) = viewModelScope.launch { repository.setAnimationsEnabled(value) }
    fun setVideoRatio(value: String) = viewModelScope.launch { repository.setVideoRatio(value) }
    fun setBufferMode(value: String) = viewModelScope.launch { repository.setBufferMode(value) }
    fun setRetryEnabled(value: Boolean) = viewModelScope.launch { repository.setRetryEnabled(value) }
    fun clearLocalData() = viewModelScope.launch { repository.clearLocalData() }
}
