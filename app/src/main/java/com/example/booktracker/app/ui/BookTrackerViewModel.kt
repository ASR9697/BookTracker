package com.example.booktracker.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.data.BookRepository
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.data.SettingsRepository
import com.example.booktracker.app.data.remote.BookMetadata
import com.example.booktracker.app.sync.WearBridge
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookTrackerViewModel(
    private val repository: BookRepository,
    private val settings: SettingsRepository,
    private val wearBridge: WearBridge
) : ViewModel() {

    val books: StateFlow<List<Book>> = repository.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val openSession: StateFlow<Session?> = repository.observeOpenSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val completedSessions: StateFlow<List<Session>> = repository.observeCompletedSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyGoal: StateFlow<Int> = settings.dailyGoal
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StreakEngine.DEFAULT_DAILY_GOAL_PAGES
        )

    val yearlyGoal: StateFlow<Int> = settings.yearlyGoal
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_YEARLY_GOAL_BOOKS
        )

    val streak: StateFlow<StreakEngine.StreakInfo> =
        combine(completedSessions, settings.dailyGoal) { sessions, goal ->
            StreakEngine.compute(sessions, goal)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StreakEngine.compute(emptyList())
        )

    init {
        // Keep the watch in step with whichever book is currently being read,
        // plus the configurable daily goal and today's page count for its dial.
        viewModelScope.launch {
            combine(
                repository.observeBooks().map { list ->
                    list.filter { it.status == BookStatus.READING.name }
                        .maxByOrNull { it.lastUpdated }
                },
                streak
            ) { book, streakInfo -> book to streakInfo }
                .filter { (book, _) -> book != null }
                .distinctUntilChanged { (oldBook, oldStreak), (newBook, newStreak) ->
                    oldBook!!.id == newBook!!.id &&
                        oldBook.title == newBook.title &&
                        oldBook.currentUnit == newBook.currentUnit &&
                        oldBook.totalUnits == newBook.totalUnits &&
                        oldStreak.dailyGoal == newStreak.dailyGoal &&
                        oldStreak.pagesToday == newStreak.pagesToday
                }
                .collect { (book, streakInfo) ->
                    wearBridge.publishActiveBook(
                        book!!,
                        streakInfo.dailyGoal,
                        streakInfo.pagesToday
                    )
                }
        }
    }

    fun addBook(title: String, authorsInput: String, totalUnits: Int) {
        val authors = authorsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch { repository.addBook(title.trim(), authors, totalUnits) }
    }

    fun addScannedBook(scanned: BookMetadata) {
        viewModelScope.launch {
            repository.addBook(
                title = scanned.title,
                authors = scanned.authors,
                totalUnits = scanned.pageCount,
                coverUrl = scanned.coverUrl,
                description = scanned.description,
                genres = scanned.genres,
                publishedDate = scanned.publishedDate
            )
        }
    }

    fun importCsv(context: Context, uri: android.net.Uri, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importCsv(context, uri)
            onComplete(count)
        }
    }

    fun promote(book: Book) {
        val next = when (book.status) {
            BookStatus.BACKLOG.name -> BookStatus.SHORTLIST
            BookStatus.SHORTLIST.name -> BookStatus.UP_NEXT
            BookStatus.UP_NEXT.name -> BookStatus.READING
            else -> return
        }
        viewModelScope.launch { repository.updateStatus(book.id, next) }
    }

    fun addProgress(book: Book, delta: Int) {
        viewModelScope.launch { repository.addProgress(book.id, delta) }
    }

    fun finishBook(book: Book, rating: Map<String, Float>) {
        viewModelScope.launch { repository.finishBook(book.id, rating) }
    }

    fun markDnf(book: Book, percentage: Float, reason: String) {
        viewModelScope.launch { repository.markDnf(book.id, percentage, reason) }
    }

    fun startSession(book: Book) {
        viewModelScope.launch { repository.startSession(book.id) }
    }

    fun endSession(book: Book, tag: String = "") {
        viewModelScope.launch { repository.endSession(book.id, tag) }
    }

    fun delete(book: Book) {
        viewModelScope.launch { repository.deleteBook(book.id) }
    }

    fun restore(book: Book) {
        viewModelScope.launch { repository.restore(book) }
    }

    fun setDailyGoal(pages: Int) {
        viewModelScope.launch { settings.setDailyGoal(pages) }
    }

    fun setYearlyGoal(books: Int) {
        viewModelScope.launch { settings.setYearlyGoal(books) }
    }

    fun sessionsFor(bookId: String): Flow<List<Session>> =
        repository.observeSessionsForBook(bookId)

    fun notesFor(bookId: String): Flow<List<MarginNote>> = repository.observeNotes(bookId)

    fun addNote(bookId: String, pageOrUnit: Int, text: String) {
        viewModelScope.launch { repository.addNote(bookId, pageOrUnit, text) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    BookTrackerViewModel(
                        ServiceLocator.repository(appContext),
                        ServiceLocator.settings(appContext),
                        WearBridge(appContext)
                    )
                }
            }
        }
    }
}
