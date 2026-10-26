package com.example.booktracker.app.data

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.RatingAxis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookModelTest {

    private val book = Book(id = "1", title = "Dune", isbn = null)

    @Test
    fun `explicit overall rating wins over the axes`() {
        val rated = book.copy(rating = mapOf(RatingAxis.OVERALL to 4.5f, "Plot" to 2f))
        assertEquals(4.5f, rated.overallRating)
    }

    @Test
    fun `without an overall score the rated axes are averaged`() {
        val rated = book.copy(rating = mapOf("Plot" to 4f, "Pacing" to 3f))
        assertEquals(3.5f, rated.overallRating)
    }

    @Test
    fun `unrated book has no overall rating`() {
        assertNull(book.overallRating)
    }

    @Test
    fun `finish date ignores later edits once recorded`() {
        val finished = book.copy(finishedAt = 1_000L, lastUpdated = 9_000L)
        assertEquals(1_000L, finished.completedAt)
    }

    @Test
    fun `rows from before finishedAt existed fall back to last update`() {
        assertEquals(9_000L, book.copy(lastUpdated = 9_000L).completedAt)
    }
}
