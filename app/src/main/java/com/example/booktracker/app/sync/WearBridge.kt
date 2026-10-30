package com.example.booktracker.app.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.booktracker.app.analytics.FinishByPlanner
import com.example.booktracker.app.analytics.StreakEngine
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.toArgb
import com.example.booktracker.app.data.AppPalette
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.service.TimerPersistence
import com.example.booktracker.app.ui.TimerMode
import com.example.booktracker.app.ui.TimerPhase
import com.example.booktracker.shared.Constants
import com.example.booktracker.shared.models.BookStatus
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Keeps the watch in step with the phone for as long as the app process lives,
 * which covers the UI, the timer service and widget taps, so the watch's controls
 * still work with the app closed. DataClient persists each item and delivers it
 * whenever the watch is reachable, so this is safe with no watch paired.
 */
object WearBridge {

    private const val TAG = "WearBridge"

    /** How far a running clock's anchor may wander before the watch is told again. */
    private const val ANCHOR_SLACK_MS = 2_000L

    private data class WatchBook(
        val id: String,
        val title: String,
        val current: Int,
        val total: Int,
        val updatedAt: Long,
        val plan: FinishByPlanner.Plan?
    )

    private data class Shelf(
        val books: List<WatchBook>,
        val dailyGoal: Int,
        val pagesToday: Int,
        val dayEndsAt: Long,
        val streak: Int,
        val accent: Long,
        val accentSecondary: Long
    )

    private data class TimerSnapshot(
        val bookId: String?,
        val running: Boolean,
        val mode: TimerMode,
        val phase: TimerPhase,
        val seconds: Int,
        val anchor: Long
    ) {
        /** Same clock as [other] once the ticking is set aside. */
        fun sameAs(other: TimerSnapshot): Boolean =
            bookId == other.bookId && running == other.running && mode == other.mode && phase == other.phase &&
                if (running) abs(anchor - other.anchor) < ANCHOR_SLACK_MS else seconds == other.seconds
    }

    fun start(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        val repository = ServiceLocator.repository(app)
        val settings = ServiceLocator.settings(app)

        scope.launch {
            combine(
                repository.observeBooks(),
                repository.observeCompletedSessions(),
                settings.dailyGoal,
                settings.dayStartsAtHour,
                combine(settings.appPalette, settings.useDynamicColor) { palette, dynamic -> accentsFor(app, palette, dynamic) }
            ) { books, sessions, goal, dayStart, accents ->
                val streak = StreakEngine.compute(sessions, goal, dayStartsAtHour = dayStart)
                Shelf(
                    books = books.filter { it.status == BookStatus.READING.name }.map {
                        WatchBook(
                            it.id, it.title, it.currentPage, it.totalPages, it.lastUpdated,
                            plan = FinishByPlanner.plan(it, sessions, streak.today, dayStartsAtHour = dayStart)
                        )
                    },
                    dailyGoal = streak.dailyGoal,
                    pagesToday = streak.pagesToday,
                    streak = streak.currentStreak,
                    dayEndsAt = streak.today.plusDays(1).atTime(dayStart, 0)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    accent = accents.first,
                    accentSecondary = accents.second
                )
            }.distinctUntilChanged().collect { publishShelf(app, it) }
        }

        // The service ticks these every second; the watch only hears when the clock
        // starts, stops, switches or jumps, and counts the seconds itself.
        scope.launch {
            combine(
                combine(ServiceLocator.timerIsRunning, ServiceLocator.timeLeftSeconds, ServiceLocator.elapsedSeconds) { running, left, elapsed ->
                    Triple(running, left, elapsed)
                },
                ServiceLocator.timerMode,
                ServiceLocator.timerPhase,
                ServiceLocator.timerBookId
            ) { (running, left, elapsed), mode, phase, bookId ->
                val countdown = mode == TimerMode.COUNTDOWN
                val seconds = if (countdown) left else elapsed
                val now = System.currentTimeMillis()
                TimerSnapshot(
                    bookId = bookId,
                    running = running && bookId != null,
                    mode = mode,
                    phase = phase,
                    seconds = seconds,
                    anchor = if (countdown) now + seconds * 1000L else now - seconds * 1000L
                )
            }.distinctUntilChanged { old, new -> old.sameAs(new) }
                .collect { publishTimer(app, it) }
        }
    }

