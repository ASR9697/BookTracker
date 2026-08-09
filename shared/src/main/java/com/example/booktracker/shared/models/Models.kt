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
    val memo: String
)

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
    val review: String? = null
) {
    val authors: List<String> get() = creators.map { it.name }
    val genres: List<String> get() = classification.tags

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
    val deviceSource: String = "phone"
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
    val markdownContent: String = ""
)

object RatingAxis {
    val ALL = listOf("Writing", "Characters", "Plot", "Pacing", "Enjoyment")
}

object DnfReasons {
    val ALL = listOf("Boring", "Bad Writing", "Too long", "Not for me")
}

data class DnfData(val abandonedPercentage: Float, val reason: String)
