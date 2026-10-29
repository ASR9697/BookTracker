package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Classification
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WrappedEngineTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 10, 26)
    private fun at(date: LocalDate): Long = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun session(bookId: String, date: LocalDate, pages: Int) = Session(
        id = "$bookId-$date", bookId = bookId, startTime = at(date), endTime = at(date) + 3_600_000L,
        durationSeconds = 3600, endPage = pages, pagesRead = pages
    )

    private fun book(id: String, finished: LocalDate? = null, rating: Float? = null, tags: List<String> = emptyList(), author: String = "A") =
        Book(
            id = id, title = id, isbn = null,
            status = if (finished != null) BookStatus.FINISHED.name else BookStatus.READING.name,
            finishedAt = finished?.let { at(it) },
            rating = rating?.let { mapOf("Overall" to it) } ?: emptyMap(),
            classification = Classification(emptyList(), tags),
            creators = listOf(Creator(CreatorRole.Author, author))
        )

    @Test
    fun `year only counts this year and finds the busiest month`() {
        val books = listOf(book("old", finished = LocalDate.of(2025, 12, 30)), book("new", finished = LocalDate.of(2026, 9, 3)))
        val sessions = listOf(
            session("old", LocalDate.of(2025, 12, 29), 300),
            session("new", LocalDate.of(2026, 8, 1), 50),
            session("new", LocalDate.of(2026, 9, 2), 120)
        )
        val summary = WrappedEngine.summarize(books, sessions, emptyList(), isYear = true, today = today, zone = zone, dayStartsAtHour = 0)

        assertEquals(listOf("new"), summary.booksFinished.map { it.id })
        assertEquals(170, summary.pagesRead)
        assertEquals(10, summary.buckets.size) // Jan..Oct
        assertEquals(120, summary.busiest?.pages)
    }

    @Test
    fun `top genre skips format tags and top book prefers the rating`() {
        val books = listOf(
            book("a", finished = LocalDate.of(2026, 3, 1), rating = 3f, tags = listOf("#Audiobook", "#Audiobook", "Memoir")),
            book("b", finished = LocalDate.of(2026, 4, 1), rating = 5f, tags = listOf("#Audiobook"))
        )
        val summary = WrappedEngine.summarize(books, emptyList(), emptyList(), true, today, zone, 0)
        assertEquals("Memoir", summary.topGenre)
        assertEquals("b", summary.topBook?.id)
    }

    @Test
    fun `quote prefers a favourite passage and skips ones too long for a slide`() {
        val notes = listOf(
            MarginNote(id = "long", content = "x".repeat(WrappedEngine.MAX_QUOTE_CHARS + 1), isFavorite = true, timestamp = at(today)),
            MarginNote(id = "plain", content = "A passage that is long enough to quote.", timestamp = at(today)),
            MarginNote(id = "fav", content = "Short and loved.", isFavorite = true, type = NoteType.BOOK_CONTENT, timestamp = at(today))
        )
        assertEquals("fav", WrappedEngine.pickQuote(notes)?.id)
    }

    @Test
    fun `month mode is empty with no reading this month`() {
        val sessions = listOf(session("x", LocalDate.of(2026, 9, 10), 40))
        val summary = WrappedEngine.summarize(emptyList(), sessions, emptyList(), isYear = false, today = today, zone = zone, dayStartsAtHour = 0)
        assertTrue(summary.isEmpty)
        assertNull(summary.busiest)
        assertEquals("October 2026", summary.periodLabel)
    }
}
