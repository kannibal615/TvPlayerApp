package com.smartvision.svplayer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.domain.parental.ParentalCatalogRepository
import com.smartvision.svplayer.domain.parental.ParentalFilterCounts
import com.smartvision.svplayer.domain.parental.ParentalHiddenFolder
import com.smartvision.svplayer.domain.parental.ParentalHiddenItem
import com.smartvision.svplayer.domain.parental.ParentalKeywordPolicy
import com.smartvision.svplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ParentalKeywordError {
    Empty,
    Duplicate,
    TooLong,
}

enum class ParentalFeedback {
    KeywordAdded,
    KeywordUpdated,
    KeywordDeleted,
    PinUpdated,
}

data class ParentalControlUiState(
    val enabled: Boolean = false,
    val pinConfigured: Boolean = false,
    val keywords: List<String> = ParentalKeywordPolicy.DefaultKeywords,
    val draft: String = "",
    val keywordError: ParentalKeywordError? = null,
    val feedback: ParentalFeedback? = null,
    val counts: ParentalFilterCounts = ParentalFilterCounts(),
    val folders: List<ParentalHiddenFolder> = emptyList(),
    val items: List<ParentalHiddenItem> = emptyList(),
    val foldersLoading: Boolean = false,
    val itemsLoading: Boolean = false,
    val resultsLoading: Boolean = false,
    val resultsError: Boolean = false,
    val hasMoreFolders: Boolean = false,
    val hasMoreItems: Boolean = false,
)

