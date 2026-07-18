package com.example.booktracker.app.data

import android.content.Context
import android.net.Uri
import com.example.booktracker.app.data.local.BookDao
import com.example.booktracker.app.data.local.BookEntity
import com.example.booktracker.shared.models.BookStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

object CsvImportEngine {

    suspend fun importFromUri(context: Context, uri: Uri, bookDao: BookDao): Int = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return@withContext 0
        
        var importedCount = 0
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            val headerLine = reader.readLine() ?: return@use
            val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }
            
            val titleIdx = headers.indexOf("title")
            val authorIdx = headers.indexOf("author")
            val isbnIdx = headers.indexOf("isbn")
            val pagesIdx = headers.indexOf("number of pages")
            val shelvesIdx = headers.indexOf("bookshelves")
            val exclusiveShelfIdx = headers.indexOf("exclusive shelf")
            
            if (titleIdx == -1) return@use // Title is mandatory

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isBlank()) continue
                val row = parseCsvLine(line!!)
                
                val title = row.getOrNull(titleIdx)?.trim() ?: continue
                if (title.isBlank()) continue
                
                val author = row.getOrNull(authorIdx)?.trim().orEmpty()
                val isbn = row.getOrNull(isbnIdx)?.replace("=", "")?.replace("\"", "")?.trim().orEmpty()
                val pagesStr = row.getOrNull(pagesIdx)?.trim().orEmpty()
                val pages = pagesStr.toIntOrNull() ?: 0
                val shelves = row.getOrNull(shelvesIdx)?.trim()?.lowercase().orEmpty()
                val exclusiveShelf = row.getOrNull(exclusiveShelfIdx)?.trim()?.lowercase().orEmpty()
                
                val combinedShelves = "$shelves $exclusiveShelf"
                
                val status = when {
                    combinedShelves.contains("read") && !combinedShelves.contains("currently-reading") && !combinedShelves.contains("to-read") -> BookStatus.FINISHED.name
                    combinedShelves.contains("currently-reading") -> BookStatus.READING.name
                    combinedShelves.contains("to-read") -> BookStatus.BACKLOG.name
                    else -> BookStatus.FINISHED.name // Default for old books typically
                }

                val authorsList = if (author.isNotBlank()) listOf(author) else emptyList()
                val authorsJson = org.json.JSONArray(authorsList).toString()

                val entity = BookEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    authors = authorsJson,
                    coverUrl = "", // No cover from CSV
                    format = "",
                    totalUnits = pages,
                    currentUnit = if (status == BookStatus.FINISHED.name) pages else 0,
                    status = status,
                    dnfPercentage = null,
                    dnfReason = null,
                    lastUpdated = System.currentTimeMillis(),
                    rating = "{}",
                    description = "",
                    genres = "[]",
                    publishedDate = ""
                )
                
                bookDao.upsert(entity)
                importedCount++
            }
        }
        importedCount
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            if (inQuotes) {
                if (char == '\"') inQuotes = false
                else current.append(char)
            } else {
                if (char == '\"') inQuotes = true
                else if (char == ',') {
                    result.add(current.toString())
                    current.clear()
                } else {
                    current.append(char)
                }
            }
        }
        result.add(current.toString())
        return result
    }
}
