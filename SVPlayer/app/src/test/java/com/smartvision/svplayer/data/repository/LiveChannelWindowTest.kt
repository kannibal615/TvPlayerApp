package com.smartvision.svplayer.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveChannelWindowTest {
    @Test
    fun centersCurrentItemAndKeepsAscendingPlaybackOrder() {
        assertEquals(
            listOf(3, 4, 5, 6, 7),
            buildCenteredWindow(
                previousNearestFirst = listOf(4, 3, 2),
                current = 5,
                nextNearestFirst = listOf(6, 7, 8),
                radius = 2,
            ),
        )
    }

    @Test
    fun keepsOnlyAvailableItemsAtCategoryEdges() {
        assertEquals(
            listOf(1, 2, 3),
            buildCenteredWindow(
                previousNearestFirst = emptyList(),
                current = 1,
                nextNearestFirst = listOf(2, 3),
                radius = 2,
            ),
        )
        assertEquals(
            listOf(3, 4, 5),
            buildCenteredWindow(
                previousNearestFirst = listOf(4, 3),
                current = 5,
                nextNearestFirst = emptyList(),
                radius = 2,
            ),
        )
    }
}
