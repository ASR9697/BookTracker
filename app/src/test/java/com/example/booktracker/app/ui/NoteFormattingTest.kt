package com.example.booktracker.app.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.booktracker.app.data.NoteDraft
import com.example.booktracker.app.journal.JournalingEngine
import com.example.booktracker.shared.models.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteFormattingTest {

    private fun v(text: String, from: Int = text.length, to: Int = from) = TextFieldValue(text, TextRange(from, to))

    @Test
    fun `bold wraps the selection and unwraps it again`() {
        val bold = NoteFormatting.toggleWrap(v("Start small today", 6, 11), NoteFormatting.BOLD)
        assertEquals("Start **small** today", bold.text)
        assertEquals(TextRange(8, 13), bold.selection)
        assertEquals("Start small today", NoteFormatting.toggleWrap(bold, NoteFormatting.BOLD).text)
    }

    @Test
    fun `bold with nothing selected leaves the cursor between the markers`() {
        val empty = NoteFormatting.toggleWrap(v("Hi ", 3), NoteFormatting.BOLD)
        assertEquals("Hi ****", empty.text)
        assertEquals(TextRange(5), empty.selection)
    }

    @Test
    fun `what the toolbar writes is what the renderer shows`() {
        val bold = JournalingEngine.render(NoteFormatting.toggleWrap(v("one two", 0, 3), NoteFormatting.BOLD).text)
        assertEquals("one two", bold.text)
        assertEquals(1, bold.spanStyles.size)
        val italic = JournalingEngine.render(NoteFormatting.toggleWrap(v("one two", 4, 7), NoteFormatting.ITALIC).text)
        assertEquals("one two", italic.text)
        assertEquals(1, italic.spanStyles.size)
    }

    @Test
    fun `bullets toggle on every selected line`() {
        val text = "first\nsecond\nthird"
        val on = NoteFormatting.toggleBullets(v(text, 2, 9))
        assertEquals("• first\n• second\nthird", on.text)
        assertEquals(text, NoteFormatting.toggleBullets(v(on.text, 0, 12)).text)
    }

    @Test
    fun `dictation lands at the cursor with a space where needed`() {
        assertEquals("Hello world", NoteFormatting.insertAtCursor(v("Hello"), " world ").text)
        assertEquals("Hello big world", NoteFormatting.insertAtCursor(v("Hello world", 5), "big").text)
        assertEquals("start", NoteFormatting.insertAtCursor(v(""), "start").text)
    }

    @Test
    fun `scanned text becomes a new paragraph`() {
        assertEquals("a\n\nb", NoteFormatting.appendParagraph(v("a\n"), "b").text)
        assertEquals("b", NoteFormatting.appendParagraph(v(""), " b ").text)
    }

    @Test
    fun `physical line breaks from OCR are joined`() {
        assertEquals("The quick brown fox", NoteFormatting.unwrapLines("The quick\n brown \n\nfox"))
    }

    @Test
    fun `drafts round-trip and ignore damaged or empty ones`() {
        val draft = NoteDraft("26", "A quote", NoteType.PERSONAL_THOUGHT)
        assertEquals(draft, NoteDraft.parse(NoteDraft.toJson(draft)))
        assertNull(NoteDraft.parse(null))
        assertNull(NoteDraft.parse("not json"))
        assertNull(NoteDraft.parse("""{"page":"1","text":"   ","type":"RANDOM"}"""))
        assertNull(NoteDraft.parse("""{"text":"hi"}"""))
    }
}
