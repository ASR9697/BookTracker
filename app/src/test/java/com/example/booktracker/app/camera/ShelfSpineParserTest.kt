package com.example.booktracker.app.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShelfSpineParserTest {

    @Test
    fun `imprints and punctuation are dropped from a spine`() {
        assertEquals("Bleak House Charles Dickens", ShelfSpineParser.clean("PENGUIN • Bleak House\nCharles Dickens"))
    }

    @Test
    fun `fragments too short to search are skipped`() {
        assertNull(ShelfSpineParser.clean("lI1"))
        assertNull(ShelfSpineParser.clean("  217  "))
        assertNull(ShelfSpineParser.clean("Vintage"))
    }

    @Test
    fun `repeated spines become one query`() {
        val queries = ShelfSpineParser.queries(listOf("Dune Frank Herbert", "DUNE  FRANK HERBERT", "Circe", "x"))
        assertEquals(listOf("Dune Frank Herbert", "Circe"), queries)
    }
}
