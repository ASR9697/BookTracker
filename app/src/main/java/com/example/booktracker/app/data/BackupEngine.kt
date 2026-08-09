package com.example.booktracker.app.data

import android.content.Context
import android.net.Uri
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.DnfData
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.booktracker.shared.models.*

/**
 * User-managed local backups: the whole Room DB plus settings as one
 * versioned JSON document inside a .zip, written wherever the user points the
 * system file picker. No cloud, no accounts — restoring on a fresh install (or
 * another device) upserts by id, so it merges rather than duplicates.
 */
object BackupEngine {

    private const val FORMAT_VERSION = 1
    private const val ENTRY_NAME = "booktracker-backup.json"

    data class Parsed(
        val snapshot: LibrarySnapshot,
        val dailyGoal: Int?,
        val yearlyGoal: Int?
    )

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        snapshot: LibrarySnapshot,
        dailyGoal: Int,
        yearlyGoal: Int
    ) = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("version", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("dailyGoalPages", dailyGoal)
                put("yearlyGoalBooks", yearlyGoal)
            })
            put("books", JSONArray(snapshot.books.map { it.toJson() }))
            put("sessions", JSONArray(snapshot.sessions.map { it.toJson() }))
            put("notes", JSONArray(snapshot.notes.map { it.toJson() }))
        }
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot open backup destination")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_NAME))
            zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri): Parsed =
        withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot open backup file")
            parse(readJsonText(bytes))
        }

    // Accepts either the .zip we write or a bare .json the user extracted.
    private fun readJsonText(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".json")) {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        return bytes.toString(Charsets.UTF_8)
    }

    private fun parse(text: String): Parsed {
        val root = JSONObject(text)
        val settings = root.optJSONObject("settings")
        return Parsed(
            snapshot = LibrarySnapshot(
                books = root.optJSONArray("books").mapObjects { it.toBook() },
                sessions = root.optJSONArray("sessions").mapObjects { it.toSession() },
                notes = root.optJSONArray("notes").mapObjects { it.toNote() }
            ),
            dailyGoal = settings?.optIntOrNull("dailyGoalPages"),
            yearlyGoal = settings?.optIntOrNull("yearlyGoalBooks")
        )
    }

    private fun Book.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("authors", JSONArray(authors))
        put("coverUrl", coverUrl)
        put("format", format)
        put("totalPages", totalPages)
        put("currentPage", currentPage)
        put("status", status)
        dnfData?.let {
            put("dnfPercentage", it.abandonedPercentage.toDouble())
            put("dnfReason", it.reason)
        }
        put("lastUpdated", lastUpdated)
        put("rating", JSONObject(rating.mapValues { it.value.toDouble() }))
        put("description", description)
        put("genres", JSONArray(genres))
        put("collections", JSONArray(classification.collections))
        put("publishedDate", publication?.date)
        put("isFavorite", isFavorite)
        put("readCount", readCount)
        plannedDate?.let { put("plannedDate", it) }
        review?.let { put("review", it) }
    }

    private fun JSONObject.toBook(): Book = Book(
        id = getString("id"),
        title = optString("title"),
        creators = optJSONArray("authors").mapStrings().map { Creator(CreatorRole.Author, it) },
        coverUrl = optString("coverUrl"),
        format = runCatching { BookFormat.valueOf(optString("format")) }.getOrDefault(BookFormat.Paperback),
        totalPages = optInt("totalPages"),
        currentPage = optInt("currentPage"),
        status = optString("status"),
        dnfData = if (has("dnfPercentage")) {
            DnfData(getDouble("dnfPercentage").toFloat(), optString("dnfReason"))
        } else null,
        lastUpdated = optLong("lastUpdated"),
        rating = optJSONObject("rating")?.let { obj ->
            obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
        } ?: emptyMap(),
        description = optString("description"),
        classification = Classification(
            optJSONArray("collections").mapStrings(),
            optJSONArray("genres").mapStrings()
        ),
        publication = Publication("", optString("publishedDate")),
        isFavorite = optBoolean("isFavorite", false),
        readCount = optInt("readCount", 0),
        isbn = null,
        plannedDate = if (has("plannedDate")) optLong("plannedDate") else null,
        review = if (has("review")) optString("review") else null
    )

    private fun Session.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("bookId", bookId)
        put("startTime", startTime)
        put("endTime", endTime)
        put("startPage", startPage)
        put("endPage", endPage)
        put("pagesRead", pagesRead)
        put("durationSeconds", durationSeconds)
        put("environmentTag", environmentTag)
    }

    private fun JSONObject.toSession(): Session = Session(
        id = getString("id"),
        bookId = optString("bookId"),
        startTime = optLong("startTime"),
        endTime = optLong("endTime"),
        startPage = optInt("startPage"),
        endPage = optInt("endPage"),
        pagesRead = optInt("pagesRead"),
        durationSeconds = optInt("durationSeconds"),
        environmentTag = optString("environmentTag")
    )

    private fun MarginNote.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("bookId", bookId)
        put("timestamp", timestamp)
        put("pageNumber", pageNumber)
        put("content", content)
        put("isFavorite", isFavorite)
    }

    private fun JSONObject.toNote(): MarginNote = MarginNote(
        id = getString("id"),
        bookId = optString("bookId"),
        timestamp = optLong("timestamp"),
        pageNumber = optInt("pageNumber"),
        content = optString("content"),
        isFavorite = optBoolean("isFavorite")
    )

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { transform(getJSONObject(it)) }
    }

    private fun JSONArray?.mapStrings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key)) getInt(key) else null
}
