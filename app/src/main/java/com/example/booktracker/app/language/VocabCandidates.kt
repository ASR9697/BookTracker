package com.example.booktracker.app.language

/**
 * The words of a foreign passage worth offering for the vocabulary list: each
 * distinct word once, in reading order, skipping numbers and the very short
 * function words ("le", "of", "и") nobody needs a flashcard for.
 */
object VocabCandidates {

    const val MIN_LETTERS = 3
    const val MAX_WORDS = 40

    private val ELISION = Regex("^(?i)(qu|[cdjlmnst])['’]")

    fun words(passage: String): List<String> {
        val seen = mutableSetOf<String>()
        return Regex("[\\p{L}\\p{M}][\\p{L}\\p{M}'’-]*").findAll(passage)
            .map { it.value.trimEnd('\'', '’', '-') }
            // French/Italian elision: "l'été" is the word "été".
            .map { it.replace(ELISION, "") }
            .filter { word -> word.count { it.isLetter() } >= MIN_LETTERS }
            .filter { seen.add(it.lowercase()) }
            .take(MAX_WORDS)
            .toList()
    }

    /** The sentence of [passage] that holds [word], kept as the word's context. */
    fun sentenceWith(passage: String, word: String): String {
        val sentences = passage.split(Regex("(?<=[.!?。！？])\\s+"))
        return sentences.firstOrNull { Regex("(?i)(?<![\\p{L}])${Regex.escape(word)}(?![\\p{L}])").containsMatchIn(it) }
            ?.trim()
            ?: passage.trim().take(200)
    }
}
