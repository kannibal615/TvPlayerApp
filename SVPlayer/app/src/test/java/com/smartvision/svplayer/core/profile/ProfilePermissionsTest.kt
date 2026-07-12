package com.smartvision.svplayer.core.profile

import com.smartvision.svplayer.core.config.ProfileType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePermissionsTest {
    @Test
    fun `kids permissions deny sensitive surfaces`() {
        val permissions = ProfilePermissions.forType(ProfileType.KIDS)
        assertFalse(permissions.canAccessSettings)
        assertFalse(permissions.canAccessNotifications)
        assertFalse(permissions.canAccessMedia)
        assertFalse(permissions.canAccessPremium)
        assertFalse(permissions.canManageProfiles)
        assertFalse(permissions.canManageParentalPin)
    }

    @Test
    fun `administrator can manage profiles and parental pin`() {
        val permissions = ProfilePermissions.forType(ProfileType.ADMIN)
        assertTrue(permissions.canManageProfiles)
        assertTrue(permissions.canManageParentalPin)
    }
}
