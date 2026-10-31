package com.example.booktracker.app.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.ReviewKind
import com.example.booktracker.shared.models.ReviewState
import com.example.booktracker.shared.models.Session
import com.example.booktracker.shared.models.VocabWord
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.SQLiteMode

/**
 * A backup written and read back is the same library: every field of every
 * record, the settings, and the note photo travelling as a zip entry.
 * Drive sync uses the same document, so this guards it too.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
// Neither a database nor TLS here, so Robolectric's native SQLite and Conscrypt
// stay unloaded and the test runs on Windows on ARM too, which they don't support.
@SQLiteMode(SQLiteMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
class BackupRoundTripTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun library(): LibrarySnapshot {
        val photo = File(NoteImageStore.dir(context), "note_photo.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        return LibrarySnapshot(
            books = listOf(FullLibrary.book),
            sessions = listOf(FullLibrary.session),
            notes = listOf(FullLibrary.note(fileUri("file://${photo.absolutePath}"))),
            words = listOf(FullLibrary.word),
            reviews = listOf(FullLibrary.review)
        )
    }

    private val settings = BackupEngine.BackupSettings(
        dailyGoal = 30,
        yearlyGoal = 24,
        monthlyGoalPages = 600,
        monthlyGoalBooks = 3,
        dayStartsAtHour = 4,
        homeLayout = "today,streak|year",
        avatar = AvatarStore.COVER_PREFIX + "book-1",
        motto = "One more chapter.",
        pinnedNoteId = "note-1"
    )

    private fun roundTrip(snapshot: LibrarySnapshot) = BackupEngine.decodeDocument(
        BackupEngine.encodeDocument(snapshot, settings).toString(2)
    ) { entry -> BackupEngine.localUriForEntry(context, entry)?.let(::fileUri) }

    /**
     * The app's "file://" + path, which on a test machine running Windows reads
     * "C:" as a host; this turns it into "file:///C:/..." and leaves Android paths as they are.
     */
    private fun fileUri(uri: String): String {
        val path = uri.removePrefix("file://").replace('\\', '/')
        return "file://" + if (path.startsWith("/")) path else "/$path"
    }

    @Test
    fun `the fixture sets every field`() {
        val library = library()
        FullLibrary.assertEveryFieldSet(library.books.single(), Book(id = "", title = "", isbn = null))
        FullLibrary.assertEveryFieldSet(library.sessions.single(), Session())
        FullLibrary.assertEveryFieldSet(library.notes.single(), MarginNote())
        FullLibrary.assertEveryFieldSet(library.words.single(), VocabWord())
        FullLibrary.assertEveryFieldSet(library.reviews.single(), ReviewState(itemId = "", kind = ReviewKind.NOTE, dueDay = 0))
        FullLibrary.assertEveryFieldSet(settings, BackupEngine.BackupSettings())
    }

    @Test
    fun `books come back whole`() {
        val library = library()
        assertEquals(library.books, roundTrip(library).snapshot.books)
    }

    @Test
    fun `sessions come back whole, apart from the device they were timed on`() {
        val library = library()
        // Not stored anywhere, the database included: a restored session reads as the phone's.
        assertEquals(library.sessions.map { it.copy(deviceSource = Session().deviceSource) }, roundTrip(library).snapshot.sessions)
    }

    @Test
    fun `notes come back whole, photo included`() {
        val library = library()
        assertEquals(library.notes, roundTrip(library).snapshot.notes)
    }

    @Test
    fun `vocabulary and review progress come back whole`() {
        val library = library()
        val restored = roundTrip(library).snapshot
        assertEquals(library.words, restored.words)
        assertEquals(library.reviews, restored.reviews)
    }

    @Test
    fun `settings come back whole`() {
        assertEquals(settings, roundTrip(library()).settings)
    }
}
