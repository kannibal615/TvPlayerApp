package com.smartvision.svplayer.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileMigrationPolicyTest {
    @Test
    fun `oldest existing profile becomes the unique administrator`() {
        val newer = profile("newer", 20L)
        val oldest = profile("oldest", 10L)
        val migrated = normalizeProfileRoles(listOf(newer, oldest))
        assertEquals(ProfileType.ADMIN, migrated.first { it.id == "oldest" }.type)
        assertEquals(ProfileType.NORMAL, migrated.first { it.id == "newer" }.type)
        assertEquals(1, migrated.count { it.type == ProfileType.ADMIN })
    }

    @Test
    fun `administrator cannot be deleted`() {
        assertThrows(IllegalArgumentException::class.java) {
            requireProfileDeletionAllowed(profile("admin", 1L).copy(type = ProfileType.ADMIN))
        }
    }

    private fun profile(id: String, createdAt: Long) = PlaylistProfile(
        id = id,
        name = id,
        source = PlaylistSource.Xtream,
        xtreamHost = "http://example.test",
        xtreamUsername = "user",
        xtreamPassword = "pass",
        createdAt = createdAt,
    )
}
