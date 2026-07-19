package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus

data class Recommendation(val book: Book, val reason: String)

/**
 * A purely local, on-device recommendation engine that heuristically matches
 * backlog/shortlist books to the genres and authors of highly-rated finished books.
 * No machine learning dependencies or network requests required.
 */
object ReadingGenome {

    fun getRecommendations(books: List<Book>): List<Recommendation> {
        val finishedHighlyRated = books.filter {
            it.status == BookStatus.FINISHED.name && 
            it.rating.values.isNotEmpty() && 
            it.rating.values.average() >= 4.0
        }

        if (finishedHighlyRated.isEmpty()) {
            return emptyList()
        }

        // Build a taste profile
        val favoriteAuthors = finishedHighlyRated.flatMap { it.authors }.filter { it.isNotBlank() }.toSet()
        val favoriteGenres = finishedHighlyRated.flatMap { it.genres }.filter { it.isNotBlank() }.toSet()
        
        // Find best source books for reasons
        val bestBooksByGenre = mutableMapOf<String, String>()
        for (book in finishedHighlyRated) {
            for (genre in book.genres) {
                if (genre.isNotBlank() && !bestBooksByGenre.containsKey(genre)) {
                    bestBooksByGenre[genre] = book.title
                }
            }
        }
        
        val bestBooksByAuthor = mutableMapOf<String, String>()
        for (book in finishedHighlyRated) {
            for (author in book.authors) {
                if (author.isNotBlank() && !bestBooksByAuthor.containsKey(author)) {
                    bestBooksByAuthor[author] = book.title
                }
            }
        }

        // Find candidates
        val candidates = books.filter { 
            it.status == BookStatus.BACKLOG.name || it.status == BookStatus.SHORTLIST.name
        }

        val scored = candidates.mapNotNull { candidate ->
            var score = 0
            var reason = ""

            // Exact author match is a strong signal
            val matchedAuthors = candidate.authors.intersect(favoriteAuthors)
            if (matchedAuthors.isNotEmpty()) {
                score += matchedAuthors.size * 5
                val primaryMatched = matchedAuthors.first()
                val lovedBook = bestBooksByAuthor[primaryMatched]
                reason = if (lovedBook != null) "Because you loved $lovedBook" else "More by $primaryMatched"
            }

            // Genre overlap
            val matchedGenres = candidate.genres.intersect(favoriteGenres)
            if (matchedGenres.isNotEmpty()) {
                score += matchedGenres.size * 2
                if (reason.isEmpty()) {
                    val primaryMatched = matchedGenres.first()
                    val lovedBook = bestBooksByGenre[primaryMatched]
                    reason = if (lovedBook != null) "Because you liked $lovedBook" else "More $primaryMatched for you"
                }
            }

            if (score > 0) {
                Recommendation(candidate, reason) to score
            } else {
                null
            }
        }

        return scored.sortedByDescending { it.second }.take(5).map { it.first }
    }
}
