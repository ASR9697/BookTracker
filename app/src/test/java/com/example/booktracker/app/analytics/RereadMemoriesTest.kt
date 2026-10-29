package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RereadMemoriesTest {

    private val book = Book(id = "b", title = "T", isbn = null, totalPages = 300, readCount = 1)

    private fun session(start: Long, from: Int, to: Int) =
        Session(id = "s$start", bookId = "b", startTime = start, endTime = start + 1, startPage = from, endPage = to, pagesRead = to - from)

    private fun note(id: String, page: Int, at: Long) = MarginNote(id = id, bookId = "b", pageNumber = page, content = id, timestamp = at)

    @Test
    fun `a first read has no earlier notes to bring back`() {
        assertNull(RereadMemories.currentReadStart(book.copy(readCount = 0, readStartedAt = 5), emptyList()))
    }

    @Test
    fun `the stored start wins, else the last jump back to the start`() {
        assertEquals(500L, RereadMemories.currentReadStart(book.copy(readStartedAt = 500), emptyList()))
        val sessions = listOf(session(100, 0, 150), session(200, 150, 300), session(300, 0, 40), session(400, 40, 90))
        assertEquals(300L, RereadMemories.currentReadStart(book, sessions))
    }

    @Test
    fun `notes from the last read come back on the pages just read`() {
        val notes = listOf(note("old-30", 30, 10), note("old-60", 60, 10), note("old-120", 120, 10), note("new-50", 50, 900))
        val reached = RereadMemories.reached(notes, readStart = 500, fromPage = 25, toPage = 60)
        assertEquals(listOf("old-30", "old-60"), reached.map { it.id })
        assertEquals(listOf("old-120"), RereadMemories.ahead(notes, 500, currentPage = 60).map { it.id })
        assertEquals(listOf("old-60", "old-30"), RereadMemories.passed(notes, 500, currentPage = 60).map { it.id })
        assertEquals(emptyList<MarginNote>(), RereadMemories.reached(notes, 500, fromPage = 60, toPage = 60))
    }
}
