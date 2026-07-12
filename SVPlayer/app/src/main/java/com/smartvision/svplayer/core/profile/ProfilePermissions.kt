package com.smartvision.svplayer.core.profile

import com.smartvision.svplayer.core.config.ProfileType

data class ProfilePermissions(
    val canAccessSettings: Boolean,
    val canAccessNotifications: Boolean,
    val canAccessMedia: Boolean,
    val canAccessPremium: Boolean,
    val canManageProfiles: Boolean,
    val canManageParentalPin: Boolean,
) {
    companion object {
        fun forType(type: ProfileType): ProfilePermissions = when (type) {
            ProfileType.ADMIN -> ProfilePermissions(
                canAccessSettings = true,
                canAccessNotifications = true,
                canAccessMedia = true,
                canAccessPremium = true,
                canManageProfiles = true,
                canManageParentalPin = true,
            )
            ProfileType.NORMAL -> ProfilePermissions(
                canAccessSettings = true,
                canAccessNotifications = true,
                canAccessMedia = true,
                canAccessPremium = true,
                canManageProfiles = false,
                canManageParentalPin = false,
            )
            ProfileType.KIDS -> ProfilePermissions(
                canAccessSettings = false,
                canAccessNotifications = false,
                canAccessMedia = false,
                canAccessPremium = false,
                canManageProfiles = false,
                canManageParentalPin = false,
            )
        }
    }
}
