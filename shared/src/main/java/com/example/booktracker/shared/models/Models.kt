package com.example.booktracker.shared.models

import java.math.BigDecimal

enum class CreatorRole {
    Author, Translator, Illustrator, Narrator
}

data class Creator(
    val role: CreatorRole,
    val name: String
)

data class Publication(
    val publisher: String,
    val date: String // Date as string (e.g. YYYY or YYYY-MM-DD)
)

enum class BookFormat {
    Paperback, Hardcover, `E-book`, Audiobook
}

/** How the reader has (or wants) the book. Borrowing is tracked by [LoanRecord]. */
enum class Ownership(val label: String) {
    NONE("Not set"),
    OWNED("Owned"),
    GIFT("Gift"),
    WISHLIST("Wishlist");

    companion object {
        /** Stored name, or Owned for records from before this field that log a purchase. */
        fun fromStored(raw: String?, hasPurchase: Boolean): Ownership =
            entries.firstOrNull { it.name == raw } ?: if (hasPurchase) OWNED else NONE
    }
}

enum class WishPriority(val label: String) {
    MUST("Must have"),
    WANT("Want"),
    MAYBE("Maybe")
}

/**
 * What the reader knows about a book they want to get. Filled in while the book
 * is on the wishlist; kept afterwards so "Got it" can prefill the purchase.
 */
data class WishInfo(
    val priority: WishPriority = WishPriority.WANT,
    /** Expected price; null when unknown. */
    val price: Double? = null,
    val currency: String = "",
    val store: String = "",
    val recommendedBy: String = "",
    val note: String = "",
    /** Release day (start of day, local) for a book that isn't out yet; null when it already is. */
    val releaseDate: Long? = null,
    /** When it went on the wishlist. */
    val addedAt: Long = 0L
)

enum class ProgressUnit {
    Page, Percentage, Chapter, Minute
}

data class SeriesInfo(
    val isPartOfSeries: Boolean,
    val seriesName: String,
    val position: Int
)

data class Classification(
    val collections: List<String>,
    val tags: List<String>
)

enum class BookStatus(val label: String) {
    TO_READ("To Read"),
    SHORTLIST("Shortlist"),
    UP_NEXT("Up Next"),
    READING("Reading"),
    PAUSED("Paused"),
    ABANDONED("Abandoned"),
    FINISHED("Finished"),
    DNF("Did Not Finish")
}

data class PurchaseLog(
    val date: Long,
    val vendor: String,
    val price: Double,
    val currency: String,
    val memo: String
)

data class LoanRecord(
    val loanDate: Long,
    val dueDate: Long,
    val lender: String,
    val memo: String,
    val returnDate: Long? = null,
    val isLentOut: Boolean = false
) {
    val isReturned: Boolean get() = returnDate != null
    val isActive: Boolean get() = returnDate == null
}

