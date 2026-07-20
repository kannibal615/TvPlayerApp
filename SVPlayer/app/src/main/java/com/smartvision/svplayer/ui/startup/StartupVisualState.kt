package com.smartvision.svplayer.ui.startup

const val StartupLoadingRevealDelayMillis = 450L
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

    val visibleProgress: Float
        get() = progress.takeIf { it > 0f }
            ?: if (totalSteps > 0) StartupMinimumVisibleProgress else 0f
}

fun shouldRevealStartupLoading(
    elapsedMillis: Long,
    startupComplete: Boolean,
    progress: StartupProgressSnapshot = StartupProgressSnapshot(),
): Boolean =
    !startupComplete &&
        elapsedMillis >= StartupLoadingRevealDelayMillis &&
        progress.progress < 1f

private const val StartupMinimumVisibleProgress = 0.08f
