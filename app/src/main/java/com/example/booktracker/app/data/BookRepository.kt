package com.example.booktracker.app.data

import com.example.booktracker.app.data.local.BookDao
import com.example.booktracker.app.data.local.MarginNoteDao
import com.example.booktracker.app.data.local.MarginNoteEntity
import com.example.booktracker.app.data.local.ReviewDao
import com.example.booktracker.app.data.local.SessionDao
import com.example.booktracker.app.data.local.SessionEntity
import com.example.booktracker.app.data.local.VocabDao
import com.example.booktracker.app.data.local.toEntity
import com.example.booktracker.app.data.local.toModel
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.DnfData
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.ReviewState
import com.example.booktracker.shared.models.Session
import com.example.booktracker.shared.models.VocabWord
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
        totalPages: Int,
        coverUrl: String = "",
        description: String = "",
        genres: List<String> = emptyList(),
        publishedDate: String = "",
        status: BookStatus = BookStatus.SHORTLIST,
        currentPage: Int = 0,
        format: com.example.booktracker.shared.models.BookFormat = com.example.booktracker.shared.models.BookFormat.Paperback,
        creators: List<com.example.booktracker.shared.models.Creator> = emptyList(),
        purchaseLog: List<com.example.booktracker.shared.models.PurchaseLog> = emptyList(),
        loanRecord: List<com.example.booktracker.shared.models.LoanRecord> = emptyList(),
        isbn: String? = null,
        publisher: String = ""
    ): Book
    suspend fun updateBook(book: Book)
    suspend fun updateStatus(id: String, status: BookStatus)
    suspend fun updateFormat(id: String, format: String)
    suspend fun updateOwnership(id: String, ownership: com.example.booktracker.shared.models.Ownership)
    suspend fun finishBook(id: String, rating: Map<String, Float>, review: String? = null)
    suspend fun markDnf(id: String, abandonedPercentage: Float, reason: String)
    suspend fun addProgress(id: String, delta: Int)
    suspend fun applyRemoteProgress(id: String, currentPage: Int, updatedAt: Long)
    /** Deletes the book with its sessions and notes, returning them so an Undo can put everything back. */
    suspend fun deleteBook(id: String): DeletedBook?
    suspend fun restoreDeleted(deleted: DeletedBook)
    suspend fun toggleFavorite(id: String)

    fun observeOpenSession(): Flow<Session?>
    fun observeCompletedSessions(): Flow<List<Session>>
    fun observeSessionsForBook(bookId: String): Flow<List<Session>>
    suspend fun startSession(bookId: String, startPage: Int? = null)
    suspend fun endSession(bookId: String, environmentTag: String = "")
    suspend fun endSessionDetailed(
        bookId: String,
        endPage: Int,
        environmentTag: String,
        startTime: Long,
        endTime: Long
    ): Session?
    suspend fun addManualSession(
        bookId: String,
        startPage: Int,
        endPage: Int,
        durationMillis: Long,
        endTime: Long = System.currentTimeMillis()
    )
    suspend fun deleteSession(id: String)
    suspend fun updateSession(session: Session)
    suspend fun importCsv(context: android.content.Context, uri: android.net.Uri): CsvImportResult

    fun observeNotes(bookId: String): Flow<List<MarginNote>>
    fun observeAllNotes(): Flow<List<MarginNote>>
    fun observeTotalNotesCount(): Flow<Int>
    suspend fun addNote(
        bookId: String,
        page: Int,
        text: String,
        type: com.example.booktracker.shared.models.NoteType =
            com.example.booktracker.shared.models.NoteType.BOOK_CONTENT,
        images: List<String> = emptyList()
    )
    suspend fun updateCollections(bookId: String, collections: List<String>)
    suspend fun setProgress(bookId: String, page: Int)
    suspend fun restartReading(bookId: String)
    suspend fun toggleNoteFavorite(id: String)
    suspend fun addRemoteNote(
        id: String,
        bookId: String,
        page: Int,
        text: String,
        timestamp: Long
    )
    suspend fun deleteNote(id: String)
    suspend fun updateNote(
        id: String,
        page: Int,
        text: String,
        type: com.example.booktracker.shared.models.NoteType,
        images: List<String>
    )

    suspend fun searchLibrary(query: String): LibrarySearchResults
    suspend fun snapshotForBackup(): LibrarySnapshot
    suspend fun restoreFromBackup(snapshot: LibrarySnapshot)
    /** Writes sessions as given, replacing any with the same id. */
    suspend fun upsertSessions(sessions: List<Session>)

    fun observeReviewStates(): Flow<List<ReviewState>>
    suspend fun saveReviewState(state: ReviewState)
    fun observeWords(): Flow<List<VocabWord>>
    suspend fun saveWord(word: VocabWord)
    suspend fun deleteWord(id: String)
    /** Sets or clears the day the reader wants to be done with the book by. */
    suspend fun setFinishBy(bookId: String, finishBy: Long?)
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
    val notes: List<MarginNote> = emptyList(),
    val words: List<VocabWord> = emptyList(),
    val reviews: List<ReviewState> = emptyList()
)

