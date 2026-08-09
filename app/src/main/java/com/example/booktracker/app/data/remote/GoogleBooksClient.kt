package com.example.booktracker.app.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class BookMetadata(
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val pageCount: Int,
    val coverUrl: String,
    val description: String = "",
    val genres: List<String> = emptyList(),
    val publishedDate: String = "",
    val currentPage: Int = 0,
    val creators: List<com.example.booktracker.shared.models.Creator> = emptyList(),
    val purchaseLog: List<com.example.booktracker.shared.models.PurchaseLog> = emptyList(),
    val loanRecord: List<com.example.booktracker.shared.models.LoanRecord> = emptyList()
)

// Typed so the UI can tell "slow down" (rate limit) from "you're offline" from a
// genuine failure, instead of showing one opaque "Search failed" for everything.
class BookSearchException(
    val kind: Kind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    enum class Kind { RATE_LIMITED, NETWORK, SERVER, UNKNOWN }
}

internal const val BOOK_CLIENT_USER_AGENT = "BookTracker/1.0 (personal reading tracker)"

/**
 * Google Books volumes endpoint — free, no API key. Google throttles keyless
 * traffic hard (HTTP 429), so unlike a naive read of `inputStream` this checks
 * the status code, drains the error stream, and retries 429/5xx with backoff
 * before giving up. Persistent failures surface as a typed [BookSearchException]
 * so callers can fall back to another free source (see [BookSearch]) rather than
 * dead-ending on an opaque error.
 */
object GoogleBooksClient {

    private const val ENDPOINT = "https://www.googleapis.com/books/v1/volumes"
    private const val MAX_ATTEMPTS = 3

    suspend fun lookup(isbn: String): BookMetadata? = withContext(Dispatchers.IO) {
        val body = httpGet("$ENDPOINT?q=isbn:${URLEncoder.encode(isbn, "UTF-8")}")
        parse(isbn, body)
    }

    suspend fun search(query: String): List<BookMetadata> = withContext(Dispatchers.IO) {
        val body = httpGet("$ENDPOINT?q=${URLEncoder.encode(query, "UTF-8")}&maxResults=20")
        parseList(body)
    }

    private suspend fun httpGet(urlString: String): String {
        var lastError: BookSearchException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", BOOK_CLIENT_USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            try {
                when (val code = connection.responseCode) {
                    in 200..299 ->
                        return connection.inputStream.bufferedReader().use { it.readText() }
                    // Transient: drain the error body (lets the socket be reused) and retry.
                    429, in 500..599 -> {
                        connection.errorStream?.bufferedReader()?.use { it.readText() }
                        lastError = BookSearchException(
                            if (code == 429) BookSearchException.Kind.RATE_LIMITED
                            else BookSearchException.Kind.SERVER,
                            "HTTP $code"
                        )
                    }
                    else -> {
                        connection.errorStream?.bufferedReader()?.use { it.readText() }
                        throw BookSearchException(BookSearchException.Kind.UNKNOWN, "HTTP $code")
                    }
                }
            } catch (e: IOException) {
                lastError = BookSearchException(BookSearchException.Kind.NETWORK, "Network error", e)
            } finally {
                connection.disconnect()
            }
            if (attempt < MAX_ATTEMPTS - 1) delay(400L * (attempt + 1)) // 400ms, then 800ms
        }
        throw lastError ?: BookSearchException(BookSearchException.Kind.UNKNOWN, "Request failed")
    }

    private fun parse(isbn: String, body: String): BookMetadata? {
        val root = JSONObject(body)
        if (root.optInt("totalItems") == 0) return null
        val volumeInfo = root.optJSONArray("items")
            ?.optJSONObject(0)
            ?.optJSONObject("volumeInfo")
            ?: return null

        return parseBook(isbn, volumeInfo)
    }

    private fun parseBook(isbn: String, volumeInfo: JSONObject): BookMetadata? {
        val title = volumeInfo.optString("title")
        if (title.isBlank()) return null

        val authorsArray = volumeInfo.optJSONArray("authors")
        val authors = if (authorsArray != null) {
            (0 until authorsArray.length()).map { authorsArray.getString(it) }
        } else {
            emptyList()
        }

        val coverUrl = volumeInfo.optJSONObject("imageLinks")
            ?.optString("thumbnail")
            .orEmpty()
            // Thumbnails often come back as http://; cleartext is blocked on modern Android.
            .replace("http://", "https://")

        return BookMetadata(
            isbn = isbn,
            title = title,
            authors = authors,
            pageCount = volumeInfo.optInt("pageCount"),
            coverUrl = coverUrl,
            description = volumeInfo.optString("description"),
            genres = volumeInfo.categories(),
            publishedDate = volumeInfo.optString("publishedDate")
        )
    }

    // Google Books categories arrive as slash-separated paths ("Fiction / Fantasy /
    // Epic"); split and dedupe them so the UI gets clean single-word-ish genre chips.
    private fun JSONObject.categories(): List<String> {
        val array = optJSONArray("categories") ?: return emptyList()
        return (0 until array.length())
            .flatMap { array.getString(it).split("/") }
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("General", ignoreCase = true) }
            .distinct()
            .take(5)
    }

    private fun parseList(body: String): List<BookMetadata> {
        val root = JSONObject(body)
        if (root.optInt("totalItems") == 0) return emptyList()
        val items = root.optJSONArray("items") ?: return emptyList()

        val results = mutableListOf<BookMetadata>()
        for (i in 0 until items.length()) {
            val volumeInfo = items.optJSONObject(i)?.optJSONObject("volumeInfo") ?: continue

            val industryIdentifiers = volumeInfo.optJSONArray("industryIdentifiers")
            var isbn = ""
            if (industryIdentifiers != null) {
                for (j in 0 until industryIdentifiers.length()) {
                    val idObj = industryIdentifiers.optJSONObject(j)
                    if (idObj?.optString("type") == "ISBN_13") {
                        isbn = idObj.optString("identifier")
                        break
                    }
                }
                if (isbn.isEmpty()) {
                     for (j in 0 until industryIdentifiers.length()) {
                        val idObj = industryIdentifiers.optJSONObject(j)
                        if (idObj?.optString("type") == "ISBN_10") {
                            isbn = idObj.optString("identifier")
                            break
                        }
                    }
                }
            }

            val book = parseBook(isbn, volumeInfo)
            if (book != null) {
                results.add(book)
            }
        }
        return results
    }
}
