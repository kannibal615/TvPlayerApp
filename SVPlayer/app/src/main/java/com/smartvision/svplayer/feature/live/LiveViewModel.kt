package com.smartvision.svplayer.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val channels: List<LiveChannel> = emptyList(),
    val selectedChannelId: Int? = null,
) {
    val selectedChannel: LiveChannel?
        get() = channels.firstOrNull { it.streamId == selectedChannelId } ?: channels.firstOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedChannelId = MutableStateFlow<Int?>(null)

    val uiState = combine(
        repository.observeLiveCategories(),
        selectedCategoryId.flatMapLatest { repository.observeLiveChannels(it) },
        selectedCategoryId,
        selectedChannelId,
    ) { categories, channels, categoryId, channelId ->
        LiveUiState(
            categories = categories,
            selectedCategoryId = categoryId ?: categories.firstOrNull()?.id,
            channels = channels,
            selectedChannelId = channelId ?: channels.firstOrNull()?.streamId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveUiState())

    init {
        viewModelScope.launch {
            val categories = repository.observeLiveCategories().filter { it.isNotEmpty() }.first()
            selectedCategoryId.value = categories.first().id
        }
    }

    fun selectCategory(category: Category) {
        selectedCategoryId.value = category.id
        selectedChannelId.value = null
    }

    fun selectChannel(channel: LiveChannel) {
        selectedChannelId.value = channel.streamId
    }

    fun toggleFavorite(channel: LiveChannel) {
        viewModelScope.launch {
            repository.toggleFavorite("live", channel.streamId.toString())
        }
    }
}
