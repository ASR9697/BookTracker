package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The numbers behind the Finished screen and the Reading Journey: when each
 * read began and ended, how long it took, and totals across a shelf.
 */
object FinishedShelf {

    /** One ended read (finished or set aside). [days] counts both ends; null when no start is known. */
    data class Read(
        val book: Book,
        val startedAt: Long?,
        val endedAt: Long,
        val days: Int?,
        val minutes: Int,
        val pages: Int
    ) {
        val finished: Boolean get() = book.status.equals(BookStatus.FINISHED.name, ignoreCase = true)
    }

    data class Summary(val books: Int, val pages: Int, val minutes: Int, val averageRating: Float?)

    fun isFinished(book: Book) = book.status.equals(BookStatus.FINISHED.name, ignoreCase = true)

    fun isAbandoned(book: Book) =
        book.status.equals(BookStatus.DNF.name, ignoreCase = true) ||
            book.status.equals(BookStatus.ABANDONED.name, ignoreCase = true)

    /**
     * The current read of [book]: sessions since it began (the re-read start on a
     * re-read) and up to the day it ended. The start is the first such session,
     * else the stored re-read start.
     */
    fun read(book: Book, sessions: List<Session>, zone: ZoneId): Read {
        val ended = book.completedAt
        val since = RereadMemories.currentReadStart(book, sessions) ?: 0L
        val endOfDay = Instant.ofEpochMilli(ended).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val mine = sessions.filter { it.bookId == book.id && it.startTime >= since && it.startTime < endOfDay }
        val started = mine.minOfOrNull { it.startTime }?.takeIf { it > 0 } ?: book.readStartedAt
        val days = started?.let {
            val from = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            val to = Instant.ofEpochMilli(ended).atZone(zone).toLocalDate()
            (ChronoUnit.DAYS.between(from, to) + 1).toInt().coerceAtLeast(1)
        }
        val pages = if (isFinished(book)) book.totalPages else book.currentPage
        return Read(book, started, ended, days, mine.sumOf { it.durationSeconds } / 60, pages)
    }

    /** Every ended read matching [include], newest first. */
    fun reads(books: List<Book>, sessions: List<Session>, zone: ZoneId, include: (Book) -> Boolean): List<Read> {
        val byBook = sessions.groupBy { it.bookId }
        return books.filter(include)
            .map { read(it, byBook[it.id].orEmpty(), zone) }
            .sortedByDescending { it.endedAt }
    }

    fun summary(reads: List<Read>): Summary {
        val ratings = reads.mapNotNull { it.book.overallRating }
        return Summary(
            books = reads.size,
            pages = reads.sumOf { it.pages },
            minutes = reads.sumOf { it.minutes },
            averageRating = ratings.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        )
    }

    /** Years that have at least one read, newest first. */
    fun years(reads: List<Read>, zone: ZoneId): List<Int> =
        reads.map { Instant.ofEpochMilli(it.endedAt).atZone(zone).year }.distinct().sortedDescending()

    fun inYear(reads: List<Read>, year: Int, zone: ZoneId): List<Read> =
        reads.filter { Instant.ofEpochMilli(it.endedAt).atZone(zone).year == year }

    /** Reads grouped by the month they ended, keeping the newest-first order. */
    fun byMonth(reads: List<Read>, zone: ZoneId): List<Pair<YearMonth, List<Read>>> =
        reads.groupBy { YearMonth.from(Instant.ofEpochMilli(it.endedAt).atZone(zone)) }
            .toList()
            .sortedByDescending { it.first }

    /** "1st", "2nd", "3rd", "11th", "22nd"… */
    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }
}
