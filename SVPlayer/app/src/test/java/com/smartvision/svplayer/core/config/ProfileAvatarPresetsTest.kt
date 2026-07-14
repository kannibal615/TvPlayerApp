package com.smartvision.svplayer.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAvatarPresetsTest {

    @Test
    fun adminAlwaysUsesDedicatedAvatar() {
        assertEquals(AdminProfileAvatarId, defaultProfileAvatarId(ProfileType.ADMIN))
        assertEquals(AdminProfileAvatarId, canonicalProfileAvatarId("classic_wave", ProfileType.ADMIN))
    }

    @Test
    fun kidsDefaultAlwaysBelongsToKidsSet() {
        repeat(32) {
            assertTrue(defaultProfileAvatarId(ProfileType.KIDS) in KidsProfileAvatarPresetIds)
        }
    }

    @Test
    fun legacyIdsMapToNewCompatibleImages() {
        assertEquals("classic_wave", canonicalProfileAvatarId("ocean", ProfileType.NORMAL))
        assertEquals("kid_star", canonicalProfileAvatarId("kids_star", ProfileType.KIDS))
    }

    @Test
    fun stableFallbackDoesNotDependOnDisplayedNameAtRenderTime() {
        val first = defaultProfileAvatarId(ProfileType.NORMAL, "profile-id-42")
        val second = defaultProfileAvatarId(ProfileType.NORMAL, "profile-id-42")

        assertEquals(first, second)
        assertTrue(first in ProfileAvatarPresetIds)
    }
}
