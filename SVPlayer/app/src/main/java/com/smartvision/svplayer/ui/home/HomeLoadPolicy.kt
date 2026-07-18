package com.smartvision.svplayer.ui.home

internal data class HomeLoadToken(
    val profileId: String,
    val catalogRevision: Long,
)

internal fun shouldApplyHomeLoadResult(
    token: HomeLoadToken,
    activeProfileId: String,
    catalogRevision: Long,
): Boolean =
    token.profileId == activeProfileId &&
        token.catalogRevision == catalogRevision

internal fun shouldApplyHomeLoadAttempt(
    token: HomeLoadToken,
    generation: Long,
    requestId: Long,
    activeProfileId: String,
    catalogRevision: Long,
    currentGeneration: Long,
    currentRequestId: Long,
): Boolean =
    generation == currentGeneration &&
        requestId == currentRequestId &&
        shouldApplyHomeLoadResult(token, activeProfileId, catalogRevision)

/**
 * Profile navigation is gated by the catalog-backed Home sections only.
 * Continue Watching is a personal, best-effort row: resolving old episode
 * metadata must never keep the user trapped in the profile picker.
 */
internal fun isHomeContentReady(
    state: HomeUiState,
    activeProfileId: String?,
    expectedCatalogRevision: Long,
): Boolean =
    state.profileId.isNotBlank() &&
        state.profileId == activeProfileId &&
        state.catalogRevision == expectedCatalogRevision &&
        !state.syncInProgress &&
        state.loadedCatalogRevision == state.catalogRevision &&
        !state.trendingLoading &&
        !state.catalogCountsLoading

internal fun HomeUiState.visibleForProfile(activeProfileId: String): HomeUiState =
    if (profileId == activeProfileId) {
        this
    } else {
        HomeUiState(
            profileId = activeProfileId,
            slides = slides,
            continueWatchingLoading = true,
            trendingLoading = true,
            catalogCountsLoading = true,
            catalogRevision = catalogRevision,
            loadedCatalogRevision = -1L,
            syncInProgress = syncInProgress,
        )
    }
