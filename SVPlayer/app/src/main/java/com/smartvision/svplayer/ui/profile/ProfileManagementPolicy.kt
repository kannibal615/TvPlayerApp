package com.smartvision.svplayer.ui.profile

enum class ProfileAreaDestination {
    INFO,
    MANAGE,
    PARENTAL,
    SYNCHRONIZATION,
    HISTORY,
    HELP,
    SETTINGS,
}

enum class ProfileInfoAction {
    CHANGE_PROFILE,
    SYNCHRONIZE_PROFILE,
}

object ProfileManagementPolicy {
    val destinations: List<ProfileAreaDestination> = listOf(
        ProfileAreaDestination.INFO,
        ProfileAreaDestination.MANAGE,
        ProfileAreaDestination.PARENTAL,
        ProfileAreaDestination.SYNCHRONIZATION,
        ProfileAreaDestination.HISTORY,
        ProfileAreaDestination.HELP,
        ProfileAreaDestination.SETTINGS,
    )

    val infoActions: Set<ProfileInfoAction> = setOf(
        ProfileInfoAction.CHANGE_PROFILE,
        ProfileInfoAction.SYNCHRONIZE_PROFILE,
    )

    fun hiddenItemCount(kidsExcluded: Int, parentalHidden: Int): Int =
        kidsExcluded.coerceAtLeast(0) + parentalHidden.coerceAtLeast(0)

    fun activationTarget(focusedProfileId: String?, confirmed: Boolean): String? =
        focusedProfileId?.takeIf { confirmed }

    fun detailsProfileId(
        focusedProfileId: String?,
        selectedProfileId: String?,
        activeProfileId: String?,
        profileIds: List<String>,
    ): String? = focusedProfileId?.takeIf(profileIds::contains)
        ?: selectedProfileId?.takeIf(profileIds::contains)
        ?: activeProfileId?.takeIf(profileIds::contains)
        ?: profileIds.firstOrNull()

    fun focusTargetAfterDeletion(
        deletedProfileId: String,
        previousProfileIds: List<String>,
        remainingProfileIds: List<String>,
    ): String? {
        if (remainingProfileIds.isEmpty()) return null
        val deletedIndex = previousProfileIds.indexOf(deletedProfileId).coerceAtLeast(0)
        return remainingProfileIds.getOrNull(deletedIndex)
            ?: remainingProfileIds.getOrNull(deletedIndex - 1)
            ?: remainingProfileIds.first()
    }
}
