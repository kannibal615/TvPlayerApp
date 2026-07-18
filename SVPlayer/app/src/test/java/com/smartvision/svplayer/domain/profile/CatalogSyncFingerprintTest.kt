package com.smartvision.svplayer.domain.profile

import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CatalogSyncFingerprintTest {
    private val base = PlaylistProfile(
        id = "profile",
        name = "Profile",
        source = PlaylistSource.Xtream,
        type = ProfileType.NORMAL,
        xtreamHost = "https://provider.example/",
        xtreamUsername = "user",
        xtreamPassword = "secret",
    )

    @Test
    fun prefixOrderAndCaseDoNotChangeTheFingerprint() {
        val first = CatalogSyncFingerprint.create(
            base.copy(selectedContentPrefixes = linkedSetOf("fr", "AR")),
        )
        val second = CatalogSyncFingerprint.create(
            base.copy(selectedContentPrefixes = linkedSetOf("AR", "FR")),
        )
        assertEquals(first, second)
    }

    @Test
    fun credentialsProfileTypeAndFiltersInvalidateTheFingerprint() {
        val original = CatalogSyncFingerprint.create(base)
        assertNotEquals(original, CatalogSyncFingerprint.create(base.copy(xtreamPassword = "new-secret")))
        assertNotEquals(original, CatalogSyncFingerprint.create(base.copy(type = ProfileType.KIDS)))
        assertNotEquals(
            original,
            CatalogSyncFingerprint.create(base.copy(selectedContentPrefixes = setOf("FR"))),
        )
    }
}
