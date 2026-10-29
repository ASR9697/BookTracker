package com.example.booktracker.app.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Word definitions from the free, keyless dictionaryapi.dev. Only the word
 * itself is sent, and only when the reader taps "Look up"; nothing else about
 * the library leaves the phone.
 */
object DictionaryClient {

    private const val ENDPOINT = "https://api.dictionaryapi.dev/api/v2/entries/en/"

    data class Definition(val partOfSpeech: String, val meaning: String, val example: String?)

    /** Up to a few senses, most common first; empty when the word isn't in the dictionary. */
    suspend fun lookUp(word: String): Result<List<Definition>> = withContext(Dispatchers.IO) {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return@withContext Result.success(emptyList())
        runCatching {
            val connection = (URL(ENDPOINT + URLEncoder.encode(clean, "UTF-8").replace("+", "%20"))
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                when (connection.responseCode) {
                    404 -> emptyList()
                    in 200..299 -> parse(connection.inputStream.bufferedReader().use { it.readText() })
                    else -> throw IOException("Dictionary HTTP ${connection.responseCode}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parse(body: String): List<Definition> {
        val result = mutableListOf<Definition>()
        val entries = JSONArray(body)
        for (i in 0 until entries.length()) {
            val meanings = entries.optJSONObject(i)?.optJSONArray("meanings") ?: continue
            for (m in 0 until meanings.length()) {
                val meaning = meanings.optJSONObject(m) ?: continue
                val part = meaning.optString("partOfSpeech")
                val definitions = meaning.optJSONArray("definitions") ?: continue
                for (d in 0 until minOf(definitions.length(), 2)) {
                    val def = definitions.optJSONObject(d) ?: continue
                    val text = def.optString("definition").trim()
                    if (text.isNotEmpty()) {
                        result += Definition(part, text, def.optString("example").takeIf { it.isNotBlank() })
                    }
                }
            }
        }
        return result.distinctBy { it.meaning }.take(6)
    }
}
