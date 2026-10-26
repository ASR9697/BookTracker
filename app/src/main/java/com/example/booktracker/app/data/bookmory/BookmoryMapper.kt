package com.example.booktracker.app.data.bookmory

import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.data.CsvImportEngine
import com.example.booktracker.app.data.LibrarySnapshot
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Classification
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.ProgressUnit
import com.example.booktracker.shared.models.Publication
import com.example.booktracker.shared.models.PurchaseLog
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import kotlin.math.roundToInt

/** What an import did, for the summary shown afterwards. */
data class BookmoryImportSummary(
    val booksAdded: Int,
    /** Books already in the library; their history and notes were added to the existing copy. */
    val booksMatched: Int,
    val sessions: Int,
    val notes: Int,
    val photos: Int,
    /** Notes whose book wasn't in the backup. */
    val notesSkipped: Int,
    /** Bookmory's day-start hour, applied so the imported streak reads the same. */
    val dayStartHour: Int? = null
)

/**
 * [snapshot] holds only new books and notes, but every imported session: sessions
 * are written over earlier imports of themselves so a re-import can correct them
 * (they were never edited in this app), while notes the reader edited are kept.
 */
data class BookmoryImport(val snapshot: LibrarySnapshot, val summary: BookmoryImportSummary)

/**
 * Turns Bookmory records into our books, sessions and notes. Pure: no Android,
 * no I/O. Every record gets an id derived from its Bookmory key, so restoring the
 * result through the normal backup merge makes a repeated import a no-op.
 */
object BookmoryMapper {

    /** A page update this long after a timer run ended still belongs to that run. */
    const val TIMER_MATCH_WINDOW_MS = 30 * 60_000L

    /**
     * @param existing books already in the library, matched by ISBN or title and author.
     * @param existingSessionIds / [existingNoteIds] ids already stored, so a repeat import
     *   reports only what is new and leaves existing notes alone.
     * @param noteImage maps a Bookmory note-photo path to a stored URI, or null if the file is missing.
     */
    fun map(
        records: BookmoryRecords,
        existing: List<Book>,
        existingSessionIds: Set<String> = emptySet(),
        existingNoteIds: Set<String> = emptySet(),
        noteImage: (String) -> String? = { null }
    ): BookmoryImport {
        val byIsbn = existing.mapNotNull { book -> CsvImportEngine.normalizeIsbn(book.isbn)?.let { it to book } }.toMap()
        val byTitle = existing.associateBy { matchKey(it.title, it.authors.firstOrNull()) }

        val tagsByBook = records.tags
            .flatMap { tag -> tag.bids.orEmpty().mapNotNull { bid -> tag.name?.takeIf { it.isNotBlank() }?.let { bid to it } } }
            .groupBy({ it.first }, { it.second })

        val books = mutableListOf<Book>()
        val sessions = mutableListOf<Session>()
        var newSessions = 0
        val bookIds = mutableMapOf<String, String>()
        var matched = 0

        records.books.forEach { (key, bm) ->
            val title = bm.title?.trim().orEmpty()
            if (title.isEmpty()) return@forEach
            val authors = bm.authors.orEmpty().filter { it.isNotBlank() }.ifEmpty { listOfNotNull(bm.author?.takeIf { it.isNotBlank() }) }
            val match = CsvImportEngine.normalizeIsbn(bm.isbn)?.let(byIsbn::get)
                ?: byTitle[matchKey(title, authors.firstOrNull())]
            val bookId = match?.id ?: "bm-$key"
            bookIds[key] = bookId
            if (match != null) matched++ else books += toBook(bookId, bm, title, authors, records, tagsByBook[key].orEmpty())
            bm.reads.orEmpty().forEachIndexed { index, read ->
                val forRead = sessionsForRead(bookId, "bm-$key-r${read.nth ?: (index + 1)}", read)
                sessions += forRead
                newSessions += forRead.count { it.id !in existingSessionIds }
            }
        }

        var photos = 0
        var skipped = 0
        val notes = records.notes.mapNotNull { (key, bm) ->
            val bookId = bm.bid?.let(bookIds::get) ?: run { skipped++; return@mapNotNull null }
            if ("bm-note-$key" in existingNoteIds) return@mapNotNull null
            val images = bm.images.orEmpty().mapNotNull(noteImage)
            photos += images.size
            val text = BookmoryRecords.quillToText(bm.contentQuill)
            if (text.isEmpty() && images.isEmpty()) return@mapNotNull null
            MarginNote(
                id = "bm-note-$key",
                bookId = bookId,
                pageNumber = bm.page?.roundToInt() ?: 0,
                content = text,
                type = noteType(bm.type),
                isFavorite = bm.like == true,
                timestamp = bm.createdAt ?: 0L,
                imageUris = images
            )
        }

        return BookmoryImport(
            snapshot = LibrarySnapshot(books = books, sessions = sessions, notes = notes),
            summary = BookmoryImportSummary(
                booksAdded = books.size,
                booksMatched = matched,
                sessions = newSessions,
                notes = notes.size,
                photos = photos,
                notesSkipped = skipped,
                dayStartHour = records.dayStartHour
            )
        )
    }

