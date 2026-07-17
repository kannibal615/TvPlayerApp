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
