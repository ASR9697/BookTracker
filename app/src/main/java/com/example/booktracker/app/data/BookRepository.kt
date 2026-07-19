package com.example.booktracker.app.data

import com.example.booktracker.app.data.local.BookDao
import com.example.booktracker.app.data.local.MarginNoteDao
import com.example.booktracker.app.data.local.MarginNoteEntity
import com.example.booktracker.app.data.local.SessionDao
import com.example.booktracker.app.data.local.SessionEntity
import com.example.booktracker.app.data.local.toEntity
import com.example.booktracker.app.data.local.toModel
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.DnfData
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single data access boundary for the UI and sync layers. Phase B adds cloud
 * sync behind this same interface, so nothing above it may touch storage
 * directly. Every local mutation stamps lastUpdated (the LWW conflict key).
 */
interface BookRepository {
    fun observeBooks(): Flow<List<Book>>
    suspend fun addBook(
        title: String,
        authors: List<String>,
        totalUnits: Int,
        coverUrl: String = "",
        description: String = "",
        genres: List<String> = emptyList(),
        publishedDate: String = "",
        status: BookStatus = BookStatus.BACKLOG,
        currentUnit: Int = 0
    ): Book
    suspend fun updateStatus(id: String, status: BookStatus)
    suspend fun updateFormat(id: String, format: String)
    suspend fun finishBook(id: String, rating: Map<String, Float>)
    suspend fun markDnf(id: String, abandonedPercentage: Float, reason: String)
    suspend fun addProgress(id: String, delta: Int)
    suspend fun applyRemoteProgress(id: String, currentUnit: Int, updatedAt: Long)
    suspend fun deleteBook(id: String)
    suspend fun restore(book: Book)
    suspend fun toggleFavorite(id: String)

    fun observeOpenSession(): Flow<Session?>
    fun observeCompletedSessions(): Flow<List<Session>>
    fun observeSessionsForBook(bookId: String): Flow<List<Session>>
    suspend fun startSession(bookId: String, startPage: Int? = null)
    suspend fun endSession(bookId: String, environmentTag: String = "")
    suspend fun deleteSession(id: String)
    suspend fun updateSession(session: Session)
    suspend fun importCsv(context: android.content.Context, uri: android.net.Uri): Int

    fun observeNotes(bookId: String): Flow<List<MarginNote>>
    fun observeTotalNotesCount(): Flow<Int>
    suspend fun addNote(bookId: String, pageOrUnit: Int, text: String)
    suspend fun addRemoteNote(
        id: String,
        bookId: String,
        pageOrUnit: Int,
        text: String,
        timestamp: Long
    )
    suspend fun deleteNote(id: String)

    suspend fun searchLibrary(query: String): LibrarySearchResults
    suspend fun snapshotForBackup(): LibrarySnapshot
    suspend fun restoreFromBackup(snapshot: LibrarySnapshot)
}

/** A margin-note hit carries its book so the UI can label and link it. */
data class LibrarySearchResults(
    val books: List<Book> = emptyList(),
    val notes: List<Pair<MarginNote, Book?>> = emptyList()
) {
    val isEmpty: Boolean get() = books.isEmpty() && notes.isEmpty()
}

data class LibrarySnapshot(
    val books: List<Book> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val notes: List<MarginNote> = emptyList()
)

