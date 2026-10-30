package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Classification
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class ReaderProfileEngineTest {

    private val utc = ZoneId.of("UTC")

    private fun finished(
        id: String,
        author: String = "A",
        stars: Float? = null,
        pages: Int = 300,
        at: Long = 1_000L,
        tags: List<String> = emptyList(),
        format: BookFormat = BookFormat.Paperback
    ) = Book(
        id = id, title = id, isbn = null, totalPages = pages, status = BookStatus.FINISHED.name,
        finishedAt = at, creators = listOf(Creator(CreatorRole.Author, author)), format = format,
        rating = stars?.let { mapOf(RatingAxis.OVERALL to it) } ?: emptyMap(),
        classification = Classification(emptyList(), tags)
    )

    @Test
    fun `hearted books come first, else the top rated`() {
        val hearted = finished("h", stars = 3f).copy(isFavorite = true)
        val loved = finished("loved", stars = 5f)
        val good = finished("good", stars = 4f)
        val meh = finished("meh", stars = 3f)
        assertEquals(ReaderProfileEngine.Favourites(listOf(hearted), marked = true), ReaderProfileEngine.favourites(listOf(hearted, loved)))
        val fallback = ReaderProfileEngine.favourites(listOf(meh, good, loved))
        assertFalse(fallback.marked)
        assertEquals(listOf("loved", "good"), fallback.books.map { it.id })
    }

    @Test
    fun `a wished-for book is never a favourite`() {
        val wished = finished("w", stars = 5f).copy(isFavorite = true, ownership = Ownership.WISHLIST)
        assertTrue(ReaderProfileEngine.favourites(listOf(wished)).books.isEmpty())
    }

    @Test
    fun `top authors by books finished, ties to the most recent`() {
        val authors = ReaderProfileEngine.topAuthors(
            listOf(
                finished("1", "Le Guin", 4f, at = 10), finished("2", "Le Guin", 5f, at = 20),
                finished("3", "Pratchett", at = 30), finished("4", "Austen", at = 40)
            )
        )
        assertEquals(listOf("Le Guin" to 2, "Austen" to 1, "Pratchett" to 1), authors.map { it.name to it.books })
        assertEquals("2", authors.first().cover.id)
    }

    private fun sessionAt(hour: Int, day: Int = 1, minutes: Int = 30, pages: Int = 20, bookId: String = "x"): Session {
        val start = LocalDateTime.of(2026, 10, day, hour, 0).atZone(utc).toInstant().toEpochMilli()
        return Session(
            id = "$day$hour", bookId = bookId, startTime = start, endTime = start + minutes * 60_000L,
            durationSeconds = minutes * 60, pagesRead = pages
        )
    }

    @Test
    fun `taste sentence names genres, a clear format and the time of day`() {
        val books = listOf(
            finished("1", tags = listOf("#Fantasy", "#History"), format = BookFormat.`E-book`),
            finished("2", tags = listOf("#Fantasy"), format = BookFormat.`E-book`),
            finished("3", tags = listOf("#History"), format = BookFormat.`E-book`)
        )
        assertEquals(
            "Mostly Fantasy and History, in ebooks, at night.",
            ReaderProfileEngine.tasteSentence(books, listOf(sessionAt(23), sessionAt(23, 2)), utc)
        )
    }

    @Test
    fun `taste sentence waits for something to say`() {
        assertNull(ReaderProfileEngine.tasteSentence(listOf(finished("1")), emptyList(), utc))
        assertEquals("Reads mostly in the morning.", ReaderProfileEngine.tasteSentence(emptyList(), listOf(sessionAt(8)), utc))
    }

    @Test
    fun `records pick the bests and skip what has no data`() {
        val long = finished("long", pages = 612)
        val reread = finished("again").copy(readCount = 2)
        val records = ReaderProfileEngine.records(
            listOf(long, reread),
            listOf(sessionAt(21, day = 3, minutes = 100, pages = 84, bookId = "long"), sessionAt(9, day = 4, minutes = 20, pages = 10)),
            utc, dayStartsAtHour = 0, locale = Locale.ENGLISH
        ).associateBy { it.label }
        assertEquals("612 pages", records["Longest book"]?.value)
        assertEquals("Read 3 times", records["Most re-read"]?.value)
        assertEquals("84 pages · Oct 3, 2026", records["Biggest day"]?.value)
        assertEquals("1h 40m", records["Longest sitting"]?.value)
        assertEquals("long", records["Longest sitting"]?.book?.id)
        assertNull(records["Languages read"])
    }

    @Test
    fun `year by year counts books and pages per year, newest first`() {
        val at2025 = LocalDateTime.of(2025, 6, 1, 12, 0).atZone(utc).toInstant().toEpochMilli()
        val at2026 = LocalDateTime.of(2026, 2, 1, 12, 0).atZone(utc).toInstant().toEpochMilli()
        val rows = ReaderProfileEngine.yearByYear(
            listOf(finished("a", at = at2025), finished("b", at = at2026), finished("c", at = at2026)),
            listOf(sessionAt(21, day = 3, pages = 40)),
            utc, dayStartsAtHour = 0
        )
        assertEquals(
            listOf(ReaderProfileEngine.YearRow(2026, 2, 40), ReaderProfileEngine.YearRow(2025, 1, 0)),
            rows
        )
    }

    @Test
    fun `a quote reads as one line`() {
        assertEquals(
            "SHIFT AN ARGUMENT Instead of looking at it",
            ReaderProfileEngine.quoteLine("  SHIFT AN ARGUMENT\n\n  Instead of looking\nat it \n")
        )
    }

    @Test
    fun `levels climb with lifetime pages`() {
        assertEquals(ReaderProfileEngine.Level("First pages", "Page turner", 100), ReaderProfileEngine.level(0))
        assertEquals(ReaderProfileEngine.Level("Bookworm", "Avid reader", 3_182), ReaderProfileEngine.level(1_818))
        assertEquals(ReaderProfileEngine.Level("Living library", null, null), ReaderProfileEngine.level(250_000))
    }

    private fun note(id: String, at: Long, favourite: Boolean = true, text: String = "line") =
        com.example.booktracker.shared.models.MarginNote(id = id, bookId = "b", content = text, timestamp = at, isFavorite = favourite)

    @Test
    fun `pinned quote is the chosen one while it lasts, else the newest favourite`() {
        val notes = listOf(note("old", 1), note("new", 3), note("plain", 5, favourite = false), note("blank", 9, text = " "))
        assertEquals("old", ReaderProfileEngine.pinnedQuote(notes, "old")?.id)
        assertEquals("new", ReaderProfileEngine.pinnedQuote(notes, "")?.id)
        // A pinned note that was deleted or un-favourited falls back too.
        assertEquals("new", ReaderProfileEngine.pinnedQuote(notes, "plain")?.id)
        assertNull(ReaderProfileEngine.pinnedQuote(listOf(note("p", 1, favourite = false)), ""))
    }
}
