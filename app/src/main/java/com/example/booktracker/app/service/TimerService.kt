package com.example.booktracker.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.booktracker.app.MainActivity
import com.example.booktracker.app.R
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.notifications.Notifications
import com.example.booktracker.app.ui.TimerMode
import com.example.booktracker.app.ui.TimerPhase
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Set once a stop is requested so late collector emissions can't re-post the
    // ongoing notification after it has been removed.
    @Volatile
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)

        serviceScope.launch {
            var lastTickTime = System.currentTimeMillis()
            var wasRunning = false

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

                        updateNotification()
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
                getSystemService(NotificationManager::class.java)?.cancel(Notifications.ID_TIMER_DONE)
                stopSelf()
            }
            ACTION_PAUSE -> {
                ServiceLocator.timerIsRunning.value = false
                persistState()
                updateNotification()
            }
            ACTION_RESUME -> {
                if (!isCountdownExhausted()) {
                    ServiceLocator.timerIsRunning.value = true
                    persistState()
                }
                updateNotification()
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

    private fun openAppIntent(): PendingIntent {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun postFinishedAlert() {
        if (!Notifications.canPost(this)) return
        val title = ServiceLocator.timerBookTitle.value
        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_TIMER_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle("Focus session complete")
            .setContentText(
                if (title != null) "Tap to log your progress in $title" else "Tap to log your reading"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        getSystemService(NotificationManager::class.java)?.notify(Notifications.ID_TIMER_DONE, notification)
    }

    private fun buildNotification(): Notification {
        val mode = ServiceLocator.timerMode.value
        val isRunning = ServiceLocator.timerIsRunning.value
        val bookTitle = ServiceLocator.timerBookTitle.value
        val timeValue = if (mode == TimerMode.STOPWATCH) ServiceLocator.elapsedSeconds.value else ServiceLocator.timeLeftSeconds.value
        val phase = ServiceLocator.timerPhase.value

        val h = timeValue / 3600
        val m = (timeValue % 3600) / 60
        val s = timeValue % 60
        val timeStr = if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }

        val title = when {
            bookTitle != null && isRunning -> "Reading: $bookTitle"
            bookTitle != null -> "Paused: $bookTitle"
            !isRunning -> "Session paused"
            mode == TimerMode.STOPWATCH -> "Reading session"
            phase == TimerPhase.WORK -> "Deep focus"
            else -> "Take a break"
        }
        val subText = when {
            mode == TimerMode.COUNTDOWN && timeValue <= 0 -> "Time's up — tap to log your session"
            mode == TimerMode.COUNTDOWN -> "$timeStr remaining"
            else -> "$timeStr reading time"
        }

        val pausePendingIntent = PendingIntent.getService(
            this, 1, Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val resumePendingIntent = PendingIntent.getService(
            this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, Notifications.CHANNEL_TIMER)
            .setContentTitle(title)
            .setContentText(subText)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (isRunning) {
            builder.addAction(0, "Pause", pausePendingIntent)
        } else if (!isCountdownExhausted()) {
            builder.addAction(0, "Resume", resumePendingIntent)
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
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
            lastTickTime = if (running) System.currentTimeMillis() else null
        )
    }
}
