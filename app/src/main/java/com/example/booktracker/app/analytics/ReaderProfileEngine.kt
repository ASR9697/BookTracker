package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Who you are as a reader, for Profile: the books you'd hand someone, the authors
 * you come back to, your taste in a sentence, and your personal bests. Pure logic.
 */
object ReaderProfileEngine {

    /** [marked] is true when these are books the reader hearted, false for the top-rated fallback. */
    data class Favourites(val books: List<Book>, val marked: Boolean)

    /** Hearted books, latest first; with none, finished books rated four stars or more, best first. */
    fun favourites(books: List<Book>, max: Int = 8): Favourites {
        val shelf = WishlistEngine.onShelf(books)
        val hearted = shelf.filter { it.isFavorite }.sortedByDescending { it.lastUpdated }
        if (hearted.isNotEmpty()) return Favourites(hearted.take(max), marked = true)
        val topRated = shelf
            .filter { FinishedShelf.isFinished(it) && (it.overallRating ?: 0f) >= 4f }
            .sortedWith(compareByDescending<Book> { it.overallRating }.thenByDescending { it.completedAt })
        return Favourites(topRated.take(max), marked = false)
    }

    /** An author and how many of their books you finished, with the best-rated one to show. */
    data class Author(val name: String, val books: Int, val cover: Book)

    /** Authors by books finished, most first (ties: the most recently read); one-offs only make up the numbers. */
    fun topAuthors(books: List<Book>, max: Int = 5): List<Author> =
        books.filter(FinishedShelf::isFinished)
            .flatMap { book -> book.authors.filter { it.isNotBlank() }.map { it.trim() to book } }
            .groupBy({ it.first }, { it.second })
            .map { (name, theirs) ->
                Author(name, theirs.size, theirs.maxWith(compareBy<Book> { it.overallRating ?: 0f }.thenBy { it.completedAt })) to
                    theirs.maxOf { it.completedAt }
            }
            .sortedWith(compareByDescending<Pair<Author, Long>> { it.first.books }.thenByDescending { it.second })
            .map { it.first }
            .take(max)

    /**
     * Your reading in a sentence, from what you finish and are reading, how, and when:
     * "Mostly Fantasy and History, in ebooks, at night." Null until there's enough to say.
     */
    fun tasteSentence(books: List<Book>, sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): String? {
        val shelf = WishlistEngine.onShelf(books).filter {
            FinishedShelf.isFinished(it) || it.status.equals(BookStatus.READING.name, ignoreCase = true)
        }
        val genres = AnalyticsEngine.genreMix(shelf)
            .filter { it.first != AnalyticsEngine.OTHER_GENRE }
            .take(2)
            .map { it.first.removePrefix("#").trim() }
        val format = shelf.groupingBy { it.format }.eachCount().maxByOrNull { it.value }
            ?.takeIf { shelf.size >= 3 && it.value * 10 >= shelf.size * 6 }
            ?.key
        val time = AnalyticsEngine.bestTimeOfDay(sessions, zone)?.let {
            when (it) {
                "Night" -> "at night"
                else -> "in the ${it.lowercase()}"
            }
        }
        val parts = listOfNotNull(format?.let { "in ${formatWord(it)}" }, time)
        return when {
            genres.isNotEmpty() -> "Mostly ${genres.joinToString(" and ")}" + parts.joinToString("") { ", $it" } + "."
            parts.isNotEmpty() -> "Reads mostly " + parts.joinToString(", ") + "."
            else -> null
        }
    }

    private fun formatWord(format: BookFormat): String = when (format) {
        BookFormat.Paperback -> "paperbacks"
        BookFormat.Hardcover -> "hardcovers"
        BookFormat.`E-book` -> "ebooks"
        BookFormat.Audiobook -> "audiobooks"
    }

    /** A personal best: what it is, its value, and the book behind it when there is one. */
    data class Record(val label: String, val value: String, val book: Book?)

    /** Personal bests, each left out until there's data behind it. */
    fun records(
        books: List<Book>,
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR,
        locale: Locale = Locale.getDefault()
    ): List<Record> {
        val shelf = WishlistEngine.onShelf(books)
        val finished = shelf.filter(FinishedShelf::isFinished)
        val byId = shelf.associateBy { it.id }
        val records = mutableListOf<Record>()

        finished.filter { it.totalPages > 0 }.maxByOrNull { it.totalPages }
            ?.let { records += Record("Longest book", "${"%,d".format(it.totalPages)} pages", it) }

        FinishedShelf.reads(finished, sessions, zone) { true }
            .filter { (it.days ?: 0) >= 1 }
            .minByOrNull { it.days!! }
            ?.let { records += Record("Fastest read", "${it.days} ${if (it.days == 1) "day" else "days"}", it.book) }

        shelf.filter { it.readCount > 0 }.maxByOrNull { it.readCount }
            ?.let { records += Record("Most re-read", "Read ${it.readCount + 1} times", it) }

        StreakEngine.pagesByDay(sessions, zone, dayStartsAtHour).maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }
            ?.let { (day: LocalDate, pages) ->
                records += Record("Biggest day", "$pages pages · ${day.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))}", null)
            }

        AnalyticsEngine.timedSessions(sessions).maxByOrNull { it.endTime - it.startTime }
            ?.let { s ->
                records += Record("Longest sitting", AnalyticsEngine.formatMinutes((s.endTime - s.startTime) / 60_000L), byId[s.bookId])
            }

        val languages = finished.map { it.language.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (languages.size > 1) {
            records += Record("Languages read", "${languages.size}: ${languages.joinToString(", ")}", null)
        }
        return records
    }
}
