package com.smartvision.svplayer.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileManagementPolicyTest {
    @Test
    fun `menu order keeps info and management as distinct leading destinations`() {
        assertEquals(
            listOf(
                ProfileAreaDestination.INFO,
                ProfileAreaDestination.MANAGE,
                ProfileAreaDestination.PARENTAL,
                ProfileAreaDestination.SYNCHRONIZATION,
                ProfileAreaDestination.HELP,
            ),
            ProfileManagementPolicy.destinations,
        )
    }

    @Test
    fun `manage focus starts on first profile or kids action when empty`() {
        assertEquals("profile:admin", ProfileManagementPolicy.firstManageFocusTarget(listOf("admin", "kids")))
        assertEquals("add:kids", ProfileManagementPolicy.firstManageFocusTarget(emptyList()))
    }

    @Test
    fun `info profile exposes activation and synchronization but no crud action`() {
        assertEquals(
            setOf(ProfileInfoAction.CHANGE_PROFILE, ProfileInfoAction.SYNCHRONIZE_PROFILE),
            ProfileManagementPolicy.infoActions,
        )
    }

    @Test
    fun `focused selected and active profiles remain distinct`() {
        val ids = listOf("admin", "selected", "focused")

        assertEquals("focused", ProfileManagementPolicy.detailsProfileId("focused", "selected", "admin", ids))
        assertEquals("selected", ProfileManagementPolicy.detailsProfileId(null, "selected", "admin", ids))
        assertEquals("admin", ProfileManagementPolicy.detailsProfileId(null, null, "admin", ids))
    }

    @Test
    fun `moving focus never activates a profile without confirmation`() {
        assertEquals(null, ProfileManagementPolicy.activationTarget("focused", confirmed = false))
        assertEquals("focused", ProfileManagementPolicy.activationTarget("focused", confirmed = true))
    }

    @Test
    fun `hidden count combines persisted kids rejects and current parental snapshot`() {
        assertEquals(482, ProfileManagementPolicy.hiddenItemCount(410, 72))
        assertEquals(0, ProfileManagementPolicy.hiddenItemCount(-1, -3))
    }

    @Test
    fun `deletion restores the same index or the previous neighbor`() {
        val before = listOf("admin", "one", "two")
        assertEquals("two", ProfileManagementPolicy.focusTargetAfterDeletion("one", before, listOf("admin", "two")))
        assertEquals("one", ProfileManagementPolicy.focusTargetAfterDeletion("two", before, listOf("admin", "one")))
        assertEquals(null, ProfileManagementPolicy.focusTargetAfterDeletion("admin", listOf("admin"), emptyList()))
    }
}
