package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.domain.model.ParentalControlScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalControlScopeTest {
    @Test
    fun `global off disables every profile`() {
        val scope = ParentalControlScope(enabled = false)

        assertFalse(scope.isEnabledFor("admin"))
        assertFalse(scope.isEnabledFor("normal"))
    }

    @Test
    fun `global on applies to every profile except explicit exclusions`() {
        val scope = ParentalControlScope(enabled = true, disabledProfileIds = setOf("admin"))

        assertFalse(scope.isEnabledFor("admin"))
        assertTrue(scope.isEnabledFor("normal"))
        assertTrue(scope.isEnabledFor("new-profile"))
    }

    @Test
    fun `disabled profile ids survive serialization and invalid data fails open`() {
        val serialized = serializeParentalDisabledProfileIds(setOf("normal", "admin", "normal"))

        assertEquals(setOf("admin", "normal"), parseParentalDisabledProfileIds(serialized))
        assertEquals(emptySet<String>(), parseParentalDisabledProfileIds("invalid"))
    }
}
