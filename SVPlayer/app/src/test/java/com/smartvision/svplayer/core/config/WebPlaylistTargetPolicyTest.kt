package com.smartvision.svplayer.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPlaylistTargetPolicyTest {
    @Test
    fun `admin and normal profiles are eligible but kids is refused`() {
        assertTrue(isEligibleWebPlaylistTarget(profile("admin", ProfileType.ADMIN)))
        assertTrue(isEligibleWebPlaylistTarget(profile("normal", ProfileType.NORMAL)))
        assertFalse(isEligibleWebPlaylistTarget(profile("kids", ProfileType.KIDS)))
    }

    @Test
    fun `new profile name gets deterministic suffix on collision`() {
        assertEquals("PlaylistWeb (3)", uniqueProfileName("PlaylistWeb", listOf("playlistweb", "PlaylistWeb (2)"), "PlaylistWeb"))
        assertEquals("Salon", uniqueProfileName("Salon", listOf("Admin"), "PlaylistWeb"))
    }

    private fun profile(id: String, type: ProfileType) = PlaylistProfile(
        id = id,
        name = id,
        source = PlaylistSource.Xtream,
        type = type,
        xtreamHost = "https://example.test",
        xtreamUsername = "user",
        xtreamPassword = "pass",
    )
}
