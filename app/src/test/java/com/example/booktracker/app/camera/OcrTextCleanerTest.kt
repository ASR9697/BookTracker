package com.example.booktracker.app.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextCleanerTest {

    @Test
    fun `a word hyphenated across lines is rejoined`() {
        assertEquals("I could not understand it", OcrTextCleaner.unwrap("I could not un-\nderstand it"))
    }

    @Test
    fun `a real hyphen before a capital is kept`() {
        assertEquals("the Anglo-Saxon kings", OcrTextCleaner.unwrap("the Anglo-\nSaxon kings"))
    }

    @Test
    fun `paragraph lines are unwrapped and blank lines kept`() {
        assertEquals(
            "One line and the next.\n\nA new paragraph.",
            OcrTextCleaner.clean("One line\n and the next.\n\n\nA new\nparagraph.")
        )
    }

    @Test
    fun `digits inside words become letters`() {
        assertEquals("the world was cold", OcrTextCleaner.fixSlips("the wor1d was c0ld"))
        assertEquals("page 101 and 1st", OcrTextCleaner.fixSlips("page 101 and 1st"))
    }

    @Test
    fun `rn read for m is fixed only into real words`() {
        assertEquals("Modern times", OcrTextCleaner.fixSlips("Modern tirnes"))
        assertEquals("the modern world", OcrTextCleaner.fixSlips("the rnodern world"))
        assertEquals("turn the corner", OcrTextCleaner.fixSlips("turn the corner"))
        assertEquals("REMEMBER", OcrTextCleaner.fixSlips("RERNEMBER"))
    }

    @Test
    fun `a lone bar or l is the pronoun I`() {
        assertEquals("And I think so", OcrTextCleaner.fixSlips("And | think so"))
        assertEquals("I said", OcrTextCleaner.fixSlips("l said"))
    }

    @Test
    fun `straight quotes become typographic`() {
        assertEquals("“Don’t go,” she said.", OcrTextCleaner.smartQuotes("\"Don't go,\" she said."))
        assertEquals("the readers’ ‘club’", OcrTextCleaner.smartQuotes("the readers' 'club'"))
    }

    @Test
    fun `spacing before punctuation and double dashes are tidied`() {
        assertEquals("Wait, no—stop!", OcrTextCleaner.tidySpacing("Wait , no -- stop !"))
    }
}
