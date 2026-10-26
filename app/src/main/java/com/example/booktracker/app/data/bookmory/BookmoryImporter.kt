package com.example.booktracker.app.data.bookmory

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.booktracker.app.data.LibrarySnapshot
import com.example.booktracker.app.data.NoteImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Reads a Bookmory backup (.bookmory zip) chosen by the reader: copies the note
 * photos into [NoteImageStore], reads the sembast entries from the bundled
 * SQLite file and hands them to [BookmoryMapper]. Nothing is written to the
 * library here; the caller restores the returned snapshot.
 */
object BookmoryImporter {

    private const val DB_ENTRY = "new_bookmory.db"
    private const val NOTE_IMAGES = "images/notes/"
    /** Stored photo names keep this prefix so a re-import overwrites instead of duplicating. */
    private const val PHOTO_PREFIX = "bm_"

    class NotABookmoryBackup : Exception("This file isn't a Bookmory backup.")

    suspend fun read(
        context: Context,
        uri: Uri,
        existing: LibrarySnapshot
    ): BookmoryImport = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "bookmory_import").apply { deleteRecursively(); mkdirs() }
        try {
            val dbFile = File(workDir, DB_ENTRY)
            val photos = mutableMapOf<String, String>()
            val photoDir = NoteImageStore.dir(context)

            val input = context.contentResolver.openInputStream(uri) ?: throw NotABookmoryBackup()
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    // Only base names are trusted, so a crafted entry can't escape our folders.
                    val base = File(name).name
                    when {
                        entry.isDirectory || base.isBlank() -> Unit
                        name == DB_ENTRY -> dbFile.outputStream().use { zip.copyTo(it) }
                        name.startsWith(NOTE_IMAGES) -> {
                            val target = File(photoDir, PHOTO_PREFIX + base)
                            target.outputStream().use { zip.copyTo(it) }
                            photos[base] = "file://${target.absolutePath}"
                        }
                    }
                    entry = zip.nextEntry
                }
            }
            if (!dbFile.isFile) throw NotABookmoryBackup()

            val records = BookmoryRecords.fromEntries(readEntries(dbFile))
            BookmoryMapper.map(
                records = records,
                existing = existing.books,
                existingSessionIds = existing.sessions.mapTo(HashSet()) { it.id },
                existingNoteIds = existing.notes.mapTo(HashSet()) { it.id }
            ) { path -> photos[File(path).name] }
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun readEntries(dbFile: File): List<BookmoryEntry> {
        val db = runCatching {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        }.getOrElse { throw NotABookmoryBackup() }
        return db.use {
            runCatching {
                it.rawQuery("SELECT store, key, value, deleted FROM entry", null).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            // Keys are text in most stores and integers in tags/authors.
                            val key = cursor.getString(1) ?: continue
                            add(
                                BookmoryEntry(
                                    store = cursor.getString(0),
                                    key = key,
                                    value = cursor.getString(2),
                                    deleted = !cursor.isNull(3) && cursor.getInt(3) != 0
                                )
                            )
                        }
                    }
                }
            }.getOrElse { throw NotABookmoryBackup() }
        }
    }
}
