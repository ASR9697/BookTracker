package com.example.booktracker.app.data

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Classification
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.DnfData
import com.example.booktracker.shared.models.LoanRecord
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.ProgressUnit
import com.example.booktracker.shared.models.Publication
import com.example.booktracker.shared.models.PurchaseLog
import com.example.booktracker.shared.models.ReviewKind
import com.example.booktracker.shared.models.ReviewState
import com.example.booktracker.shared.models.SeriesInfo
import com.example.booktracker.shared.models.Session
import com.example.booktracker.shared.models.VocabWord
import com.example.booktracker.shared.models.WishInfo
import com.example.booktracker.shared.models.WishPriority
import org.junit.Assert.assertTrue

/**
 * One of everything, with every field away from its default, so a round trip
 * that drops a field fails. [assertEveryFieldSet] keeps it that way: add a
 * field to a model and the tests ask for it here first.
 */
object FullLibrary {

    val book = Book(
        id = "book-1",
        title = "The Left Hand of Darkness",
        isbn = "9780441478125",
        coverUrl = "https://covers.example/lhod.jpg",
        totalPages = 304,
        currentPage = 120,
        language = "en",
        creators = listOf(Creator(CreatorRole.Author, "Ursula K. Le Guin"), Creator(CreatorRole.Translator, "Someone Else")),
        publication = Publication("Ace", "1969-03"),
        format = BookFormat.Hardcover,
        progressUnit = ProgressUnit.Percentage,
        description = "Winter, a planet of ice.",
        seriesInfo = SeriesInfo(isPartOfSeries = true, seriesName = "Hainish Cycle", position = 6),
        classification = Classification(collections = listOf("Classics"), tags = listOf("Science Fiction")),
        status = BookStatus.DNF.name,
        rating = mapOf("Writing" to 4.5f, "Plot" to 3f),
        dnfData = DnfData(abandonedPercentage = 0.25f, reason = "Not for me"),
        purchaseLog = listOf(PurchaseLog(date = 1_700_000_000_000, vendor = "Shop", price = 12.5, currency = "EUR", memo = "gift card")),
        loanRecord = listOf(LoanRecord(loanDate = 1_700_000_100_000, dueDate = 1_700_900_000_000, lender = "Sam", memo = "careful", returnDate = 1_700_500_000_000, isLentOut = true)),
        dateAdded = 1_690_000_000_000,
        lastUpdated = 1_710_000_000_000,
        isFavorite = true,
        readCount = 2,
        plannedDate = 1_720_000_000_000,
        review = "Quietly huge.",
        finishedAt = 1_705_000_000_000,
        ownership = Ownership.WISHLIST,
        finishBy = 1_730_000_000_000,
        readStartedAt = 1_701_000_000_000,
        audioMinutes = 840,
        audioSpeed = 1.25f,
        wish = WishInfo(
            priority = WishPriority.MUST,
            price = 9.99,
            currency = "USD",
            store = "Local bookshop",
            recommendedBy = "Alex",
            note = "the 50th anniversary edition",
            releaseDate = 1_740_000_000_000,
            addedAt = 1_695_000_000_000
        )
    )

    val session = Session(
        id = "session-1",
        bookId = "book-1",
        startTime = 1_700_000_000_000,
        endTime = 1_700_001_800_000,
        durationSeconds = 1_500,
        startPage = 100,
        endPage = 120,
        pagesRead = 20,
        environmentTag = "Train",
        deviceSource = "watch",
        zoneId = "Asia/Kolkata",
        focus = 4
    )

    fun note(imageUri: String) = MarginNote(
        id = "note-1",
        bookId = "book-1",
        pageNumber = 87,
        content = "Light is the left hand of darkness.",
        type = NoteType.PERSONAL_THOUGHT,
        isFavorite = true,
        timestamp = 1_700_000_500_000,
        markdownContent = "**Light** is the left hand of darkness.",
        imageUris = listOf(imageUri)
    )

    val word = VocabWord(
        id = "word-1",
        word = "shifgrethor",
        definition = "prestige, face",
        bookId = "book-1",
        pageNumber = 14,
        context = "It's a matter of shifgrethor.",
        addedAt = 1_700_000_600_000
    )

    val review = ReviewState(
        itemId = "note-1",
        kind = ReviewKind.WORD,
        dueDay = 20_000,
        intervalDays = 6,
        reps = 3,
        lapses = 1,
        lastReviewedAt = 1_700_000_700_000
    )

    /**
     * Fails naming every field of [sample] still equal to [blank]'s, apart from
     * [allowedSame]. [blank] is the model built with only its required arguments.
     */
    fun assertEveryFieldSet(sample: Any, blank: Any, allowedSame: Set<String> = emptySet()) {
        val same = sample.javaClass.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
            .filter { it.get(sample) == it.get(blank) }
            .map { it.name }
            .filterNot { it in allowedSame }
        assertTrue(
            "${sample.javaClass.simpleName} fixture leaves $same at the default; set them so the round trip covers them",
            same.isEmpty()
        )
    }
}
