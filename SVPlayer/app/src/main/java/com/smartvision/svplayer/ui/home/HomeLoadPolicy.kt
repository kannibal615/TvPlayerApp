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
