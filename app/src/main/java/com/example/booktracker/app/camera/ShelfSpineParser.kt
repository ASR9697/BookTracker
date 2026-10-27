package com.example.booktracker.app.camera

/**
 * Turns the text read off a photo of a bookshelf into search queries, one per
 * spine. Each recognised block is usually one spine (title, often the author);
 * publisher imprints and stray fragments are dropped so every query has a fair
 * chance of finding the book.
 */
object ShelfSpineParser {

    /** At most this many spines are looked up from one photo. */
    const val MAX_SPINES = 30

    // Only names that are almost never part of a title: "House" or "Classics" would eat real titles.
    private val IMPRINTS = setOf(
        "penguin", "vintage", "picador", "bloomsbury", "harpercollins", "faber", "macmillan",
        "gollancz", "scribner", "hachette", "puffin", "corgi", "abacus", "virago", "fontana",
        "bantam", "ballantine", "knopf", "doubleday", "hodder", "stoughton", "rupa", "westland", "jaico"
    )

    fun queries(blocks: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (block in blocks) {
            val cleaned = clean(block) ?: continue
            if (seen.add(cleaned.lowercase())) result += cleaned
            if (result.size == MAX_SPINES) break
        }
        return result
    }

    /** The searchable words of one spine, or null when nothing worth searching is left. */
    fun clean(block: String): String? {
        val words = block
            .replace(Regex("[^\\p{L}\\p{N}'&:\\- ]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim('-', ':', '\'') }
            .filter { it.isNotEmpty() }
            .filterNot { it.lowercase() in IMPRINTS }
        if (words.isEmpty()) return null
        val letters = words.sumOf { word -> word.count { it.isLetter() } }
        // A lone number, a couple of letters or OCR noise like "lI1" won't find a book.
        if (letters < 4) return null
        return words.joinToString(" ").take(80)
    }
}
