package com.example.booktracker.app.language

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabCandidatesTest {

    @Test
    fun `each word is offered once, short words and numbers skipped`() {
        assertEquals(
            listOf("chat", "dort", "dans", "maison"),
            VocabCandidates.words("Le chat dort. Le Chat dort dans la maison 1984.")
        )
    }

    @Test
    fun `accented and apostrophe words stay whole`() {
        assertEquals(listOf("été", "aujourd'hui"), VocabCandidates.words("L'été, aujourd'hui."))
    }

    @Test
    fun `the context is the sentence holding the word`() {
        val passage = "Il pleut. Le chat dort sur le lit. Fin."
        assertEquals("Le chat dort sur le lit.", VocabCandidates.sentenceWith(passage, "dort"))
    }
}
