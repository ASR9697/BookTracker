package com.example.booktracker.app.sync

import com.example.booktracker.app.data.LibrarySnapshot
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    private fun book(id: String, page: Int = 0, updated: Long = 0) =
        Book(id = id, title = id, isbn = null, currentPage = page, lastUpdated = updated)

    private fun note(id: String, book: String, text: String) = MarginNote(id = id, bookId = book, content = text)

    @Test
    fun `first sync combines both libraries`() {
        val merged = SyncMerge.merge(
            base = null,
            local = LibrarySnapshot(books = listOf(book("a"))),
            remote = LibrarySnapshot(books = listOf(book("b")))
        )
        assertEquals(setOf("a", "b"), merged.books.map { it.id }.toSet())
    }

    @Test
    fun `an edit on one side wins over the untouched other side`() {
        val base = LibrarySnapshot(notes = listOf(note("n", "a", "old")), books = listOf(book("a")))
        val remote = base.copy(notes = listOf(note("n", "a", "edited on tablet")))
        val merged = SyncMerge.merge(base, local = base, remote = remote)
        assertEquals("edited on tablet", merged.notes.single().content)
    }

    @Test
    fun `a delete on one side removes the record`() {
        val base = LibrarySnapshot(books = listOf(book("a"), book("b")))
        val local = base.copy(books = listOf(book("a")))
        val merged = SyncMerge.merge(base, local, remote = base)
        assertEquals(listOf("a"), merged.books.map { it.id })
    }

    @Test
    fun `an edit beats a delete`() {
        val base = LibrarySnapshot(books = listOf(book("a")))
        val merged = SyncMerge.merge(
            base,
            local = LibrarySnapshot(),
            remote = LibrarySnapshot(books = listOf(book("a", page = 40, updated = 5)))
        )
        assertEquals(40, merged.books.single().currentPage)
    }

    @Test
    fun `when both edit a book the later edit wins`() {
        val base = LibrarySnapshot(books = listOf(book("a", page = 10, updated = 1)))
        val merged = SyncMerge.merge(
            base,
            local = LibrarySnapshot(books = listOf(book("a", page = 20, updated = 2))),
            remote = LibrarySnapshot(books = listOf(book("a", page = 30, updated = 3)))
        )
        assertEquals(30, merged.books.single().currentPage)
    }

    @Test
    fun `sessions of a deleted book go with it`() {
        val session = Session(id = "s", bookId = "b", endTime = 1)
        val base = LibrarySnapshot(books = listOf(book("b")), sessions = listOf(session))
        val local = base.copy(books = emptyList(), sessions = emptyList())
        val remote = base.copy(sessions = listOf(session, Session(id = "s2", bookId = "b", endTime = 2)))
        val merged = SyncMerge.merge(base, local, remote)
        assertTrue(merged.books.isEmpty())
        assertTrue(merged.sessions.isEmpty())
    }

    @Test
    fun `changes list what to write and what to remove`() {
        val from = LibrarySnapshot(books = listOf(book("a"), book("b")))
        val to = LibrarySnapshot(books = listOf(book("a", page = 3), book("c")))
        val changes = SyncMerge.changes(from, to)
        assertEquals(setOf("a", "c"), changes.snapshot.books.map { it.id }.toSet())
        assertEquals(setOf("b"), changes.deletedBooks)
    }
}
