package com.example.booktracker.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.app.analytics.BadgeInput
import com.example.booktracker.app.analytics.BadgeStatus
import com.example.booktracker.app.analytics.ReviewEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.analytics.TandemEngine
import com.example.booktracker.app.sync.DriveAuth
import com.example.booktracker.app.sync.DriveClient
import com.example.booktracker.app.sync.DriveSync
import com.example.booktracker.app.worker.DriveSyncScheduler
import com.example.booktracker.app.data.AppPalette
import com.example.booktracker.app.data.BackupEngine
import com.example.booktracker.app.data.BookRepository
import com.example.booktracker.app.data.CsvImportResult
import com.example.booktracker.app.data.DeletedBook
import com.example.booktracker.app.data.HomeLayout
import com.example.booktracker.app.data.bookmory.BookmoryImportSummary
import com.example.booktracker.app.data.bookmory.BookmoryImporter
import com.example.booktracker.app.data.LibrarySearchResults
import com.example.booktracker.app.data.AvatarStore
import com.example.booktracker.app.data.NoteImageStore
import kotlinx.coroutines.Dispatchers
import com.example.booktracker.app.data.ReminderSettings
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.data.SettingsRepository
import com.example.booktracker.app.data.AppPrefs
import com.example.booktracker.app.util.CsvExporter
import com.example.booktracker.app.worker.AutoBackupWorker
import com.example.booktracker.app.data.ThemeMode
import com.example.booktracker.app.data.remote.BookMetadata
import com.example.booktracker.app.journal.NoiseType
import com.example.booktracker.app.journal.SoundscapeEngine
import com.example.booktracker.app.service.TimerPersistence
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.LoanRecord
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.ReviewState
import com.example.booktracker.shared.models.Session
import com.example.booktracker.shared.models.VocabWord
import com.example.booktracker.shared.models.WishInfo
import java.time.ZoneId
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DriveSyncState {
    data object Idle : DriveSyncState
    data object Syncing : DriveSyncState
    data class NeedsConsent(val sender: android.content.IntentSender) : DriveSyncState
    data class Done(val message: String) : DriveSyncState
    data class Failed(val message: String) : DriveSyncState
}

