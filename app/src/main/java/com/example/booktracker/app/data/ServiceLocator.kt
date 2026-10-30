package com.example.booktracker.app.data

import android.content.Context
import androidx.room.Room
import com.example.booktracker.app.data.local.AppDatabase

/**
 * Minimal manual DI. Both the UI (via ViewModel) and WearSyncService — which
 * runs without the Activity — resolve the repository through here, so they
 * share one database instance.
 */
object ServiceLocator {
    @Volatile
    private var database: AppDatabase? = null

    // Global Timer State
    val timerBookId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val timerBookTitle = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    // Count-up until the saved preference loads (see BookTrackerViewModel.applyPreferredTimerMode).
    val timerMode = kotlinx.coroutines.flow.MutableStateFlow(com.example.booktracker.app.ui.TimerMode.STOPWATCH)
    val timerIsRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val timeLeftSeconds = kotlinx.coroutines.flow.MutableStateFlow(25 * 60)
    val elapsedSeconds = kotlinx.coroutines.flow.MutableStateFlow(0)
    val customWorkMinutes = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    /** Time added to this session's countdown with "+5 min"; cleared when a session starts or ends. */
    val countdownExtraSeconds = kotlinx.coroutines.flow.MutableStateFlow(0)
    val activeReadingSeconds = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** The page this session aims to reach; null without a page goal. */
    val sessionPageTarget = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    /** When the running clock was last paused; null while it runs or before it starts. */
    val timerPausedAt = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)

    /** The one place the clock starts and stops, so the pause time is always recorded. */
    fun setTimerRunning(running: Boolean) {
        if (timerIsRunning.value && !running) timerPausedAt.value = System.currentTimeMillis()
        if (running) timerPausedAt.value = null
        timerIsRunning.value = running
    }

    /** True while MainActivity is started; the timer only alerts when the reader has left the app. */
    val appInForeground = kotlinx.coroutines.flow.MutableStateFlow(false)

    // Countdown expiry. Replays the latest event so it isn't lost when it fires
    // with no screen collecting (app backgrounded, or the timer screen closed);
    // the consumer clears it with [consumeTimerExpired] once handled.
    private val _timerExpiredEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val timerExpiredEvents: kotlinx.coroutines.flow.SharedFlow<Unit> = _timerExpiredEvents

    fun notifyTimerExpired() {
        _timerExpiredEvents.tryEmit(Unit)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun consumeTimerExpired() {
        _timerExpiredEvents.resetReplayCache()
    }

    fun repository(context: Context): BookRepository =
        db(context).let { RoomBookRepository(it.bookDao(), it.sessionDao(), it.marginNoteDao(), it.reviewDao(), it.vocabDao()) }

    fun settings(context: Context): SettingsRepository =
        SettingsRepository(context.applicationContext)

    @Volatile
    private var dndManager: DndManager? = null

    fun dndManager(context: Context): DndManager =
        dndManager ?: synchronized(this) {
            dndManager ?: DndManager(
                context.applicationContext,
                repository(context),
                settings(context)
            ).also { dndManager = it }
        }

    /** Empties the library: every table, cover and note photo, and Drive sync's merge base. Preferences stay. */
    suspend fun wipeLibrary(context: Context) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        db(context).clearAllTables()
        listOf("covers", NoteImageStore.DIR, "sync").forEach { java.io.File(context.filesDir, it).deleteRecursively() }
    }

    private fun db(context: Context): AppDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "booktracker.db"
            )
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .build().also { database = it }
        }
}
