package com.smartvision.svplayer.ui.activation

import com.smartvision.svplayer.data.activation.ActivationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivationEntryStateTest {
    @Test
    fun unresolvedRemoteAccessKeepsInitialSurfaceHidden() {
        val state = ActivationUiState(
            localStateReady = true,
            initialAccessResolved = false,
            checking = true,
            activated = false,
        )

        assertFalse(state.canRevealInitialSurface)
    }

    @Test
    fun activeDeviceCanRevealPlaylistOrHomeSurface() {
        val state = ActivationUiState(
            localStateReady = true,
            initialAccessResolved = true,
            checking = false,
            activated = true,
        )

        assertTrue(state.canRevealInitialSurface)
    }

    @Test
    fun confirmedInactiveDeviceCanRevealActivationSurface() {
        val state = ActivationUiState(
            localStateReady = true,
            initialAccessResolved = true,
            checking = false,
            activated = false,
        )

        assertTrue(state.canRevealInitialSurface)
    }

    @Test
    fun activationErrorCanRevealActionableSurfaceAfterResolution() {
        val state = ActivationUiState(
            localStateReady = true,
            initialAccessResolved = true,
            checking = false,
            activated = false,
            error = ActivationUiError.NetworkUnavailable,
        )

        assertTrue(state.canRevealInitialSurface)
    }

    @Test
    fun availableTrialUsesTrialOffer() {
        assertEquals(
            ActivationOfferMode.TrialAvailable,
            resolveActivationOfferMode(
                status = ActivationStatus.Pending,
                trialStatus = "available",
                licenseStatus = "inactive",
            ),
        )
    }

    @Test
    fun expiredTrialUsesFreeWithAdsOffer() {
        assertEquals(
            ActivationOfferMode.FreeWithAds,
            resolveActivationOfferMode(
                status = ActivationStatus.Expired,
                trialStatus = "expired",
                licenseStatus = "inactive",
            ),
        )
    }

    @Test
    fun expiredLicenseUsesFreeWithAdsOffer() {
        assertEquals(
            ActivationOfferMode.FreeWithAds,
            resolveActivationOfferMode(
                status = ActivationStatus.Expired,
                trialStatus = "available",
                licenseStatus = "expired",
            ),
        )
    }

    @Test
    fun blockedDeviceNeverUsesTrialOffer() {
        assertEquals(
            ActivationOfferMode.Blocked,
            resolveActivationOfferMode(
                status = ActivationStatus.Blocked,
                trialStatus = "available",
                licenseStatus = "inactive",
            ),
        )
    }

    @Test
    fun pendingXtreamKeepsTrialOfferStateUntilPlayableSourceGate() {
        assertEquals(
            ActivationOfferMode.TrialAvailable,
            resolveActivationOfferMode(
                status = ActivationStatus.Active,
                trialStatus = "pending_xtream",
                licenseStatus = "inactive",
            ),
        )
    }

    @Test
    fun freeWithAdsActiveDoesNotChangePersistentTvCode() {
        val first = ActivationUiState(
            publicDeviceCode = "LAUU9M",
            shortCode = "LEGACY99",
            freeWithAdsStatus = "active",
            offerMode = ActivationOfferMode.FreeWithAds,
        )
        val recreated = first.copy()

        assertEquals("LAUU9M", recreated.publicDeviceCode)
        assertTrue(recreated.purchaseUrl.contains("device=LAUU9M"))
        assertFalse(recreated.purchaseUrl.contains("device_id="))
        assertFalse(recreated.purchaseUrl.contains(recreated.shortCode))
    }
}
