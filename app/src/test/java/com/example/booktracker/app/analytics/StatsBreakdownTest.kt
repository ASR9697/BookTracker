package com.example.booktracker.app.analytics

import com.example.booktracker.app.analytics.StatsBreakdown.DayPart
import com.example.booktracker.app.analytics.StatsBreakdown.Length
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsBreakdownTest {

    private val utc = ZoneId.of("UTC")

    private fun at(time: LocalDateTime, minutes: Int, pages: Int = 10): Session {
        val start = time.atZone(utc).toInstant().toEpochMilli()
        return Session(
            id = "$time", bookId = "b", startTime = start,
            endTime = start + minutes * 60_000L, durationSeconds = minutes * 60, pagesRead = pages
        )
    }

    @Test
    fun `rhythm sums minutes by weekday and part of day`() {
        // 2026-10-25 is a Sunday.
        val grid = StatsBreakdown.rhythm(
            listOf(
                at(LocalDateTime.of(2026, 10, 25, 20, 0), 30),
                at(LocalDateTime.of(2026, 10, 25, 21, 30), 15),
                at(LocalDateTime.of(2026, 10, 26, 8, 0), 20)
            ),
            utc
        )
        assertEquals(45, grid[6][DayPart.EVENING.ordinal])
        assertEquals(20, grid[0][DayPart.MORNING.ordinal])
        assertEquals(DayOfWeek.SUNDAY to DayPart.EVENING, StatsBreakdown.peak(grid))
    }

    @Test
    fun `untimed progress still shows in the rhythm`() {
        val untimed = at(LocalDateTime.of(2026, 10, 27, 13, 0), 0, pages = 6).copy(durationSeconds = 0)
        assertEquals(12, StatsBreakdown.rhythm(listOf(untimed), utc)[1][DayPart.AFTERNOON.ordinal])
    }

    @Test
    fun `no reading means no peak`() {
        assertNull(StatsBreakdown.peak(StatsBreakdown.rhythm(emptyList(), utc)))
    }

    @Test
    fun `pace averages sessions and days to finish`() {
        val sessions = listOf(
            at(LocalDateTime.of(2026, 10, 1, 9, 0), 30, pages = 15),
            at(LocalDateTime.of(2026, 10, 2, 9, 0), 60, pages = 30)
        )
        val book = Book(id = "b", title = "B", isbn = null)
        val reads = listOf(
            FinishedShelf.Read(book, startedAt = 0, endedAt = 1, days = 4, minutes = 0, pages = 0),
            FinishedShelf.Read(book, startedAt = null, endedAt = 1, days = null, minutes = 0, pages = 0),
            FinishedShelf.Read(book, startedAt = 0, endedAt = 1, days = 10, minutes = 0, pages = 0)
        )
        val pace = StatsBreakdown.pace(sessions, reads)
        assertEquals(45, pace.averageSessionMinutes)
        assertEquals(60, pace.longestSessionMinutes)
        assertEquals(7f, pace.averageDaysToFinish!!, 0.001f)
        assertEquals(30f, pace.pagesPerHour!!, 0.001f)
    }

    private fun book(id: String, pages: Int, format: BookFormat = BookFormat.Paperback, stars: Float? = null) =
        Book(
            id = id, title = id, isbn = null, totalPages = pages, format = format,
            rating = stars?.let { mapOf(RatingAxis.OVERALL to it) } ?: emptyMap()
        )

    @Test
    fun `shelf splits lengths, formats and ratings`() {
        val read = listOf(book("a", 150), book("b", 320, BookFormat.Hardcover), book("c", 610), book("d", 0))
        val finished = listOf(read[0].copy(rating = mapOf(RatingAxis.OVERALL to 4.4f)), read[2].copy(rating = mapOf(RatingAxis.OVERALL to 5f)))
        val shelf = StatsBreakdown.shelf(read, finished, abandoned = 1)
        assertEquals(mapOf(Length.SHORT to 1, Length.MEDIUM to 1, Length.LONG to 1), shelf.lengths)
        assertEquals(BookFormat.Paperback to 3, shelf.formats.first())
        assertEquals(4.7f, shelf.averageRating!!, 0.001f)
        assertEquals(listOf(0, 0, 0, 1, 1), shelf.ratingSpread)
        assertEquals(2, shelf.finished)
        assertEquals(1, shelf.abandoned)
    }

    @Test
    fun `sessions in a period follow the reading day`() {
        val range = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        val inside = at(LocalDateTime.of(2026, 10, 31, 23, 0), 10)
        val outside = at(LocalDateTime.of(2026, 9, 30, 12, 0), 10)
        assertEquals(listOf(inside), StatsBreakdown.sessionsIn(listOf(inside, outside), range, utc, 0))
    }
}
