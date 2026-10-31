package com.example.booktracker.app.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.booktracker.app.data.FullLibrary
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What the app saves is what it reads back: every field through the entities and their mappers. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StorageRoundTripTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After
    fun close() = db.close()

    @Test
    fun `a book comes back whole`() = runBlocking {
        db.bookDao().upsert(FullLibrary.book.toEntity())
        assertEquals(FullLibrary.book, db.bookDao().getById(FullLibrary.book.id)?.toModel())
    }

    @Test
    fun `a session comes back whole, apart from the device it was timed on`() = runBlocking {
        db.bookDao().upsert(FullLibrary.book.toEntity())
        db.sessionDao().upsert(FullLibrary.session.toEntity())
        assertEquals(
            FullLibrary.session.copy(deviceSource = Session().deviceSource),
            db.sessionDao().getAll().single().toModel()
        )
    }

    @Test
    fun `a note comes back whole, apart from its markdown copy`() = runBlocking {
        val note = FullLibrary.note("file:///data/note_images/a.jpg")
        db.bookDao().upsert(FullLibrary.book.toEntity())
        db.marginNoteDao().upsert(note.toEntity())
        // Only backups carry markdownContent; the stored note keeps its text in content.
        assertEquals(note.copy(markdownContent = MarginNote().markdownContent), db.marginNoteDao().getById(note.id)?.toModel())
    }

    @Test
    fun `vocabulary and review progress come back whole`() = runBlocking {
        db.vocabDao().upsert(FullLibrary.word.toEntity())
        db.reviewDao().upsert(FullLibrary.review.toEntity())
        assertEquals(FullLibrary.word, db.vocabDao().getAll().single().toModel())
        assertEquals(FullLibrary.review, db.reviewDao().getAll().single().toModel())
    }

    @Test
    fun `search finds a book saved after the database was made`() = runBlocking {
        db.bookDao().upsert(FullLibrary.book.toEntity())
        assertEquals(listOf(FullLibrary.book.id), db.bookDao().search("Darkness").map { it.id })
    }
}
