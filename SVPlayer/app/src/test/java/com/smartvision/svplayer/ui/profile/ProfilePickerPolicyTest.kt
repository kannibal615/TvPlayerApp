package com.smartvision.svplayer.ui.profile

import com.smartvision.svplayer.core.config.CredentialsMode
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistProfileStatus
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfilePickerPolicyTest {
    @Test
    fun keepsOnlyConfiguredProfilesAndPreservesRealNames() {
        val renamed = profile("normal", "Famille Martin", ProfileType.NORMAL, createdAt = 1L)
        val admin = profile("admin", "Compte principal", ProfileType.ADMIN, createdAt = 9L)
        val unconfigured = PlaylistProfile(
            id = "empty",
            name = "PlaylistWeb",
            source = PlaylistSource.Xtream,
            status = PlaylistProfileStatus.NotConfigured,
        )

        val result = orderProfilePickerProfiles(listOf(renamed, unconfigured, admin))

        assertEquals(listOf("Compte principal", "Famille Martin"), result.map { it.name })
        assertFalse(result.any { it.id == "empty" })
    }

    @Test
    fun activeProfileWinsInitialFocusEvenWhenItIsNotAdmin() {
        val profiles = listOf(
            profile("admin", "Gestion", ProfileType.ADMIN, 1L),
            profile("active", "東京", ProfileType.NORMAL, 2L),
        )
        assertEquals("active", initialProfilePickerId(profiles, "active"))
    }

    @Test
    fun missingActiveFallsBackToAdminThenFirstProfile() {
        val normal = profile("normal", "Salon", ProfileType.NORMAL, 1L)
        val admin = profile("admin", "Gestion", ProfileType.ADMIN, 2L)
        assertEquals("admin", initialProfilePickerId(listOf(normal, admin), "missing"))
        assertEquals("normal", initialProfilePickerId(listOf(normal), "missing"))
    }

    private fun profile(
        id: String,
        name: String,
        type: ProfileType,
        createdAt: Long,
    ) = PlaylistProfile(
        id = id,
        name = name,
        source = PlaylistSource.Xtream,
        type = type,
        credentialsMode = CredentialsMode.SHARED_WITH_ADMIN,
        createdAt = createdAt,
        status = PlaylistProfileStatus.Active,
    )
}
