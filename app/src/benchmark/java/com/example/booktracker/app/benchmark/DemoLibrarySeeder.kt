package com.example.booktracker.app.benchmark

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.app.data.LibrarySnapshot
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Classification
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Benchmark build only: fills an empty install with a believable library (books
 * in every state, two months of nightly sessions, notes) and skips the welcome,
 * so the startup-profile recording walks Home, Library, a book page, Notes, Stats
 * and Profile as a real reader sees them. Runs before Application.onCreate, so the
 * first screen already has its data.
 */
class DemoLibrarySeeder : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return true
        runBlocking {
            val repository = ServiceLocator.repository(context)
            if (repository.snapshotForBackup().books.isEmpty()) {
                val library = demoLibrary()
                repository.restoreFromBackup(library)
                val settings = ServiceLocator.settings(context)
                settings.updatePrefs { it.copy(onboarded = true) }
                // Its history already earns badges; mark them seen so no unlock dialog covers the walk.
                val monthly = BadgeEngine.monthlyChallenge(
                    library.books, library.sessions, library.notes.map { it.timestamp }, LocalDate.now()
                ).badge.id
                settings.markBadgesSeen(BadgeEngine.BADGES.map { it.id }.toSet() + monthly)
            }
        }
        return true
    }

    private fun demoLibrary(): LibrarySnapshot {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun dayMillis(daysAgo: Long, hour: Int) =
            today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

        // Title, author, pages, status, pages read; covers come from Open Library where it has them.
        val shelf = listOf(
            DemoBook("Pride and Prejudice", "Jane Austen", "9780141439518", 480, BookStatus.READING, 212),
            DemoBook("Moby-Dick", "Herman Melville", "9780142437247", 720, BookStatus.READING, 96),
            DemoBook("Middlemarch", "George Eliot", "9780141439549", 880, BookStatus.TO_READ, 0),
            DemoBook("Jane Eyre", "Charlotte Brontë", "9780141441146", 532, BookStatus.FINISHED, 532),
            DemoBook("Great Expectations", "Charles Dickens", "9780141439563", 544, BookStatus.FINISHED, 544),
            DemoBook("Frankenstein", "Mary Shelley", "9780141439471", 280, BookStatus.FINISHED, 280),
            DemoBook("The Odyssey", "Homer", "9780140268867", 560, BookStatus.UP_NEXT, 0),
            DemoBook("Walden", "Henry David Thoreau", "9780691096124", 352, BookStatus.SHORTLIST, 0),
            DemoBook("Dracula", "Bram Stoker", "9780141439846", 488, BookStatus.FINISHED, 488),
            DemoBook("Little Women", "Louisa May Alcott", "9780147514011", 512, BookStatus.TO_READ, 0)
        )
        val books = shelf.mapIndexed { i, demo ->
            val finished = demo.status == BookStatus.FINISHED
            Book(
                id = "demo-$i",
                title = demo.title,
                isbn = demo.isbn,
                coverUrl = "https://covers.openlibrary.org/b/isbn/${demo.isbn}-L.jpg?default=false",
                totalPages = demo.pages,
                currentPage = demo.read,
                creators = listOf(Creator(CreatorRole.Author, demo.author)),
                classification = Classification(emptyList(), listOf("Classics", "Fiction")),
                status = demo.status.name,
                rating = if (finished) mapOf(RatingAxis.OVERALL to 3.5f + (i % 3) * 0.5f) else emptyMap(),
                dateAdded = dayMillis(90L - i, 12),
                lastUpdated = dayMillis(if (demo.status == BookStatus.READING) i.toLong() else 20L + i, 21),
                finishedAt = if (finished) dayMillis(10L + i * 9, 22) else null
            )
        }

        // Most nights over two months, on whichever book was in hand then.
        val random = Random(42)
        val sessions = (0L until 60L).filter { it == 0L || random.nextInt(10) < 8 }.map { daysAgo ->
            val book = books[if (daysAgo < 20) (daysAgo % 2).toInt() else 3 + (daysAgo % 4).toInt()]
            val minutes = 15 + random.nextInt(35)
            val pages = minutes / 2 + random.nextInt(8)
            val start = dayMillis(daysAgo, 20 + random.nextInt(3))
            Session(
                id = "demo-session-$daysAgo",
                bookId = book.id,
                startTime = start,
                endTime = start + minutes * 60_000L,
                durationSeconds = minutes * 60,
                startPage = 1,
                endPage = 1 + pages,
                pagesRead = pages,
                zoneId = zone.id,
                focus = 1 + random.nextInt(5)
            )
        }

        val notes = (0 until 14).map { i ->
            val book = books[i % 6]
            MarginNote(
                id = "demo-note-$i",
                bookId = book.id,
                pageNumber = 10 + i * 17,
                content = NOTE_LINES[i % NOTE_LINES.size],
                type = if (i % 3 == 0) NoteType.PERSONAL_THOUGHT else NoteType.BOOK_CONTENT,
                isFavorite = i % 4 == 0,
                timestamp = dayMillis(i * 3L, 21)
            )
        }
        return LibrarySnapshot(books = books, sessions = sessions, notes = notes)
    }

    private class DemoBook(
        val title: String,
        val author: String,
        val isbn: String,
        val pages: Int,
        val status: BookStatus,
        val read: Int
    )

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        val NOTE_LINES = listOf(
            "It is a truth universally acknowledged, and yet I keep doubting it.",
            "The sea as a mirror for whatever the narrator won't say aloud.",
            "Reread this chapter; the pacing changes completely on a second pass.",
            "Lovely sentence about lamplight and rain. Copy it into the journal.",
            "Why does every character here lie to the people they love most?",
            "The letter scene: the whole book turns on these three pages.",
            "Note for book club: compare the two proposals side by side."
        )
    }
}
