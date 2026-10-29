package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class FinishedShelfTest {

    private val zone = ZoneOffset.UTC

    private fun at(date: String, hour: Int = 12) =
        LocalDate.parse(date).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun book(id: String, finished: String, status: BookStatus = BookStatus.FINISHED, rating: Float? = null) = Book(
        id = id, title = id, isbn = null, totalPages = 300, currentPage = 120, status = status.name,
        finishedAt = at(finished), rating = rating?.let { mapOf(RatingAxis.OVERALL to it) } ?: emptyMap()
    )

    private fun session(bookId: String, date: String, minutes: Int, hour: Int = 9) =
        Session(id = "$bookId$date$hour", bookId = bookId, startTime = at(date, hour), endTime = at(date, hour) + minutes * 60_000L, durationSeconds = minutes * 60)

    @Test
    fun `a read runs from its first session to the day it ended, both days counted`() {
        val sessions = listOf(session("a", "2026-03-01", 30), session("a", "2026-03-08", 45, hour = 22), session("b", "2026-03-02", 99))
        val read = FinishedShelf.read(book("a", "2026-03-08"), sessions, zone)
        assertEquals(8, read.days)
        assertEquals(75, read.minutes)
        assertEquals(300, read.pages)
    }

    @Test
    fun `no sessions means no known start, and a set-aside book counts the pages reached`() {
        val read = FinishedShelf.read(book("a", "2026-03-08", BookStatus.DNF), emptyList(), zone)
        assertNull(read.days)
        assertEquals(120, read.pages)
    }

    @Test
    fun `summary totals pages and averages only rated books`() {
        val reads = listOf(book("a", "2026-01-01", rating = 4f), book("b", "2026-02-01", rating = 5f), book("c", "2026-03-01"))
            .map { FinishedShelf.read(it, emptyList(), zone) }
        val summary = FinishedShelf.summary(reads)
        assertEquals(3, summary.books)
        assertEquals(900, summary.pages)
        assertEquals(4.5f, summary.averageRating!!, 0.001f)
    }

    @Test
    fun `reads sort newest first and group by year and month`() {
        val books = listOf(book("a", "2025-12-30"), book("b", "2026-03-02"), book("c", "2026-03-20"), book("d", "2026-01-01", BookStatus.DNF))
        val reads = FinishedShelf.reads(books, emptyList(), zone, FinishedShelf::isFinished)
        assertEquals(listOf("c", "b", "a"), reads.map { it.book.id })
        assertEquals(listOf(2026, 2025), FinishedShelf.years(reads, zone))
        assertEquals(listOf("c", "b"), FinishedShelf.inYear(reads, 2026, zone).map { it.book.id })
        assertEquals(listOf(YearMonth.of(2026, 3), YearMonth.of(2025, 12)), FinishedShelf.byMonth(reads, zone).map { it.first })
    }

    @Test
    fun `ordinals`() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "102nd"), listOf(1, 2, 3, 4, 11, 12, 13, 21, 102).map(FinishedShelf::ordinal))
    }
}
