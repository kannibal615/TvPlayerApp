package com.smartvision.svplayer.data.tmdb

import com.smartvision.svplayer.domain.model.PlayerSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbAccessPolicyTest {
    @Test
    fun `tmdb is enabled by default`() {
        assertTrue(PlayerSettings().tmdbApiEnabled)
    }

    @Test
    fun `disabled setting blocks cache and network access`() {
        assertFalse(TmdbAccessPolicy.canUse(apiEnabled = false))
        assertFalse(TmdbAccessPolicy.canRequestNetwork(apiEnabled = false, tokenConfigured = true))
    }

    @Test
    fun `reactivation restores cache access and allows configured network`() {
        assertTrue(TmdbAccessPolicy.canUse(apiEnabled = true))
        assertTrue(TmdbAccessPolicy.canRequestNetwork(apiEnabled = true, tokenConfigured = true))
        assertFalse(TmdbAccessPolicy.canRequestNetwork(apiEnabled = true, tokenConfigured = false))
    }
}
