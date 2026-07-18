package com.smartvision.svplayer.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import android.view.KeyEvent

class PlayerNavigationPolicyTest {
    @Test
    fun liveBackExitsEvenWhenTheMainOverlayIsVisible() {
        assertEquals(
            PlayerBackAction.ExitPlayer,
            resolvePlayerBackAction(
                adGateActive = false,
                episodesPanelVisible = false,
                brightnessMode = false,
                nextEpisodeCountdownVisible = false,
                secondaryMenuOpen = false,
            ),
        )
    }

    @Test
    fun liveBackClosesSecondarySurfacesBeforeExiting() {
        assertEquals(
            PlayerBackAction.CloseSecondaryMenu,
            resolvePlayerBackAction(false, false, false, false, true),
        )
        assertEquals(
            PlayerBackAction.CloseEpisodes,
            resolvePlayerBackAction(false, true, false, false, false),
        )
        assertEquals(
            PlayerBackAction.Ignore,
            resolvePlayerBackAction(true, false, false, false, false),
        )
    }

    @Test
    fun seriesDetailsFollowsNextAndPrecedesExitInDpadOrder() {
        assertEquals(
            listOf(
                VodControlBrightness,
                VodControlRestart,
                VodControlPrevious,
                VodControlSeekBack,
                VodControlPlayPause,
                VodControlSeekForward,
                VodControlNext,
                VodControlSeriesDetails,
                VodControlExit,
            ),
            vodEnabledControlOrder(
                hasPrevious = true,
                canSeek = true,
                hasNext = true,
                showSeriesDetails = true,
            ),
        )
    }

    @Test
    fun liveVerticalZappingIsDelegatedWhileASecondaryPanelIsOpen() {
        assertEquals(
            LiveRemoteAction.DelegateToSecondaryPanel,
            resolveLiveRemoteAction(KeyEvent.KEYCODE_DPAD_DOWN, secondaryPanelOpen = true),
        )
        assertEquals(
            LiveRemoteAction.ZapPrevious,
            resolveLiveRemoteAction(KeyEvent.KEYCODE_DPAD_UP, secondaryPanelOpen = false),
        )
    }
}
