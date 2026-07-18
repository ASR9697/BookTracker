package com.example.booktracker.app.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * OpenLibrary search — keyless and far more lenient than Google Books, used as a
 * free fallback when Google throttles (429) or errors out. Covers come from the
 * numeric cover id; page counts are the edition median and are sometimes absent
 * (0), which is fine — the app treats a manual/unknown page count the same way.
 */
object OpenLibraryClient {

    private const val SEARCH = "https://openlibrary.org/search.json"
    // Restrict the payload to the handful of fields we map — OpenLibrary docs are huge otherwise.
    private const val FIELDS =
        "title,author_name,cover_i,number_of_pages_median,isbn,first_publish_year,subject"

    suspend fun search(query: String): List<BookMetadata> = withContext(Dispatchers.IO) {
        val url = "$SEARCH?q=${URLEncoder.encode(query, "UTF-8")}&limit=20&fields=$FIELDS"
        parseDocs(httpGet(url))
    }

    suspend fun lookupIsbn(isbn: String): BookMetadata? = withContext(Dispatchers.IO) {
        val url = "$SEARCH?q=isbn:${URLEncoder.encode(isbn, "UTF-8")}&limit=1&fields=$FIELDS"
        parseDocs(httpGet(url)).firstOrNull()
    }

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", BOOK_CLIENT_USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw BookSearchException(
                    if (code == 429) BookSearchException.Kind.RATE_LIMITED
                    else BookSearchException.Kind.SERVER,
                    "OpenLibrary HTTP $code"
                )
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw BookSearchException(BookSearchException.Kind.NETWORK, "Network error", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseDocs(body: String): List<BookMetadata> {
        val docs = JSONObject(body).optJSONArray("docs") ?: return emptyList()
        val results = mutableListOf<BookMetadata>()
        for (i in 0 until docs.length()) {
            val doc = docs.optJSONObject(i) ?: continue
            val title = doc.optString("title")
            if (title.isBlank()) continue

            val authorsArray = doc.optJSONArray("author_name")
            val authors = if (authorsArray != null) {
                (0 until authorsArray.length()).map { authorsArray.getString(it) }
            } else {
                emptyList()
            }

            val coverId = doc.optInt("cover_i", 0)
            val coverUrl =
                if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-M.jpg" else ""

            val isbnArray = doc.optJSONArray("isbn")
            var isbn = ""
            if (isbnArray != null && isbnArray.length() > 0) {
                // Prefer a 13-digit ISBN; fall back to whatever comes first.
                for (j in 0 until isbnArray.length()) {
                    val candidate = isbnArray.optString(j)
                    if (candidate.length == 13) {
                        isbn = candidate
                        break
                    }
                }
                if (isbn.isEmpty()) isbn = isbnArray.optString(0)
            }

            val subjects = doc.optJSONArray("subject")
            val genres = if (subjects != null) {
                (0 until minOf(subjects.length(), 5)).map { subjects.getString(it).trim() }
            } else {
                emptyList()
            }
            val publishYear = doc.optInt("first_publish_year", 0)

            results.add(BookMetadata(
                isbn = isbn,
                title = title,
                authors = authors,
                pageCount = doc.optInt("number_of_pages_median", 0),
                coverUrl = coverUrl,
                // No description in OpenLibrary search results; the field stays empty.
                genres = genres,
                publishedDate = if (publishYear > 0) publishYear.toString() else ""
            ))
        }
        return results
    }
}
