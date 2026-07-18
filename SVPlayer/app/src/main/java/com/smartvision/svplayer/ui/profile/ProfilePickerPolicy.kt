package com.smartvision.svplayer.ui.profile

import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType

data class ProfileSelectionRequest(
    val requestId: Long,
    val profileId: String,
)

data class ProfileHomeReadyToken(
    val profileId: String,
    val catalogRevision: Long,
)

fun orderProfilePickerProfiles(profiles: List<PlaylistProfile>): List<PlaylistProfile> =
    profiles
        .filter { it.isConfigured }
        .sortedWith(
            compareBy<PlaylistProfile> { if (it.type == ProfileType.ADMIN) 0 else 1 }
                .thenBy { it.createdAt },
        )

fun initialProfilePickerId(
    profiles: List<PlaylistProfile>,
    activeProfileId: String?,
): String? = activeProfileId
    ?.takeIf { activeId -> profiles.any { it.id == activeId } }
    ?: profiles.firstOrNull { it.type == ProfileType.ADMIN }?.id
    ?: profiles.firstOrNull()?.id

fun shouldSynchronizeProfileCatalog(
    hasLocalCatalog: Boolean,
    catalogCurrent: Boolean,
    synchronizationDue: Boolean,
): Boolean = !hasLocalCatalog || !catalogCurrent || synchronizationDue

fun canRevealProfilePickerAfterHome(
    openRequested: Boolean,
    homeIsActive: Boolean,
    appInForeground: Boolean,
): Boolean = openRequested && homeIsActive && appInForeground

fun canDisplayGlobalProfilePicker(
    pickerWanted: Boolean,
    homeIsActive: Boolean,
    openRequested: Boolean,
): Boolean = pickerWanted && homeIsActive && !openRequested

fun canStartProfileSelectionFromPicker(
    homeIsActive: Boolean,
    appInForeground: Boolean,
    selectionInProgress: Boolean,
): Boolean = homeIsActive && appInForeground && !selectionInProgress

fun canCompleteProfileSelection(
    request: ProfileSelectionRequest?,
    completedRequestId: Long?,
    activeProfileId: String?,
    homeReadyToken: ProfileHomeReadyToken?,
    catalogRevision: Long,
    appInForeground: Boolean,
): Boolean =
    appInForeground &&
        request != null &&
        completedRequestId == request.requestId &&
        activeProfileId == request.profileId &&
        homeReadyToken != null &&
        homeReadyToken.profileId == request.profileId &&
        homeReadyToken.catalogRevision == catalogRevision
