package com.smartvision.svplayer.data.parental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalFilterQueryBuilderTest {
    @Test
    fun `hidden query covers every supported catalog type without duplicate keyword joins`() {
        val query = ParentalFilterQueryBuilder.items(
            profileId = "profile-1",
            keywords = listOf("adult", "xxx"),
            offset = 0,
            limit = 40,
        )

        assertTrue(query.sql.contains("FROM live_streams"))
        assertTrue(query.sql.contains("FROM movies"))
        assertTrue(query.sql.contains("FROM series"))
        assertTrue(query.sql.contains("FROM episodes"))
        assertTrue(query.sql.contains("EXISTS"))
        assertTrue(query.sql.contains("ORDER BY section, folderName, title"))
        assertEquals(8, query.argCount)
    }

    @Test
    fun `folder query groups section and provider folder id`() {
        val query = ParentalFilterQueryBuilder.folders(
            profileId = "profile-1",
            keywords = listOf("adult"),
            offset = 40,
            limit = 40,
        )

        assertTrue(query.sql.contains("GROUP BY section, folderId, folderName"))
        assertTrue(query.sql.contains("LIMIT ? OFFSET ?"))
        assertEquals(7, query.argCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `query refuses an empty keyword list`() {
        ParentalFilterQueryBuilder.itemCount("profile-1", emptyList())
    }
}
