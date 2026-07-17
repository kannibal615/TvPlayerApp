package com.smartvision.svplayer.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class UserContentProfileScopeTest {
    @Test
    fun `playback progress keeps the profile captured by the player session`() {
        assertEquals(
            "walid-profile",
            progressOwnerProfileId(
                contentType = UserContentType.Episode,
                sessionProfileId = "walid-profile",
            ),
        )
    }

    @Test
    fun `local media remains in its shared local scope`() {
        assertEquals(
            "local_media",
            progressOwnerProfileId(
                contentType = UserContentType.LocalMedia,
                sessionProfileId = "walid-profile",
            ),
        )
    }
}
