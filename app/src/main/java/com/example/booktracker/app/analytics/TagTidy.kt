package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book

/**
 * Housekeeping for tags. Tags double as genres, and books added by search arrive
 * with a long tail of one-off tags; renaming one to an existing tag merges them,
 * and renaming to nothing removes it.
 */
object TagTidy {
    /** Every tag with the number of books carrying it, most common first, then A to Z. */
    fun counts(books: List<Book>): List<Pair<String, Int>> =
        books.flatMap { it.classification.tags.distinct() }
            .filter { it.isNotBlank() }
            .groupingBy { it }.eachCount()
            .toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })

    /**
     * The books that carry [from], with it renamed to [to] (or dropped when [to] is
     * null or blank). A book left with the same tag twice, ignoring case, keeps the
     * first. Books without [from] are not returned, so only they need saving.
     */
    fun apply(books: List<Book>, from: String, to: String?): List<Book> {
        val target = to?.trim()?.takeIf { it.isNotEmpty() }
        return books.mapNotNull { book ->
            val tags = book.classification.tags
            if (from !in tags) return@mapNotNull null
            val renamed = tags.mapNotNull { if (it == from) target else it }
            book.copy(classification = book.classification.copy(tags = renamed.distinctBy { it.lowercase() }))
        }
    }
}