class BookTrackerViewModel(
    private val appContext: Context,
    private val repository: BookRepository,
    private val settings: SettingsRepository
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

    val driveSyncEnabled: StateFlow<Boolean> = settings.driveSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val driveLastSync: StateFlow<Long> = settings.driveLastSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _driveSync = MutableStateFlow<DriveSyncState>(DriveSyncState.Idle)

    /** Progress of a sync the reader started; background syncs don't show here. */
    val driveSync: StateFlow<DriveSyncState> = _driveSync

    /** A finished or failed sync's message is news once; leaving Settings clears it. */
    fun clearDriveStatus() {
        if (_driveSync.value is DriveSyncState.Done || _driveSync.value is DriveSyncState.Failed) _driveSync.value = DriveSyncState.Idle
    }

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

    val words: StateFlow<List<VocabWord>> = repository.observeWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviewStates: StateFlow<List<ReviewState>> = repository.observeReviewStates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Today's review, in order. Null until the library has loaded, so the review
     * screen can tell "nothing due" from "not read yet". Built from the database
     * flows directly for the same reason.
     */
    val reviewQueue: StateFlow<List<ReviewEngine.Item>?> = combine(
        repository.observeAllNotes(),
        repository.observeWords(),
        repository.observeReviewStates(),
        streak
    ) { notes, wordList, states, streakInfo ->
        val today = streakInfo.today.toEpochDay()
        val zone = ZoneId.systemDefault()
        val reviewedToday = ReviewEngine.reviewedOn(states, today) {
            StreakEngine.readingDate(it, zone, streakInfo.dayStartsAtHour).toEpochDay()
        }
        ReviewEngine.dailyQueue(
            items = ReviewEngine.noteItems(notes) + ReviewEngine.wordItems(wordList),
            states = states.associateBy { it.itemId },
            today = today,
            reviewedToday = reviewedToday
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val avatar: StateFlow<String> = settings.avatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val hapticsEnabled: StateFlow<Boolean> = settings.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val reduceMotion: StateFlow<Boolean> = settings.reduceMotion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setHapticsEnabled(enabled: Boolean) { viewModelScope.launch { settings.setHapticsEnabled(enabled) } }
    fun setReduceMotion(enabled: Boolean) { viewModelScope.launch { settings.setReduceMotion(enabled) } }
    fun setKeepScreenOn(enabled: Boolean) { viewModelScope.launch { settings.setKeepScreenOn(enabled) } }

    /** Eager: the activity reads the lock delay and screen flags before any screen subscribes. */
    val prefs: StateFlow<AppPrefs> = settings.prefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs())

    fun updatePrefs(transform: (AppPrefs) -> AppPrefs) { viewModelScope.launch { settings.updatePrefs(transform) } }

    /**
     * The first-run welcome: an empty library and a welcome not yet seen. False
     * until both have loaded, so it never flashes up for a reader with books.
     */
    val showWelcome: StateFlow<Boolean> = combine(repository.observeBooks(), settings.prefs) { books, prefs ->
        books.isEmpty() && !prefs.onboarded
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Readers who had books before the welcome existed never need it.
        viewModelScope.launch {
            repository.observeBooks().first { it.isNotEmpty() }
            if (!settings.prefs.first().onboarded) settings.updatePrefs { it.copy(onboarded = true) }
        }
    }

    fun finishWelcome() = updatePrefs { it.copy(onboarded = true) }

    /** Writes the library as a Goodreads-style CSV to [uri]. */
    fun exportCsv(context: Context, uri: android.net.Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val csv = CsvExporter.toGoodreadsCsv(repository.observeBooks().first(), ZoneId.systemDefault())
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } ?: error("no stream")
                }
            }.isSuccess
            onDone(ok)
        }
    }

    /** Writes the weekly automatic backup right now, into the folder the reader picked. */
    fun backupNow(context: Context, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val folder = settings.prefs.first().autoBackupFolder
            val ok = folder.isNotBlank() && runCatching {
                AutoBackupWorker.backupNow(context.applicationContext, android.net.Uri.parse(folder))
            }.isSuccess
            onDone(ok)
        }
    }

    /**
     * Deletes every book, session, note, review card and word, with their covers
     * and photos. Preferences stay. Refused while Drive sync is on, because the
     * next sync would bring the whole library straight back.
     */
    fun deleteAllData(context: Context, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (settings.driveSyncEnabled.first()) {
                onDone(false)
                return@launch
            }
            val ok = runCatching { ServiceLocator.wipeLibrary(context) }.isSuccess
            onDone(ok)
        }
    }

    // Replaying history for earned dates is a few hundred evaluations, so keep it off the main thread.
    val badgeStatuses: StateFlow<List<BadgeStatus>> = combine(
        books,
        completedSessions,
        streak,
        allNotes
    ) { b, s, st, notes ->
        val noteTimes = notes.map { it.timestamp }
        val statuses = BadgeEngine.statuses(BadgeInput(b, s, st.longestStreak, noteTimes.size))
        BadgeEngine.withEarnedDates(statuses, b, s, noteTimes, st.dayStartsAtHour)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthlyChallenge: StateFlow<BadgeStatus?> = combine(
        books,
        completedSessions,
        streak,
        allNotes
    ) { b, s, st, notes ->
        BadgeEngine.monthlyChallenge(b, s, notes.map { it.timestamp }, st.today, dayStartsAtHour = st.dayStartsAtHour)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val unlockedBadges: StateFlow<Set<String>> = badgeStatuses
        .map { list -> list.filter { it.unlocked }.map { it.badge.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _newUnlockEvents = Channel<Set<String>>(Channel.BUFFERED)
    val newUnlockEvents = _newUnlockEvents.receiveAsFlow()

    val tbrCurationBooks: StateFlow<List<Book>> = books.map { list ->
        // Wished-for books aren't on the pile until the reader has them.
        com.example.booktracker.app.analytics.WishlistEngine.onShelf(list).filter { it.status.equals(BookStatus.TO_READ.name, ignoreCase = true) || it.status.equals("BACKLOG", ignoreCase = true) }
            .sortedByDescending { it.lastUpdated }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTimerBook: StateFlow<Book?> = combine(books, ServiceLocator.timerBookId) { bookList, id ->
        bookList.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val timerMode: StateFlow<TimerMode> = ServiceLocator.timerMode.asStateFlow()
    val timerIsRunning: StateFlow<Boolean> = ServiceLocator.timerIsRunning.asStateFlow()
    val timeLeftSeconds: StateFlow<Int> = ServiceLocator.timeLeftSeconds.asStateFlow()
    val elapsedSeconds: StateFlow<Int> = ServiceLocator.elapsedSeconds.asStateFlow()

    val workMinutes: StateFlow<Int> = settings.workMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 25)

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
        // Home-screen widgets refresh from WidgetRefresher, which starts with the app.

        viewModelScope.launch {
            combine(unlockedBadges, settings.seenBadges) { unlocked, seen ->
                val newBadges = unlocked - seen
                if (newBadges.isNotEmpty()) {
                    _newUnlockEvents.trySend(newBadges)
                    settings.markBadgesSeen(newBadges)
                }
            }.collect()
        }

        // The clock also starts and stops from the notification, widgets and watch;
        // the soundscape follows it wherever the tap came from.
        viewModelScope.launch {
            ServiceLocator.timerIsRunning.collect { running ->
                if (running && _noiseType.value != NoiseType.NONE) soundscapeEngine.play(_noiseType.value)
                else soundscapeEngine.stop()
            }
        }

        // Timer recovery after process death. Skipped when the timer is already
        // live in this process (e.g. the activity was recreated while the
        // foreground service kept running) — the in-memory state is newer.
        viewModelScope.launch {
            if (ServiceLocator.timerBookId.value != null) return@launch
            val savedBookId = settings.activeTimerBookId.firstOrNull() ?: run {
                applyPreferredTimerMode()
                return@launch
            }
            // A timer left "running" for half a day (or restored from a device
            // backup) isn't a real session any more; drop it rather than resume it.
            val savedTick = settings.activeTimerLastTickTime.firstOrNull()
            val bookExists = repository.observeBooks().first().any { it.id == savedBookId }
            if (!bookExists || (savedTick != null && System.currentTimeMillis() - savedTick > STALE_TIMER_MS)) {
                settings.clearTimerState()
                applyPreferredTimerMode()
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
            }
            ServiceLocator.activeReadingSeconds.value = settings.activeTimerActiveReading.firstOrNull() ?: 0
            ServiceLocator.sessionPageTarget.value = settings.activeTimerPageTarget.firstOrNull()

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

    private val _pendingRoute = MutableStateFlow<String?>(null)

    /** A screen a widget asked to open; the nav host shows it and then consumes it. */
    val pendingRoute: StateFlow<String?> = _pendingRoute

    fun openRoute(route: String?) {
        if (!route.isNullOrBlank()) _pendingRoute.value = route
    }

    fun consumeRoute() {
        _pendingRoute.value = null
    }

    fun gradeReview(item: ReviewEngine.Item, grade: ReviewEngine.Grade) {
        viewModelScope.launch {
            val previous = repository.observeReviewStates().first().find { it.itemId == item.id }
            repository.saveReviewState(
                ReviewEngine.schedule(previous, item.id, item.kind, grade, streak.value.today.toEpochDay(), System.currentTimeMillis())
            )
        }
    }

    fun saveWord(word: VocabWord) {
        viewModelScope.launch {
            repository.saveWord(
                if (word.id.isBlank()) word.copy(id = java.util.UUID.randomUUID().toString(), addedAt = System.currentTimeMillis())
                else word
            )
        }
    }

    fun deleteWord(word: VocabWord) {
        viewModelScope.launch { repository.deleteWord(word.id) }
    }

    fun setFinishBy(book: Book, finishBy: Long?) {
        viewModelScope.launch { repository.setFinishBy(book.id, finishBy) }
    }

    /** Connects Drive the first time, then syncs; asks the UI to show Google's consent screen when needed. */
    fun syncDrive() {
        if (_driveSync.value == DriveSyncState.Syncing) return
        _driveSync.value = DriveSyncState.Syncing
        viewModelScope.launch {
            when (val outcome = DriveAuth.authorize(appContext)) {
                is DriveAuth.Outcome.Token -> runDriveSync(outcome.value)
                is DriveAuth.Outcome.NeedsConsent -> _driveSync.value = DriveSyncState.NeedsConsent(outcome.sender)
                is DriveAuth.Outcome.Failed -> _driveSync.value = DriveSyncState.Failed(outcome.message)
            }
        }
    }

    /** The consent screen was shown; keep the spinner while it is up. */
    fun driveConsentShown() {
        if (_driveSync.value is DriveSyncState.NeedsConsent) _driveSync.value = DriveSyncState.Syncing
    }

    fun onDriveConsent(data: android.content.Intent?) {
        val token = DriveAuth.fromConsent(appContext, data)
        if (token == null) {
            _driveSync.value = DriveSyncState.Failed("Drive access wasn't granted, so nothing was synced.")
            return
        }
        viewModelScope.launch { runDriveSync(token) }
    }

    private suspend fun runDriveSync(token: String) {
        _driveSync.value = DriveSyncState.Syncing
        runCatching { DriveSync.run(appContext, token) }
            .onSuccess { summary ->
                settings.setDriveSyncEnabled(true)
                settings.setDriveLastSync(System.currentTimeMillis())
                DriveSyncScheduler.apply(appContext, enabled = true)
                _driveSync.value = DriveSyncState.Done(
                    if (summary.changedHere == 0) "Everything was already up to date."
                    else "Synced. ${summary.changedHere} change${if (summary.changedHere == 1) "" else "s"} came in from your other devices."
                )
            }
            .onFailure { error ->
                _driveSync.value = DriveSyncState.Failed(
                    when ((error as? DriveClient.DriveException)?.code) {
                        401, 403 -> "Google turned down the Drive request. Try connecting again."
                        null -> error.message?.takeIf { "Another device" in it } ?: "Couldn't reach Google Drive. Check your connection."
                        else -> "Drive had a problem (${error.code}). Try again later."
                    }
                )
            }
    }

    /** Stops syncing on this phone. What's already in Drive stays there until the reader removes it. */
    fun disconnectDrive() {
        viewModelScope.launch {
            settings.setDriveSyncEnabled(false)
            DriveSyncScheduler.apply(appContext, enabled = false)
            DriveSync.reset(appContext)
            _driveSync.value = DriveSyncState.Idle
        }
    }

    fun setAudiobook(book: Book, minutes: Int, speed: Float) {
        viewModelScope.launch { repository.setAudiobook(book.id, minutes, speed) }
    }

    /** Listening to the audiobook edition: logged as a session that moves the page by the narration covered. */
    fun logListening(book: Book, minutes: Int) {
        if (minutes <= 0 || !TandemEngine.isTandem(book)) return
        val endPage = TandemEngine.pageAfterListening(book, minutes)
        logManualSession(book.id, book.currentPage, endPage, minutes)
    }

    /** Jumps both editions to where the audiobook player says the reader is. */
    fun setAudioPosition(book: Book, position: Int) {
        setProgress(book, TandemEngine.pageForAudio(book, position))
    }

    /** Books picked from a shelf photo, added in one go; marked owned when the reader said so. */
    fun addShelfBooks(found: List<BookMetadata>, status: BookStatus, owned: Boolean) {
        viewModelScope.launch {
            found.forEach { scanned ->
                val added = repository.addBook(
                    title = scanned.title,
                    authors = scanned.authors,
                    totalPages = scanned.pageCount,
                    coverUrl = scanned.coverUrl,
                    description = scanned.description,
                    genres = scanned.genres,
                    publishedDate = scanned.publishedDate,
                    status = status,
                    creators = scanned.creators,
                    isbn = scanned.isbn,
                    publisher = scanned.publisher
                )
                if (owned) repository.updateOwnership(added.id, Ownership.OWNED)
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

    /**
     * Renames [from] on every book that has it, merging into [to] if a tag of that name
     * already exists, or removing it when [to] is blank. Touched books get a new
     * lastUpdated so the change wins when synced.
     */
    fun tidyTag(from: String, to: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            com.example.booktracker.app.analytics.TagTidy.apply(books.value, from, to)
                .forEach { repository.updateBook(it.copy(lastUpdated = now)) }
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
        viewModelScope.launch {
            val current = repository.observeBooks().first().find { it.id == book.id } ?: book
            // Going on the wishlist starts its details, so the page can sort by when it was added.
            if (ownership == Ownership.WISHLIST && current.wish == null) {
                val now = System.currentTimeMillis()
                repository.updateBook(current.copy(ownership = ownership, wish = WishInfo(addedAt = now), lastUpdated = now))
            } else {
                repository.updateOwnership(book.id, ownership)
            }
        }
    }

    /** Priority, expected price, store, who recommended it and when it comes out. */
    fun setWish(book: Book, wish: WishInfo) {
        viewModelScope.launch {
            val current = repository.observeBooks().first().find { it.id == book.id } ?: book
            repository.updateBook(current.copy(wish = wish, lastUpdated = System.currentTimeMillis()))
        }
    }

    /**
     * "Got it": off the wishlist and onto the shelf. With a [purchase] it was
     * bought and the purchase is logged; without one it was a gift.
     */
    fun gotWished(book: Book, purchase: com.example.booktracker.shared.models.PurchaseLog?) {
        viewModelScope.launch {
            val current = repository.observeBooks().first().find { it.id == book.id } ?: book
            repository.updateBook(
                current.copy(
                    ownership = if (purchase != null) Ownership.OWNED else Ownership.GIFT,
                    purchaseLog = current.purchaseLog + listOfNotNull(purchase),
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    /** Adds a searched or scanned book straight to the wishlist, on the to-read pile. */
    fun addToWishlist(scanned: BookMetadata, format: BookFormat, onAdded: (Book) -> Unit = {}) {
        viewModelScope.launch {
            val added = repository.addBook(
                title = scanned.title,
                authors = scanned.authors,
                totalPages = scanned.pageCount,
                coverUrl = scanned.coverUrl,
                description = scanned.description,
                genres = scanned.genres,
                publishedDate = scanned.publishedDate,
                status = BookStatus.TO_READ,
                format = format,
                creators = scanned.creators,
                isbn = scanned.isbn,
                publisher = scanned.publisher
            )
            val wished = added.copy(ownership = Ownership.WISHLIST, wish = WishInfo(addedAt = added.dateAdded))
            repository.updateBook(wished)
            onAdded(wished)
        }
    }

    /** Replaces the book's purchase records; a first purchase also marks an unset book as owned. */
    fun setPurchases(book: Book, purchases: List<com.example.booktracker.shared.models.PurchaseLog>) {
        viewModelScope.launch {
            val current = repository.observeBooks().first().find { it.id == book.id } ?: book
            repository.updateBook(
                current.copy(
                    purchaseLog = purchases,
                    ownership = if (purchases.isNotEmpty() && current.ownership == Ownership.NONE) Ownership.OWNED else current.ownership,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
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
                ServiceLocator.timerPausedAt.value = null
                ServiceLocator.sessionPageTarget.value = null
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
        val endPage: Int,
        /** So "your usual pace" can leave this session out. */
        val sessionId: String = "",
        val focus: Int = 0
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
        focus: Int = 0,
        /** A line copied from the book on the save screen; kept as a note on [endPage]. */
        quote: String = "",
        /** An earlier session to join this one onto; see [BookRepository.endSessionDetailed]. */
        mergeWithId: String? = null,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val saved = repository.endSessionDetailed(
                bookId = book.id,
                endPage = endPage,
                environmentTag = environmentTag,
                startTime = startTime,
                endTime = endTime,
                focus = focus,
                mergeWithId = mergeWithId
            )
            if (quote.isNotBlank()) repository.addNote(book.id, endPage, quote.trim(), NoteType.BOOK_CONTENT, emptyList())
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
                endPage = saved?.endPage ?: endPage,
                sessionId = saved?.id.orEmpty(),
                focus = saved?.focus ?: focus
            )
            if (ServiceLocator.timerBookId.value == book.id) setTimerBook(null)
            onSaved()
        }
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

    /** Sets the profile picture to a picked photo; onDone(false) if it couldn't be read. */
    fun setAvatarPhoto(uri: android.net.Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val stored = AvatarStore.importPhoto(appContext, uri)
            if (stored != null) settings.setAvatar(stored)
            onDone(stored != null)
        }
    }

    fun setAvatarCover(bookId: String) {
        viewModelScope.launch {
            AvatarStore.clearPhoto(appContext)
            settings.setAvatar(AvatarStore.COVER_PREFIX + bookId)
        }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            AvatarStore.clearPhoto(appContext)
            settings.setAvatar("")
        }
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
            ServiceLocator.timerPausedAt.value = null
            ServiceLocator.sessionPageTarget.value = null
            ServiceLocator.consumeTimerExpired()
            soundscapeEngine.stop()
            viewModelScope.launch {
                settings.clearTimerState()
            }
        }
    }

    /**
     * With no timer to resume, open the way the reader last chose: count-up, or a
     * countdown of their last length. Only an explicit pick in the timer sheet
     * changes the preference (see [setTimerMode]).
     */
    private suspend fun applyPreferredTimerMode() {
        if (settings.preferredTimerMode.first() == TimerMode.COUNTDOWN.name) {
            ServiceLocator.timerMode.value = TimerMode.COUNTDOWN
            ServiceLocator.timeLeftSeconds.value = (ServiceLocator.customWorkMinutes.value ?: settings.workMinutes.first()) * 60
        } else {
            ServiceLocator.timerMode.value = TimerMode.STOPWATCH
            ServiceLocator.elapsedSeconds.value = 0
        }
    }

    fun setTimerMode(mode: TimerMode) {
        // Remembered even if it's already the current mode, so re-picking it sticks.
        viewModelScope.launch { settings.setPreferredTimerMode(mode.name) }
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
        if (ServiceLocator.timerMode.value == TimerMode.COUNTDOWN && !ServiceLocator.timerIsRunning.value) {
            ServiceLocator.timeLeftSeconds.value = (mins ?: workMinutes.value) * 60
        }
    }

    /** Length of the countdown in seconds, for progress rings. */
    fun countdownTotalSeconds(): Int =
        ((ServiceLocator.customWorkMinutes.value ?: workMinutes.value) * 60).coerceAtLeast(1)

    fun setTimerRunning(isRunning: Boolean) {
        // A finished countdown can't resume from 00:00; the reader saves it instead.
        if (isRunning && ServiceLocator.timerMode.value == TimerMode.COUNTDOWN && ServiceLocator.timeLeftSeconds.value <= 0) {
            return
        }
        ServiceLocator.setTimerRunning(isRunning)
        persistTimer()
    }

    /**
     * Stops a clock nobody answered "still reading?" for, and takes back the time
     * spent waiting for that answer: it's the one stretch known not to be reading.
     */
    fun pauseUnattended(waitedSeconds: Int) {
        setTimerRunning(false)
        ServiceLocator.activeReadingSeconds.value = (ServiceLocator.activeReadingSeconds.value - waitedSeconds).coerceAtLeast(0)
        if (ServiceLocator.timerMode.value == TimerMode.STOPWATCH) {
            ServiceLocator.elapsedSeconds.value = (ServiceLocator.elapsedSeconds.value - waitedSeconds).coerceAtLeast(0)
        } else {
            ServiceLocator.timeLeftSeconds.value = (ServiceLocator.timeLeftSeconds.value + waitedSeconds).coerceAtMost(countdownTotalSeconds())
        }
        persistTimer()
    }

    /** The page this session aims for, or null to read without a page goal. */
    fun setPageTarget(page: Int?) {
        ServiceLocator.sessionPageTarget.value = page
        persistTimer()
    }

    private fun persistTimer() {
        viewModelScope.launch { TimerPersistence.save(appContext) }
    }

    fun setWorkMinutes(mins: Int) {
        viewModelScope.launch { settings.setWorkMinutes(mins) }
        ServiceLocator.customWorkMinutes.value = null
        if (!ServiceLocator.timerIsRunning.value) {
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
                    settings = settings.backupSettings()
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
                parsed.settings.avatar?.let { settings.setAvatar(it) }
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
                        ServiceLocator.settings(appContext)
                    )
                }
            }
        }
    }
}
