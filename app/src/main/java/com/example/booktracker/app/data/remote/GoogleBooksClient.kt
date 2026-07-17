package com.example.booktracker.app.data.remote

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ScannedBook(
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val pageCount: Int,
    val coverUrl: String
)

/**
 * ISBN lookup against the Google Books volumes endpoint, which is free and
 * needs no API key. Returns null when the ISBN has no match; throws on
 * network failure (callers decide how to surface that).
 */
object GoogleBooksClient {

    private const val ENDPOINT = "https://www.googleapis.com/books/v1/volumes"

    suspend fun lookup(isbn: String): ScannedBook? = withContext(Dispatchers.IO) {
        val connection =
            (URL("$ENDPOINT?q=isbn:$isbn").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(isbn, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(isbn: String, body: String): ScannedBook? {
        val root = JSONObject(body)
        if (root.optInt("totalItems") == 0) return null
        val volumeInfo = root.optJSONArray("items")
            ?.optJSONObject(0)
            ?.optJSONObject("volumeInfo")
            ?: return null

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

        return ScannedBook(
            isbn = isbn,
            title = title,
            authors = authors,
            pageCount = volumeInfo.optInt("pageCount"),
            coverUrl = coverUrl
        )
    }
}
