package com.example.booktracker.app.camera

import com.example.booktracker.app.camera.PageNumberDetector.Line
import org.junit.Assert.assertEquals
import org.junit.Test

class PageNumberDetectorTest {

    @Test
    fun `a lone number at the bottom edge is the page`() {
        val lines = listOf(
            Line("It was the best of times, it was the worst of times,", 0.3f),
            Line("in the year 1775 there were", 0.5f),
            Line("143", 0.96f)
        )
        assertEquals(143, PageNumberDetector.candidates(lines, currentPage = 120, totalPages = 400).first().page)
    }

    @Test
    fun `numbers past the last page are ignored`() {
        val lines = listOf(Line("1984", 0.95f), Line("212", 0.04f))
        val pages = PageNumberDetector.candidates(lines, currentPage = 200, totalPages = 350).map { it.page }
        assertEquals(listOf(212), pages)
    }

    @Test
    fun `the number nearest past the bookmark beats a far one`() {
        val lines = listOf(Line("88", 0.95f), Line("12", 0.95f))
        assertEquals(88, PageNumberDetector.candidates(lines, currentPage = 70, totalPages = 300).first().page)
    }

    @Test
    fun `running headers give their page number`() {
        val lines = listOf(Line("56  THE NAME OF THE WIND", 0.03f), Line("Chapter 12 begins here with 3 dogs", 0.4f))
        assertEquals(listOf(56), PageNumberDetector.candidates(lines, currentPage = 50, totalPages = 600).map { it.page })
    }

    @Test
    fun `decorated page numbers are read`() {
        val lines = listOf(Line("— 77 —", 0.97f), Line("p. 78", 0.02f))
        val pages = PageNumberDetector.candidates(lines, currentPage = 70, totalPages = 300).map { it.page }.toSet()
        assertEquals(setOf(77, 78), pages)
    }
}
