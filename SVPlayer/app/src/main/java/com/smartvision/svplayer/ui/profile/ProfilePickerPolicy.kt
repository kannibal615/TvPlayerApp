package com.smartvision.svplayer.ui.profile

import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType

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
