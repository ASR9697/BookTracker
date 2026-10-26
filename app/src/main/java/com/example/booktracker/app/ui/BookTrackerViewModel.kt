package com.example.booktracker.app.ui

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.data.AppPalette
import com.example.booktracker.app.data.BackupEngine
import com.example.booktracker.app.data.BookRepository
import com.example.booktracker.app.data.CsvImportResult
import com.example.booktracker.app.data.DeletedBook
import com.example.booktracker.app.data.HomeLayout
import com.example.booktracker.app.data.bookmory.BookmoryImportSummary
import com.example.booktracker.app.data.bookmory.BookmoryImporter
import com.example.booktracker.app.data.LibrarySearchResults
import com.example.booktracker.app.data.NoteImageStore
import com.example.booktracker.app.data.ReminderSettings
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.data.SettingsRepository
import com.example.booktracker.app.data.ThemeMode
import com.example.booktracker.app.data.remote.BookMetadata
import com.example.booktracker.app.journal.NoiseType
import com.example.booktracker.app.journal.SoundscapeEngine
import com.example.booktracker.app.service.TimerPersistence
import com.example.booktracker.app.sync.WearBridge
import com.example.booktracker.app.widget.ReadingWidget
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.LoanRecord
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakEngine.DEFAULT_DAILY_GOAL_PAGES)

    val yearlyGoal: StateFlow<Int> = settings.yearlyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_YEARLY_GOAL_BOOKS)

    val monthlyGoalPages: StateFlow<Int> = settings.monthlyGoalPages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 500)

    val monthlyGoalBooks: StateFlow<Int> = settings.monthlyGoalBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 2)

    val dndDuringSession: StateFlow<Boolean> = settings.dndDuringSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .map { ThemeMode.entries.getOrElse(it) { ThemeMode.SYSTEM } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val useDynamicColor: StateFlow<Boolean> = settings.useDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val appPalette: StateFlow<AppPalette> = settings.appPalette
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPalette.BOOKMORY_AMBER)

    val useAppLock: StateFlow<Boolean> = settings.useAppLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Null until settings load — the activity keeps its splash up until then. */
    val appLockSetting: StateFlow<Boolean?> = settings.useAppLock
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val reminder: StateFlow<ReminderSettings> = settings.reminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings(false, 20, 0))

    val userName: StateFlow<String> = settings.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Reader")

    val dayStartsAtHour: StateFlow<Int> = settings.dayStartsAtHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    val homeLayout: StateFlow<HomeLayout> = settings.homeLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLayout.DEFAULT)

    val streak: StateFlow<StreakEngine.StreakInfo> =
        combine(completedSessions, settings.dailyGoal, settings.dayStartsAtHour) { sessions, goal, dayStart ->
            StreakEngine.compute(sessions, goal, dayStartsAtHour = dayStart)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StreakEngine.compute(emptyList())
        )

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
        list.filter { it.status.equals(BookStatus.TO_READ.name, ignoreCase = true) || it.status.equals("BACKLOG", ignoreCase = true) }
            .sortedByDescending { it.lastUpdated }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTimerBook: StateFlow<Book?> = combine(books, ServiceLocator.timerBookId) { bookList, id ->
        bookList.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val timerPhase: StateFlow<TimerPhase> = ServiceLocator.timerPhase.asStateFlow()
    val timerMode: StateFlow<TimerMode> = ServiceLocator.timerMode.asStateFlow()
    val timerIsRunning: StateFlow<Boolean> = ServiceLocator.timerIsRunning.asStateFlow()
    val timeLeftSeconds: StateFlow<Int> = ServiceLocator.timeLeftSeconds.asStateFlow()
    val elapsedSeconds: StateFlow<Int> = ServiceLocator.elapsedSeconds.asStateFlow()

    val workMinutes: StateFlow<Int> = settings.workMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 25)

    val breakMinutes: StateFlow<Int> = settings.breakMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    private val _noiseType = MutableStateFlow(NoiseType.NONE)
    val noiseType: StateFlow<NoiseType> = _noiseType

    private val _timerVolume = MutableStateFlow(0.5f)
    val timerVolume: StateFlow<Float> = _timerVolume

    val soundscapeEngine = SoundscapeEngine()

    // Books removed in this session, kept so the Undo snackbar can restore their
    // sessions and notes too (the delete cascades to both).
    private val recentlyDeleted = mutableMapOf<String, DeletedBook>()

    init {
        // Photos from notes deleted (and not undone) in an earlier session, or
        // picked into a note sheet that was then dismissed.
        viewModelScope.launch {
            val notes = repository.observeAllNotes().first()
            NoteImageStore.sweepOrphans(appContext, notes.flatMap { it.imageUris })
        }
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

        // The home-screen widget only re-renders when asked, so refresh it whenever
        // progress or the streak changes (debounced so +1 taps don't thrash it).
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            combine(repository.observeBooks(), streak) { books, streakInfo ->
                books.filter { it.status == BookStatus.READING.name }
                    .map { Triple(it.id, it.currentPage, it.totalPages) } to streakInfo.currentStreak
            }
                .distinctUntilChanged()
                .drop(1)
                .debounce(1_000)
                .collect { runCatching { ReadingWidget().updateAll(appContext) } }
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
                ServiceLocator.timerBookId
            ) { (isRunning, timeLeft, elapsed), phase, mode, bookId ->
                val timeValue = if (mode == TimerMode.STOPWATCH) elapsed else timeLeft
                wearBridge.publishTimerState(isRunning, timeValue, phase.ordinal, bookId, mode.name)
            }.collect()
        }

        viewModelScope.launch {
            ServiceLocator.timerControlEvents.collect { action ->
                if (action == "PAUSE" && timerIsRunning.value) {
                    setTimerRunning(false)
                } else if (action == "RESUME" && !timerIsRunning.value && ServiceLocator.timerBookId.value != null) {
                    setTimerRunning(true)
                }
            }
        }

        // Timer recovery after process death. Skipped when the timer is already
        // live in this process (e.g. the activity was recreated while the
        // foreground service kept running) — the in-memory state is newer.
        viewModelScope.launch {
            if (ServiceLocator.timerBookId.value != null) return@launch
            val savedBookId = settings.activeTimerBookId.firstOrNull() ?: return@launch
            // A timer left "running" for half a day (or restored from a device
            // backup) isn't a real session any more; drop it rather than resume it.
            val savedTick = settings.activeTimerLastTickTime.firstOrNull()
            val bookExists = repository.observeBooks().first().any { it.id == savedBookId }
            if (!bookExists || (savedTick != null && System.currentTimeMillis() - savedTick > STALE_TIMER_MS)) {
                settings.clearTimerState()
                return@launch
            }
            ServiceLocator.timerBookId.value = savedBookId
            ServiceLocator.timerBookTitle.value = repository.observeBooks().first().find { it.id == savedBookId }?.title
            val savedMode = settings.activeTimerMode.firstOrNull()
            if (savedMode == TimerMode.STOPWATCH.name) {
                ServiceLocator.timerMode.value = TimerMode.STOPWATCH
                ServiceLocator.elapsedSeconds.value = settings.activeTimerElapsed.firstOrNull() ?: 0
            } else {
                ServiceLocator.timerMode.value = TimerMode.COUNTDOWN
                ServiceLocator.timeLeftSeconds.value = settings.activeTimerTimeLeft.firstOrNull() ?: 0
                if (settings.activeTimerPhase.firstOrNull() == TimerPhase.BREAK.name) {
                    ServiceLocator.timerPhase.value = TimerPhase.BREAK
                }
            }
            ServiceLocator.activeReadingSeconds.value = settings.activeTimerActiveReading.firstOrNull() ?: 0

            // Background State Fix: elapsed = now - last persisted tick
            val wasRunning = settings.activeTimerIsRunning.firstOrNull() ?: false
            if (wasRunning) {
                val lastTick = settings.activeTimerLastTickTime.firstOrNull() ?: System.currentTimeMillis()
                val missedSeconds = ((System.currentTimeMillis() - lastTick) / 1000).toInt().coerceAtLeast(0)

                if (savedMode == TimerMode.STOPWATCH.name) {
                    ServiceLocator.elapsedSeconds.value += missedSeconds
                    ServiceLocator.activeReadingSeconds.value += missedSeconds
                    setTimerRunning(true)
                } else {
                    val timeLeft = ServiceLocator.timeLeftSeconds.value
                    // Only the time up to expiry was actually spent reading.
                    ServiceLocator.activeReadingSeconds.value += missedSeconds.coerceAtMost(timeLeft)
                    if (timeLeft - missedSeconds <= 0) {
                        ServiceLocator.timeLeftSeconds.value = 0
                        setTimerRunning(false)
                        ServiceLocator.notifyTimerExpired()
                    } else {
                        ServiceLocator.timeLeftSeconds.value = timeLeft - missedSeconds
                        setTimerRunning(true)
                    }
                }
            }
        }
    }

    fun shareNote(context: Context, note: MarginNote, book: Book) {
        QuoteShareHelper.shareQuote(context, note, book)
    }

    fun addScannedBook(
        scanned: BookMetadata,
        status: BookStatus = BookStatus.TO_READ,
        format: BookFormat = BookFormat.Paperback
    ) {
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
                loanRecord = scanned.loanRecord,
                isbn = scanned.isbn,
                publisher = scanned.publisher
            )
        }
    }

    /** onComplete receives the result, or null when the file couldn't be read. */
    fun importCsv(context: Context, uri: android.net.Uri, onComplete: (CsvImportResult?) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { repository.importCsv(context, uri) }.getOrNull()
            onComplete(result)
        }
    }

    /**
     * Imports a Bookmory backup into the library through the normal backup merge,
     * so anything already imported is left alone. Fails with
     * [BookmoryImporter.NotABookmoryBackup] for files that aren't one.
     */
    fun importBookmory(context: Context, uri: android.net.Uri, onComplete: (Result<BookmoryImportSummary>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val import = BookmoryImporter.read(context, uri, repository.snapshotForBackup())
                repository.restoreFromBackup(import.snapshot.copy(sessions = emptyList()))
                repository.upsertSessions(import.snapshot.sessions)
                // Same day boundary as Bookmory, so the imported streak reads the same.
                import.summary.dayStartHour?.let { settings.setDayStartsAtHour(it) }
                import.summary
            }
            onComplete(result)
        }
    }

    fun promote(book: Book) {
        val next = when (book.status.uppercase()) {
            BookStatus.TO_READ.name, "BACKLOG" -> BookStatus.SHORTLIST
            BookStatus.SHORTLIST.name -> BookStatus.UP_NEXT
            BookStatus.UP_NEXT.name, BookStatus.PAUSED.name -> BookStatus.READING
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

    /**
     * Moves a book to any pipeline status. Finishing and abandoning carry their own
     * payloads (rating, reason), so callers route those through [finishBook]/[markDnf].
     */
    fun changeStatus(book: Book, status: BookStatus) {
        viewModelScope.launch {
            when {
                status == BookStatus.READING && (book.status == BookStatus.FINISHED.name || book.status == BookStatus.DNF.name) ->
                    repository.restartReading(book.id)
                else -> repository.updateStatus(book.id, status)
            }
        }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch { repository.toggleFavorite(book.id) }
    }

    fun addProgress(book: Book, delta: Int) {
        viewModelScope.launch { repository.addProgress(book.id, delta) }
    }

    fun recordLoan(book: Book, loan: LoanRecord) {
        // One active loan at a time: saving over an open loan replaces it rather
        // than stacking a second "active" record.
        val updatedLoans = book.loanRecord.filterNot { it.isActive } + loan
        viewModelScope.launch {
            repository.updateBook(book.copy(loanRecord = updatedLoans, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun markLoanReturned(book: Book) {
        val now = System.currentTimeMillis()
        val updatedLoans = book.loanRecord.map {
            if (it.isActive) it.copy(returnDate = now) else it
        }
        viewModelScope.launch {
            repository.updateBook(book.copy(loanRecord = updatedLoans, lastUpdated = System.currentTimeMillis()))
        }
    }

    /** Saves edited book details. Progress is clamped to the (possibly new) page count. */
    fun updateBookDetails(book: Book) {
        val ceiling = if (book.totalPages > 0) book.totalPages else Int.MAX_VALUE
        viewModelScope.launch {
            repository.updateBook(
                book.copy(
                    currentPage = book.currentPage.coerceIn(0, ceiling),
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun setFormat(book: Book, format: String) {
        viewModelScope.launch { repository.updateFormat(book.id, format) }
    }

    fun setOwnership(book: Book, ownership: Ownership) {
        viewModelScope.launch { repository.updateOwnership(book.id, ownership) }
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

    /**
     * Opens the focus timer on [book]. Switching from another book's timer starts a
     * fresh clock — the old book's elapsed time must not carry over to this one.
     */
    fun startFocus(book: Book) {
        val switching = ServiceLocator.timerBookId.value != book.id
        if (switching) ServiceLocator.timerIsRunning.value = false
        viewModelScope.launch {
            // Starting this book's session closes any other open one; that runs while
            // the timer still points at the old book so its measured time is kept.
            repository.startSession(book.id, null)
            if (switching) {
                soundscapeEngine.stop()
                ServiceLocator.activeReadingSeconds.value = 0
                ServiceLocator.elapsedSeconds.value = 0
                ServiceLocator.timerPhase.value = TimerPhase.WORK
                val workMins = ServiceLocator.customWorkMinutes.value ?: workMinutes.value
                ServiceLocator.timeLeftSeconds.value = workMins * 60
                ServiceLocator.consumeTimerExpired()
            }
            setTimerBook(book.id, book.title)
            TimerPersistence.save(appContext)
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

    private val _lastSessionResult = MutableStateFlow<SessionResult?>(null)
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
            if (ServiceLocator.timerBookId.value == book.id) setTimerBook(null)
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
            if (ServiceLocator.timerBookId.value == bookId || ServiceLocator.timerBookId.value == null) {
                ServiceLocator.activeReadingSeconds.value = 0
                setTimerBook(null)
            }
        }
    }

    /**
     * Leaving the timer without ever pressing Start: drop the empty session, the
     * idle mini player, and the Reading status the open session gave the book.
     */
    fun leaveUnstartedFocus(book: Book, previousStatus: String) {
        viewModelScope.launch {
            repository.observeOpenSession().firstOrNull()?.takeIf { it.bookId == book.id }?.let {
                repository.deleteSession(it.id)
            }
            if (ServiceLocator.timerBookId.value == book.id) {
                ServiceLocator.activeReadingSeconds.value = 0
                setTimerBook(null)
            }
            val restore = if (previousStatus == "BACKLOG") BookStatus.TO_READ
            else BookStatus.entries.find { it.name == previousStatus }
            if (restore != null && restore != BookStatus.READING) repository.updateStatus(book.id, restore)
        }
    }

    fun logManualSession(bookId: String, startPage: Int, endPage: Int, durationMinutes: Int, endTime: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.addManualSession(bookId, startPage, endPage, durationMinutes * 60_000L, endTime)
        }
    }

    fun delete(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book.id)?.let { recentlyDeleted[book.id] = it }
            if (ServiceLocator.timerBookId.value == book.id) setTimerBook(null)
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch { repository.deleteSession(session.id) }
    }

    fun updateSession(session: Session) {
        viewModelScope.launch { repository.updateSession(session) }
    }

    /** Undo for [delete]: brings back the book with its sessions and notes. */
    fun restore(book: Book) {
        viewModelScope.launch {
            val deleted = recentlyDeleted.remove(book.id) ?: DeletedBook(book, emptyList(), emptyList())
            repository.restoreDeleted(deleted)
        }
    }

    fun setHomeLayout(layout: HomeLayout) {
        viewModelScope.launch { settings.setHomeLayout(layout) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { settings.setUserName(name.trim().ifBlank { "Reader" }) }
    }

    fun setDailyGoal(pages: Int) {
        viewModelScope.launch { settings.setDailyGoal(pages) }
    }

    fun setYearlyGoal(books: Int) {
        viewModelScope.launch { settings.setYearlyGoal(books) }
    }

    fun setMonthlyGoalPages(pages: Int) {
        viewModelScope.launch { settings.setMonthlyGoalPages(pages) }
    }

    fun setMonthlyGoalBooks(books: Int) {
        viewModelScope.launch { settings.setMonthlyGoalBooks(books) }
    }

    fun setDndDuringSession(enabled: Boolean) {
        viewModelScope.launch { settings.setDndDuringSession(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode.ordinal) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setUseDynamicColor(enabled) }
    }

    fun setAppPalette(palette: AppPalette) {
        viewModelScope.launch { settings.setAppPalette(palette) }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch { settings.setReminder(enabled, hour, minute) }
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
    fun setTimerBook(bookId: String?, bookTitle: String? = null) {
        ServiceLocator.timerBookId.value = bookId
        ServiceLocator.timerBookTitle.value = bookTitle
        if (bookId == null) {
            ServiceLocator.timerIsRunning.value = false
            ServiceLocator.consumeTimerExpired()
            soundscapeEngine.stop()
            viewModelScope.launch {
                settings.clearTimerState()
            }
        }
    }

    fun setTimerPhase(phase: TimerPhase) {
        ServiceLocator.timerPhase.value = phase
        if (ServiceLocator.timerMode.value == TimerMode.COUNTDOWN) {
            val workMins = ServiceLocator.customWorkMinutes.value ?: workMinutes.value
            ServiceLocator.timeLeftSeconds.value = if (phase == TimerPhase.WORK) workMins * 60 else breakMinutes.value * 60
        }
    }

    fun setTimerMode(mode: TimerMode) {
        if (ServiceLocator.timerMode.value == mode) return
        ServiceLocator.timerMode.value = mode
        if (mode == TimerMode.STOPWATCH) {
            ServiceLocator.elapsedSeconds.value = 0
        } else {
            val workMins = ServiceLocator.customWorkMinutes.value ?: workMinutes.value
            ServiceLocator.timeLeftSeconds.value = workMins * 60
        }
        persistTimer()
    }

    fun setCustomWorkMinutes(mins: Int?) {
        ServiceLocator.customWorkMinutes.value = mins
        if (ServiceLocator.timerMode.value == TimerMode.COUNTDOWN &&
            ServiceLocator.timerPhase.value == TimerPhase.WORK &&
            !ServiceLocator.timerIsRunning.value) {
            ServiceLocator.timeLeftSeconds.value = (mins ?: workMinutes.value) * 60
        }
    }

    /** Length of the current countdown phase in seconds, for progress rings. */
    fun countdownTotalSeconds(): Int {
        val minutes = if (ServiceLocator.timerPhase.value == TimerPhase.BREAK) {
            breakMinutes.value
        } else {
            ServiceLocator.customWorkMinutes.value ?: workMinutes.value
        }
        return (minutes * 60).coerceAtLeast(1)
    }

    fun setTimerRunning(isRunning: Boolean) {
        // A finished countdown can't resume from 00:00; the reader saves it instead.
        if (isRunning && ServiceLocator.timerMode.value == TimerMode.COUNTDOWN && ServiceLocator.timeLeftSeconds.value <= 0) {
            return
        }
        ServiceLocator.timerIsRunning.value = isRunning
        if (isRunning) {
            if (_noiseType.value != NoiseType.NONE) soundscapeEngine.play(_noiseType.value)
        } else {
            soundscapeEngine.stop()
        }
        persistTimer()
    }

    private fun persistTimer() {
        viewModelScope.launch { TimerPersistence.save(appContext) }
    }

    fun setWorkMinutes(mins: Int) {
        viewModelScope.launch { settings.setWorkMinutes(mins) }
        ServiceLocator.customWorkMinutes.value = null
        if (ServiceLocator.timerPhase.value == TimerPhase.WORK && !ServiceLocator.timerIsRunning.value) {
            ServiceLocator.timeLeftSeconds.value = mins * 60
        }
    }

    fun setBreakMinutes(mins: Int) {
        viewModelScope.launch { settings.setBreakMinutes(mins) }
        if (ServiceLocator.timerPhase.value == TimerPhase.BREAK && !ServiceLocator.timerIsRunning.value) {
            ServiceLocator.timeLeftSeconds.value = mins * 60
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
        if (ServiceLocator.timerIsRunning.value) {
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
        type: NoteType = NoteType.BOOK_CONTENT,
        images: List<String> = emptyList()
    ) {
        viewModelScope.launch { repository.addNote(bookId, page, text, type, images) }
    }

    fun updateNote(noteId: String, page: Int, text: String, type: NoteType, images: List<String>) {
        viewModelScope.launch { repository.updateNote(noteId, page, text, type, images) }
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
                    settings = BackupEngine.BackupSettings(
                        dailyGoal = settings.dailyGoal.first(),
                        yearlyGoal = settings.yearlyGoal.first(),
                        monthlyGoalPages = settings.monthlyGoalPages.first(),
                        monthlyGoalBooks = settings.monthlyGoalBooks.first(),
                        dayStartsAtHour = settings.dayStartsAtHour.first(),
                        homeLayout = settings.homeLayout.first().encode()
                    )
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
                parsed.settings.dailyGoal?.let { settings.setDailyGoal(it) }
                parsed.settings.yearlyGoal?.let { settings.setYearlyGoal(it) }
                parsed.settings.monthlyGoalPages?.let { settings.setMonthlyGoalPages(it) }
                parsed.settings.monthlyGoalBooks?.let { settings.setMonthlyGoalBooks(it) }
                parsed.settings.dayStartsAtHour?.let { settings.setDayStartsAtHour(it) }
                parsed.settings.homeLayout?.let { settings.setHomeLayout(HomeLayout.decode(it)) }
                onDone(parsed.snapshot.books.size)
            } catch (e: Exception) {
                onDone(-1)
            }
        }
    }

    override fun onCleared() {
        soundscapeEngine.stop()
        super.onCleared()
    }

    companion object {
        private const val STALE_TIMER_MS = 12 * 60 * 60 * 1000L

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
