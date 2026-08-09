package com.example.booktracker.app.ui

import android.content.Context
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
import kotlinx.coroutines.flow.firstOrNull
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
        .map { com.example.booktracker.app.data.ThemeMode.entries[it] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.example.booktracker.app.data.ThemeMode.SYSTEM)

    val useDynamicColor: StateFlow<Boolean> = settings.useDynamicColor
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            true
        )

    val useAppLock: StateFlow<Boolean> = settings.useAppLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val userName: StateFlow<String> = settings.userName
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "Reader"
        )

    val dayStartsAtHour: StateFlow<Int> = settings.dayStartsAtHour
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            3
        )

    val streak: StateFlow<StreakEngine.StreakInfo> =
        combine(completedSessions, settings.dailyGoal, settings.dayStartsAtHour) { sessions, goal, dayStart ->
            StreakEngine.compute(sessions, goal, dayStartsAtHour = dayStart)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StreakEngine.compute(emptyList())
        )

    val estimatedTimeRemainingMap: StateFlow<Map<String, String>> = combine(
        books, completedSessions
    ) { bks, sessions ->
        bks.associate { book ->
            val bookSessions = sessions.filter { it.bookId == book.id && it.durationSeconds > 0 && it.pagesRead > 0 }
            val totalPagesRead = bookSessions.sumOf { it.pagesRead }
            val totalDuration = bookSessions.sumOf { it.durationSeconds }
            
            val remainingStr = if (totalPagesRead > 0 && totalDuration > 0) {
                val avgSpeed = totalPagesRead.toDouble() / totalDuration // pages per sec
                val remainingPages = book.totalPages - book.currentPage
                if (remainingPages > 0) {
                    val remainingSeconds = (remainingPages / avgSpeed).toLong()
                    val hours = remainingSeconds / 3600
                    val minutes = (remainingSeconds % 3600) / 60
                    if (hours > 0) "${hours}h ${minutes}m remaining" else "${minutes}m remaining"
                } else null
            } else null
            
            book.id to remainingStr
        }.mapNotNull { (id, str) -> if (str != null) id to str else null }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val allNotes: StateFlow<List<MarginNote>> = repository.observeAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        list.filter { it.status == BookStatus.SHORTLIST.name }
            .sortedByDescending { it.lastUpdated }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTimerBook: StateFlow<Book?> = combine(books, com.example.booktracker.app.data.ServiceLocator.timerBookId) { bookList, id ->
        bookList.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val timerPhase: StateFlow<TimerPhase> = com.example.booktracker.app.data.ServiceLocator.timerPhase.asStateFlow()
    val timerMode: StateFlow<TimerMode> = com.example.booktracker.app.data.ServiceLocator.timerMode.asStateFlow()
    val timerIsRunning: StateFlow<Boolean> = com.example.booktracker.app.data.ServiceLocator.timerIsRunning.asStateFlow()
    val timeLeftSeconds: StateFlow<Int> = com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.asStateFlow()
    val elapsedSeconds: StateFlow<Int> = com.example.booktracker.app.data.ServiceLocator.elapsedSeconds.asStateFlow()
    
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
                            old.currentPage == new.currentPage &&
                            old.totalPages == new.totalPages
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
        
        // Timer Sync to WearOS
        viewModelScope.launch {
            combine(
                combine(timerIsRunning, timeLeftSeconds, elapsedSeconds) { run, left, elap -> Triple(run, left, elap) },
                timerPhase,
                timerMode,
                com.example.booktracker.app.data.ServiceLocator.timerBookId
            ) { (isRunning, timeLeft, elapsed), phase, mode, bookId ->
                val timeValue = if (mode == TimerMode.STOPWATCH) elapsed else timeLeft
                wearBridge.publishTimerState(isRunning, timeValue, phase.ordinal, bookId, mode.name)
            }.collect()
        }
        
        viewModelScope.launch {
            com.example.booktracker.app.data.ServiceLocator.timerControlEvents.collect { action ->
                if (action == "PAUSE" && timerIsRunning.value) {
                    setTimerRunning(false)
                } else if (action == "RESUME" && !timerIsRunning.value && com.example.booktracker.app.data.ServiceLocator.timerBookId.value != null) {
                    setTimerRunning(true)
                }
            }
        }

        // Timer Recovery
        viewModelScope.launch {
            val savedBookId = settings.activeTimerBookId.firstOrNull()
            if (savedBookId != null) {
                com.example.booktracker.app.data.ServiceLocator.timerBookId.value = savedBookId
                val savedMode = settings.activeTimerMode.firstOrNull()
                if (savedMode == TimerMode.STOPWATCH.name) {
                    com.example.booktracker.app.data.ServiceLocator.timerMode.value = TimerMode.STOPWATCH
                    com.example.booktracker.app.data.ServiceLocator.elapsedSeconds.value = settings.activeTimerElapsed.firstOrNull() ?: 0
                } else {
                    com.example.booktracker.app.data.ServiceLocator.timerMode.value = TimerMode.COUNTDOWN
                    com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = settings.activeTimerTimeLeft.firstOrNull() ?: 0
                    val savedPhase = settings.activeTimerPhase.firstOrNull()
                    if (savedPhase == TimerPhase.BREAK.name) {
                        com.example.booktracker.app.data.ServiceLocator.timerPhase.value = TimerPhase.BREAK
                    }
                }
                com.example.booktracker.app.data.ServiceLocator.activeReadingSeconds.value = settings.activeTimerActiveReading.firstOrNull() ?: 0
                
                // Background State Fix: Calculate elapsed time using current_timestamp - start_timestamp
                val wasRunning = settings.activeTimerIsRunning.firstOrNull() ?: false
                if (wasRunning) {
                    val lastTick = settings.activeTimerLastTickTime.firstOrNull() ?: System.currentTimeMillis()
                    val now = System.currentTimeMillis()
                    val missedSeconds = ((now - lastTick) / 1000).toInt().coerceAtLeast(0)
                    
                    if (savedMode == TimerMode.STOPWATCH.name) {
                        com.example.booktracker.app.data.ServiceLocator.elapsedSeconds.value += missedSeconds
                        setTimerRunning(true)
                    } else {
                        val newTime = com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value - missedSeconds
                        if (newTime <= 0) {
                            com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = 0
                            setTimerRunning(false)
                            com.example.booktracker.app.data.ServiceLocator.notifyTimerExpired()
                        } else {
                            com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = newTime
                            setTimerRunning(true)
                        }
                    }
                    com.example.booktracker.app.data.ServiceLocator.activeReadingSeconds.value += missedSeconds
                }
            }
        }
    }

    fun addBook(title: String, authorsInput: String, totalPages: Int) {
        val authors = authorsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch { repository.addBook(title.trim(), authors, totalPages) }
    }

    fun shareNote(context: android.content.Context, note: MarginNote, book: Book) {
        QuoteShareHelper.shareQuote(context, note, book)
    }

    fun addScannedBook(scanned: BookMetadata, status: BookStatus = BookStatus.SHORTLIST, format: com.example.booktracker.shared.models.BookFormat = com.example.booktracker.shared.models.BookFormat.Paperback) {
        viewModelScope.launch {
            repository.addBook(
                title = scanned.title,
                authors = scanned.authors,
                totalPages = scanned.pageCount,
                coverUrl = scanned.coverUrl,
                description = scanned.description,
                genres = scanned.genres,
                publishedDate = scanned.publishedDate,
                status = status,
                currentPage = scanned.currentPage,
                format = format,
                creators = scanned.creators,
                purchaseLog = scanned.purchaseLog,
                loanRecord = scanned.loanRecord
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

    fun finishBook(book: Book, rating: Map<String, Float>, review: String? = null) {
        viewModelScope.launch { repository.finishBook(book.id, rating, review) }
    }

    fun readAgain(book: Book) {
        viewModelScope.launch { repository.restartReading(book.id) }
    }

    fun setProgress(book: Book, page: Int) {
        viewModelScope.launch { repository.setProgress(book.id, page) }
    }

    fun setCollections(book: Book, collections: List<String>) {
        viewModelScope.launch { repository.updateCollections(book.id, collections) }
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
            if (endPage != book.currentPage) {
                repository.addProgress(book.id, endPage - book.currentPage)
            }
            repository.endSession(book.id, environmentTag)
            if (book.status != BookStatus.FINISHED.name) {
                repository.updateStatus(book.id, BookStatus.READING)
            }
        }
    }

    /** What the result screen renders. Everything else it needs it re-derives live. */
    data class SessionResult(
        val bookId: String,
        val savedAt: Long,
        val durationSeconds: Int,
        val startPage: Int,
        val endPage: Int
    )

    private val _lastSessionResult = kotlinx.coroutines.flow.MutableStateFlow<SessionResult?>(null)
    val lastSessionResult: StateFlow<SessionResult?> = _lastSessionResult

    /**
     * The single commit point for the save-review screen. Both entry points (home
     * card and focus timer) route through here, so the environment tag, the
     * confirmed page, and the edited timestamps are captured identically.
     */
    fun saveSession(
        book: Book,
        endPage: Int,
        environmentTag: String,
        startTime: Long,
        endTime: Long,
        newStatus: BookStatus?,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val saved = repository.endSessionDetailed(
                bookId = book.id,
                endPage = endPage,
                environmentTag = environmentTag,
                startTime = startTime,
                endTime = endTime
            )
            // FINISHED/DNF are applied by their own calls so rating and DNF payloads
            // are not lost; anything else falls back to keeping the book READING.
            if (newStatus != null && newStatus != BookStatus.FINISHED && newStatus != BookStatus.DNF) {
                repository.updateStatus(book.id, newStatus)
            } else if (newStatus == null && book.status != BookStatus.FINISHED.name) {
                repository.updateStatus(book.id, BookStatus.READING)
            }
            _lastSessionResult.value = SessionResult(
                bookId = book.id,
                savedAt = endTime,
                durationSeconds = saved?.durationSeconds
                    ?: ((endTime - startTime) / 1000L).toInt().coerceAtLeast(0),
                startPage = saved?.startPage ?: book.currentPage,
                endPage = saved?.endPage ?: endPage
            )
            setTimerBook(null)
            onSaved()
        }
    }

    fun clearSessionResult() {
        _lastSessionResult.value = null
    }

    /** Drops the running session without recording it. */
    fun discardOpenSession(bookId: String) {
        viewModelScope.launch {
            repository.observeOpenSession().firstOrNull()?.takeIf { it.bookId == bookId }?.let {
                repository.deleteSession(it.id)
            }
            ServiceLocator.activeReadingSeconds.value = 0
            setTimerBook(null)
        }
    }

    fun logManualSession(bookId: String, startPage: Int, endPage: Int, durationMinutes: Int) {
        viewModelScope.launch {
            repository.addManualSession(bookId, startPage, endPage, durationMinutes * 60_000L)
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
        viewModelScope.launch { settings.setThemeMode(mode.ordinal) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setUseDynamicColor(enabled) }
    }



    fun planBook(bookId: String, timestamp: Long?) {
        viewModelScope.launch {
            val book = repository.observeBooks().firstOrNull()?.find { it.id == bookId }
            if (book != null) {
                repository.updateBook(book.copy(plannedDate = timestamp))
            }
        }
    }

    // Timer Controls
    fun setTimerBook(bookId: String?) {
        com.example.booktracker.app.data.ServiceLocator.timerBookId.value = bookId
        if (bookId == null) {
            com.example.booktracker.app.data.ServiceLocator.timerIsRunning.value = false
            soundscapeEngine.stop()
            viewModelScope.launch {
                settings.clearTimerState()
            }
        }
    }

    fun setTimerPhase(phase: TimerPhase) {
        com.example.booktracker.app.data.ServiceLocator.timerPhase.value = phase
        if (com.example.booktracker.app.data.ServiceLocator.timerMode.value == TimerMode.COUNTDOWN) {
            val workMins = com.example.booktracker.app.data.ServiceLocator.customWorkMinutes.value ?: workMinutes.value
            com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = if (phase == TimerPhase.WORK) workMins * 60 else breakMinutes.value * 60
        }
    }

    fun setTimerMode(mode: TimerMode) {
        com.example.booktracker.app.data.ServiceLocator.timerMode.value = mode
        if (mode == TimerMode.STOPWATCH) {
            com.example.booktracker.app.data.ServiceLocator.elapsedSeconds.value = 0
        } else {
            val workMins = com.example.booktracker.app.data.ServiceLocator.customWorkMinutes.value ?: workMinutes.value
            com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = workMins * 60
        }
    }

    fun setCustomWorkMinutes(mins: Int?) {
        com.example.booktracker.app.data.ServiceLocator.customWorkMinutes.value = mins
        if (com.example.booktracker.app.data.ServiceLocator.timerMode.value == TimerMode.COUNTDOWN && 
            com.example.booktracker.app.data.ServiceLocator.timerPhase.value == TimerPhase.WORK && 
            !com.example.booktracker.app.data.ServiceLocator.timerIsRunning.value) {
            com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = (mins ?: workMinutes.value) * 60
        }
    }

    fun setTimerRunning(isRunning: Boolean) {
        com.example.booktracker.app.data.ServiceLocator.timerIsRunning.value = isRunning
        if (isRunning) {
            viewModelScope.launch {
                settings.saveTimerState(
                    mode = com.example.booktracker.app.data.ServiceLocator.timerMode.value.name,
                    bookId = com.example.booktracker.app.data.ServiceLocator.timerBookId.value,
                    phase = com.example.booktracker.app.data.ServiceLocator.timerPhase.value.name,
                    isRunning = true,
                    timeLeft = com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value,
                    elapsed = com.example.booktracker.app.data.ServiceLocator.elapsedSeconds.value,
                    activeReading = com.example.booktracker.app.data.ServiceLocator.activeReadingSeconds.value,
                    lastTickTime = System.currentTimeMillis()
                )
            }
            if (_noiseType.value != NoiseType.NONE) soundscapeEngine.play(_noiseType.value)
        } else {
            soundscapeEngine.stop()
            viewModelScope.launch {
                settings.saveTimerState(
                    mode = com.example.booktracker.app.data.ServiceLocator.timerMode.value.name,
                    bookId = com.example.booktracker.app.data.ServiceLocator.timerBookId.value,
                    phase = com.example.booktracker.app.data.ServiceLocator.timerPhase.value.name,
                    isRunning = false,
                    timeLeft = com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value,
                    elapsed = com.example.booktracker.app.data.ServiceLocator.elapsedSeconds.value,
                    activeReading = com.example.booktracker.app.data.ServiceLocator.activeReadingSeconds.value,
                    lastTickTime = null
                )
            }
        }
    }

    fun setWorkMinutes(mins: Int) {
        viewModelScope.launch { settings.setWorkMinutes(mins) }
        if (com.example.booktracker.app.data.ServiceLocator.timerPhase.value == TimerPhase.WORK && !com.example.booktracker.app.data.ServiceLocator.timerIsRunning.value) {
            com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = mins * 60
        }
    }
    
    fun setBreakMinutes(mins: Int) {
        viewModelScope.launch { settings.setBreakMinutes(mins) }
        if (com.example.booktracker.app.data.ServiceLocator.timerPhase.value == TimerPhase.BREAK && !com.example.booktracker.app.data.ServiceLocator.timerIsRunning.value) {
            com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value = mins * 60
        }
    }

    fun setDayStartsAtHour(hour: Int) {
        viewModelScope.launch { settings.setDayStartsAtHour(hour) }
    }
    
    fun setUseAppLock(enabled: Boolean) {
        viewModelScope.launch { settings.setUseAppLock(enabled) }
    }

    fun setNoiseType(type: NoiseType) {
        _noiseType.value = type
        if (com.example.booktracker.app.data.ServiceLocator.timerIsRunning.value) {
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

    fun addNote(
        bookId: String,
        page: Int,
        text: String,
        type: com.example.booktracker.shared.models.NoteType =
            com.example.booktracker.shared.models.NoteType.BOOK_CONTENT
    ) {
        viewModelScope.launch { repository.addNote(bookId, page, text, type) }
    }

    fun toggleNoteFavorite(noteId: String) {
        viewModelScope.launch { repository.toggleNoteFavorite(noteId) }
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
