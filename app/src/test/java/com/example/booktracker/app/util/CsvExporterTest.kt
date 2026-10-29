package com.example.booktracker.app.util

import com.example.booktracker.app.data.hourInWindow
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.RatingAxis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CsvExporterTest {

    private fun at(date: String) = LocalDate.parse(date).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `fields with commas or quotes are quoted`() {
        assertEquals("plain", CsvExporter.quote("plain"))
        assertEquals("\"a, b\"", CsvExporter.quote("a, b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.quote("say \"hi\""))
    }

    @Test
    fun `a finished book becomes a read row with its rating and date`() {
        val book = Book(
            id = "1", title = "Dune, Book One", isbn = "9780441013593", totalPages = 412,
            creators = listOf(Creator(CreatorRole.Author, "Frank Herbert")), status = BookStatus.FINISHED.name,
            rating = mapOf(RatingAxis.OVERALL to 4.5f), finishedAt = at("2026-03-08"), dateAdded = at("2026-01-02")
        )
        val lines = CsvExporter.toGoodreadsCsv(listOf(book), ZoneOffset.UTC).trim().lines()
        assertEquals(2, lines.size)
        assertEquals("\"Dune, Book One\",Frank Herbert,,9780441013593,5,Paperback,412,2026/03/08,2026/01/02,read,,1", lines[1])
    }

    @Test
    fun `hour windows can run past midnight`() {
        assertTrue(hourInWindow(23, 22, 7))
        assertTrue(hourInWindow(3, 22, 7))
        assertFalse(hourInWindow(7, 22, 7))
        assertTrue(hourInWindow(10, 9, 17))
        assertFalse(hourInWindow(17, 9, 17))
    }
}
