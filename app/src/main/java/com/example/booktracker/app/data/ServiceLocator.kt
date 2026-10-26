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

    private val _timerControlEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 5)
    val timerControlEvents: kotlinx.coroutines.flow.SharedFlow<String> = _timerControlEvents

    // Global Timer State
    val timerBookId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val timerBookTitle = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val timerPhase = kotlinx.coroutines.flow.MutableStateFlow(com.example.booktracker.app.ui.TimerPhase.WORK)
    val timerMode = kotlinx.coroutines.flow.MutableStateFlow(com.example.booktracker.app.ui.TimerMode.COUNTDOWN)
    val timerIsRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val timeLeftSeconds = kotlinx.coroutines.flow.MutableStateFlow(25 * 60)
    val elapsedSeconds = kotlinx.coroutines.flow.MutableStateFlow(0)
    val customWorkMinutes = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    val activeReadingSeconds = kotlinx.coroutines.flow.MutableStateFlow(0)

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

    fun emitTimerControlEvent(action: String) {
        _timerControlEvents.tryEmit(action)
    }

    fun repository(context: Context): BookRepository =
        db(context).let { RoomBookRepository(it.bookDao(), it.sessionDao(), it.marginNoteDao()) }

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
