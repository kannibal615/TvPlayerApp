package com.smartvision.svplayer.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.model.PlaybackRequest
import com.smartvision.svplayer.domain.usecase.BuildPlaybackRequestUseCase
import com.smartvision.svplayer.domain.usecase.SavePlaybackProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val request: PlaybackRequest? = null,
    val error: String? = null,
)

class PlayerViewModel(
    private val kind: PlaybackKind,
    private val id: String,
    private val buildPlaybackRequest: BuildPlaybackRequestUseCase,
    private val savePlaybackProgress: SavePlaybackProgressUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = PlayerUiState(loading = true)
            val request = buildPlaybackRequest(kind, id)
            _uiState.value = if (request == null) {
                PlayerUiState(loading = false, error = "Media introuvable")
            } else {
                PlayerUiState(loading = false, request = request)
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            savePlaybackProgress(kind, id, positionMs, durationMs)
        }
    }
}