/** Everything a book delete removes, held so the Undo snackbar can restore it whole. */
data class DeletedBook(
    val book: Book,
    val sessions: List<Session>,
    val notes: List<MarginNote>
)

class RoomBookRepository(
    private val bookDao: BookDao,
    private val sessionDao: SessionDao,
    private val marginNoteDao: MarginNoteDao,
    private val reviewDao: ReviewDao,
    private val vocabDao: VocabDao
) : BookRepository {

    override suspend fun importCsv(context: android.content.Context, uri: android.net.Uri): CsvImportResult {
        return CsvImportEngine.importFromUri(context, uri, bookDao)
    }

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun addBook(
        title: String,
        authors: List<String>,
        totalPages: Int,
        coverUrl: String,
        description: String,
        genres: List<String>,
        publishedDate: String,
        status: BookStatus,
        currentPage: Int,
        format: com.example.booktracker.shared.models.BookFormat,
        creators: List<com.example.booktracker.shared.models.Creator>,
        purchaseLog: List<com.example.booktracker.shared.models.PurchaseLog>,
        loanRecord: List<com.example.booktracker.shared.models.LoanRecord>,
        isbn: String?,
        publisher: String
    ): Book {
        val now = System.currentTimeMillis()
        val ceiling = if (totalPages > 0) totalPages else Int.MAX_VALUE
        val startPage = currentPage.coerceIn(0, ceiling)
        val finished = status == BookStatus.FINISHED
        val book = Book(
            dateAdded = now,
            id = UUID.randomUUID().toString(),
            title = title,
            creators = if (creators.isNotEmpty()) creators else authors.map { com.example.booktracker.shared.models.Creator(com.example.booktracker.shared.models.CreatorRole.Author, it) },
            coverUrl = coverUrl,
            // A book added as already read starts at its last page.
            currentPage = if (finished && totalPages > 0) totalPages else startPage,
            totalPages = totalPages,
            status = status.name,
            lastUpdated = now,
            description = description,
            classification = com.example.booktracker.shared.models.Classification(emptyList(), genres),
            publication = com.example.booktracker.shared.models.Publication(publisher, publishedDate),
            format = format,
            progressUnit = com.example.booktracker.shared.models.ProgressUnit.Page,
            purchaseLog = purchaseLog,
            loanRecord = loanRecord,
            isbn = isbn?.takeIf { it.isNotBlank() && !it.startsWith("manual_") },
            finishedAt = if (finished) now else null,
            // Logging a purchase while adding the book says the reader owns it.
            ownership = if (purchaseLog.isNotEmpty()) com.example.booktracker.shared.models.Ownership.OWNED
            else com.example.booktracker.shared.models.Ownership.NONE
        )
        bookDao.upsert(book.toEntity())
        return book
    }

    override suspend fun updateBook(book: Book) {
        bookDao.upsert(book.toEntity())
    }

    override suspend fun updateStatus(id: String, status: BookStatus) {
        val entity = bookDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        val ended = status == BookStatus.FINISHED || status == BookStatus.DNF
        bookDao.upsert(
            entity.copy(
                status = status.name,
                lastUpdated = now,
                // Keep an existing finish date when re-saving an ended book; clear it
                // when the book goes back into the reading pipeline.
                finishedAt = if (ended) entity.finishedAt ?: now else null,
                currentPage = if (status == BookStatus.FINISHED && entity.totalPages > 0) {
                    entity.totalPages
                } else {
                    entity.currentPage
                }
            )
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

    override suspend fun updateOwnership(id: String, ownership: com.example.booktracker.shared.models.Ownership) {
        val entity = bookDao.getById(id) ?: return
        bookDao.upsert(entity.copy(ownership = ownership.name, lastUpdated = System.currentTimeMillis()))
    }

    override suspend fun finishBook(id: String, rating: Map<String, Float>, review: String?) {
        val book = bookDao.getById(id)?.toModel() ?: return
        val now = System.currentTimeMillis()
        // Close any running session first so it records the pages actually read,
        // before the bookmark jumps to the last page below.
        sessionDao.getOpenSessionForBook(id)?.let { finalizeSession(it) }
        bookDao.upsert(
            book.copy(
                status = BookStatus.FINISHED.name,
                rating = rating,
                review = review?.takeIf { it.isNotBlank() },
                currentPage = if (book.totalPages > 0) book.totalPages else book.currentPage,
                finishedAt = book.finishedAt.takeIf { book.status == BookStatus.FINISHED.name } ?: now,
                lastUpdated = now
            ).toEntity()
        )
    }

    override suspend fun markDnf(id: String, abandonedPercentage: Float, reason: String) {
        val book = bookDao.getById(id)?.toModel() ?: return
        val now = System.currentTimeMillis()
        sessionDao.getOpenSessionForBook(id)?.let { finalizeSession(it) }
        bookDao.upsert(
            book.copy(
                status = BookStatus.DNF.name,
                dnfData = DnfData(abandonedPercentage, reason),
                finishedAt = now,
                lastUpdated = now
            ).toEntity()
        )
    }

    override suspend fun addProgress(id: String, delta: Int) {
        val entity = bookDao.getById(id) ?: return
        val ceiling = if (entity.totalPages > 0) entity.totalPages else Int.MAX_VALUE
        val newUnit = (entity.currentPage + delta).coerceIn(0, ceiling)
        val now = System.currentTimeMillis()
        bookDao.upsert(entity.copy(currentPage = newUnit, lastUpdated = now))
        recordDeltaOutsideSession(id, entity.currentPage, newUnit, now, source = "phone")
    }

    override suspend fun applyRemoteProgress(id: String, currentPage: Int, updatedAt: Long) {
        val entity = bookDao.getById(id) ?: return
        // Last-Write-Wins: ignore stale updates from the watch.
        if (updatedAt <= entity.lastUpdated) return
        val ceiling = if (entity.totalPages > 0) entity.totalPages else Int.MAX_VALUE
        val newUnit = currentPage.coerceIn(0, ceiling)
        bookDao.upsert(entity.copy(currentPage = newUnit, lastUpdated = updatedAt))
        recordDeltaOutsideSession(id, entity.currentPage, newUnit, updatedAt, source = "watch")
    }

    override suspend fun deleteBook(id: String): DeletedBook? {
        val book = bookDao.getById(id) ?: return null
        val deleted = DeletedBook(
            book = book.toModel(),
            sessions = sessionDao.getForBook(id).map { it.toModel() },
            notes = marginNoteDao.getForBook(id).map { it.toModel() }
        )
        sessionDao.deleteByBookId(id)
        marginNoteDao.deleteByBookId(id)
        bookDao.deleteById(id)
        return deleted
    }

    // Re-inserts a removed book verbatim (id, status, progress, lastUpdated) and
    // then its children, so an Undo restores reading history and notes too.
    override suspend fun restoreDeleted(deleted: DeletedBook) {
        bookDao.upsert(deleted.book.toEntity())
        if (deleted.sessions.isNotEmpty()) sessionDao.upsert(deleted.sessions.map { it.toEntity() })
        if (deleted.notes.isNotEmpty()) marginNoteDao.upsert(deleted.notes.map { it.toEntity() })
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

    override fun observeAllNotes(): Flow<List<MarginNote>> =
        marginNoteDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override fun observeTotalNotesCount(): Flow<Int> =
        marginNoteDao.observeTotalNotesCount()

    override suspend fun addNote(
        bookId: String,
        page: Int,
        text: String,
        type: com.example.booktracker.shared.models.NoteType,
        images: List<String>
    ) {
        marginNoteDao.upsert(
            MarginNote(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                timestamp = System.currentTimeMillis(),
                pageNumber = page,
                content = text,
                type = type,
                imageUris = images
            ).toEntity()
        )
    }

    override suspend fun updateCollections(bookId: String, collections: List<String>) {
        val book = bookDao.getById(bookId)?.toModel() ?: return
        bookDao.upsert(
            book.copy(
                classification = book.classification.copy(collections = collections),
                lastUpdated = System.currentTimeMillis()
            ).toEntity()
        )
    }

    /** Jumps progress to an absolute page, routed through the same delta bookkeeping. */
    override suspend fun setProgress(bookId: String, page: Int) {
        val entity = bookDao.getById(bookId) ?: return
        addProgress(bookId, page - entity.currentPage)
    }

    /**
     * Starts the book over. Past sessions are deliberately kept — they are a real
     * record of reading that happened — so only the live position resets, and the
     * pass counter goes up to distinguish this read from the last one.
     */
    override suspend fun restartReading(bookId: String) {
        val entity = bookDao.getById(bookId) ?: return
        val now = System.currentTimeMillis()
        bookDao.upsert(
            entity.copy(
                status = BookStatus.READING.name,
                currentPage = 0,
                readCount = entity.readCount + 1,
                finishedAt = null,
                dnfData = null,
                // A deadline belonged to the last read; the start lets old notes resurface.
                finishBy = null,
                readStartedAt = now,
                lastUpdated = now
            )
        )
    }

    // A note dictated on the watch arrives with its own client-generated id, so
    // upsert makes re-delivery (Data Layer replays items on reconnect) idempotent.
    override suspend fun addRemoteNote(
        id: String,
        bookId: String,
        page: Int,
        text: String,
        timestamp: Long
    ) {
        marginNoteDao.upsert(
            MarginNoteEntity(
                id = id,
                bookId = bookId,
                timestamp = timestamp,
                pageNumber = page,
                content = text,
                type = com.example.booktracker.shared.models.NoteType.BOOK_CONTENT.name,
                isFavorite = false
            )
        )
    }

    override suspend fun toggleNoteFavorite(id: String) {
        val existing = marginNoteDao.getById(id) ?: return
        marginNoteDao.updateFavorite(id, !existing.isFavorite)
    }

    override suspend fun deleteNote(id: String) {
        marginNoteDao.deleteById(id)
        reviewDao.deleteById(id)
    }

    override suspend fun updateNote(
        id: String,
        page: Int,
        text: String,
        type: com.example.booktracker.shared.models.NoteType,
        images: List<String>
    ) {
        val existing = marginNoteDao.getById(id)?.toModel() ?: return
        marginNoteDao.upsert(existing.copy(pageNumber = page, content = text, type = type, imageUris = images).toEntity())
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

    // Turns raw user input into an FTS4 prefix query ("dune her" -> "dune* her*").
    // Only letters and digits survive (the FTS tokenizer splits on everything
    // else anyway), and bare operator words are lower-cased, so nothing the user
    // types — "title:x", "a OR", quotes — can become an FTS syntax error.
    private fun toFtsQuery(raw: String): String? {
        val tokens = raw.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() }
            .map { token -> if (token in FTS_OPERATORS) token.lowercase() else token }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }

    private companion object {
        val FTS_OPERATORS = setOf("AND", "OR", "NOT", "NEAR")
        /** Quick page taps this close to the first one are folded into one record. */
        const val QUICK_LOG_MERGE_MS = 10 * 60_000L
        val PIPELINE_STATUSES = setOf(
            BookStatus.TO_READ.name, BookStatus.SHORTLIST.name,
            BookStatus.UP_NEXT.name, BookStatus.PAUSED.name, "BACKLOG"
        )
    }

    override suspend fun snapshotForBackup(): LibrarySnapshot = LibrarySnapshot(
        books = bookDao.getAll().map { it.toModel() },
        sessions = sessionDao.getAll().map { it.toModel() },
        notes = marginNoteDao.getAll().map { it.toModel() },
        words = vocabDao.getAll().map { it.toModel() },
        reviews = reviewDao.getAll().map { it.toModel() }
    )

    override suspend fun upsertSessions(sessions: List<Session>) {
        if (sessions.isNotEmpty()) sessionDao.upsert(sessions.map { it.toEntity() })
    }

    override fun observeReviewStates(): Flow<List<ReviewState>> =
        reviewDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun saveReviewState(state: ReviewState) {
        reviewDao.upsert(state.toEntity())
    }

    override fun observeWords(): Flow<List<VocabWord>> =
        vocabDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun saveWord(word: VocabWord) {
        vocabDao.upsert(word.toEntity())
    }

    override suspend fun deleteWord(id: String) {
        vocabDao.deleteById(id)
        reviewDao.deleteById(id)
    }

    override suspend fun setFinishBy(bookId: String, finishBy: Long?) {
        val entity = bookDao.getById(bookId) ?: return
        bookDao.upsert(entity.copy(finishBy = finishBy, lastUpdated = System.currentTimeMillis()))
    }

    /** Zone a new session is recorded in; see [Session.zoneId]. */
    private fun currentZoneId(): String = java.time.ZoneId.systemDefault().id

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

        if (snapshot.words.isNotEmpty()) {
            val existingWords = vocabDao.getAll().map { it.id }.toSet()
            val wordsToUpsert = snapshot.words.filter { it.id !in existingWords }.map { it.toEntity() }
            if (wordsToUpsert.isNotEmpty()) vocabDao.upsert(wordsToUpsert)
        }

        if (snapshot.reviews.isNotEmpty()) {
            val existingReviews = reviewDao.getAll().associateBy { it.itemId }
            val reviewsToUpsert = snapshot.reviews.filter { backup ->
                val existing = existingReviews[backup.itemId]
                existing == null || backup.lastReviewedAt > existing.lastReviewedAt
            }.map { it.toEntity() }
            if (reviewsToUpsert.isNotEmpty()) reviewDao.upsert(reviewsToUpsert)
        }
    }

    override suspend fun startSession(bookId: String, startPage: Int?) {
        val book = bookDao.getById(bookId) ?: return
        val finalStartUnit = startPage ?: book.currentPage

        // Reading a book from the to-read pipeline (or a paused one) makes it a
        // book in progress. Ended books go through restartReading instead.
        val promote = book.status in PIPELINE_STATUSES
        if (promote || (startPage != null && startPage != book.currentPage)) {
            bookDao.upsert(
                book.copy(
                    currentPage = finalStartUnit,
                    status = if (promote) BookStatus.READING.name else book.status,
                    lastUpdated = System.currentTimeMillis()
                )
            )
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
                startPage = finalStartUnit,
                endPage = finalStartUnit,
                pagesRead = 0,
                durationSeconds = 0,
                environmentTag = "",
                zoneId = currentZoneId()
            )
        )
    }

    override suspend fun endSession(bookId: String, environmentTag: String) {
        sessionDao.getOpenSessionForBook(bookId)?.let { finalizeSession(it, environmentTag) }
    }

    /**
     * Closes a session with values the reader confirmed on the save screen rather
     * than whatever the clock and the book row happened to hold. Handles the case
     * where no session is open (progress logged without a running timer) by
     * writing a standalone record, so both entry points land on the same shape.
     */
    override suspend fun endSessionDetailed(
        bookId: String,
        endPage: Int,
        environmentTag: String,
        startTime: Long,
        endTime: Long
    ): Session? {
        val book = bookDao.getById(bookId) ?: return null
        val ceiling = if (book.totalPages > 0) book.totalPages else Int.MAX_VALUE
        val cappedEnd = endPage.coerceIn(0, ceiling)

        val open = sessionDao.getOpenSessionForBook(bookId)
        val startPage = open?.startPage ?: book.currentPage

        bookDao.upsert(book.copy(currentPage = cappedEnd, lastUpdated = System.currentTimeMillis()))

        val finalized = (
            open ?: SessionEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                startTime = startTime,
                endTime = 0L,
                durationSeconds = 0,
                startPage = startPage,
                endPage = startPage,
                pagesRead = 0,
                environmentTag = "",
                zoneId = currentZoneId()
            )
            ).copy(
            startTime = startTime,
            endTime = endTime,
            endPage = cappedEnd,
            pagesRead = (cappedEnd - startPage).coerceAtLeast(0),
            durationSeconds = ((endTime - startTime) / 1000L).toInt().coerceAtLeast(0),
            environmentTag = environmentTag
        )
        sessionDao.upsert(finalized)

        if (ServiceLocator.timerBookId.value == bookId) ServiceLocator.activeReadingSeconds.value = 0
        return finalized.toModel()
    }

    private suspend fun finalizeSession(open: SessionEntity, tag: String = "") {
        val book = bookDao.getById(open.bookId)
        val endPage = book?.currentPage ?: open.startPage
        
        // The timer's measured reading time only describes the book it is running for.
        val timerOwnsSession = ServiceLocator.timerBookId.value == open.bookId
        val activeSeconds = if (timerOwnsSession) ServiceLocator.activeReadingSeconds.value else 0
        val endTime = System.currentTimeMillis()

        // Align startTime to the actively-read time so pauses don't inflate the duration.
        val correctedStartTime = if (activeSeconds > 0) {
            endTime - (activeSeconds * 1000L)
        } else {
            open.startTime
        }
        
        sessionDao.upsert(
            open.copy(
                startTime = correctedStartTime,
                endTime = endTime,
                endPage = endPage,
                pagesRead = (endPage - open.startPage).coerceAtLeast(0),
                // Timer sessions used to leave this at 0, which silently excluded
                // them from every consumer that filters on durationSeconds > 0.
                durationSeconds = ((endTime - correctedStartTime) / 1000L).toInt().coerceAtLeast(0),
                environmentTag = tag.ifBlank { open.environmentTag }
            )
        )
        
        if (timerOwnsSession) ServiceLocator.activeReadingSeconds.value = 0
    }

    override suspend fun addManualSession(
        bookId: String,
        startPage: Int,
        endPage: Int,
        durationMillis: Long,
        endTime: Long
    ) {
        val book = bookDao.getById(bookId) ?: return
        val now = System.currentTimeMillis()
        val ceiling = if (book.totalPages > 0) book.totalPages else Int.MAX_VALUE
        val cappedStart = startPage.coerceIn(0, ceiling)
        val cappedEnd = endPage.coerceIn(0, ceiling)
        sessionDao.insert(
            SessionEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                startTime = endTime - durationMillis,
                endTime = endTime,
                startPage = cappedStart,
                endPage = cappedEnd,
                pagesRead = (cappedEnd - cappedStart).coerceAtLeast(0),
                durationSeconds = (durationMillis / 1000).toInt(),
                environmentTag = "Manual Entry",
                zoneId = currentZoneId()
            )
        )

        // Logging an older session must never move the bookmark backwards.
        if (cappedEnd > book.currentPage) {
            val keepStatus = book.status == BookStatus.FINISHED.name || book.status == BookStatus.DNF.name
            bookDao.upsert(
                book.copy(
                    currentPage = cappedEnd,
                    lastUpdated = now,
                    status = if (keepStatus) book.status else BookStatus.READING.name
                )
            )
        }
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
     * captures it via endPage - startPage, which also covers watch taps made
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
        if (gained == 0) return
        if (sessionDao.getOpenSessionForBook(bookId) != null) return
        // The quick log this change continues: an untimed record ending where this one starts.
        val quickLog = sessionDao.getLatestCompletedForBook(bookId)?.takeIf {
            it.durationSeconds == 0 && it.startTime == it.endTime && it.environmentTag.isEmpty() &&
                it.endPage == oldUnit && at - it.endTime in 0..QUICK_LOG_MERGE_MS
        }
        if (gained < 0) {
            // Taking pages back (Undo, a mistaken +5 on the widget) trims the record they went into.
            if (quickLog != null) {
                val end = newUnit.coerceAtLeast(quickLog.startPage)
                if (end == quickLog.startPage) sessionDao.deleteById(quickLog.id)
                else sessionDao.upsert(quickLog.copy(endPage = end, pagesRead = end - quickLog.startPage))
            }
            return
        }
        // Several taps a moment apart are one bit of reading, not a pile of one-page sessions.
        if (quickLog != null) {
            sessionDao.upsert(quickLog.copy(endPage = newUnit, pagesRead = quickLog.pagesRead + gained))
            return
        }
        sessionDao.insert(
            SessionEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                startTime = at,
                endTime = at,
                startPage = oldUnit,
                endPage = newUnit,
                pagesRead = gained,
                durationSeconds = 0,
                environmentTag = "",
                zoneId = currentZoneId()
            )
        )
    }
}
