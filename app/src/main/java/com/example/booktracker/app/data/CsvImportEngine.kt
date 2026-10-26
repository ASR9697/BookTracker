package com.example.booktracker.app.data

import android.content.Context
import android.net.Uri
import com.example.booktracker.app.data.local.BookDao
import com.example.booktracker.app.data.local.toEntity
import com.example.booktracker.app.data.local.toModel
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Classification
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.Publication
import com.example.booktracker.shared.models.RatingAxis
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CsvImportResult(val imported: Int, val skippedDuplicates: Int)

/**
 * Imports Goodreads and StoryGraph library exports. Both are plain CSV with
 * quoted fields that can contain commas, escaped quotes and line breaks (reviews),
 * so the whole file is tokenised rather than read line by line. Rows already in
 * the library (same ISBN, or same title + author) are skipped, which makes a
 * repeated import safe.
 */
object CsvImportEngine {

    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/M/d")
    )

    private val statusShelves = setOf(
        "read", "currently-reading", "to-read", "did-not-finish", "dnf", "paused"
    )

    suspend fun importFromUri(context: Context, uri: Uri, bookDao: BookDao): CsvImportResult =
        withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return@withContext CsvImportResult(0, 0)

            val rows = parseCsv(text.removePrefix("\uFEFF"))
            if (rows.isEmpty()) return@withContext CsvImportResult(0, 0)

            val headers = rows.first().map { it.trim().lowercase() }
            fun col(vararg names: String): Int = names.firstNotNullOfOrNull { name ->
                headers.indexOf(name).takeIf { it >= 0 }
            } ?: -1

            val titleIdx = col("title")
            if (titleIdx == -1) return@withContext CsvImportResult(0, 0)
            val authorIdx = col("author", "authors")
            val additionalAuthorsIdx = col("additional authors")
            val isbn13Idx = col("isbn13")
            val isbnIdx = col("isbn", "isbn/uid")
            val pagesIdx = col("number of pages", "pages")
            val publisherIdx = col("publisher")
            val yearIdx = col("original publication year", "year published")
            val bindingIdx = col("binding", "format")
            val ratingIdx = col("my rating", "star rating")
            val dateReadIdx = col("date read", "last date read")
            val dateAddedIdx = col("date added")
            val exclusiveShelfIdx = col("exclusive shelf", "read status")
            val shelvesIdx = col("bookshelves", "tags")
            val reviewIdx = col("my review", "review")

            val existing = bookDao.getAll().map { it.toModel() }
            val knownIsbns = existing.mapNotNull { normalizeIsbn(it.isbn) }.toMutableSet()
            val knownKeys = existing.map { dedupeKey(it.title, it.authors.firstOrNull()) }.toMutableSet()

            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            var skipped = 0
            val toInsert = mutableListOf<Book>()

            for (row in rows.drop(1)) {
                fun cell(idx: Int): String = row.getOrNull(idx)?.trim().orEmpty()

                val title = cell(titleIdx)
                if (title.isBlank()) continue

                val authors = buildList {
                    // StoryGraph joins multiple authors with commas in one cell.
                    cell(authorIdx).split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
                    cell(additionalAuthorsIdx).split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { if (it !in this) add(it) }
                }
                val isbn = normalizeIsbn(cell(isbn13Idx)) ?: normalizeIsbn(cell(isbnIdx))
                val key = dedupeKey(title, authors.firstOrNull())
                if ((isbn != null && isbn in knownIsbns) || key in knownKeys) {
                    skipped++
                    continue
                }
                isbn?.let { knownIsbns += it }
                knownKeys += key

                val shelf = cell(exclusiveShelfIdx).lowercase()
                val status = when (shelf) {
                    "read" -> BookStatus.FINISHED
                    "currently-reading" -> BookStatus.READING
                    "did-not-finish", "dnf" -> BookStatus.DNF
                    "paused" -> BookStatus.PAUSED
                    else -> BookStatus.TO_READ
                }
                val pages = cell(pagesIdx).toIntOrNull()?.coerceAtLeast(0) ?: 0
                val rating = cell(ratingIdx).toFloatOrNull()?.takeIf { it > 0f }?.coerceAtMost(5f)
                val dateRead = parseDate(cell(dateReadIdx), zone)
                val dateAdded = parseDate(cell(dateAddedIdx), zone) ?: now
                val collections = cell(shelvesIdx)
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.lowercase() !in statusShelves }
                    .distinct()
                val finishedAt = when (status) {
                    // Unknown read dates fall back to when the book was added rather
                    // than today, so an import doesn't flood this year's stats.
                    BookStatus.FINISHED, BookStatus.DNF -> dateRead ?: dateAdded
                    else -> null
                }

                toInsert += Book(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    isbn = isbn,
                    totalPages = pages,
                    currentPage = if (status == BookStatus.FINISHED) pages else 0,
                    creators = authors.map { Creator(CreatorRole.Author, it) },
                    publication = Publication(cell(publisherIdx), cell(yearIdx)),
                    format = mapFormat(cell(bindingIdx)),
                    classification = Classification(collections, emptyList()),
                    status = status.name,
                    rating = rating?.let { mapOf(RatingAxis.OVERALL to it) } ?: emptyMap(),
                    review = cell(reviewIdx)
                        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                        .trim()
                        .takeIf { it.isNotBlank() },
                    dateAdded = dateAdded,
                    lastUpdated = now,
                    finishedAt = finishedAt
                )
            }

            if (toInsert.isNotEmpty()) bookDao.upsert(toInsert.map { it.toEntity() })
            CsvImportResult(imported = toInsert.size, skippedDuplicates = skipped)
        }

    /** RFC 4180 tokeniser: quoted fields may hold commas, line breaks and "" escapes. */
    internal fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row += field.toString()
                        field.clear()
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row += field.toString()
                        field.clear()
                        if (row.any { it.isNotBlank() }) rows += row
                        row = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            if (row.any { it.isNotBlank() }) rows += row
        }
        return rows
    }

    /** Goodreads wraps ISBNs as ="0123456789" to stop spreadsheets mangling them. */
    internal fun normalizeIsbn(raw: String?): String? {
        val digits = raw.orEmpty().filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase()
        return digits.takeIf { it.length == 10 || it.length == 13 }
    }

    internal fun dedupeKey(title: String, author: String?): String =
        title.lowercase().filter { it.isLetterOrDigit() } + "|" +
            author.orEmpty().lowercase().filter { it.isLetterOrDigit() }

    private fun parseDate(raw: String, zone: ZoneId): Long? {
        val value = raw.trim()
        if (value.isBlank()) return null
        // A StoryGraph range ("2023/05/01-2023/05/14") counts from its last date.
        val candidates = listOf(value, value.substringAfterLast('-').trim())
        for (candidate in candidates) {
            for (format in dateFormats) {
                val date = runCatching { LocalDate.parse(candidate, format) }.getOrNull() ?: continue
                return date.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun mapFormat(raw: String): BookFormat {
        val value = raw.lowercase()
        return when {
            value.contains("audio") -> BookFormat.Audiobook
            value.contains("kindle") || value.contains("ebook") || value.contains("e-book") ||
                value.contains("digital") || value.contains("nook") -> BookFormat.`E-book`
            value.contains("hardcover") || value.contains("hardback") -> BookFormat.Hardcover
            else -> BookFormat.Paperback
        }
    }
}
