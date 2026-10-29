package com.example.booktracker.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.booktracker.app.MainActivity
import com.example.booktracker.app.R
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.notifications.Notifications
import com.example.booktracker.app.ui.TimerMode
import com.example.booktracker.app.ui.TimerPhase
import com.example.booktracker.app.widget.DeepLink
import com.example.booktracker.app.widget.WidgetArt
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Set once a stop is requested so late collector emissions can't re-post the
    // ongoing notification after it has been removed.
    @Volatile
    private var stopping = false

    /** The timer book's position and goal, for the notification's text, bar and alert. */
    private data class SessionInfo(
        val bookId: String,
        val unit: String,
        val currentPage: Int,
        val totalPages: Int,
        val startPage: Int,
        val target: Int?,
        val coverUrl: String
    ) {
        val delta: Int get() = (currentPage - startPage).coerceAtLeast(0)
        val goalPages: Int? get() = target?.let { (it - startPage).coerceAtLeast(1) }
        val goalReached: Boolean get() = target != null && currentPage >= target
    }

    @Volatile
    private var info: SessionInfo? = null

    @Volatile
    private var cover: Bitmap? = null
    private var coverUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)

        serviceScope.launch {
            var lastTickTime = System.currentTimeMillis()
            var wasRunning = false
            var lastNotified = 0L

            while (isActive) {
                val isRunning = ServiceLocator.timerIsRunning.value

                if (isRunning && !wasRunning) {
                    // Just started or resumed
                    lastTickTime = System.currentTimeMillis()
                }

                if (isRunning) {
                    val now = System.currentTimeMillis()
                    val delta = now - lastTickTime
                    if (delta >= 1000) {
                        val secondsPassed = (delta / 1000).toInt()
                        lastTickTime += secondsPassed * 1000

                        ServiceLocator.activeReadingSeconds.value += secondsPassed

                        if (ServiceLocator.timerMode.value == TimerMode.COUNTDOWN) {
                            val newTimeLeft = ServiceLocator.timeLeftSeconds.value - secondsPassed
                            if (newTimeLeft <= 0) {
                                onCountdownFinished()
                            } else {
                                ServiceLocator.timeLeftSeconds.value = newTimeLeft
                            }
                        } else {
                            ServiceLocator.elapsedSeconds.value += secondsPassed
                        }

                        // The clock in the notification ticks by itself; a redraw now and
                        // then keeps its base honest after the device sleeps.
                        if (now - lastNotified >= 60_000) {
                            lastNotified = now
                            updateNotification()
                        }
                    }
                }

                wasRunning = isRunning
                delay(if (isRunning) 200 else 500)
            }
        }

        serviceScope.launch { ServiceLocator.timerPhase.collect { updateNotification() } }
        serviceScope.launch { ServiceLocator.timerMode.collect { updateNotification() } }
        serviceScope.launch { ServiceLocator.timerBookTitle.collect { updateNotification() } }
        serviceScope.launch { ServiceLocator.timerIsRunning.collect { updateNotification() } }
        serviceScope.launch {
            ServiceLocator.settings(this@TimerService).prefs.collect {
                hideTitle = it.hideTitles
                updateNotification()
            }
        }
        serviceScope.launch {
            val repository = ServiceLocator.repository(this@TimerService)
            combine(
                ServiceLocator.timerBookId,
                repository.observeBooks(),
                repository.observeOpenSession(),
                ServiceLocator.sessionPageTarget
            ) { bookId, books, open, target ->
                val book = books.find { it.id == bookId } ?: return@combine null
                SessionInfo(
                    bookId = book.id,
                    unit = FormatAdaptabilityLayer.unitAbbrev(book),
                    currentPage = book.currentPage,
                    totalPages = book.totalPages,
                    startPage = open?.takeIf { it.bookId == book.id }?.startPage ?: book.currentPage,
                    target = target,
                    coverUrl = book.coverUrl
                )
            }.distinctUntilChanged().collect { next ->
                val previous = info
                info = next
                if (next != null && next.coverUrl != coverUrl) {
                    coverUrl = next.coverUrl
                    val px = (64 * resources.displayMetrics.density).toInt()
                    cover = WidgetArt.cover(this@TimerService, next.coverUrl, px, px, 8 * resources.displayMetrics.density)
                }
                // Out of the app the timer screen can't celebrate, so the notification does.
                if (next != null && next.goalReached && previous?.bookId == next.bookId && previous.goalReached == false) {
                    postGoalReachedAlert(next)
                }
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                stopping = false
                startForegroundService()
            }
            ACTION_STOP -> {
                stopping = true
                stopForeground(STOP_FOREGROUND_REMOVE)
                getSystemService(NotificationManager::class.java)?.apply {
                    cancel(Notifications.ID_TIMER_DONE)
                    cancel(Notifications.ID_GOAL_REACHED)
                }
                stopSelf()
            }
            ACTION_PAUSE -> {
                ServiceLocator.setTimerRunning(false)
                persistState()
                updateNotification()
            }
            ACTION_RESUME -> {
                if (!isCountdownExhausted()) {
                    ServiceLocator.setTimerRunning(true)
                    persistState()
                }
                updateNotification()
            }
            ACTION_PAGE_FORWARD -> {
                // Logged straight to the book; the timer screen and widgets pick it up from there.
                ServiceLocator.timerBookId.value?.let { bookId ->
                    serviceScope.launch { ServiceLocator.repository(applicationContext).addProgress(bookId, 1) }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun onCountdownFinished() {
        ServiceLocator.timeLeftSeconds.value = 0
        ServiceLocator.timerIsRunning.value = false
        persistState()
        ServiceLocator.notifyTimerExpired()
        // In the foreground the app itself reacts (it opens the save screen), so
        // only alert when the reader has left the app.
        if (!ServiceLocator.appInForeground.value) postFinishedAlert()
    }

    private fun isCountdownExhausted(): Boolean =
        ServiceLocator.timerMode.value == TimerMode.COUNTDOWN && ServiceLocator.timeLeftSeconds.value <= 0

    private fun persistState() {
        serviceScope.launch { TimerPersistence.save(applicationContext) }
    }

    private fun startForegroundService() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifications.ID_TIMER, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifications.ID_TIMER, notif)
        }
    }

    private fun updateNotification() {
        if (stopping || ServiceLocator.timerBookId.value == null) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Notifications.ID_TIMER, buildNotification())
    }

    /** The timer for the session's book, or the app's front page when there is none. */
    private fun openTimerIntent(): PendingIntent {
        val bookId = ServiceLocator.timerBookId.value
        val intent = if (bookId != null) DeepLink.intent(this, "focus/$bookId") else Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Straight to the save screen, which stops the clock and asks for the page. */
    private fun finishIntent(bookId: String): PendingIntent = PendingIntent.getActivity(
        this, 4, DeepLink.intent(this, "session_save/$bookId"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode, Intent(this, TimerService::class.java).apply { this.action = action },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun postFinishedAlert() {
        if (!Notifications.canPost(this)) return
        val title = shownTitle()
        val bookId = ServiceLocator.timerBookId.value
        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_TIMER_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle("Focus session complete")
            .setContentText(
                if (title != null) "Tap to log your progress in $title" else "Tap to log your reading"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(if (bookId != null) finishIntent(bookId) else openTimerIntent())
            .build()
        getSystemService(NotificationManager::class.java)?.notify(Notifications.ID_TIMER_DONE, notification)
    }

    private fun postGoalReachedAlert(session: SessionInfo) {
        if (ServiceLocator.appInForeground.value || !Notifications.canPost(this)) return
        val title = shownTitle()
        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_TIMER_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle("Page goal reached")
            .setContentText(
                "${session.unit} ${session.currentPage}${if (title != null) " in $title" else ""}. Keep going, or finish up."
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openTimerIntent())
            .addAction(0, "Finish", finishIntent(session.bookId))
            .build()
        getSystemService(NotificationManager::class.java)?.notify(Notifications.ID_GOAL_REACHED, notification)
    }

    /** Mirrors the "hide titles" privacy setting; the lock screen shows this notification. */
    @Volatile
    private var hideTitle = false

    private fun shownTitle(): String? = ServiceLocator.timerBookTitle.value?.let { if (hideTitle) "your book" else it }

    private fun buildNotification(): Notification {
        val mode = ServiceLocator.timerMode.value
        val isRunning = ServiceLocator.timerIsRunning.value
        val bookTitle = shownTitle()
        val countdown = mode == TimerMode.COUNTDOWN
        val timeValue = if (countdown) ServiceLocator.timeLeftSeconds.value else ServiceLocator.elapsedSeconds.value
        val phase = ServiceLocator.timerPhase.value
        val session = info?.takeIf { it.bookId == ServiceLocator.timerBookId.value }
        val expired = countdown && timeValue <= 0

        val title = when {
            expired -> "Time's up"
            bookTitle != null && isRunning -> "Reading: $bookTitle"
            bookTitle != null -> "Paused: $bookTitle"
            !isRunning -> "Session paused"
            !countdown -> "Reading session"
            phase == TimerPhase.WORK -> "Deep focus"
            else -> "Take a break"
        }
        // Position first, since it's what the actions change; the clock sits in the header.
        val pageLine = session?.let {
            buildString {
                append("${it.unit} ${it.currentPage}")
                if (it.totalPages > 0) append(" / ${it.totalPages}")
                if (it.delta > 0) append(" · +${it.delta} this session")
            }
        }
        val goalLine = session?.goalPages?.let { pages ->
            if (session.goalReached) "Page goal reached" else "Goal: ${session.delta} of $pages pages"
        }
        val text = when {
            expired -> "Tap Finish to log your session"
            isRunning -> listOfNotNull(pageLine, goalLine).joinToString(" · ").ifEmpty { "Reading" }
            else -> listOfNotNull("${formatClock(ServiceLocator.activeReadingSeconds.value)} read so far", pageLine).joinToString(" · ")
        }

        val builder = NotificationCompat.Builder(this, Notifications.CHANNEL_TIMER)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentIntent(openTimerIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // A live clock the system ticks itself: counting up from the start, or down to the end.
        if (isRunning && !expired) {
            val now = System.currentTimeMillis()
            builder.setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(countdown)
                .setWhen(if (countdown) now + timeValue * 1000L else now - timeValue * 1000L)
        } else {
            builder.setShowWhen(false)
        }

        // The bar follows the page goal when there is one, the whole book otherwise.
        session?.let {
            val goal = it.goalPages
            when {
                goal != null -> builder.setProgress(goal, it.delta.coerceAtMost(goal), false)
                it.totalPages > 0 -> builder.setProgress(it.totalPages, it.currentPage.coerceAtMost(it.totalPages), false)
            }
        }
        if (!hideTitle) cover?.let { builder.setLargeIcon(it) }

        if (isRunning) {
            builder.addAction(0, "Pause", serviceIntent(ACTION_PAUSE, 1))
        } else if (!expired) {
            builder.addAction(0, "Resume", serviceIntent(ACTION_RESUME, 2))
        }
        if (session != null && (session.totalPages <= 0 || session.currentPage < session.totalPages) && !expired) {
            builder.addAction(0, "+1 page", serviceIntent(ACTION_PAGE_FORWARD, 3))
        }
        ServiceLocator.timerBookId.value?.let { builder.addAction(0, "Finish", finishIntent(it)) }

        return builder.build()
    }

    private fun formatClock(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_PAGE_FORWARD = "ACTION_PAGE_FORWARD"
    }
}

/**
 * Writes the in-memory timer to settings so a killed process can resume it. Both
 * the UI and the notification actions change timer state, so both save through here.
 */
object TimerPersistence {
    suspend fun save(context: Context) {
        val settings = ServiceLocator.settings(context)
        val bookId = ServiceLocator.timerBookId.value
        if (bookId == null) {
            settings.clearTimerState()
            return
        }
        val running = ServiceLocator.timerIsRunning.value
        settings.saveTimerState(
            mode = ServiceLocator.timerMode.value.name,
            bookId = bookId,
            phase = ServiceLocator.timerPhase.value.name,
            isRunning = running,
            timeLeft = ServiceLocator.timeLeftSeconds.value,
            elapsed = ServiceLocator.elapsedSeconds.value,
            activeReading = ServiceLocator.activeReadingSeconds.value,
            lastTickTime = if (running) System.currentTimeMillis() else null,
            pageTarget = ServiceLocator.sessionPageTarget.value
        )
    }
}
