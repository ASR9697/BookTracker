package com.example.booktracker.app.data.remote

import kotlinx.coroutines.CancellationException

/**
 * Book metadata lookup facade. Tries Google Books first (richer page counts and
 * covers) and falls back to the keyless OpenLibrary API when Google throttles
 * (429) or errors — so search keeps working without an API key, an account, or
 * any paid service. Only throws when *both* free sources fail, and surfaces the
 * most informative [BookSearchException] so the UI can explain why.
 */
object BookSearch {

    suspend fun search(query: String): List<BookMetadata> {
        val google = attempt { GoogleBooksClient.search(query) }
        google.getOrNull()?.let { if (it.isNotEmpty()) return it }

        // Google returned nothing or failed → try the more lenient OpenLibrary.
        val open = attempt { OpenLibraryClient.search(query) }
        open.getOrNull()?.let { return it }

        throw google.exceptionOrNull() as? BookSearchException
            ?: open.exceptionOrNull() as? BookSearchException
            ?: BookSearchException(BookSearchException.Kind.UNKNOWN, "Search failed")
    }

    suspend fun lookup(isbn: String): BookMetadata? {
        attempt { GoogleBooksClient.lookup(isbn) }.getOrNull()?.let { return it }
        return attempt { OpenLibraryClient.lookupIsbn(isbn) }.getOrNull()
    }

    // Like runCatching, but never swallows coroutine cancellation — otherwise a
    // new keystroke cancelling the in-flight search would be turned into a
    // "Google failed" and pointlessly hit the fallback.
    private suspend inline fun <T> attempt(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