    /**
     * The colours the phone app is actually wearing: the wallpaper's when dynamic
     * colour is on, otherwise the chosen palette. Dark-theme tones either way, since
     * the watch is always on black.
     */
    private fun accentsFor(context: Context, palette: AppPalette, dynamic: Boolean): Pair<Long, Long> {
        if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = dynamicDarkColorScheme(context)
            return scheme.primary.toArgb().toLong() to scheme.secondary.toArgb().toLong()
        }
        return palette.primaryHex to palette.secondaryHex
    }

    /** Pause or resume asked for on the watch: the same rules as the notification's buttons. */
    suspend fun applyTimerControl(context: Context, action: String) {
        if (ServiceLocator.timerBookId.value == null) return
        val running = ServiceLocator.timerIsRunning.value
        when (action) {
            Constants.TIMER_ACTION_PAUSE -> if (running) ServiceLocator.setTimerRunning(false) else return
            Constants.TIMER_ACTION_RESUME -> {
                val exhausted = ServiceLocator.timerMode.value == TimerMode.COUNTDOWN && ServiceLocator.timeLeftSeconds.value <= 0
                if (!running && !exhausted) ServiceLocator.setTimerRunning(true) else return
            }
            else -> return
        }
        TimerPersistence.save(context)
    }

    private suspend fun publishShelf(context: Context, shelf: Shelf) {
        val request = PutDataMapRequest.create(Constants.ACTIVE_BOOKS_PATH).apply {
            dataMap.putDataMapArrayList(Constants.KEY_BOOKS_LIST, ArrayList(shelf.books.map { book ->
                DataMap().apply {
                    putString(Constants.KEY_BOOK_ID, book.id)
                    putString(Constants.KEY_TITLE, book.title)
                    putInt(Constants.KEY_CURRENT_UNIT, book.current)
                    putInt(Constants.KEY_TOTAL_UNITS, book.total)
                    putLong(Constants.KEY_UPDATED_AT, book.updatedAt)
                    book.plan?.let { plan ->
                        putInt(Constants.KEY_PLAN_PER_DAY, plan.pagesPerDay)
                        putInt(Constants.KEY_PLAN_READ_TODAY, plan.readToday)
                        putLong(Constants.KEY_PLAN_TARGET_DAY, plan.target.toEpochDay())
                        putString(Constants.KEY_PLAN_STATUS, plan.status.name)
                    }
                }
            }))
            dataMap.putInt(Constants.KEY_DAILY_GOAL, shelf.dailyGoal)
            dataMap.putInt(Constants.KEY_PAGES_TODAY, shelf.pagesToday)
            dataMap.putLong(Constants.KEY_DAY_ENDS_AT, shelf.dayEndsAt)
            dataMap.putInt(Constants.KEY_STREAK, shelf.streak)
            dataMap.putLong(Constants.KEY_ACCENT, shelf.accent)
            dataMap.putLong(Constants.KEY_ACCENT_SECONDARY, shelf.accentSecondary)
        }.asPutDataRequest().setUrgent()
        put(context, request, "active books")
    }

    private suspend fun publishTimer(context: Context, timer: TimerSnapshot) {
        val request = PutDataMapRequest.create(Constants.TIMER_STATE_PATH).apply {
            dataMap.putBoolean(Constants.KEY_TIMER_RUNNING, timer.running)
            dataMap.putInt(Constants.KEY_TIMER_SECONDS, timer.seconds)
            dataMap.putLong(Constants.KEY_TIMER_ANCHOR, if (timer.running) timer.anchor else 0L)
            dataMap.putString(Constants.KEY_TIMER_PHASE, timer.phase.name)
            dataMap.putString(Constants.KEY_TIMER_MODE, timer.mode.name)
            timer.bookId?.let { dataMap.putString(Constants.KEY_TIMER_BOOK_ID, it) }
        }.asPutDataRequest().setUrgent()
        put(context, request, "timer state")
    }

    private suspend fun put(context: Context, request: com.google.android.gms.wearable.PutDataRequest, what: String) {
        try {
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish $what to wearable", e)
        }
    }
}
