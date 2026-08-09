package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the "time remaining" chain that the session result screen depends on.
 * These broke silently before, because timer sessions were persisted with
 * durationSeconds = 0 and were filtered out downstream.
 */
class AnalyticsEngineTest {

    private fun timedSession(pages: Int, minutes: Long, id: String = "s"): Session {
        val start = 1_000_000L
        return Session(
            id = id,
            bookId = "b",
            startTime = start,
            endTime = start + minutes * 60_000L,
            durationSeconds = (minutes * 60).toInt(),
            startPage = 0,
            endPage = pages,
            pagesRead = pages
        )
    }

    private fun book(total: Int, current: Int) =
        Book(id = "b", title = "T", isbn = null, totalPages = total, currentPage = current)

    @Test
    fun `pages per hour is null without any timed session`() {
        assertNull(AnalyticsEngine.pagesPerHour(emptyList()))
    }

    @Test
    fun `instant progress deltas do not count as timed sessions`() {
        // startTime == endTime is how progress logged outside a session is stored.
        val instant = Session(id = "i", bookId = "b", startTime = 5L, endTime = 5L, pagesRead = 30)
        assertNull(AnalyticsEngine.pagesPerHour(listOf(instant)))
    }

    @Test
    fun `pages per hour weights by total time not per-session average`() {
        // 60 pages in 60 min, then 10 pages in 60 min => 70 pages / 2h = 35 p/h.
        // A naive mean of the two rates would give 35 too, so make them uneven:
        // 90 pages in 30 min and 10 pages in 90 min => 100 pages / 2h = 50 p/h.
        val sessions = listOf(
            timedSession(pages = 90, minutes = 30, id = "a"),
            timedSession(pages = 10, minutes = 90, id = "b")
        )
        val rate = AnalyticsEngine.pagesPerHour(sessions)!!
        assertEquals(50f, rate, 0.01f)
    }

    @Test
    fun `estimated minutes left uses measured velocity`() {
        // 60 pages/hour, 120 pages remaining => 120 minutes.
        val sessions = listOf(timedSession(pages = 60, minutes = 60))
        val left = AnalyticsEngine.estimatedMinutesLeft(book(total = 200, current = 80), sessions)
        assertEquals(120L, left)
    }

    @Test
    fun `estimated minutes left is zero on a finished book`() {
        val sessions = listOf(timedSession(pages = 60, minutes = 60))
        assertEquals(0L, AnalyticsEngine.estimatedMinutesLeft(book(300, 300), sessions))
    }

    @Test
    fun `estimated minutes left is unknown without a total page count`() {
        val sessions = listOf(timedSession(pages = 60, minutes = 60))
        assertNull(AnalyticsEngine.estimatedMinutesLeft(book(total = 0, current = 40), sessions))
    }

    @Test
    fun `estimated minutes left is unknown before any timed reading`() {
        assertNull(AnalyticsEngine.estimatedMinutesLeft(book(300, 40), emptyList()))
    }

    @Test
    fun `format minutes renders hours and minutes`() {
        assertEquals("<1m", AnalyticsEngine.formatMinutes(0))
        assertEquals("45m", AnalyticsEngine.formatMinutes(45))
        assertEquals("2h 15m", AnalyticsEngine.formatMinutes(135))
        assertTrue(AnalyticsEngine.formatMinutes(743).startsWith("12h"))
    }
}