class RoomBookRepository(
    private val bookDao: BookDao,
    private val sessionDao: SessionDao,
    private val marginNoteDao: MarginNoteDao
) : BookRepository {

    override suspend fun importCsv(context: android.content.Context, uri: android.net.Uri): Int {
        return CsvImportEngine.importFromUri(context, uri, bookDao)
    }

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun addBook(
        title: String,
        authors: List<String>,
        totalUnits: Int,
        coverUrl: String,
        description: String,
        genres: List<String>,
        publishedDate: String,
        status: BookStatus,
        currentUnit: Int
    ): Book {
        val book = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            authors = authors,
            coverUrl = coverUrl,
            currentUnit = currentUnit,
            totalUnits = totalUnits,
            status = status.name,
            lastUpdated = System.currentTimeMillis(),
            description = description,
            genres = genres,
            publishedDate = publishedDate
        )
        bookDao.upsert(book.toEntity())
        return book
    }

    override suspend fun updateStatus(id: String, status: BookStatus) {
        val entity = bookDao.getById(id) ?: return
        bookDao.upsert(
            entity.copy(status = status.name, lastUpdated = System.currentTimeMillis())
        )
        // A book leaving READING (finished, DNF, demoted) closes its running session.
        if (status != BookStatus.READING) {
            sessionDao.getOpenSessionForBook(id)?.let { finalizeSession(it) }
        }
    }

    override suspend fun updateFormat(id: String, format: String) {
        val entity = bookDao.getById(id) ?: return
        bookDao.upsert(entity.copy(format = format, lastUpdated = System.currentTimeMillis()))
    }

    override suspend fun finishBook(id: String, rating: Map<String, Float>) {
        val book = bookDao.getById(id)?.toModel() ?: return
        bookDao.upsert(
            book.copy(
                status = BookStatus.FINISHED.name,
                rating = rating,
                lastUpdated = System.currentTimeMillis()
            ).toEntity()
        )
        sessionDao.getOpenSessionForBook(id)?.let { finalizeSession(it) }
    }

    override suspend fun markDnf(id: String, abandonedPercentage: Float, reason: String) {
        val book = bookDao.getById(id)?.toModel() ?: return
        bookDao.upsert(
            book.copy(
                status = BookStatus.DNF.name,
                dnfData = DnfData(abandonedPercentage, reason),
                lastUpdated = System.currentTimeMillis()
            ).toEntity()
        )
        sessionDao.getOpenSessionForBook(id)?.let { finalizeSession(it) }
    }

    override suspend fun addProgress(id: String, delta: Int) {
        val entity = bookDao.getById(id) ?: return
        val ceiling = if (entity.totalUnits > 0) entity.totalUnits else Int.MAX_VALUE
        val newUnit = (entity.currentUnit + delta).coerceIn(0, ceiling)
        val now = System.currentTimeMillis()
        bookDao.upsert(entity.copy(currentUnit = newUnit, lastUpdated = now))
        recordDeltaOutsideSession(id, entity.currentUnit, newUnit, now, source = "phone")
    }

    override suspend fun applyRemoteProgress(id: String, currentUnit: Int, updatedAt: Long) {
        val entity = bookDao.getById(id) ?: return
        // Last-Write-Wins: ignore stale updates from the watch.
        if (updatedAt <= entity.lastUpdated) return
        val ceiling = if (entity.totalUnits > 0) entity.totalUnits else Int.MAX_VALUE
        val newUnit = currentUnit.coerceIn(0, ceiling)
        bookDao.upsert(entity.copy(currentUnit = newUnit, lastUpdated = updatedAt))
        recordDeltaOutsideSession(id, entity.currentUnit, newUnit, updatedAt, source = "watch")
    }

    override suspend fun deleteBook(id: String) {
        sessionDao.deleteByBookId(id)
        marginNoteDao.deleteByBookId(id)
        bookDao.deleteById(id)
    }

    // Re-inserts a removed book verbatim (id, status, progress, lastUpdated) so
    // an Undo restores it to its exact prior state and pipeline position.
    override suspend fun restore(book: Book) {
        bookDao.upsert(book.toEntity())
    }

    override suspend fun toggleFavorite(id: String) {
        bookDao.getById(id)?.let {
            bookDao.upsert(it.copy(isFavorite = !it.isFavorite, lastUpdated = System.currentTimeMillis()))
        }
    }

    override fun observeOpenSession(): Flow<Session?> =
        sessionDao.observeOpenSession().map { it?.toModel() }

    override fun observeCompletedSessions(): Flow<List<Session>> =
        sessionDao.observeCompleted().map { entities -> entities.map { it.toModel() } }

    override fun observeSessionsForBook(bookId: String): Flow<List<Session>> =
        sessionDao.observeForBook(bookId).map { entities -> entities.map { it.toModel() } }

    override fun observeNotes(bookId: String): Flow<List<MarginNote>> =
        marginNoteDao.observeForBook(bookId).map { entities -> entities.map { it.toModel() } }

    override fun observeTotalNotesCount(): Flow<Int> =
        marginNoteDao.observeTotalNotesCount()

    override suspend fun addNote(bookId: String, pageOrUnit: Int, text: String) {
        marginNoteDao.upsert(
            MarginNoteEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                timestamp = System.currentTimeMillis(),
                pageOrUnit = pageOrUnit,
                markdownContent = text,
                isVoiceDictated = false
            )
        )
    }

    // A note dictated on the watch arrives with its own client-generated id, so
    // upsert makes re-delivery (Data Layer replays items on reconnect) idempotent.
    override suspend fun addRemoteNote(
        id: String,
        bookId: String,
        pageOrUnit: Int,
        text: String,
        timestamp: Long
    ) {
        marginNoteDao.upsert(
            MarginNoteEntity(
                id = id,
                bookId = bookId,
                timestamp = timestamp,
                pageOrUnit = pageOrUnit,
                markdownContent = text,
                isVoiceDictated = true
            )
        )
    }

    override suspend fun deleteNote(id: String) {
        marginNoteDao.deleteById(id)
    }

    override suspend fun searchLibrary(query: String): LibrarySearchResults {
        val ftsQuery = toFtsQuery(query) ?: return LibrarySearchResults()
        val books = bookDao.search(ftsQuery).map { it.toModel() }
        val bookCache = books.associateBy { it.id }.toMutableMap()
        
        val marginNotesEntities = marginNoteDao.search(ftsQuery)
        val missingBookIds = marginNotesEntities.map { it.bookId }.distinct().filter { !bookCache.containsKey(it) }
        if (missingBookIds.isNotEmpty()) {
            val missingBooks = bookDao.getByIds(missingBookIds).map { it.toModel() }
            missingBooks.forEach { bookCache[it.id] = it }
        }

        val notes = marginNotesEntities.map { note ->
            val book = bookCache[note.bookId]
            note.toModel() to book
        }
        return LibrarySearchResults(books, notes)
    }

    // Turns raw user input into an FTS4 prefix query ("dune her" -> "dune* her*"),
    // stripping quotes and operators so nothing the user types is a syntax error.
    private fun toFtsQuery(raw: String): String? {
        val tokens = raw.split(Regex("\\s+"))
            .map { it.replace(Regex("[\"'*^()-]"), "") }
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }

    override suspend fun snapshotForBackup(): LibrarySnapshot = LibrarySnapshot(
        books = bookDao.getAll().map { it.toModel() },
        sessions = sessionDao.getAll().map { it.toModel() },
        notes = marginNoteDao.getAll().map { it.toModel() }
    )

    override suspend fun restoreFromBackup(snapshot: LibrarySnapshot) {
        val existingBooks = bookDao.getAll().associateBy { it.id }
        val booksToUpsert = snapshot.books.filter { backupBook ->
            val existing = existingBooks[backupBook.id]
            existing == null || backupBook.lastUpdated > existing.lastUpdated
        }.map { it.toEntity() }

        if (booksToUpsert.isNotEmpty()) {
            bookDao.upsert(booksToUpsert)
        }
        
        if (snapshot.sessions.isNotEmpty()) {
            val existingSessions = sessionDao.getAll().associateBy { it.id }
            val sessionsToUpsert = snapshot.sessions.filter { backupSession ->
                val existing = existingSessions[backupSession.id]
                existing == null || (backupSession.endTime != 0L && existing.endTime == 0L) || backupSession.endTime > existing.endTime
            }.map { it.toEntity() }
            if (sessionsToUpsert.isNotEmpty()) {
                sessionDao.upsert(sessionsToUpsert)
            }
        }
        
        if (snapshot.notes.isNotEmpty()) {
            val existingNotes = marginNoteDao.getAll().associateBy { it.id }
            val notesToUpsert = snapshot.notes.filter { backupNote ->
                val existing = existingNotes[backupNote.id]
                existing == null || backupNote.timestamp > existing.timestamp
            }.map { it.toEntity() }
            if (notesToUpsert.isNotEmpty()) {
                marginNoteDao.upsert(notesToUpsert)
            }
        }
    }

    override suspend fun startSession(bookId: String, startPage: Int?) {
        val book = bookDao.getById(bookId) ?: return
        val finalStartUnit = startPage ?: book.currentUnit

        if (startPage != null && startPage != book.currentUnit) {
            bookDao.upsert(book.copy(currentUnit = startPage, lastUpdated = System.currentTimeMillis()))
        }

        sessionDao.getOpenSession()?.let { open ->
            if (open.bookId == bookId) return // already running for this book
            finalizeSession(open) // switching books auto-closes the other session
        }
        sessionDao.insert(
            SessionEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                startTime = System.currentTimeMillis(),
                endTime = 0L,
                startUnit = finalStartUnit,
                endUnit = finalStartUnit,
                unitsRead = 0,
                deviceSource = "phone",
                environmentTag = "",
                isInterrupted = false
            )
        )
    }

    override suspend fun endSession(bookId: String, environmentTag: String) {
        sessionDao.getOpenSessionForBook(bookId)?.let { finalizeSession(it, environmentTag) }
    }

    private suspend fun finalizeSession(open: SessionEntity, tag: String = "") {
        val book = bookDao.getById(open.bookId)
        val endUnit = book?.currentUnit ?: open.startUnit
        sessionDao.upsert(
            open.copy(
                endTime = System.currentTimeMillis(),
                endUnit = endUnit,
                unitsRead = (endUnit - open.startUnit).coerceAtLeast(0),
                environmentTag = tag.ifBlank { open.environmentTag }
            )
        )
    }

    override suspend fun deleteSession(id: String) {
        sessionDao.deleteById(id)
    }

    override suspend fun updateSession(session: Session) {
        sessionDao.upsert(session.toEntity())
    }

    /**
     * Progress made while no session is running still needs to count toward
     * streaks and analytics, so each delta becomes a self-contained session.
     * While a session IS open, the delta is skipped here — finalizeSession
     * captures it via endUnit - startUnit, which also covers watch taps made
     * during a phone session.
     */
    private suspend fun recordDeltaOutsideSession(
        bookId: String,
        oldUnit: Int,
        newUnit: Int,
        at: Long,
        source: String
    ) {
        val gained = newUnit - oldUnit
        if (gained <= 0) return
        if (sessionDao.getOpenSessionForBook(bookId) != null) return
        sessionDao.insert(
            SessionEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                startTime = at,
                endTime = at,
                startUnit = oldUnit,
                endUnit = newUnit,
                unitsRead = gained,
                deviceSource = source,
                environmentTag = "",
                isInterrupted = false
            )
        )
    }
}
