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
        publishedDate: String = ""
    ): Book
    suspend fun updateStatus(id: String, status: BookStatus)
    suspend fun finishBook(id: String, rating: Map<String, Float>)
    suspend fun markDnf(id: String, abandonedPercentage: Float, reason: String)
    suspend fun addProgress(id: String, delta: Int)
    suspend fun applyRemoteProgress(id: String, currentUnit: Int, updatedAt: Long)
    suspend fun deleteBook(id: String)
    suspend fun restore(book: Book)

    fun observeOpenSession(): Flow<Session?>
    fun observeCompletedSessions(): Flow<List<Session>>
    fun observeSessionsForBook(bookId: String): Flow<List<Session>>
    suspend fun startSession(bookId: String)
    suspend fun endSession(bookId: String, environmentTag: String = "")
    suspend fun importCsv(context: android.content.Context, uri: android.net.Uri): Int

    fun observeNotes(bookId: String): Flow<List<MarginNote>>
    suspend fun addNote(bookId: String, pageOrUnit: Int, text: String)
    suspend fun deleteNote(id: String)
}

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
        publishedDate: String
    ): Book {
        val book = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            authors = authors,
            coverUrl = coverUrl,
            totalUnits = totalUnits,
            status = BookStatus.BACKLOG.name,
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
        bookDao.deleteById(id)
    }

    // Re-inserts a removed book verbatim (id, status, progress, lastUpdated) so
    // an Undo restores it to its exact prior state and pipeline position.
    override suspend fun restore(book: Book) {
        bookDao.upsert(book.toEntity())
    }

    override fun observeOpenSession(): Flow<Session?> =
        sessionDao.observeOpenSession().map { it?.toModel() }

    override fun observeCompletedSessions(): Flow<List<Session>> =
        sessionDao.observeCompleted().map { entities -> entities.map { it.toModel() } }

    override fun observeSessionsForBook(bookId: String): Flow<List<Session>> =
        sessionDao.observeForBook(bookId).map { entities -> entities.map { it.toModel() } }

    override fun observeNotes(bookId: String): Flow<List<MarginNote>> =
        marginNoteDao.observeForBook(bookId).map { entities -> entities.map { it.toModel() } }

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

    override suspend fun deleteNote(id: String) {
        marginNoteDao.deleteById(id)
    }

    override suspend fun startSession(bookId: String) {
        val book = bookDao.getById(bookId) ?: return
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
                startUnit = book.currentUnit,
                endUnit = book.currentUnit,
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
        val endUnit = bookDao.getById(open.bookId)?.currentUnit ?: open.startUnit
        sessionDao.upsert(
            open.copy(
                endTime = System.currentTimeMillis(),
                endUnit = endUnit,
                unitsRead = (endUnit - open.startUnit).coerceAtLeast(0),
                environmentTag = tag.ifBlank { open.environmentTag }
            )
        )
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
