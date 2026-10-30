package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import java.time.LocalDate
import java.time.ZoneId

/**
 * What on Home needs the reader today, most pressing first: library books overdue
 * or due soon, books planned for today, and wished-for books out today. Empty on
 * an ordinary day, so the card it fills stays out of the way.
 */
object HeadsUp {

    enum class Kind { LOAN_OVERDUE, LOAN_DUE, PLANNED, RELEASED }

    data class Item(val kind: Kind, val book: Book, val title: String, val detail: String)

    fun today(books: List<Book>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Item> {
        val loans = LoanEngine.urgentLoans(books, today, zone).map { (book, status) ->
            val overdue = status.urgency == LoanEngine.LoanUrgency.OVERDUE
            Item(
                kind = if (overdue) Kind.LOAN_OVERDUE else Kind.LOAN_DUE,
                book = book,
                title = if (overdue) "Library book overdue" else "Library book due soon",
                detail = "${book.title} · ${status.detailText}"
            )
        }
        // The planner stores a day as UTC midnight, so it's compared as an epoch day.
        val planned = WishlistEngine.onShelf(books)
            .filter { it.plannedDate?.let { at -> Math.floorDiv(at, DAY_MS) } == today.toEpochDay() }
            .filterNot { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
            .map { Item(Kind.PLANNED, it, "Planned for today", it.title) }
        val released = WishlistEngine.releasedOn(books, today, zone)
            .map { Item(Kind.RELEASED, it, "Out today from your wishlist", it.title) }
        return loans + planned + released
    }

    private const val DAY_MS = 86_400_000L
}