    /**
     * Bookmory logs "now on page N" and, separately, timer runs. Each forward
     * page update becomes a session; when it lands within a timer run (or shortly
     * after), the session takes that run's start and duration so time and speed
     * stats carry over. Timer runs of a minute or more with no page update become
     * time-only sessions, since Bookmory counts those days toward the streak too.
     * Every session keeps the zone it was recorded in.
     */
    fun sessionsForRead(bookId: String, idPrefix: String, read: BmRead): List<Session> {
        val logs = read.pageLogs.orEmpty()
            .filter { it.page != null && it.createdAt != null }
            .sortedBy { it.createdAt }
        val timers = read.timers.orEmpty()
            .filter { it.startedAt != null && (it.elapsedSec ?: 0) > 0 }
            .sortedBy { it.startedAt }
            .toMutableList()

        var previous = 0
        val fromLogs = logs.mapNotNull { log ->
            val page = log.page!!.roundToInt()
            val at = log.createdAt!!
            val timer = timers.lastOrNull { t ->
                val start = t.startedAt!!
                at >= start && at <= start + t.elapsedSec!! * 1000 + TIMER_MATCH_WINDOW_MS
            }
            val startPage = previous
            previous = page
            if (page <= startPage) return@mapNotNull null
            timer?.let(timers::remove)
            val start = timer?.startedAt ?: at
            val seconds = timer?.elapsedSec ?: 0L
            Session(
                id = "$idPrefix-$at",
                bookId = bookId,
                startTime = start,
                endTime = if (timer != null) start + seconds * 1000 else at,
                durationSeconds = seconds.toInt(),
                startPage = startPage,
                endPage = page,
                pagesRead = page - startPage,
                zoneId = timer?.zone ?: log.zone.orEmpty()
            )
        }

        // Whatever timer runs are left matched no page update.
        val timeOnly = timers
            .filter { it.elapsedSec!! >= StreakEngine.MIN_TIMED_READING_SECONDS }
            .map { t ->
                val start = t.startedAt!!
                val page = logs.lastOrNull { it.createdAt!! <= start }?.page?.roundToInt() ?: 0
                Session(
                    id = "$idPrefix-t$start",
                    bookId = bookId,
                    startTime = start,
                    endTime = start + t.elapsedSec!! * 1000,
                    durationSeconds = t.elapsedSec.toInt(),
                    startPage = page,
                    endPage = page,
                    pagesRead = 0,
                    zoneId = t.zone.orEmpty()
                )
            }
        return (fromLogs + timeOnly).sortedBy { it.startTime }
    }

