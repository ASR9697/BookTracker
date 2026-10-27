package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.SeriesInfo
import com.example.booktracker.shared.models.Session
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeEngineTest {

    private fun finished(n: Int) = (1..n).map {
        Book(id = "b$it", title = "B$it", isbn = null, status = BookStatus.FINISHED.name)
    }

    private fun statusOf(id: String, input: BadgeInput) =
        BadgeEngine.statuses(input).first { it.badge.id == id }

    @Test
    fun `streak badges use the longest streak, so they stay earned after a break`() {
        val input = BadgeInput(emptyList(), emptyList(), longestStreak = 31, notesCount = 0)
        val unlocked = BadgeEngine.evaluate(input)
        assertTrue("streak_keeper" in unlocked)
        assertTrue("century_streak" in unlocked)
    }

    @Test
    fun `locked badge reports progress toward its target`() {
        val status = statusOf("century_streak", BadgeInput(emptyList(), emptyList(), longestStreak = 20, notesCount = 0))
        assertFalse(status.unlocked)
        assertEquals(20f / 30f, status.fraction, 0.001f)
        assertEquals("20 / 30 days", status.progressLabel)
    }

    @Test
    fun `progress label caps at the target once earned`() {
        val status = statusOf("bookworm", BadgeInput(finished(8), emptyList(), longestStreak = 0, notesCount = 0))
        assertTrue(status.unlocked)
        assertEquals(1f, status.fraction, 0f)
        assertEquals("5 / 5 books", status.progressLabel)
    }

    @Test
    fun `next up is the locked badge closest to its target`() {
        // Bookworm at 4/5 beats Bibliophile at 4/20 and Fire Walker at 0/30.
        val statuses = BadgeEngine.statuses(BadgeInput(finished(4), emptyList(), longestStreak = 0, notesCount = 0))
        assertEquals("bookworm", BadgeEngine.nextUp(statuses)?.badge?.id)
    }

    private val zone: ZoneId = ZoneId.of("UTC")
    private fun at(date: LocalDate, hour: Int = 12): Long = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun sessionOn(date: LocalDate, pages: Int = 10): Session {
        val start = at(date)
        return Session(id = "s-$date", bookId = "b", startTime = start, endTime = start + 1_800_000L,
            durationSeconds = 1800, endPage = pages, pagesRead = pages)
    }

    @Test
    fun `earned date is the session that first met the target`() {
        // A 7-day streak from Jul 1 is met on Jul 7.
        val start = LocalDate.of(2026, 7, 1)
        val sessions = (0L..9L).map { sessionOn(start.plusDays(it)) }
        val input = BadgeInput(emptyList(), sessions, longestStreak = 10, notesCount = 0)
        val dated = BadgeEngine.withEarnedDates(BadgeEngine.statuses(input), emptyList(), sessions, emptyList(), 0, zone)

        assertEquals(at(start.plusDays(6)), dated.first { it.badge.id == "streak_keeper" }.earnedAt)
        assertEquals(at(start), dated.first { it.badge.id == "first_chapter" }.earnedAt)
        assertEquals(null, dated.first { it.badge.id == "century_streak" }.earnedAt)
    }

    @Test
    fun `earned date for a books badge is when the fifth book was finished`() {
        val books = (1..6).map {
            Book(id = "b$it", title = "B$it", isbn = null, status = BookStatus.FINISHED.name,
                finishedAt = at(LocalDate.of(2026, 8, it)))
        }
        val input = BadgeInput(books, emptyList(), 0, 0)
        val dated = BadgeEngine.withEarnedDates(BadgeEngine.statuses(input), books, emptyList(), emptyList(), 0, zone)
        assertEquals(at(LocalDate.of(2026, 8, 5)), dated.first { it.badge.id == "bookworm" }.earnedAt)
    }

    @Test
    fun `monthly challenge only counts this month and rotates by month`() {
        val oct = LocalDate.of(2026, 10, 26)
        val sessions = listOf(sessionOn(LocalDate.of(2026, 9, 30), 500), sessionOn(LocalDate.of(2026, 10, 2), 40))
        val october = BadgeEngine.monthlyChallenge(emptyList(), sessions, emptyList(), oct, zone, 0)
        val november = BadgeEngine.monthlyChallenge(emptyList(), sessions, emptyList(), oct.plusMonths(1), zone, 0)

        assertEquals(BadgeEngine.MONTHLY_TIER, october.badge.tier)
        assertTrue(october.badge.id != november.badge.id)
        // September's 500 pages never leak into October.
        assertTrue(october.current <= 40)
    }

    @Test
    fun `new series badge counts finished books in one series`() {
        fun inSeries(id: String, series: String) = Book(
            id = id, title = id, isbn = null, status = BookStatus.FINISHED.name,
            seriesInfo = SeriesInfo(isPartOfSeries = true, seriesName = series, position = 1)
        )
        val books = listOf(inSeries("a", "Dune"), inSeries("b", "dune "), inSeries("c", "Dune"), inSeries("d", "Other"))
        assertTrue("series_completer" in BadgeEngine.evaluate(BadgeInput(books, emptyList(), 0, 0)))
    }

    @Test
    fun `next up is null when everything is earned`() {
        val statuses = BadgeEngine.statuses(BadgeInput(emptyList(), emptyList(), 0, 0))
            .map { it.copy(current = it.badge.target) }
        assertEquals(null, BadgeEngine.nextUp(statuses))
    }
}
