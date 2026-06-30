package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.MediaSection
import com.smartvision.svplayer.domain.repository.LocalCatalogSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCatalogSnapshotCacheTest {
    @Test
    fun `returns stored live snapshot as cached on second read`() {
        val cache = LocalCatalogSnapshotCache()
        val snapshot = LocalCatalogSnapshot(
            categories = listOf(Category("sports", "Sports", MediaSection.Live, count = 1)),
            items = listOf(
                LiveChannel(
                    streamId = 10,
                    number = 1,
                    name = "Sports HD",
                    categoryId = "sports",
                    categoryName = "Sports",
                    logoUrl = null,
                    currentProgram = null,
                    timeRange = null,
                ),
            ),
        )

        val stored = cache.putLive(snapshot)
        val cached = cache.getLive()

        assertFalse(stored.fromCache)
        assertTrue(cached?.fromCache == true)
        assertEquals(snapshot.categories, cached?.categories)
        assertEquals(snapshot.items, cached?.items)
    }

    @Test
    fun `invalidate clears all stored snapshots`() {
        val cache = LocalCatalogSnapshotCache()
        cache.putLive(LocalCatalogSnapshot(emptyList(), emptyList()))
        cache.putMovies(LocalCatalogSnapshot(emptyList(), emptyList()))
        cache.putSeries(LocalCatalogSnapshot(emptyList(), emptyList()))

        cache.invalidate()

        assertNull(cache.getLive())
        assertNull(cache.getMovies())
        assertNull(cache.getSeries())
    }
}
