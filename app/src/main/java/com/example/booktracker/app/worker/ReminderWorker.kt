package com.example.booktracker.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.booktracker.app.MainActivity
import com.example.booktracker.app.R
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.data.ServiceLocator
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = ServiceLocator.settings(context)
        val dailyGoal = settings.dailyGoal.firstOrNull() ?: StreakEngine.DEFAULT_DAILY_GOAL_PAGES
        val repository = ServiceLocator.repository(context)
        val sessions = repository.observeCompletedSessions().firstOrNull() ?: emptyList()
        
        val todayStr = LocalDate.now().toString()
        val todayProgress = sessions
            .filter { SessionDateHelper.getDateString(it.startTime) == todayStr }
            .sumOf { it.pagesRead }

        if (todayProgress < dailyGoal) {
            val progressPercent = if (dailyGoal > 0) ((todayProgress.toFloat() / dailyGoal) * 100).toInt() else 0
            val message = if (todayProgress == 0) {
                "Time for a reading session! Maintain your streak."
            } else {
                "You're $progressPercent% there! Read ${dailyGoal - todayProgress} more pages to hit your goal."
            }
            showNotification("Daily Reading Goal", message)
        }

        return Result.success()
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reading_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reading Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to reach your daily reading goal"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            // Ideally use R.drawable.ic_launcher_foreground, but android.R.drawable.ic_dialog_info works for now
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}

object SessionDateHelper {
    fun getDateString(timestamp: Long): String {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate().toString()
    }
}
