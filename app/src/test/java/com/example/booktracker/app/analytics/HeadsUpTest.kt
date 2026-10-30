package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.LoanRecord
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.WishInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class HeadsUpTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 30)

    private fun millis(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun book(id: String, block: Book.() -> Book = { this }) = Book(id = id, title = id, isbn = null).block()

    private fun borrowed(id: String, due: LocalDate) = book(id) {
        copy(loanRecord = listOf(LoanRecord(loanDate = millis(due.minusDays(21)), dueDate = millis(due), lender = "Library", memo = "")))
    }

    /** As the planner stores it: the day at UTC midnight. */
    private fun planned(id: String, day: LocalDate, status: BookStatus = BookStatus.TO_READ) = book(id) {
        copy(plannedDate = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), status = status.name)
    }

    private fun wished(id: String, release: LocalDate) = book(id) {
        copy(ownership = Ownership.WISHLIST, wish = WishInfo(releaseDate = millis(release)))
    }

    @Test
    fun `an ordinary day has nothing`() {
        assertTrue(HeadsUp.today(listOf(book("a"), planned("b", today.plusDays(1))), today, zone).isEmpty())
    }

    @Test
    fun `loans come first, then plans, then releases`() {
        val items = HeadsUp.today(
            listOf(
                wished("out", today),
                planned("plan", today),
                borrowed("soon", today.plusDays(2)),
                borrowed("late", today.minusDays(3))
            ),
            today, zone
        )
        assertEquals(listOf("late", "soon", "plan", "out"), items.map { it.book.id })
        assertEquals(
            listOf(HeadsUp.Kind.LOAN_OVERDUE, HeadsUp.Kind.LOAN_DUE, HeadsUp.Kind.PLANNED, HeadsUp.Kind.RELEASED),
            items.map { it.kind }
        )
    }

    @Test
    fun `a finished book or a wished one isn't planned reading`() {
        val items = HeadsUp.today(
            listOf(
                planned("done", today, BookStatus.FINISHED),
                planned("wished", today).copy(ownership = Ownership.WISHLIST)
            ),
            today, zone
        )
        assertTrue(items.isEmpty())
    }

    private fun finishedOn(id: String, day: LocalDate) = book(id) { copy(status = BookStatus.FINISHED.name, finishedAt = millis(day)) }

    private fun note(bookId: String, day: LocalDate, text: String, favorite: Boolean = false, type: NoteType = NoteType.BOOK_CONTENT) =
        MarginNote(id = "$bookId$text", bookId = bookId, content = text, timestamp = millis(day) + 3_600_000L, isFavorite = favorite, type = type)

    @Test
    fun `on this day prefers the most recent year's finished book`() {
        val item = HeadsUp.onThisDay(
            listOf(finishedOn("old", today.minusYears(3)), finishedOn("recent", today.minusYears(1)), finishedOn("now", today)),
            emptyList(), today, zone
        )
        assertEquals("recent", item?.book?.id)
        assertEquals("On this day in 2025", item?.title)
        assertEquals("You finished recent", item?.detail)
    }

    @Test
    fun `on this day falls back to a highlight, favourites first`() {
        val books = listOf(book("b"))
        val notes = listOf(
            note("b", today.minusYears(1), "plain"),
            note("b", today.minusYears(1), "loved", favorite = true),
            note("b", today.minusYears(1), "a thought", type = NoteType.PERSONAL_THOUGHT),
            note("b", today.minusDays(1).minusYears(1), "wrong day", favorite = true)
        )
        val item = HeadsUp.onThisDay(books, notes, today, zone)
        assertEquals("“loved” · b", item?.detail)
    }

    @Test
    fun `nothing from this date means no memory`() {
        assertNull(HeadsUp.onThisDay(listOf(finishedOn("x", today.minusDays(2))), emptyList(), today, zone))
    }
}
