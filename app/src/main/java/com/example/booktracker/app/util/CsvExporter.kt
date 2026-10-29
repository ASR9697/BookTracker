package com.example.booktracker.app.util

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * The library as a CSV in Goodreads' export layout, so it can be imported into
 * Goodreads, StoryGraph or a spreadsheet. Only the columns this app knows are filled.
 */
object CsvExporter {

    private val HEADER = listOf(
        "Title", "Author", "Additional Authors", "ISBN13", "My Rating", "Binding", "Number of Pages",
        "Date Read", "Date Added", "Exclusive Shelf", "My Review", "Read Count"
    )

    private val DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    fun toGoodreadsCsv(books: List<Book>, zone: ZoneId): String = buildString {
        appendLine(HEADER.joinToString(","))
        books.sortedBy { it.title.lowercase() }.forEach { book ->
            val finished = book.status.equals(BookStatus.FINISHED.name, ignoreCase = true)
            val row = listOf(
                book.title,
                book.authors.firstOrNull().orEmpty(),
                book.authors.drop(1).joinToString(", "),
                book.isbn.orEmpty(),
                book.overallRating?.roundToInt()?.toString() ?: "0",
                book.format.name,
                book.totalPages.takeIf { it > 0 }?.toString().orEmpty(),
                if (finished) date(book.completedAt, zone) else "",
                date(book.dateAdded, zone),
                shelf(book.status),
                book.review.orEmpty(),
                (book.readCount + if (finished) 1 else 0).toString()
            )
            appendLine(row.joinToString(",") { quote(it) })
        }
    }

    private fun shelf(status: String): String = when {
        status.equals(BookStatus.FINISHED.name, ignoreCase = true) -> "read"
        status.equals(BookStatus.READING.name, ignoreCase = true) -> "currently-reading"
        status.equals(BookStatus.DNF.name, ignoreCase = true) || status.equals(BookStatus.ABANDONED.name, ignoreCase = true) -> "did-not-finish"
        else -> "to-read"
    }

    private fun date(millis: Long, zone: ZoneId): String =
        if (millis <= 0L) "" else Instant.ofEpochMilli(millis).atZone(zone).format(DATE)

    /** Quotes a field when it holds a comma, quote or line break, doubling inner quotes. */
    internal fun quote(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
