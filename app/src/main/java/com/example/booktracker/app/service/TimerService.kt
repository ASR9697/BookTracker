package com.example.booktracker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import com.example.booktracker.app.MainActivity
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.ui.TimerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
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
                        
                        val mode = ServiceLocator.timerMode.value
                        if (mode == TimerMode.COUNTDOWN) {
                            val newTimeLeft = ServiceLocator.timeLeftSeconds.value - secondsPassed
                            if (newTimeLeft <= 0) {
                                ServiceLocator.timeLeftSeconds.value = 0
                                ServiceLocator.timerIsRunning.value = false
                                ServiceLocator.notifyTimerExpired()
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
                delay(100)
            }
        }
        
        serviceScope.launch {
            ServiceLocator.timerPhase.collect { updateNotification() }
        }
        serviceScope.launch {
            ServiceLocator.timerMode.collect { updateNotification() }
        }
        serviceScope.launch {
            ServiceLocator.timerBookTitle.collect { updateNotification() }
        }
        serviceScope.launch {
            ServiceLocator.timerIsRunning.collect { updateNotification() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
            ACTION_PAUSE -> {
                ServiceLocator.timerIsRunning.value = false
                updateNotification()
            }
            ACTION_RESUME -> {
                ServiceLocator.timerIsRunning.value = true
                updateNotification()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
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
        val timeStr = if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)

        val title = if (bookTitle != null) {
            if (isRunning) "Reading: $bookTitle" else "Paused: $bookTitle"
        } else {
            if (isRunning) {
                if (mode == TimerMode.STOPWATCH) "Reading Session" else if (phase.name == "WORK") "Deep Focus" else "Take a Break"
            } else {
                "Session Paused"
            }
        }

        val subText = if (mode == TimerMode.COUNTDOWN) "$timeStr remaining" else "$timeStr reading time"

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val resumeIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
        val resumePendingIntent = PendingIntent.getService(
            this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openPendingIntent)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        if (isRunning) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        }
        builder.addAction(android.R.drawable.ic_menu_view, "Open", openPendingIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        private const val CHANNEL_ID = "TimerChannel"
        private const val NOTIFICATION_ID = 1
    }
}