class ParentalControlViewModel(
    private val settingsRepository: SettingsRepository,
    private val parentalCatalogRepository: ParentalCatalogRepository,
    private val activeProfileId: StateFlow<String?>,
    private val activeProfileIdProvider: () -> String,
    catalogRevision: StateFlow<Long>,
) : ViewModel() {
    private val visible = MutableStateFlow(false)
    private val refreshNonce = MutableStateFlow(0L)
    private val _uiState = MutableStateFlow(ParentalControlUiState())
    val uiState: StateFlow<ParentalControlUiState> = _uiState.asStateFlow()

    init {
        observeSource(settingsRepository, catalogRevision)
    }

    fun setVisible(value: Boolean) {
        visible.value = value
    }

    fun updateDraft(value: String) {
        _uiState.update { it.copy(draft = value, keywordError = null, feedback = null) }
    }

    fun addDraft(): Boolean {
        val clean = ParentalKeywordPolicy.normalize(listOf(_uiState.value.draft)).firstOrNull()
            ?: run {
                _uiState.update { it.copy(keywordError = ParentalKeywordError.Empty, feedback = null) }
                return false
            }
        val current = _uiState.value.keywords
        if (current.any { it.equals(clean, ignoreCase = true) }) {
            _uiState.update { it.copy(keywordError = ParentalKeywordError.Duplicate, feedback = null) }
            return false
        }
        val next = current + clean
        if (!ParentalKeywordPolicy.fitsStorage(next)) {
            _uiState.update { it.copy(keywordError = ParentalKeywordError.TooLong, feedback = null) }
            return false
        }
        persistKeywords(next, ParentalFeedback.KeywordAdded, clearDraft = true)
        return true
    }

    fun updateKeyword(index: Int, value: String): Boolean {
        val clean = ParentalKeywordPolicy.normalize(listOf(value)).firstOrNull()
            ?: run {
                _uiState.update { it.copy(keywordError = ParentalKeywordError.Empty, feedback = null) }
                return false
            }
        val current = _uiState.value.keywords
        if (index !in current.indices) return false
        if (current.withIndex().any { (otherIndex, keyword) -> otherIndex != index && keyword.equals(clean, ignoreCase = true) }) {
            _uiState.update { it.copy(keywordError = ParentalKeywordError.Duplicate, feedback = null) }
            return false
        }
        val next = current.toMutableList().apply { this[index] = clean }
        if (!ParentalKeywordPolicy.fitsStorage(next)) {
            _uiState.update { it.copy(keywordError = ParentalKeywordError.TooLong, feedback = null) }
            return false
        }
        persistKeywords(next, ParentalFeedback.KeywordUpdated)
        return true
    }

    fun deleteKeyword(index: Int) {
        val current = _uiState.value.keywords
        if (index !in current.indices) return
        persistKeywords(current.filterIndexed { itemIndex, _ -> itemIndex != index }, ParentalFeedback.KeywordDeleted)
    }

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setParentalControlEnabled(value) }
    }

    fun setPin(value: String) {
        viewModelScope.launch {
            settingsRepository.setParentalPin(value)
            _uiState.update { it.copy(feedback = ParentalFeedback.PinUpdated) }
        }
    }

    fun retry() {
        refreshNonce.value += 1L
    }

    fun loadMoreFolders() {
        val state = _uiState.value
        if (!state.enabled || state.foldersLoading || !state.hasMoreFolders) return
        val profileId = activeProfileIdProvider()
        viewModelScope.launch {
            _uiState.update { it.copy(foldersLoading = true) }
            runCatching {
                parentalCatalogRepository.folders(profileId, state.keywords, state.folders.size, ResultsPageSize)
            }.onSuccess { page ->
                _uiState.update {
                    val merged = (it.folders + page).distinctBy(ParentalHiddenFolder::stableKey)
                    it.copy(
                        folders = merged,
                        foldersLoading = false,
                        hasMoreFolders = merged.size < it.counts.folders,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(foldersLoading = false, resultsError = true) }
            }
        }
    }

    fun loadMoreItems() {
        val state = _uiState.value
        if (!state.enabled || state.itemsLoading || !state.hasMoreItems) return
        val profileId = activeProfileIdProvider()
        viewModelScope.launch {
            _uiState.update { it.copy(itemsLoading = true) }
            runCatching {
                parentalCatalogRepository.items(profileId, state.keywords, state.items.size, ResultsPageSize)
            }.onSuccess { page ->
                _uiState.update {
                    val merged = (it.items + page).distinctBy(ParentalHiddenItem::stableKey)
                    it.copy(
                        items = merged,
                        itemsLoading = false,
                        hasMoreItems = merged.size < it.counts.items,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(itemsLoading = false, resultsError = true) }
            }
        }
    }

    fun clearTransientMessage() {
        _uiState.update { it.copy(keywordError = null, feedback = null) }
    }

    private fun persistKeywords(values: List<String>, feedback: ParentalFeedback, clearDraft: Boolean = false) {
        _uiState.update {
            it.copy(
                keywords = values,
                draft = if (clearDraft) "" else it.draft,
                keywordError = null,
                feedback = feedback,
            )
        }
        viewModelScope.launch {
            settingsRepository.replaceParentalKeywords(values)
        }
    }

    private fun observeSource(settingsRepository: SettingsRepository, catalogRevision: StateFlow<Long>) {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                activeProfileId,
                catalogRevision,
                visible,
                refreshNonce,
            ) { settings, profileId, revision, isVisible, nonce ->
                RefreshKey(settings, profileId.orEmpty(), revision, isVisible, nonce)
            }.collectLatest { key ->
                val keywords = key.settings.parentalKeywordValues
                _uiState.update {
                    it.copy(
                        enabled = key.settings.parentalControlEnabled,
                        pinConfigured = key.settings.parentalPin.isNotBlank(),
                        keywords = keywords,
                        keywordError = null,
                    )
                }
                if (!key.visible || !key.settings.parentalControlEnabled || keywords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            counts = ParentalFilterCounts(),
                            folders = emptyList(),
                            items = emptyList(),
                            resultsLoading = false,
                            resultsError = false,
                            hasMoreFolders = false,
                            hasMoreItems = false,
                        )
                    }
                    return@collectLatest
                }
                delay(150)
                refreshResults(key.profileId.ifBlank(activeProfileIdProvider), keywords)
            }
        }
    }

    private suspend fun refreshResults(profileId: String, keywords: List<String>) {
        _uiState.update { it.copy(resultsLoading = true, resultsError = false) }
        runCatching {
            kotlinx.coroutines.coroutineScope {
                val counts = async { parentalCatalogRepository.counts(profileId, keywords) }
                val folders = async { parentalCatalogRepository.folders(profileId, keywords, 0, ResultsPageSize) }
                val items = async { parentalCatalogRepository.items(profileId, keywords, 0, ResultsPageSize) }
                Triple(counts.await(), folders.await(), items.await())
            }
        }.onSuccess { (counts, folders, items) ->
            _uiState.update {
                it.copy(
                    counts = counts,
                    folders = folders,
                    items = items,
                    resultsLoading = false,
                    resultsError = false,
                    hasMoreFolders = folders.size < counts.folders,
                    hasMoreItems = items.size < counts.items,
                )
            }
        }.onFailure {
            _uiState.update { it.copy(resultsLoading = false, resultsError = true) }
        }
    }

    private data class RefreshKey(
        val settings: com.smartvision.svplayer.domain.model.PlayerSettings,
        val profileId: String,
        val catalogRevision: Long,
        val visible: Boolean,
        val nonce: Long,
    )

    private companion object {
        const val ResultsPageSize = 40
    }
}
