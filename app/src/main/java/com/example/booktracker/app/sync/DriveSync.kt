package com.example.booktracker.app.sync

import android.content.Context
import android.net.Uri
import com.example.booktracker.app.data.BackupEngine
import com.example.booktracker.app.data.LibrarySnapshot
import com.example.booktracker.app.data.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * One round of Drive sync: pull the library document from the app's Drive
 * folder, merge it with this phone ([SyncMerge]) against the copy both last
 * agreed on, push the result, then write the difference into the local
 * database. Cover and note photos travel as separate files named by their
 * backup entry ("note_images/<name>"); their names are unique, so a file is
 * uploaded or downloaded once and never compared.
 *
 * The open reading session (a running timer) never syncs.
 */
object DriveSync {

    private const val DOCUMENT = "library.json"
    private const val MAX_ATTEMPTS = 3
    private val mutex = Mutex()

    data class Summary(val changedHere: Int, val uploadedFiles: Int, val downloadedFiles: Int)

    suspend fun run(context: Context, token: String): Summary = mutex.withLock {
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val drive = DriveClient(token)
            repeat(MAX_ATTEMPTS) {
                attempt(app, drive)?.let { return@withContext it }
            }
            throw IOException("Another device kept syncing at the same time. Try again in a moment.")
        }
    }

    /** Forgets the agreed copy, so the next sync combines both sides instead of replaying deletions. */
    fun reset(context: Context) {
        baseFile(context).delete()
    }

    /** Null when Drive changed underneath this attempt and it should start over. */
    private suspend fun attempt(app: Context, drive: DriveClient): Summary? {
        val repository = ServiceLocator.repository(app)
        val resolve = { entry: String -> BackupEngine.localUriForEntry(app, entry) }
        val noSettings = BackupEngine.BackupSettings()

        val files = drive.list()
        val remoteDoc = files[DOCUMENT]
        val remote = remoteDoc?.let { BackupEngine.decodeDocument(drive.download(it.id).toString(Charsets.UTF_8), resolve).snapshot }
            ?: LibrarySnapshot()
        // No document in Drive (first sync, or app data cleared there): never treat that
        // as "everything was deleted", start from both sides instead.
        val base = if (remoteDoc == null) null else baseFile(app).takeIf { it.isFile }?.let {
            BackupEngine.decodeDocument(it.readText(), resolve).snapshot
        }
        val raw = repository.snapshotForBackup().let { s -> s.copy(sessions = s.sessions.filter { it.endTime != 0L }) }
        // Through the same JSON as the other two, so only real edits compare unequal.
        val local = BackupEngine.decodeDocument(BackupEngine.encodeDocument(raw, noSettings).toString(), resolve).snapshot
        val merged = SyncMerge.merge(base, local, remote)

        // Photos the merged library points at: fetch what this phone lacks, send what Drive lacks.
        val referenced = (merged.books.map { it.coverUrl } + merged.notes.flatMap { it.imageUris })
            .mapNotNull { uri -> BackupEngine.entryForLocalUri(uri)?.let { it to uri } }
            .toMap()
        var downloaded = 0
        var uploaded = 0
        for ((entry, uri) in referenced) {
            val file = File(Uri.parse(uri).path ?: continue)
            val remoteFile = files[entry]
            when {
                !file.isFile && remoteFile != null -> {
                    file.parentFile?.mkdirs()
                    file.writeBytes(drive.download(remoteFile.id))
                    downloaded++
                }
                file.isFile && remoteFile == null -> {
                    drive.create(entry, "image/jpeg", file.readBytes())
                    uploaded++
                }
            }
        }

        val mergedText = BackupEngine.encodeDocument(merged, noSettings).toString()
        if (remoteDoc == null) {
            drive.create(DOCUMENT, "application/json", mergedText.toByteArray())
        } else {
            if (drive.version(remoteDoc.id) != remoteDoc.version) return null
            drive.update(remoteDoc.id, "application/json", mergedText.toByteArray())
        }

        val changes = SyncMerge.changes(local, merged)
        if (!changes.isEmpty) repository.applySync(changes)
        baseFile(app).apply { parentFile?.mkdirs() }.writeText(mergedText)

        val changedHere = changes.snapshot.let { it.books.size + it.sessions.size + it.notes.size + it.words.size + it.reviews.size } +
            changes.deletedBooks.size + changes.deletedSessions.size + changes.deletedNotes.size +
            changes.deletedWords.size + changes.deletedReviews.size
        return Summary(changedHere, uploaded, downloaded)
    }

    private fun baseFile(context: Context) = File(File(context.filesDir, "sync"), "base.json")
}
