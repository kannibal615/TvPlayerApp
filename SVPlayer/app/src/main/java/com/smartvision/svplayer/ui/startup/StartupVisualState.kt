package com.smartvision.svplayer.ui.startup

const val StartupLoadingRevealDelayMillis = 700L
const val StartupMinimumLogoOnlyMillis = 450L

enum class StartupVisualPhase {
    LogoOnly,
    Loading,
    TransitionOut,
}

enum class StartupStage {
    Initializing,
    CheckingActivation,
    PreparingHome,
    Starting,
}

data class StartupProgressSnapshot(
    val stage: StartupStage = StartupStage.Initializing,
    val completedSteps: Int = 0,
    val totalSteps: Int = 3,
) {
    val progress: Float
        get() = if (totalSteps <= 0) {
            0f
        } else {
            (completedSteps.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
        }
}

fun shouldRevealStartupLoading(
    elapsedMillis: Long,
    startupComplete: Boolean,
): Boolean = !startupComplete && elapsedMillis >= StartupLoadingRevealDelayMillis