data class Book(
    val id: String,
    val title: String,
    val isbn: String?,
    val coverUrl: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val language: String = "",
    val creators: List<Creator> = emptyList(),
    val publication: Publication? = null,
    val format: BookFormat = BookFormat.Paperback,
    val progressUnit: ProgressUnit = ProgressUnit.Page,
    val description: String = "",
    val seriesInfo: SeriesInfo? = null,
    val classification: Classification = Classification(emptyList(), emptyList()),
    val status: String = BookStatus.TO_READ.name,
    val rating: Map<String, Float> = emptyMap(),
    val dnfData: DnfData? = null,
    val purchaseLog: List<PurchaseLog> = emptyList(),
    val loanRecord: List<LoanRecord> = emptyList(),
    val dateAdded: Long = 0,
    val lastUpdated: Long = 0,
    val isFavorite: Boolean = false,
    /** How many times this book has been finished and restarted. 0 = first read. */
    val readCount: Int = 0,
    val plannedDate: Long? = null,
    val review: String? = null,
    /** When the current read ended (finished or abandoned); null while unfinished. */
    val finishedAt: Long? = null,
    val ownership: Ownership = Ownership.NONE,
    /** Start of the day the reader wants to be done by; null when no deadline is set. */
    val finishBy: Long? = null,
    /**
     * When the current re-read began, so notes from earlier reads can resurface.
     * Null on a first read, and on re-reads started before this was stored.
     */
    val readStartedAt: Long? = null,
    /**
     * Length of the audiobook edition at 1x, in minutes, when the reader follows
     * the book in print and audio together; 0 when they don't. Pages stay the one
     * measure of progress and the audio position is derived from them.
     */
    val audioMinutes: Int = 0,
    /** Playback speed the reader listens at (1.0, 1.25, 1.5…). */
    val audioSpeed: Float = 1f,
    /** Wishlist details; null for a book that was never wished for. */
    val wish: WishInfo? = null
) {
    val authors: List<String> get() = creators.map { it.name }
    val genres: List<String> get() = classification.tags

    /**
     * The date a finished book counts toward stats. lastUpdated moves on every edit
     * (favourite, loan, collections…), so it's only a fallback for rows written
     * before finishedAt existed.
     */
    val completedAt: Long get() = finishedAt ?: lastUpdated

    /** Star rating out of 5: the explicit overall score, else the mean of rated axes; null if unrated. */
    val overallRating: Float?
        get() = rating[RatingAxis.OVERALL]
            ?: rating.filterKeys { it != RatingAxis.OVERALL }.values
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toFloat()

    /** "First reading", "2nd reading", … as shown in the detail header. */
    val readingLabel: String
        get() = when (readCount) {
            0 -> "First reading"
            1 -> "2nd reading"
            2 -> "3rd reading"
            else -> "${readCount + 1}th reading"
        }
}

data class Session(
    val id: String = "",
    val bookId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationSeconds: Int = 0,
    val startPage: Int = 0,
    val endPage: Int = 0,
    val pagesRead: Int = 0,
    val environmentTag: String = "",
    val deviceSource: String = "phone",
    /**
     * Time zone the session was recorded in (e.g. "Asia/Kolkata"), so its reading
     * day stays put after travel. Blank on sessions from before this was stored;
     * those fall back to the phone's current zone.
     */
    val zoneId: String = "",
    /** How focused the reader felt, 1 (distracted) to 5 (deep focus); 0 when not rated. */
    val focus: Int = 0
)

enum class NoteType {
    BOOK_CONTENT, PERSONAL_THOUGHT, RANDOM
}

data class MarginNote(
    val id: String = "",
    val bookId: String = "",
    val pageNumber: Int = 0,
    val content: String = "",
    val type: NoteType = NoteType.BOOK_CONTENT,
    val isFavorite: Boolean = false,
    val timestamp: Long = 0L,
    val markdownContent: String = "",
    /** Attached photos as "file://" URIs, in the order they were added. */
    val imageUris: List<String> = emptyList()
)

/** What a [ReviewState] schedules: a saved highlight or a vocabulary word. */
enum class ReviewKind { NOTE, WORD }

/**
 * Spaced-review progress for one highlight or word: the day it comes back
 * (epoch day) and how far apart its reviews have grown.
 */
data class ReviewState(
    val itemId: String,
    val kind: ReviewKind,
    val dueDay: Long,
    val intervalDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAt: Long = 0L
)

/** A word met while reading, with what it means and where it was found. */
data class VocabWord(
    val id: String = "",
    val word: String = "",
    val definition: String = "",
    val bookId: String? = null,
    val pageNumber: Int = 0,
    /** The sentence it appeared in, if the reader kept it. */
    val context: String = "",
    val addedAt: Long = 0L
)

object RatingAxis {
    /** The headline star rating; the axes below are optional detail. */
    const val OVERALL = "Overall"
    val ALL = listOf("Writing", "Characters", "Plot", "Pacing", "Enjoyment")
}

object DnfReasons {
    val ALL = listOf("Boring", "Bad Writing", "Too long", "Not for me")
}

data class DnfData(val abandonedPercentage: Float, val reason: String)
