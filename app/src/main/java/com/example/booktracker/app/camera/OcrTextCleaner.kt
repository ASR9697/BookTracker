package com.example.booktracker.app.camera

/**
 * Tidies text read off a printed page before it becomes a note: rejoins words
 * split across lines ("un-\nderstand"), unwraps the physical line breaks of a
 * paragraph, fixes the usual optical slips ("wor1d", "| think", "rnodern") and
 * sets straight quotes as typographic ones. Every fix is conservative: when a
 * rule can't tell an error from a real word it leaves the text alone.
 */
object OcrTextCleaner {

    /** Cleans one recognised block; blank lines between paragraphs survive as one blank line. */
    fun clean(raw: String): String =
        raw.replace("\r\n", "\n")
            .split(Regex("\\n\\s*\\n"))
            .map { paragraph -> cleanParagraph(paragraph) }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")

    private fun cleanParagraph(paragraph: String): String {
        val joined = unwrap(paragraph)
        val words = fixSlips(joined)
        return smartQuotes(tidySpacing(words))
    }

    /** Joins the physical lines of a paragraph, rejoining words a line end hyphenated. */
    fun unwrap(paragraph: String): String {
        val lines = paragraph.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val out = StringBuilder()
        for (line in lines) {
            if (out.isEmpty()) {
                out.append(line)
                continue
            }
            val last = out.last()
            val prev = out.getOrNull(out.length - 2)
            when {
                // "un-" + "derstand": a soft break. A capital after it ("Anglo-" + "Saxon") is a real hyphen.
                last == '-' && prev != null && prev.isLetter() && line.first().isLowerCase() -> {
                    out.setLength(out.length - 1)
                    out.append(line)
                }
                last == '-' && prev != null && prev.isLetter() -> out.append(line)
                // A dash ending a line runs straight into the next word.
                last == '—' || last == '–' -> out.append(line)
                else -> out.append(' ').append(line)
            }
        }
        return out.toString()
    }

    /** Character-level slips, fixed per word so real words are never touched. */
    fun fixSlips(text: String): String =
        WORD.replace(text) { match -> fixWord(match.value) }
            // A lone "|" or "l" before a word is almost always the pronoun I.
            .replace(Regex("(^|(?<=[\\s“\"(]))[|l](?=\\s+\\p{L})")) { "I" }

    private fun fixWord(word: String): String {
        var fixed = word
        // Digits wedged between letters: "wor1d", "c0ld", "5ome" stays (could be a code).
        if (fixed.any { it.isLetter() } && fixed.any { it.isDigit() }) {
            fixed = Regex("(?<=\\p{L})[10](?=\\p{L})").replace(fixed) { if (it.value == "1") "l" else "o" }
        }
        // "rn" read for "m": only when the result is a common word and the original is not.
        val lower = fixed.lowercase()
        if ("rn" in lower && lower !in COMMON_WORDS) {
            mSpellings(lower).firstOrNull { it in COMMON_WORDS }?.let { fixed = matchCase(fixed, it) }
        }
        return fixed
    }

    /** Every way of reading some of the "rn"s in [word] as "m", fewest changes first. */
    private fun mSpellings(word: String): List<String> {
        val at = Regex("rn").findAll(word).map { it.range.first }.toList().take(4)
        return (1 until (1 shl at.size))
            .sortedBy { Integer.bitCount(it) }
            .map { mask ->
                val sb = StringBuilder(word)
                // Right to left so earlier offsets stay valid as the word shrinks.
                at.indices.reversed().filter { mask and (1 shl it) != 0 }
                    .forEach { sb.replace(at[it], at[it] + 2, "m") }
                sb.toString()
            }
    }

    private fun matchCase(original: String, replacement: String): String = when {
        original.all { !it.isLetter() || it.isUpperCase() } -> replacement.uppercase()
        original.first().isUpperCase() -> replacement.replaceFirstChar { it.uppercase() }
        else -> replacement
    }

    /** Doubled spaces, spaces before punctuation, and "--" as a dash. */
    fun tidySpacing(text: String): String =
        text.replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex(" +([,.;:!?])"), "$1")
            .replace(Regex(" ?-- ?"), "—")
            .trim()

    /** Straight quotes become curly ones: opening after a space or bracket, closing otherwise. */
    fun smartQuotes(text: String): String {
        val out = StringBuilder(text.length)
        for ((i, c) in text.withIndex()) {
            val before = text.getOrNull(i - 1)
            val opens = before == null || before.isWhitespace() || before in "([{—–“‘"
            out.append(
                when (c) {
                    '"' -> if (opens) '“' else '”'
                    // Apostrophes inside words ("don't") and at their end ("readers'") close.
                    '\'' -> if (opens) '‘' else '’'
                    else -> c
                }
            )
        }
        return out.toString()
    }

    private val WORD = Regex("[\\p{L}\\p{N}]+")

    // Words where "m" is often read as "rn"; small on purpose, every entry is a word
    // whose "rn" spelling is not itself a word.
    private val COMMON_WORDS = setOf(
        "modern", "more", "most", "mother", "moment", "morning", "make", "made", "man", "men", "many",
        "much", "must", "my", "me", "may", "might", "mind", "money", "month", "move", "music", "time",
        "times", "come", "came", "some", "same", "name", "home", "him", "them", "from", "seem", "seemed",
        "seems", "room", "form", "warm", "arm", "arms", "harm", "farm", "storm", "small", "smile", "smiled",
        "memory", "remember", "remembered", "summer", "number", "woman", "women", "human", "himself",
        "themselves", "something", "sometimes", "moon", "mouth", "am", "whom", "become", "became",
        "welcome", "simple", "dream", "dreams", "stream", "team", "mile", "miles", "meet", "met", "mean",
        "meant", "means", "moved", "mourn", "mark", "marked"
    )
}
