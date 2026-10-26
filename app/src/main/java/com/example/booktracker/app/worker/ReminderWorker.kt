package com.example.booktracker.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.booktracker.app.MainActivity
import com.example.booktracker.app.R
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.data.ReminderSettings
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.notifications.Notifications
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = ServiceLocator.settings(context)
        if (!settings.reminder.first().enabled || !Notifications.canPost(context)) {
            return Result.success()
        }
        val dailyGoal = settings.dailyGoal.first()
        val dayStartsAtHour = settings.dayStartsAtHour.first()
        val sessions = ServiceLocator.repository(context).observeCompletedSessions().first()

        val zone = java.time.ZoneId.systemDefault()
        val today = StreakEngine.readingDate(System.currentTimeMillis(), zone, dayStartsAtHour)
        val todayProgress = sessions
            .filter { StreakEngine.sessionDay(it, zone, dayStartsAtHour) == today }
            .sumOf { it.pagesRead }

        if (todayProgress < dailyGoal) {
            val message = if (todayProgress == 0) {
                "A few pages today keeps your streak alive."
            } else {
                "${dailyGoal - todayProgress} more pages to hit today's goal of $dailyGoal."
            }
            showNotification("Time to read", message)
        }

        return Result.success()
    }

    private fun showNotification(title: String, content: String) {
        Notifications.ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(Notifications.ID_REMINDER, notification)
    }
}

/** Keeps the daily reminder's WorkManager job in line with the reader's settings. */
object ReminderScheduler {
    private const val WORK_NAME = "daily_reading_reminder"

    fun apply(context: Context, reminder: ReminderSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!reminder.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var target = now.withHour(reminder.hour).withMinute(reminder.minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, target).toMillis(), TimeUnit.MILLISECONDS)
            .addTag("reading_reminders")
            .build()

        // Re-enqueue so a changed time takes effect immediately rather than after the next run.
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }
}
