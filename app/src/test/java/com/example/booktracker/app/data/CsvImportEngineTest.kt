package com.example.booktracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvImportEngineTest {

    @Test
    fun `quoted fields keep commas and escaped quotes`() {
        val rows = CsvImportEngine.parseCsv(
            "Title,Author\n\"Dune, Deluxe Edition\",\"Frank \"\"The Prophet\"\" Herbert\"\n"
        )
        assertEquals(listOf("Title", "Author"), rows[0])
        assertEquals(listOf("Dune, Deluxe Edition", "Frank \"The Prophet\" Herbert"), rows[1])
    }

    @Test
    fun `line breaks inside a quoted review stay in one row`() {
        val rows = CsvImportEngine.parseCsv(
            "Title,My Review\r\nDune,\"Great.\nWould read again.\"\r\nEmma,Fine\r\n"
        )
        assertEquals(3, rows.size)
        assertEquals("Great.\nWould read again.", rows[1][1])
        assertEquals(listOf("Emma", "Fine"), rows[2])
    }

    @Test
    fun `blank lines are skipped and a missing trailing newline still ends the last row`() {
        val rows = CsvImportEngine.parseCsv("Title\n\nDune\n\nEmma")
        assertEquals(listOf(listOf("Title"), listOf("Dune"), listOf("Emma")), rows)
    }

    @Test
    fun `goodreads isbn wrappers are stripped`() {
        assertEquals("9780441013593", CsvImportEngine.normalizeIsbn("=\"9780441013593\""))
        assertEquals("044101359X", CsvImportEngine.normalizeIsbn("=\"044101359x\""))
    }

    @Test
    fun `values that are not isbns are rejected`() {
        assertNull(CsvImportEngine.normalizeIsbn("=\"\""))
        assertNull(CsvImportEngine.normalizeIsbn("B00ABC123"))
        assertNull(CsvImportEngine.normalizeIsbn(null))
    }
}
