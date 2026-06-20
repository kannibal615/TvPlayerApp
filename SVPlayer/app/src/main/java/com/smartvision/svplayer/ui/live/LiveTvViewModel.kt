package com.smartvision.svplayer.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamLiveCategory
import com.smartvision.svplayer.data.models.XtreamLiveStream
import com.smartvision.svplayer.data.repository.XtreamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveTvCategory(
    val id: String,
    val label: String,
    val count: Int?,
    val kind: LiveTvCategoryKind,
)

enum class LiveTvCategoryKind {
    France,
    Sport,
    Info,
    Cinema,
    Kids,
    Documentary,
    Generic,
}

data class LiveTvChannel(
    val streamId: Int,
    val number: String,
    val logoText: String,
    val logoUrl: String?,
    val name: String,
    val program: String,
    val genre: String,
    val timeRange: String,
    val progress: Float,
    val description: String,
    val nextProgram: String,
    val nextTimeRange: String,
    val streamUrl: String,
    val fallbackStreamUrl: String,
    val quality: String = "HD",
    val isFavorite: Boolean = false,
)

data class LiveTvUiState(
    val categoriesLoading: Boolean = true,
    val channelsLoading: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<LiveTvCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val channels: List<LiveTvChannel> = emptyList(),
    val focusedChannelId: Int? = null,
    val selectedChannelId: Int? = null,
) {
    val selectedCategory: LiveTvCategory?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedChannel: LiveTvChannel?
        get() = channels.firstOrNull { it.streamId == selectedChannelId }

    val focusedChannel: LiveTvChannel?
        get() = channels.firstOrNull { it.streamId == focusedChannelId }
}

class LiveTvViewModel(
    private val xtreamRepository: XtreamRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    private var channelsJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        channelsJob?.cancel()
        viewModelScope.launch {
            _uiState.value = LiveTvUiState(categoriesLoading = true)
            runCatching {
                xtreamRepository.getLiveCategories().map { it.toUiCategory() }
            }.onSuccess { categories ->
                if (categories.isEmpty()) {
                    _uiState.value = LiveTvUiState(
                        categoriesLoading = false,
                        errorMessage = "Aucune categorie Live TV retournee par Xtream.",
                    )
                    return@onSuccess
                }

                _uiState.update {
                    it.copy(
                        categoriesLoading = false,
                        errorMessage = null,
                        categories = categories,
                        selectedCategoryId = categories.first().id,
                    )
                }
                loadChannels(categories.first().id)
            }.onFailure { error ->
                _uiState.value = LiveTvUiState(
                    categoriesLoading = false,
                    errorMessage = error.userMessage("Impossible de charger les categories Xtream."),
                )
            }
        }
    }

    fun selectCategory(category: LiveTvCategory) {
        if (_uiState.value.selectedCategoryId == category.id && _uiState.value.channels.isNotEmpty()) {
            return
        }
        loadChannels(category.id)
    }

    fun focusChannel(channel: LiveTvChannel) {
        _uiState.update { state ->
            state.copy(focusedChannelId = channel.streamId)
        }
    }

    fun activateChannel(channel: LiveTvChannel): Boolean {
        val shouldOpenFullPlayer = _uiState.value.selectedChannelId == channel.streamId
        startPreview(channel)
        return shouldOpenFullPlayer
    }

    fun startPreview(channel: LiveTvChannel) {
        _uiState.update { state ->
            state.copy(
                selectedChannelId = channel.streamId,
                focusedChannelId = channel.streamId,
                errorMessage = null,
            )
        }
    }

    fun toggleFavorite(channel: LiveTvChannel) {
        _uiState.update { state ->
            val updatedChannels = state.channels.map {
                if (it.streamId == channel.streamId) it.copy(isFavorite = !it.isFavorite) else it
            }
            state.copy(channels = updatedChannels)
        }
    }

    fun retryCurrentCategory() {
        val categoryId = _uiState.value.selectedCategoryId
        if (categoryId == null) {
            loadCategories()
        } else {
            loadChannels(categoryId)
        }
    }

    private fun loadChannels(categoryId: String) {
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Live TV"
            _uiState.update { state ->
                state.copy(
                    channelsLoading = true,
                    errorMessage = null,
                    selectedCategoryId = categoryId,
                    channels = emptyList(),
                    focusedChannelId = null,
                    selectedChannelId = null,
                )
            }

            runCatching {
                xtreamRepository.getLiveStreams(categoryId).mapIndexed { index, stream ->
                    stream.toUiChannel(index = index, categoryLabel = categoryLabel, xtreamRepository = xtreamRepository)
                }
            }.onSuccess { channels ->
                _uiState.update { state ->
                    val refreshedCategories = state.categories.map { category ->
                        if (category.id == categoryId) category.copy(count = channels.size) else category
                    }
                    state.copy(
                        channelsLoading = false,
                        categories = refreshedCategories,
                        channels = channels,
                        focusedChannelId = channels.firstOrNull()?.streamId,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        channelsLoading = false,
                        channels = emptyList(),
                        focusedChannelId = null,
                        selectedChannelId = null,
                        errorMessage = error.userMessage("Impossible de charger les chaines Xtream."),
                    )
                }
            }
        }
    }
}

