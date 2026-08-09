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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START) {
            startForegroundService()
        } else if (action == ACTION_STOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
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
        val timeValue = if (mode == TimerMode.STOPWATCH) ServiceLocator.elapsedSeconds.value else ServiceLocator.timeLeftSeconds.value
        val phase = ServiceLocator.timerPhase.value

        val h = timeValue / 3600
        val m = (timeValue % 3600) / 60
        val s = timeValue % 60
        val timeStr = if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
        val title = if (mode == TimerMode.STOPWATCH) "Reading Session" else if (phase.name == "WORK") "Deep Focus" else "Take a Break"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(timeStr)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
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
        private const val CHANNEL_ID = "TimerChannel"
        private const val NOTIFICATION_ID = 1
    }
}
