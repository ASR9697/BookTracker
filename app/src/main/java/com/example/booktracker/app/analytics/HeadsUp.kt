package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * What on Home needs the reader today, most pressing first: library books overdue
 * or due soon, books planned for today, wished-for books out today, and one memory
 * from this date in an earlier year. Empty on an ordinary day, so the card it fills
 * stays out of the way.
 */
object HeadsUp {

    enum class Kind { LOAN_OVERDUE, LOAN_DUE, PLANNED, RELEASED, ON_THIS_DAY }

    data class Item(val kind: Kind, val book: Book, val title: String, val detail: String)

    fun today(
        books: List<Book>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        notes: List<MarginNote> = emptyList()
    ): List<Item> {
        val loans = LoanEngine.urgentLoans(books, today, zone).map { (book, status) ->
            val overdue = status.urgency == LoanEngine.LoanUrgency.OVERDUE
            Item(
                kind = if (overdue) Kind.LOAN_OVERDUE else Kind.LOAN_DUE,
                book = book,
                title = if (overdue) "Library book overdue" else "Library book due soon",
                detail = "${book.title} · ${status.detailText}"
            )
        }
        // The planner stores a day as UTC midnight, so it's compared as an epoch day. A
        // finished book planned for today counts too: the planner offers them, for re-reads.
        val planned = WishlistEngine.onShelf(books)
            .filter { it.plannedDate?.let { at -> Math.floorDiv(at, DAY_MS) } == today.toEpochDay() }
            .map { Item(Kind.PLANNED, it, "Planned for today", it.title) }
        val released = WishlistEngine.releasedOn(books, today, zone)
            .map { Item(Kind.RELEASED, it, "Out today from your wishlist", it.title) }
        return loans + planned + released + listOfNotNull(onThisDay(books, notes, today, zone))
    }

    /**
     * One memory from this date in an earlier year, the most recent year first: a book
     * finished then, else a highlight saved then (a favourite before any other).
     */
    fun onThisDay(books: List<Book>, notes: List<MarginNote>, today: LocalDate, zone: ZoneId): Item? {
        fun yearIfThisDay(millis: Long): Int? {
            val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            return day.year.takeIf {
                day.monthValue == today.monthValue && day.dayOfMonth == today.dayOfMonth && day.year < today.year
            }
        }
        books.mapNotNull { book -> book.finishedAt?.let(::yearIfThisDay)?.let { it to book } }
            .maxByOrNull { it.first }
            ?.let { (year, book) -> return Item(Kind.ON_THIS_DAY, book, "On this day in $year", "You finished ${book.title}") }
        val byId = books.associateBy { it.id }
        return notes
            .filter { it.content.isNotBlank() && (it.isFavorite || it.type == NoteType.BOOK_CONTENT) }
            .mapNotNull { note -> yearIfThisDay(note.timestamp)?.let { year -> byId[note.bookId]?.let { Triple(year, note, it) } } }
            .maxWithOrNull(compareBy({ it.first }, { it.second.isFavorite }))
            ?.let { (year, note, book) ->
                Item(Kind.ON_THIS_DAY, book, "On this day in $year", "“${ReaderProfileEngine.quoteLine(note.content)}” · ${book.title}")
            }
    }

    private const val DAY_MS = 86_400_000L
}