private fun XtreamLiveCategory.toUiCategory(): LiveTvCategory =
    LiveTvCategory(
        id = id,
        label = name,
        count = count,
        kind = name.categoryKind(),
    )

private fun XtreamLiveStream.toUiChannel(
    index: Int,
    categoryLabel: String,
    xtreamRepository: XtreamRepository,
): LiveTvChannel {
    val displayName = name.cleanedChannelName()
    return LiveTvChannel(
        streamId = streamId,
        number = (number.takeIf { it > 0 } ?: (index + 1)).toString().padStart(3, '0'),
        logoText = displayName.logoFallback(),
        logoUrl = streamIcon?.takeIf { it.isNotBlank() },
        name = displayName,
        program = "Direct",
        genre = categoryLabel,
        timeRange = "Live",
        progress = 0f,
        description = "Flux live ${categoryLabel.lowercase()} fourni par Xtream.",
        nextProgram = "EPG non disponible",
        nextTimeRange = "A suivre",
        streamUrl = xtreamRepository.buildLiveStreamUrl(this),
        fallbackStreamUrl = xtreamRepository.buildLiveStreamFallbackUrl(streamId),
    )
}

private fun String.categoryKind(): LiveTvCategoryKind {
    val normalized = lowercase()
    return when {
        "france" in normalized || "french" in normalized -> LiveTvCategoryKind.France
        "sport" in normalized || "foot" in normalized -> LiveTvCategoryKind.Sport
        "info" in normalized || "news" in normalized || "actual" in normalized -> LiveTvCategoryKind.Info
        "cin" in normalized || "movie" in normalized || "film" in normalized -> LiveTvCategoryKind.Cinema
        "kids" in normalized || "jeunesse" in normalized || "enfant" in normalized -> LiveTvCategoryKind.Kids
        "doc" in normalized || "decouverte" in normalized -> LiveTvCategoryKind.Documentary
        else -> LiveTvCategoryKind.Generic
    }
}

private fun String.cleanedChannelName(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" SD", "", ignoreCase = true)
        .trim()
        .ifBlank { "Chaine TV" }

private fun String.logoFallback(): String {
    val compact = replace(Regex("[^A-Za-z0-9+]"), "")
    return when {
        compact.isBlank() -> "TV"
        compact.length <= 5 -> compact.uppercase()
        else -> split(" ", "-", "_")
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .take(3)
            .joinToString("")
            .ifBlank { compact.take(3).uppercase() }
    }
}

private fun Throwable.userMessage(fallback: String): String =
    when (this) {
        is IllegalStateException -> message ?: fallback
        else -> fallback
    }
