package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Everything the Wrapped story shows for one period (this year or this month),
 * worked out once so every slide and the share card agree.
 */
object WrappedEngine {

    /** Quotes longer than this don't fit a slide. */
    const val MAX_QUOTE_CHARS = 280

    data class Bucket(val label: String, val pages: Int)

    data class AuthorStat(val name: String, val books: Int, val pages: Int)

    data class Summary(
        val isYear: Boolean,
        /** "2026" or "October 2026". */
        val periodLabel: String,
        val booksFinished: List<Book>,
        val pagesRead: Int,
        val minutesRead: Long,
        val activeDays: Int,
        val longestStreak: Int,
        val topGenre: String?,
        val topVibe: String?,
        val topBook: Book?,
        val topAuthor: AuthorStat?,
        /** Pages per month (year) or per day (month), oldest first, for the bar chart. */
        val buckets: List<Bucket>,
        val quote: MarginNote?,
        val quoteBook: Book?
    ) {
        val busiest: Bucket? get() = buckets.filter { it.pages > 0 }.maxByOrNull { it.pages }
        val isEmpty: Boolean get() = booksFinished.isEmpty() && pagesRead == 0 && activeDays == 0
    }

    fun summarize(
        books: List<Book>,
        sessions: List<Session>,
        notes: List<MarginNote>,
        isYear: Boolean,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
    ): Summary {
        val start = if (isYear) today.withDayOfYear(1) else today.withDayOfMonth(1)
        fun inPeriod(day: LocalDate) = !day.isBefore(start) && !day.isAfter(today)
        fun inPeriod(millis: Long) = millis > 0 && inPeriod(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())

        val periodSessions = sessions.filter { it.startTime > 0 && inPeriod(StreakEngine.sessionDay(it, zone, dayStartsAtHour)) }
        val pagesByDay = StreakEngine.pagesByDay(periodSessions, zone, dayStartsAtHour)

        val finished = books
            .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) && inPeriod(it.completedAt) }
            .sortedBy { it.completedAt }
        val pagesByBook = periodSessions.filter(StreakEngine::isReading)
            .groupBy { it.bookId }.mapValues { (_, list) -> list.sumOf { it.pagesRead } }
        val booksById = books.associateBy { it.id }
        val periodBooks = (finished + pagesByBook.keys.mapNotNull { booksById[it] }).distinctBy { it.id }

        // Same rule as the profile: books finished without any logged reading still count their pages.
        val untrackedPages = finished.filter { it.id !in pagesByBook }.sumOf { it.totalPages }

        return Summary(
            isYear = isYear,
            periodLabel = if (isYear) today.year.toString() else today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            booksFinished = finished,
            pagesRead = pagesByDay.values.sum() + untrackedPages,
            minutesRead = AnalyticsEngine.timedSessions(periodSessions).sumOf { it.endTime - it.startTime } / 60_000L,
            activeDays = pagesByDay.size,
            longestStreak = StreakEngine.longestStreak(pagesByDay.keys),
            topGenre = AnalyticsEngine.topGenre(periodBooks),
            topVibe = periodSessions.mapNotNull { it.environmentTag.takeIf { tag -> tag.isNotBlank() } }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
            topBook = topBook(finished, periodBooks, pagesByBook),
            topAuthor = topAuthor(periodBooks, pagesByBook),
            buckets = buckets(pagesByDay, isYear, start, today),
            quote = pickQuote(notes.filter { inPeriod(it.timestamp) }),
            quoteBook = null
        ).let { summary -> summary.copy(quoteBook = summary.quote?.let { booksById[it.bookId] }) }
    }

    /** Best-rated finished book; with nothing rated, the one read most this period. */
    private fun topBook(finished: List<Book>, periodBooks: List<Book>, pagesByBook: Map<String, Int>): Book? =
        finished.filter { it.overallRating != null }
            .maxWithOrNull(compareBy<Book> { it.overallRating }.thenBy { it.completedAt })
            ?: periodBooks.maxByOrNull { pagesByBook[it.id] ?: it.totalPages }

    /** Author of the most books read this period, ties broken by pages read. */
    private fun topAuthor(periodBooks: List<Book>, pagesByBook: Map<String, Int>): AuthorStat? =
        periodBooks
            .flatMap { book -> book.authors.filter { it.isNotBlank() }.map { it.trim() to book } }
            .groupBy({ it.first }, { it.second })
            .map { (name, list) -> AuthorStat(name, list.size, list.sumOf { pagesByBook[it.id] ?: 0 }) }
            .maxWithOrNull(compareBy<AuthorStat> { it.books }.thenBy { it.pages })

    private fun buckets(pagesByDay: Map<LocalDate, Int>, isYear: Boolean, start: LocalDate, today: LocalDate): List<Bucket> =
        if (isYear) {
            (1..today.monthValue).map { month ->
                Bucket(
                    label = java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    pages = pagesByDay.filterKeys { it.monthValue == month }.values.sum()
                )
            }
        } else {
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }
                .map { day -> Bucket(day.dayOfMonth.toString(), pagesByDay[day] ?: 0) }
                .toList()
        }

    /**
     * Favourite passage from a book first, then any favourite, then the fullest
     * book passage that still fits a slide.
     */
    fun pickQuote(notes: List<MarginNote>): MarginNote? {
        val fits = notes.filter { it.content.isNotBlank() && it.content.length <= MAX_QUOTE_CHARS }
        return fits.filter { it.isFavorite && it.type == NoteType.BOOK_CONTENT }.maxByOrNull { it.timestamp }
            ?: fits.filter { it.isFavorite }.maxByOrNull { it.timestamp }
            ?: fits.filter { it.type == NoteType.BOOK_CONTENT && it.content.length >= 20 }.maxByOrNull { it.content.length }
    }
}
