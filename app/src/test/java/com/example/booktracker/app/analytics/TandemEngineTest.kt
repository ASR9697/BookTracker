package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TandemEngineTest {

    private val book = Book(id = "b", title = "T", isbn = null, totalPages = 300, currentPage = 150, audioMinutes = 600)

    @Test
    fun `audio position is the same fraction as the page`() {
        assertEquals(300, TandemEngine.audioPositionFor(book))
        assertEquals(150, TandemEngine.pageForAudio(book, 300))
    }

    @Test
    fun `listening faster covers more pages`() {
        // 20 real minutes at 1.5x = 30 narration minutes = 15 pages of a 2-min-per-page book.
        assertEquals(165, TandemEngine.pageAfterListening(book.copy(audioSpeed = 1.5f), 20))
        assertEquals(160, TandemEngine.pageAfterListening(book, 20))
    }

    @Test
    fun `listening past the end stops at the last page`() {
        assertEquals(300, TandemEngine.pageAfterListening(book, 10_000))
    }

    @Test
    fun `time left shrinks with speed`() {
        assertEquals(300, TandemEngine.listeningMinutesLeft(book))
        assertEquals(240, TandemEngine.listeningMinutesLeft(book.copy(audioSpeed = 1.25f)))
    }

    @Test
    fun `a book without an audiobook is not tandem`() {
        assertFalse(TandemEngine.isTandem(book.copy(audioMinutes = 0)))
        assertEquals(150, TandemEngine.pageAfterListening(book.copy(audioMinutes = 0), 30))
    }

    @Test
    fun `lengths parse in the forms people type`() {
        assertEquals(580, TandemEngine.parseLength("9:40"))
        assertEquals(580, TandemEngine.parseLength("9h 40m"))
        assertEquals(540, TandemEngine.parseLength("9h"))
        assertEquals(45, TandemEngine.parseLength("45m"))
        assertEquals(580, TandemEngine.parseLength("580"))
        assertNull(TandemEngine.parseLength("soon"))
    }

    @Test
    fun `clock and speed read naturally`() {
        assertEquals("3h 05m", TandemEngine.formatClock(185))
        assertEquals("42m", TandemEngine.formatClock(42))
        assertEquals("1.25x", TandemEngine.formatSpeed(1.25f))
        assertEquals("2x", TandemEngine.formatSpeed(2f))
    }
}
