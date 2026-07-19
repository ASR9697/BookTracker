package com.example.booktracker.app.ui

import android.content.Context
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.data.BackupEngine
import com.example.booktracker.app.data.BookRepository
import com.example.booktracker.app.data.LibrarySearchResults
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.data.SettingsRepository
import com.example.booktracker.app.data.LibrarySnapshot
import com.example.booktracker.app.data.remote.BookMetadata
import com.example.booktracker.app.sync.WearBridge
import com.example.booktracker.app.journal.NoiseType
import com.example.booktracker.app.journal.SoundscapeEngine
import com.example.booktracker.app.ui.TimerPhase
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

class BookTrackerViewModel(
    private val appContext: Context,
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

    val dndDuringSession: StateFlow<Boolean> = settings.dndDuringSession
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false
        )

    val themeMode: StateFlow<com.example.booktracker.app.data.ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.example.booktracker.app.data.ThemeMode.SYSTEM)

    val useDynamicColor: StateFlow<Boolean> = settings.useDynamicColor
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            true
        )

    val userName: StateFlow<String> = settings.userName
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "Reader"
        )

    val streak: StateFlow<StreakEngine.StreakInfo> =
        combine(completedSessions, settings.dailyGoal) { sessions, goal ->
            StreakEngine.compute(sessions, goal)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StreakEngine.compute(emptyList())
        )

    val totalNotesCount: StateFlow<Int> = repository.observeTotalNotesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unlockedBadges: StateFlow<Set<String>> = combine(
        books,
        completedSessions,
        streak,
        totalNotesCount
    ) { b, s, st, notes ->
        BadgeEngine.evaluate(b, s, st.currentStreak, notes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _newUnlockEvents = Channel<Set<String>>(Channel.BUFFERED)
    val newUnlockEvents = _newUnlockEvents.receiveAsFlow()
    
    val tbrCurationBooks: StateFlow<List<Book>> = books.map { list ->
        list.filter { it.status == BookStatus.BACKLOG.name }
            .sortedByDescending { it.lastUpdated }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Timer State
    private val _timerBookId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    
    val activeTimerBook: StateFlow<Book?> = combine(books, _timerBookId) { bookList, id ->
        bookList.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _timerPhase = kotlinx.coroutines.flow.MutableStateFlow(TimerPhase.WORK)
    val timerPhase: StateFlow<TimerPhase> = _timerPhase

    private val _timerIsRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning

    private val _timeLeftSeconds = kotlinx.coroutines.flow.MutableStateFlow(25 * 60)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds
    
    val workMinutes: StateFlow<Int> = settings.workMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 25)
    
    val breakMinutes: StateFlow<Int> = settings.breakMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    private val _noiseType = kotlinx.coroutines.flow.MutableStateFlow(NoiseType.NONE)
    val noiseType: StateFlow<NoiseType> = _noiseType
    
    private val _timerVolume = kotlinx.coroutines.flow.MutableStateFlow(0.5f)
    val timerVolume: StateFlow<Float> = _timerVolume
    
    val soundscapeEngine = SoundscapeEngine()

    init {
        viewModelScope.launch {
            combine(
                repository.observeBooks().map { list ->
                    list.filter { it.status == BookStatus.READING.name }
                },
                streak
            ) { books, streakInfo -> books to streakInfo }
                .distinctUntilChanged { (oldBooks, oldStreak), (newBooks, newStreak) ->
                    oldBooks.size == newBooks.size &&
                        oldBooks.zip(newBooks).all { (old, new) ->
                            old.id == new.id &&
                            old.title == new.title &&
                            old.currentUnit == new.currentUnit &&
                            old.totalUnits == new.totalUnits
                        } &&
                        oldStreak.dailyGoal == newStreak.dailyGoal &&
                        oldStreak.pagesToday == newStreak.pagesToday
                }
                .collect { (books, streakInfo) ->
                    wearBridge.publishActiveBooks(
                        books,
                        streakInfo.dailyGoal,
                        streakInfo.pagesToday
                    )
                }
        }

        viewModelScope.launch {
            combine(unlockedBadges, settings.seenBadges) { unlocked, seen ->
                val newBadges = unlocked - seen
                if (newBadges.isNotEmpty()) {
                    _newUnlockEvents.trySend(newBadges)
                    settings.markBadgesSeen(newBadges)
                }
            }.collect()
        }
        
        // Timer Sync
        viewModelScope.launch {
            combine(
                _timerIsRunning,
                _timeLeftSeconds,
                _timerPhase,
                _timerBookId
            ) { isRunning, timeLeft, phase, bookId ->
                wearBridge.publishTimerState(isRunning, timeLeft, phase.ordinal, bookId)
            }.collect()
        }
        
        viewModelScope.launch {
            com.example.booktracker.app.data.ServiceLocator.timerControlEvents.collect { action ->
                if (action == "PAUSE" && _timerIsRunning.value) {
                    setTimerRunning(false)
                } else if (action == "RESUME" && !_timerIsRunning.value && _timerBookId.value != null) {
                    setTimerRunning(true)
                }
            }
        }
    }

    fun addBook(title: String, authorsInput: String, totalUnits: Int) {
        val authors = authorsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch { repository.addBook(title.trim(), authors, totalUnits) }
    }

    fun addScannedBook(scanned: BookMetadata, status: BookStatus = BookStatus.BACKLOG) {
        viewModelScope.launch {
            repository.addBook(
                title = scanned.title,
                authors = scanned.authors,
                totalUnits = scanned.pageCount,
                coverUrl = scanned.coverUrl,
                description = scanned.description,
                genres = scanned.genres,
                publishedDate = scanned.publishedDate,
                status = status,
                currentUnit = scanned.currentUnit
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

    fun promoteToReading(book: Book) {
        viewModelScope.launch {
            repository.updateStatus(book.id, BookStatus.READING)
        }
    }

    fun pauseBook(bookId: String) {
        viewModelScope.launch {
            repository.updateStatus(bookId, BookStatus.PAUSED)
        }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch { repository.toggleFavorite(book.id) }
    }

    fun addProgress(book: Book, delta: Int) {
        viewModelScope.launch { repository.addProgress(book.id, delta) }
    }

    fun setFormat(book: Book, format: String) {
        viewModelScope.launch { repository.updateFormat(book.id, format) }
    }

    fun finishBook(book: Book, rating: Map<String, Float>) {
        viewModelScope.launch { repository.finishBook(book.id, rating) }
    }

    fun readAgain(book: Book) {
        viewModelScope.launch {
            repository.updateStatus(book.id, BookStatus.READING)
            repository.addProgress(book.id, -book.currentUnit)
        }
    }

    fun markDnf(book: Book, percentage: Float, reason: String) {
        viewModelScope.launch { repository.markDnf(book.id, percentage, reason) }
    }

    fun startSession(book: Book, startPage: Int? = null) {
        viewModelScope.launch { 
            repository.startSession(book.id, startPage) 
        }
    }

    fun endSession(book: Book, endPage: Int, environmentTag: String) {
        viewModelScope.launch {
            if (endPage != book.currentUnit) {
                repository.addProgress(book.id, endPage - book.currentUnit)
            }
            repository.endSession(book.id, environmentTag)
            if (book.status != BookStatus.FINISHED.name) {
                repository.updateStatus(book.id, BookStatus.READING)
            }
        }
    }

    fun delete(book: Book) {
        viewModelScope.launch { repository.deleteBook(book.id) }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch { repository.deleteSession(session.id) }
    }

    fun updateSession(session: Session) {
        viewModelScope.launch { repository.updateSession(session) }
    }

    fun restore(book: Book) {
        viewModelScope.launch { repository.restore(book) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { settings.setUserName(name) }
    }

    fun setDailyGoal(pages: Int) {
        viewModelScope.launch { settings.setDailyGoal(pages) }
    }

    fun setYearlyGoal(books: Int) {
        viewModelScope.launch { settings.setYearlyGoal(books) }
    }

    fun setDndDuringSession(enabled: Boolean) {
        viewModelScope.launch { settings.setDndDuringSession(enabled) }
    }

    fun setThemeMode(mode: com.example.booktracker.app.data.ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setUseDynamicColor(enabled) }
    }



    // Timer Controls
    fun setTimerBook(bookId: String?) {
        _timerBookId.value = bookId
        if (bookId == null) {
            _timerIsRunning.value = false
            soundscapeEngine.stop()
        }
    }

    fun setTimerPhase(phase: TimerPhase) {
        _timerPhase.value = phase
        _timeLeftSeconds.value = if (phase == TimerPhase.WORK) workMinutes.value * 60 else breakMinutes.value * 60
    }

    private var timerJob: kotlinx.coroutines.Job? = null

    fun setTimerRunning(isRunning: Boolean) {
        _timerIsRunning.value = isRunning
        timerJob?.cancel()
        if (isRunning) {
            if (_noiseType.value != NoiseType.NONE) soundscapeEngine.play(_noiseType.value)
            timerJob = viewModelScope.launch {
                while (_timeLeftSeconds.value > 0) {
                    delay(1000L)
                    _timeLeftSeconds.value -= 1
                    if (_timeLeftSeconds.value == 0) {
                        if (_timerPhase.value == TimerPhase.WORK) {
                            _timerPhase.value = TimerPhase.BREAK
                            _timeLeftSeconds.value = breakMinutes.value * 60
                        } else {
                            _timerPhase.value = TimerPhase.WORK
                            _timeLeftSeconds.value = workMinutes.value * 60
                        }
                        _timerIsRunning.value = false
                        soundscapeEngine.stop()
                        break
                    }
                }
            }
        } else {
            soundscapeEngine.stop()
        }
    }

    fun setWorkMinutes(mins: Int) {
        viewModelScope.launch { settings.setWorkMinutes(mins) }
        if (_timerPhase.value == TimerPhase.WORK && !_timerIsRunning.value) {
            _timeLeftSeconds.value = mins * 60
        }
    }
    
    fun setBreakMinutes(mins: Int) {
        viewModelScope.launch { settings.setBreakMinutes(mins) }
        if (_timerPhase.value == TimerPhase.BREAK && !_timerIsRunning.value) {
            _timeLeftSeconds.value = mins * 60
        }
    }

    fun setNoiseType(type: NoiseType) {
        _noiseType.value = type
        if (_timerIsRunning.value) {
            if (type == NoiseType.NONE) soundscapeEngine.stop()
            else soundscapeEngine.play(type)
        }
    }
    
    fun setTimerVolume(vol: Float) {
        _timerVolume.value = vol
        soundscapeEngine.setVolume(vol)
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

    suspend fun searchLibrary(query: String): LibrarySearchResults =
        repository.searchLibrary(query)

    fun exportBackup(context: Context, uri: android.net.Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                BackupEngine.exportToUri(
                    context = context,
                    uri = uri,
                    snapshot = repository.snapshotForBackup(),
                    dailyGoal = settings.dailyGoal.first(),
                    yearlyGoal = settings.yearlyGoal.first()
                )
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    /** onDone receives the number of restored books, or -1 on failure. */
    fun importBackup(context: Context, uri: android.net.Uri, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val parsed = BackupEngine.importFromUri(context, uri)
                repository.restoreFromBackup(parsed.snapshot)
                parsed.dailyGoal?.let { settings.setDailyGoal(it) }
                parsed.yearlyGoal?.let { settings.setYearlyGoal(it) }
                onDone(parsed.snapshot.books.size)
            } catch (e: Exception) {
                onDone(-1)
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    BookTrackerViewModel(
                        appContext,
                        ServiceLocator.repository(appContext),
                        ServiceLocator.settings(appContext),
                        WearBridge(appContext)
                    )
                }
            }
        }
    }
}
