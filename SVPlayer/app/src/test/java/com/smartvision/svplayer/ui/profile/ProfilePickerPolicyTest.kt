package com.smartvision.svplayer.ui.profile

import com.smartvision.svplayer.core.config.CredentialsMode
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistProfileStatus
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun selectionCompletesOnlyForTheMatchingRequestAndReadyProfile() {
        val request = ProfileSelectionRequest(requestId = 42L, profileId = "walid")

        assertTrue(canCompleteProfileSelection(request, 42L, "walid", appInForeground = true))
        assertFalse(canCompleteProfileSelection(request, 41L, "walid", appInForeground = true))
        assertFalse(canCompleteProfileSelection(request, 42L, "nouran", appInForeground = true))
        assertFalse(canCompleteProfileSelection(request, 42L, "walid", appInForeground = false))
    }

    @Test
    fun globalPickerIsRevealedOnlyAfterHomeIsActiveInForeground() {
        assertFalse(canRevealProfilePickerAfterHome(openRequested = false, homeIsActive = true, appInForeground = true))
        assertFalse(canRevealProfilePickerAfterHome(openRequested = true, homeIsActive = false, appInForeground = true))
        assertFalse(canRevealProfilePickerAfterHome(openRequested = true, homeIsActive = true, appInForeground = false))
        assertTrue(canRevealProfilePickerAfterHome(openRequested = true, homeIsActive = true, appInForeground = true))
    }

    @Test
    fun globalPickerCanCoverStartupBeforeHomeBecomesVisible() {
        assertTrue(
            canDisplayGlobalProfilePicker(
                pickerWanted = true,
                homeIsActive = false,
                openRequested = false,
                appInForeground = true,
                waitingForFirstRoute = true,
            ),
        )
    }

    @Test
    fun globalPickerCannotBeDisplayedOverARestoredNonHomeRouteOrDuringStaging() {
        assertFalse(
            canDisplayGlobalProfilePicker(
                pickerWanted = true,
                homeIsActive = false,
                openRequested = false,
                appInForeground = true,
                waitingForFirstRoute = false,
            ),
        )
        assertFalse(
            canDisplayGlobalProfilePicker(
                pickerWanted = true,
                homeIsActive = true,
                openRequested = true,
                appInForeground = true,
                waitingForFirstRoute = false,
            ),
        )
        assertFalse(
            canDisplayGlobalProfilePicker(
                pickerWanted = true,
                homeIsActive = true,
                openRequested = false,
                appInForeground = false,
                waitingForFirstRoute = false,
            ),
        )
        assertTrue(
            canDisplayGlobalProfilePicker(
                pickerWanted = true,
                homeIsActive = true,
                openRequested = false,
                appInForeground = true,
                waitingForFirstRoute = false,
            ),
        )
    }

    @Test
    fun consumedPickerOpeningRequestCannotRevealTwice() {
        var openRequested = true
        assertTrue(canRevealProfilePickerAfterHome(openRequested, homeIsActive = true, appInForeground = true))

        openRequested = false
        assertFalse(canRevealProfilePickerAfterHome(openRequested, homeIsActive = true, appInForeground = true))
    }

    @Test
    fun profileSelectionStartsOnlyFromForegroundHomeAndOnlyOnce() {
        assertFalse(
            canStartProfileSelectionFromPicker(
                homeIsActive = false,
                appInForeground = true,
                selectionInProgress = false,
            ),
        )
        assertFalse(
            canStartProfileSelectionFromPicker(
                homeIsActive = true,
                appInForeground = false,
                selectionInProgress = false,
            ),
        )
        assertFalse(
            canStartProfileSelectionFromPicker(
                homeIsActive = true,
                appInForeground = true,
                selectionInProgress = true,
            ),
        )
        assertTrue(
            canStartProfileSelectionFromPicker(
                homeIsActive = true,
                appInForeground = true,
                selectionInProgress = false,
            ),
        )
    }

    @Test
    fun repeatedAdminKidsAdminSelectionRejectsEveryLateResult() {
        val firstAdmin = ProfileSelectionRequest(requestId = 1L, profileId = "admin")
        val kids = ProfileSelectionRequest(requestId = 2L, profileId = "kids")
        val finalAdmin = ProfileSelectionRequest(requestId = 3L, profileId = "admin")

        assertFalse(canCompleteProfileSelection(finalAdmin, 1L, "admin", appInForeground = true))
        assertFalse(canCompleteProfileSelection(finalAdmin, 2L, "kids", appInForeground = true))
        assertFalse(canCompleteProfileSelection(finalAdmin, 2L, "admin", appInForeground = true))
        assertTrue(canCompleteProfileSelection(finalAdmin, 3L, "admin", appInForeground = true))

        assertTrue(canCompleteProfileSelection(firstAdmin, 1L, "admin", appInForeground = true))
        assertTrue(canCompleteProfileSelection(kids, 2L, "kids", appInForeground = true))
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
