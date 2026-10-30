package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Classification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTidyTest {
    private fun book(id: String, vararg tags: String) =
        Book(id = id, title = id, isbn = null, classification = Classification(emptyList(), tags.toList()))

    @Test
    fun `counts lists the most common tags first`() {
        val books = listOf(book("a", "#Zen", "#Calm"), book("b", "#Calm"), book("c", "#Calm", "#Zen", "#Art"))
        assertEquals(listOf("#Calm" to 3, "#Zen" to 2, "#Art" to 1), TagTidy.counts(books))
    }

    @Test
    fun `renaming only returns books that carry the tag`() {
        val books = listOf(book("a", "#Old", "#Keep"), book("b", "#Keep"))
        val changed = TagTidy.apply(books, "#Old", "#New")
        assertEquals(listOf("a"), changed.map { it.id })
        assertEquals(listOf("#New", "#Keep"), changed.single().classification.tags)
    }

    @Test
    fun `renaming to an existing tag merges without a duplicate`() {
        val books = listOf(book("a", "#SelfHelp", "#Self-Help"))
        val merged = TagTidy.apply(books, "#Self-Help", "#selfhelp").single()
        assertEquals(listOf("#SelfHelp"), merged.classification.tags)
    }

    @Test
    fun `a blank or null name removes the tag`() {
        val books = listOf(book("a", "#Gone", "#Stay"))
        assertEquals(listOf("#Stay"), TagTidy.apply(books, "#Gone", null).single().classification.tags)
        assertEquals(listOf("#Stay"), TagTidy.apply(books, "#Gone", "   ").single().classification.tags)
        assertTrue(TagTidy.apply(books, "#Missing", "#X").isEmpty())
    }
}