    private fun toBook(
        id: String,
        bm: BmBook,
        title: String,
        authors: List<String>,
        records: BookmoryRecords,
        tags: List<String>
    ): Book {
        val reads = bm.reads.orEmpty().sortedBy { it.nth ?: 0 }
        val lastRead = reads.lastOrNull()
        val status = bookStatus(lastRead?.status ?: bm.statusList?.lastOrNull())
        val lastStar = reads.lastOrNull { it.status == "DONE" && (it.star ?: 0.0) > 0 }?.star?.toFloat()
        return Book(
            id = id,
            title = title,
            isbn = CsvImportEngine.normalizeIsbn(bm.isbn),
            coverUrl = bm.image?.takeIf { it.startsWith("http") }.orEmpty(),
            totalPages = bm.totalPage?.roundToInt() ?: 0,
            currentPage = bm.curPage?.roundToInt() ?: 0,
            language = bm.language.orEmpty(),
            creators = authors.map { Creator(CreatorRole.Author, it) },
            publication = Publication(bm.publisher.orEmpty(), bm.publicationDate.orEmpty()),
            format = bookFormat(bm.bookType),
            progressUnit = if (bm.pageType.equals("PERCENT", ignoreCase = true)) ProgressUnit.Percentage else ProgressUnit.Page,
            description = bm.description.orEmpty(),
            classification = Classification(
                collections = bm.collectionKeys.orEmpty().mapNotNull { records.collections[it]?.name?.takeIf(String::isNotBlank) },
                tags = tags
            ),
            status = status.name,
            rating = lastStar?.let { mapOf(RatingAxis.OVERALL to it) }.orEmpty(),
            purchaseLog = bm.purchases.orEmpty().map {
                PurchaseLog(
                    date = it.purchasedAt ?: 0L,
                    vendor = it.from.orEmpty(),
                    price = it.price ?: 0.0,
                    currency = it.currencyCode.orEmpty(),
                    memo = it.memo.orEmpty()
                )
            },
            dateAdded = bm.createdAt ?: 0L,
            lastUpdated = bm.updatedAt ?: bm.createdAt ?: 0L,
            readCount = (reads.size - 1).coerceAtLeast(0),
            review = lastRead?.comment?.takeIf { it.isNotBlank() },
            finishedAt = when (status) {
                BookStatus.FINISHED -> bm.lastReadDoneDate ?: lastRead?.end
                BookStatus.DNF -> bm.lastGiveUpDate ?: lastRead?.end
                else -> null
            },
            ownership = when {
                bm.wishlist == true -> Ownership.WISHLIST
                !bm.gifts.isNullOrEmpty() -> Ownership.GIFT
                !bm.purchases.isNullOrEmpty() -> Ownership.OWNED
                else -> Ownership.NONE
            }
        )
    }

    /** Title up to any subtitle, plus first author, ignoring case and punctuation. */
    internal fun matchKey(title: String, author: String?): String =
        CsvImportEngine.dedupeKey(title.substringBefore(':').substringBefore(" (").trim(), author)

    internal fun bookStatus(raw: String?): BookStatus = when (raw?.uppercase()) {
        "DONE" -> BookStatus.FINISHED
        "READING" -> BookStatus.READING
        "PAUSE", "PAUSED" -> BookStatus.PAUSED
        "GIVE_UP", "GIVEUP", "GAVE_UP" -> BookStatus.DNF
        else -> BookStatus.TO_READ
    }

    internal fun bookFormat(raw: String?): BookFormat {
        val value = raw.orEmpty().lowercase()
        return when {
            value.contains("audio") -> BookFormat.Audiobook
            value.contains("ebook") || value.contains("e_book") || value.contains("e-book") -> BookFormat.`E-book`
            value.contains("hard") -> BookFormat.Hardcover
            else -> BookFormat.Paperback
        }
    }

    internal fun noteType(raw: String?): NoteType = when {
        raw.equals("BOOK_CONTENT", ignoreCase = true) -> NoteType.BOOK_CONTENT
        raw.orEmpty().contains("THOUGHT", ignoreCase = true) -> NoteType.PERSONAL_THOUGHT
        else -> NoteType.RANDOM
    }
}
