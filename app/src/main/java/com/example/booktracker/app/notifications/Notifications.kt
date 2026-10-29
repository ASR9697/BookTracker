package com.example.booktracker.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Notification channels in one place so every sender agrees on ids and importance. */
object Notifications {
    const val CHANNEL_TIMER = "TimerChannel"
    const val CHANNEL_TIMER_ALERTS = "timer_alerts"
    const val CHANNEL_REMINDERS = "reading_reminders"
    const val CHANNEL_RECAP = "weekly_recap"

    const val ID_TIMER = 1
    const val ID_TIMER_DONE = 2
    const val ID_GOAL_REACHED = 3
    const val ID_REMINDER = 1001
    const val ID_STREAK_NUDGE = 1002
    const val ID_WEEKLY_RECAP = 1003
    const val ID_REVIEW_REMINDER = 1004
    const val ID_WISHLIST_RELEASE = 1005

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_TIMER, "Focus timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows the running reading timer"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_TIMER_ALERTS, "Timer finished", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerts you when a focus session ends"
                },
                NotificationChannel(CHANNEL_REMINDERS, "Reading reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Your daily goal reminder, streak nudges and review reminders"
                },
                NotificationChannel(CHANNEL_RECAP, "Weekly recap", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Your week in reading, on Sunday evenings"
                }
            )
        )
    }

    /** POST_NOTIFICATIONS only exists from Android 13; earlier versions always allow posting. */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
