package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.WishInfo
import com.example.booktracker.shared.models.WishPriority
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * The buying side of a book: what's wished for, what it'd cost, what a series
 * is missing, what's been spent this month and what comes out today. Reading
 * status is left alone, so a wished-for book can sit anywhere on the pile.
 */
object WishlistEngine {

    enum class Sort(val label: String) { PRIORITY("Priority"), ADDED("Newest"), PRICE("Price"), AUTHOR("Author") }

    /** Statuses that mean the reader has started a series and might want the next one. */
    private val STARTED = setOf(BookStatus.READING.name, BookStatus.PAUSED.name, BookStatus.FINISHED.name)

    fun wishlist(books: List<Book>): List<Book> = books.filter { it.ownership == Ownership.WISHLIST }

    /**
     * Books that are actually the reader's: everything but the wishlist. A wished-for
     * book isn't on the shelf yet, so piles, counts and "what next" lists leave it out.
     */
    fun onShelf(books: List<Book>): List<Book> = books.filter { it.ownership != Ownership.WISHLIST }

    fun info(book: Book): WishInfo = book.wish ?: WishInfo(addedAt = book.dateAdded)

    fun sorted(list: List<Book>, sort: Sort): List<Book> = when (sort) {
        Sort.PRIORITY -> list.sortedWith(compareBy<Book> { info(it).priority.ordinal }.thenByDescending { info(it).addedAt })
        Sort.ADDED -> list.sortedByDescending { info(it).addedAt }
        // Unpriced books go last rather than counting as free.
        Sort.PRICE -> list.sortedWith(compareBy<Book> { info(it).price == null }.thenBy { info(it).price ?: 0.0 })
        Sort.AUTHOR -> list.sortedBy { it.authors.firstOrNull()?.lowercase() ?: "￿" }
    }

    /** Expected cost per currency, and how many books have a price at all. */
    data class Total(val byCurrency: Map<String, Double>, val priced: Int, val count: Int)

    fun total(list: List<Book>): Total {
        val priced = list.mapNotNull { book -> info(book).let { w -> w.price?.let { w.currency.ifBlank { "" } to it } } }
        return Total(
            byCurrency = priced.groupBy({ it.first }, { it.second }).mapValues { (_, prices) -> prices.sum() },
            priced = priced.size,
            count = list.size
        )
    }

    /** A wished-for book in a series the reader has already started. */
    fun inStartedSeries(book: Book, all: List<Book>): Boolean {
        val series = book.seriesInfo?.takeIf { it.isPartOfSeries }?.seriesName?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
        return all.any { other ->
            other.id != book.id && other.status in STARTED &&
                other.seriesInfo?.seriesName?.trim()?.lowercase() == series
        }
    }

    /** The next number of a series the reader has started, when the library doesn't have it yet. */
    data class SeriesGap(val seriesName: String, val nextPosition: Int, val lastRead: Book)

    fun seriesGaps(all: List<Book>): List<SeriesGap> =
        all.filter { it.seriesInfo?.isPartOfSeries == true && !it.seriesInfo?.seriesName.isNullOrBlank() }
            .groupBy { it.seriesInfo!!.seriesName.trim().lowercase() }
            .mapNotNull { (_, books) ->
                val read = books.filter { it.status in STARTED && (it.seriesInfo?.position ?: 0) > 0 }
                val last = read.maxByOrNull { it.seriesInfo!!.position } ?: return@mapNotNull null
                val next = last.seriesInfo!!.position + 1
                if (books.any { it.seriesInfo?.position == next }) null
                else SeriesGap(last.seriesInfo!!.seriesName.trim(), next, last)
            }
            .sortedByDescending { it.lastRead.lastUpdated }

    /** What was spent on books in [month], per currency, from the purchase log. */
    fun spentIn(all: List<Book>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Map<String, Double> =
        all.flatMap { it.purchaseLog }
            .filter { YearMonth.from(Instant.ofEpochMilli(it.date).atZone(zone)) == month }
            .groupBy({ it.currency.ifBlank { "" } }, { it.price })
            .mapValues { (_, prices) -> prices.sum() }

    /** Wished-for books that come out on [day]. */
    fun releasedOn(all: List<Book>, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Book> =
        wishlist(all).filter { book ->
            info(book).releaseDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == day } == true
        }

    /** Not out yet as of [today]. */
    fun isUpcoming(book: Book, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        info(book).releaseDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().isAfter(today) } == true

    /** The list as plain text for a message: priority groups, then title, author and ISBN. */
    fun shareText(list: List<Book>, ownerName: String): String = buildString {
        append(if (ownerName.isBlank()) "My book wishlist" else "$ownerName's book wishlist")
        append('\n')
        WishPriority.entries.forEach { priority ->
            val group = list.filter { info(it).priority == priority }
            if (group.isEmpty()) return@forEach
            append('\n').append(priority.label).append('\n')
            group.forEach { book ->
                append("• ").append(book.title)
                book.authors.firstOrNull()?.let { append(" by ").append(it) }
                book.isbn?.takeIf { it.isNotBlank() }?.let { append(" (ISBN ").append(it).append(')') }
                append('\n')
            }
        }
    }.trimEnd()

    /** A web search that finds the book for sale, at the store the reader noted when there is one. */
    fun findQuery(book: Book): String = listOfNotNull(
        book.title,
        book.authors.firstOrNull(),
        book.isbn?.takeIf { it.isNotBlank() },
        info(book).store.takeIf { it.isNotBlank() } ?: "buy"
    ).joinToString(" ")
}
