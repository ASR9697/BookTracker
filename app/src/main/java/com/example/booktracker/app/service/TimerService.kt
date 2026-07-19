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

class TimerService : Service() {

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
        } else if (action == ACTION_UPDATE) {
            val timeLeft = intent?.getIntExtra(EXTRA_TIME_LEFT, 0) ?: 0
            val phase = intent?.getStringExtra(EXTRA_PHASE) ?: "WORK"
            updateNotification(timeLeft, phase)
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                buildNotification(0, "WORK"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(0, "WORK"))
        }
    }

    private fun updateNotification(timeLeft: Int, phase: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(timeLeft, phase))
    }

    private fun buildNotification(timeLeft: Int, phase: String): Notification {
        val m = timeLeft / 60
        val s = timeLeft % 60
        val timeStr = String.format("%02d:%02d", m, s)
        val title = if (phase == "WORK") "Deep Focus" else "Take a Break"

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
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val EXTRA_TIME_LEFT = "EXTRA_TIME_LEFT"
        const val EXTRA_PHASE = "EXTRA_PHASE"
        private const val CHANNEL_ID = "TimerChannel"
        private const val NOTIFICATION_ID = 1
    }
}
