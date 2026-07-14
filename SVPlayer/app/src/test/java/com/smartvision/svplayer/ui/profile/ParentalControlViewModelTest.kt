package com.smartvision.svplayer.ui.profile

import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.parental.ParentalCatalogRepository
import com.smartvision.svplayer.domain.parental.ParentalFilterCounts
import com.smartvision.svplayer.domain.parental.ParentalHiddenFolder
import com.smartvision.svplayer.domain.parental.ParentalHiddenItem
import com.smartvision.svplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentalControlViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `disabled control clears results without querying Room`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(PlayerSettings(parentalControlEnabled = false))
        val repository = FakeParentalCatalogRepository()
        val viewModel = createViewModel(settings, repository)

        viewModel.setVisible(true)
        advanceUntilIdle()

        assertEquals(0, repository.countCalls)
        assertEquals(ParentalFilterCounts(), viewModel.uiState.value.counts)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun `profile and catalog revision changes cancel stale calculation and reload`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(enabledSettings())
        val repository = FakeParentalCatalogRepository(delayMillis = 1_000)
        val profileId = MutableStateFlow<String?>("profile-a")
        val revision = MutableStateFlow(0L)
        val viewModel = createViewModel(settings, repository, profileId, revision)

        viewModel.setVisible(true)
        advanceTimeBy(200)
        profileId.value = "profile-b"
        advanceTimeBy(200)
        revision.value = 1L
        advanceUntilIdle()

        assertTrue(repository.startedProfiles.containsAll(listOf("profile-a", "profile-b")))
        assertTrue(repository.cancelledCalls > 0)
        assertEquals(ParentalFilterCounts(folders = 1, items = 1), viewModel.uiState.value.counts)
    }

    @Test
    fun `error keeps state retryable and retry restores results`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(enabledSettings())
        val repository = FakeParentalCatalogRepository(fail = true)
        val viewModel = createViewModel(settings, repository)

        viewModel.setVisible(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.resultsError)

        repository.fail = false
        viewModel.retry()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.resultsError)
        assertEquals(1, viewModel.uiState.value.counts.items)
        assertEquals("movie:1", viewModel.uiState.value.items.single().stableKey)
    }

    @Test
    fun `add edit and delete update keyword cards immediately`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(enabledSettings())
        val viewModel = createViewModel(settings, FakeParentalCatalogRepository())
        advanceUntilIdle()

        viewModel.updateDraft("  violence  ")
        assertTrue(viewModel.addDraft())
        assertEquals(listOf("adult", "violence"), viewModel.uiState.value.keywords)
        assertEquals("", viewModel.uiState.value.draft)

        assertTrue(viewModel.updateKeyword(1, "nudity"))
        assertEquals(listOf("adult", "nudity"), viewModel.uiState.value.keywords)

        viewModel.deleteKeyword(0)
        assertEquals(listOf("nudity"), viewModel.uiState.value.keywords)
        advanceUntilIdle()
    }

    private fun createViewModel(
        settings: FakeSettingsRepository,
        repository: FakeParentalCatalogRepository,
        profileId: MutableStateFlow<String?> = MutableStateFlow("profile-a"),
        revision: MutableStateFlow<Long> = MutableStateFlow(0L),
    ) = ParentalControlViewModel(
        settingsRepository = settings,
        parentalCatalogRepository = repository,
        activeProfileId = profileId,
        activeProfileIdProvider = { profileId.value ?: "default" },
        catalogRevision = revision,
    )

    private fun enabledSettings() = PlayerSettings(
        parentalControlEnabled = true,
        parentalKeywordValues = listOf("adult"),
        parentalKeywords = "adult",
    )
}

private class FakeParentalCatalogRepository(
    var fail: Boolean = false,
    private val delayMillis: Long = 0,
) : ParentalCatalogRepository {
    var countCalls = 0
    var cancelledCalls = 0
    val startedProfiles = mutableListOf<String>()

    override suspend fun counts(profileId: String, keywords: List<String>): ParentalFilterCounts {
        countCalls += 1
        startedProfiles += profileId
        delayedResponse()
        return ParentalFilterCounts(folders = 1, items = 1)
    }

    override suspend fun folders(profileId: String, keywords: List<String>, offset: Int, limit: Int): List<ParentalHiddenFolder> {
        delayedResponse()
        return listOf(ParentalHiddenFolder("movies:adult", "movies", "adult", "Adult", 1))
    }

    override suspend fun items(
        profileId: String,
        keywords: List<String>,
        folder: ParentalHiddenFolder,
        offset: Int,
        limit: Int,
    ): List<ParentalHiddenItem> {
        delayedResponse()
        return listOf(
            ParentalHiddenItem(
                stableKey = "movie:1",
                type = com.smartvision.svplayer.domain.parental.ParentalHiddenContentType.Movie,
                contentId = "1",
                title = "Adult movie",
                folderName = "Adult",
                imageUrl = null,
                secondaryLabel = "Movie",
                duration = null,
            ),
        )
    }

    override suspend fun itemCount(profileId: String, keywords: List<String>, folder: ParentalHiddenFolder): Int = 1
    override suspend fun hiddenStableKeys(profileId: String, keywords: List<String>): Set<String> = setOf("movie:1")
    override suspend fun deleteProfileSnapshot(profileId: String) = Unit

    private suspend fun delayedResponse() {
        if (fail) error("query failed")
        if (delayMillis <= 0) return
        var completed = false
        try {
            delay(delayMillis)
            completed = true
        } finally {
            if (!completed) cancelledCalls += 1
        }
    }
}

private class FakeSettingsRepository(initial: PlayerSettings) : SettingsRepository {
    private val mutableSettings = MutableStateFlow(initial)
    override val settings: Flow<PlayerSettings> = mutableSettings

    override suspend fun setParentalControlEnabled(value: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(parentalControlEnabled = value)
    }

    override suspend fun setParentalPin(value: String) {
        mutableSettings.value = mutableSettings.value.copy(parentalPin = "configured")
    }

    override fun verifyParentalPin(value: String) = true
    override suspend fun setParentalKeywords(value: String) = Unit
    override suspend fun replaceParentalKeywords(values: List<String>) {
        mutableSettings.value = mutableSettings.value.copy(
            parentalKeywordValues = values,
            parentalKeywords = values.joinToString("; "),
        )
    }

    override suspend fun resetParentalControl() = Unit
    override suspend fun setDisplaySize(value: String) = Unit
    override suspend fun setLanguage(value: String) = Unit
    override suspend fun setSyncFrequency(value: String) = Unit
    override suspend fun setAutostartEnabled(value: Boolean) = Unit
    override suspend fun setBackgroundSyncEnabled(value: Boolean) = Unit
    override suspend fun setFocusStyle(value: String) = Unit
    override suspend fun setFocusColor(value: String) = Unit
    override suspend fun setFocusEffect(value: String) = Unit
    override suspend fun setFocusBackground(value: String) = Unit
    override suspend fun setFocusSelectedColor(value: String) = Unit
    override suspend fun setFocusActiveColor(value: String) = Unit
    override suspend fun setFocusParentColor(value: String) = Unit
    override suspend fun setAnimationsEnabled(value: Boolean) = Unit
    override suspend fun setVideoRatio(value: String) = Unit
    override suspend fun setBufferMode(value: String) = Unit
    override suspend fun setRetryEnabled(value: Boolean) = Unit
    override suspend fun clearLocalData() = Unit
}
