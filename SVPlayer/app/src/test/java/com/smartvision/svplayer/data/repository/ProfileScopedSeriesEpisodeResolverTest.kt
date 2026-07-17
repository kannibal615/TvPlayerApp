package com.smartvision.svplayer.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileScopedSeriesEpisodeResolverTest {
    @Test
    fun localEpisodesAvoidTheRemoteFallback() = runTest {
        var remoteCalls = 0

        val result = getOrFetchProfileScopedSeriesEpisodes(
            capturedProfileId = "walid",
            seriesId = 7,
            activeProfileId = { "walid" },
            loadLocal = { _, _ -> listOf("local") },
            fetchRemote = { _, _ ->
                remoteCalls += 1
                listOf("remote")
            },
            persist = { _, _, _ -> },
        )

        assertEquals(listOf("local"), result)
        assertEquals(0, remoteCalls)
    }

    @Test
    fun remoteFallbackIsPersistedForTheCapturedProfile() = runTest {
        val persisted = mutableListOf<String>()

        val result = getOrFetchProfileScopedSeriesEpisodes(
            capturedProfileId = "walid",
            seriesId = 7,
            activeProfileId = { "walid" },
            loadLocal = { _, _ -> emptyList() },
            fetchRemote = { _, _ -> listOf("episode-1", "episode-2") },
            persist = { profileId, _, episodes ->
                persisted += "$profileId:${episodes.joinToString()}"
            },
        )

        assertEquals(listOf("episode-1", "episode-2"), result)
        assertEquals(listOf("walid:episode-1, episode-2"), persisted)
    }

    @Test
    fun lateRemoteResultNeverLeaksIntoTheNewActiveProfile() = runTest {
        var activeProfileId = "walid"
        var persistedForProfile: String? = null

        val result = getOrFetchProfileScopedSeriesEpisodes(
            capturedProfileId = "walid",
            seriesId = 7,
            activeProfileId = { activeProfileId },
            loadLocal = { _, _ -> emptyList() },
            fetchRemote = { _, _ ->
                activeProfileId = "nouran"
                listOf("episode-1")
            },
            persist = { profileId, _, _ -> persistedForProfile = profileId },
        )

        assertTrue(result.isEmpty())
        assertEquals("walid", persistedForProfile)
    }
}
