package com.example.booktracker.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidthTierTest {

    @Test
    fun `breakpoints follow the Material window classes`() {
        assertEquals(WidthTier.COMPACT, WidthTier.forWidth(411))
        assertEquals(WidthTier.MEDIUM, WidthTier.forWidth(600))
        assertEquals(WidthTier.MEDIUM, WidthTier.forWidth(839))
        assertEquals(WidthTier.EXPANDED, WidthTier.forWidth(840))
        assertEquals(WidthTier.EXPANDED, WidthTier.forWidth(1199))
        assertEquals(WidthTier.LARGE, WidthTier.forWidth(1280))
    }

    @Test
    fun `tiers compare as wider-or-equal`() {
        assertTrue(WidthTier.LARGE.atLeastExpanded)
        assertTrue(WidthTier.MEDIUM.atLeastMedium)
        assertFalse(WidthTier.MEDIUM.atLeastExpanded)
        assertFalse(WidthTier.COMPACT.atLeastMedium)
    }
}
