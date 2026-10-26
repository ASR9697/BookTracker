package com.example.booktracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `missing or blank setting gives the default layout`() {
        assertEquals(HomeLayout.DEFAULT, HomeLayout.decode(null))
        assertEquals(HomeLayout.DEFAULT, HomeLayout.decode(""))
    }

    @Test
    fun `encode and decode round-trip order and hidden sections`() {
        val layout = HomeLayout.DEFAULT.move(HomeLayout.DEFAULT.order.lastIndex, 0).toggle(HomeSection.GOALS)
        assertEquals(layout, HomeLayout.decode(layout.encode()))
        assertEquals(HomeSection.SHORTCUTS, layout.order.first())
        assertEquals(false, HomeSection.GOALS in layout.visible)
    }

    @Test
    fun `unknown and repeated names are dropped and new sections appended`() {
        // An old save that predates UP_NEXT and SHORTCUTS, with junk from a future version.
        val layout = HomeLayout.decode("-MONTHLY_PAGES,STREAK,RECOMMENDATIONS,STREAK,GOALS")
        assertEquals(
            listOf(
                HomeSection.MONTHLY_PAGES, HomeSection.STREAK, HomeSection.GOALS,
                HomeSection.UP_NEXT, HomeSection.OWNERSHIP, HomeSection.SHORTCUTS
            ),
            layout.order
        )
        assertEquals(setOf(HomeSection.MONTHLY_PAGES), layout.hidden)
    }

    @Test
    fun `move ignores out-of-range indices`() {
        assertEquals(HomeLayout.DEFAULT, HomeLayout.DEFAULT.move(-1, 2))
        assertEquals(HomeLayout.DEFAULT, HomeLayout.DEFAULT.move(0, 9))
    }

    @Test
    fun `toggle hides then shows a section`() {
        val hidden = HomeLayout.DEFAULT.toggle(HomeSection.STREAK)
        assertEquals(setOf(HomeSection.STREAK), hidden.hidden)
        assertEquals(HomeLayout.DEFAULT, hidden.toggle(HomeSection.STREAK))
    }
}
