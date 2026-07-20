package com.smartvision.svplayer.ui.startup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupVisualStateTest {
    @Test
    fun fastStartupDoesNotRevealLoading() {
        assertFalse(shouldRevealStartupLoading(elapsedMillis = 420L, startupComplete = false))
        assertFalse(shouldRevealStartupLoading(elapsedMillis = 720L, startupComplete = true))
    }

    @Test
    fun slowStartupRevealsLoadingAfterRealDelay() {
        assertFalse(shouldRevealStartupLoading(StartupLoadingRevealDelayMillis - 1L, startupComplete = false))
        assertTrue(shouldRevealStartupLoading(StartupLoadingRevealDelayMillis, startupComplete = false))
    }

    @Test
    fun completedStartupDoesNotRevealLoadingLateAtHandoff() {
        assertFalse(
            shouldRevealStartupLoading(
                elapsedMillis = StartupLoadingRevealDelayMillis,
                startupComplete = false,
                progress = StartupProgressSnapshot(StartupStage.Starting, 3, 3),
            ),
        )
    }

    @Test
    fun progressComesOnlyFromCompletedStepsAndSupportsFailOpenCompletion() {
        assertEquals(2f / 3f, StartupProgressSnapshot(StartupStage.PreparingHome, 2, 3).progress)
        assertEquals(1f, StartupProgressSnapshot(StartupStage.Starting, 3, 3).progress)
        assertEquals(0f, StartupProgressSnapshot(StartupStage.Initializing, 0, 0).progress)
    }

    @Test
    fun visibleProgressKeepsLoadingBarNonEmptyDuringEarlyStartup() {
        assertEquals(0.08f, StartupProgressSnapshot(StartupStage.Initializing, 0, 3).visibleProgress)
        assertEquals(1f / 3f, StartupProgressSnapshot(StartupStage.CheckingActivation, 1, 3).visibleProgress)
        assertEquals(0f, StartupProgressSnapshot(StartupStage.Initializing, 0, 0).visibleProgress)
    }
}
