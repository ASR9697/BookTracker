package com.example.booktracker.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.app.R
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.ReviewEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.analytics.WishlistEngine
import com.example.booktracker.app.data.AppPrefs
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.notifications.Notifications
import com.example.booktracker.app.widget.DeepLink
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * The optional nudges from Settings > Notifications, on top of the daily goal
 * reminder: a streak about to lapse, a Sunday recap and a daily review. Each
 * re-reads its switch when it runs and stays silent during quiet hours.
 */
private suspend fun Context.nudgePrefs(): AppPrefs? {
    val prefs = ServiceLocator.settings(this).prefs.first()
    return prefs.takeIf { Notifications.canPost(this) && !it.isQuiet(LocalTime.now().hour) }
}

private fun Context.postNudge(channel: String, id: Int, route: String, title: String, text: String) {
    Notifications.ensureChannels(this)
    val open = PendingIntent.getActivity(
        this, id, DeepLink.intent(this, route),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(this, channel)
        .setSmallIcon(R.drawable.ic_stat_book)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    getSystemService(NotificationManager::class.java)?.notify(id, notification)
}

/** In the evening, if there's a streak going and nothing read yet today. */
class StreakNudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.nudgePrefs() ?: return Result.success()
        if (!prefs.streakNudge) return Result.success()
        val settings = ServiceLocator.settings(applicationContext)
        val sessions = ServiceLocator.repository(applicationContext).observeCompletedSessions().first()
        val streak = StreakEngine.compute(sessions, settings.dailyGoal.first(), dayStartsAtHour = settings.dayStartsAtHour.first())
        if (streak.currentStreak > 0 && streak.pagesToday == 0) {
            applicationContext.postNudge(
                Notifications.CHANNEL_REMINDERS, Notifications.ID_STREAK_NUDGE, "home",
                "Keep your ${streak.currentStreak}-day streak",
                "Nothing read yet today. A page or two before bed keeps it going."
            )
        }
        return Result.success()
    }
}

/** Sunday evening: the last seven reading days in one line. */
class WeeklyRecapWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.nudgePrefs() ?: return Result.success()
        if (!prefs.weeklyRecap) return Result.success()
        val dayStart = ServiceLocator.settings(applicationContext).dayStartsAtHour.first()
        val sessions = ServiceLocator.repository(applicationContext).observeCompletedSessions().first()
        val zone = ZoneId.systemDefault()
        val today = StreakEngine.readingDate(System.currentTimeMillis(), zone, dayStart)
        val week = sessions.filter {
            val day = StreakEngine.sessionDay(it, zone, dayStart)
            !day.isBefore(today.minusDays(6)) && !day.isAfter(today)
        }
        val pages = week.sumOf { it.pagesRead }
        val minutes = week.sumOf { it.durationSeconds } / 60L
        val days = week.map { StreakEngine.sessionDay(it, zone, dayStart) }.distinct().size
        val text = if (week.isEmpty()) {
            "A quiet week. Your current book is waiting where you left it."
        } else {
            "$pages pages · ${AnalyticsEngine.formatMinutes(minutes)} · read on $days of 7 days"
        }
        applicationContext.postNudge(Notifications.CHANNEL_RECAP, Notifications.ID_WEEKLY_RECAP, "stats", "Your week in reading", text)
        return Result.success()
    }
}

/** Once a day, when there are notes or words due for review. */
class ReviewReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.nudgePrefs() ?: return Result.success()
        if (!prefs.reviewReminder) return Result.success()
        val repository = ServiceLocator.repository(applicationContext)
        val dayStart = ServiceLocator.settings(applicationContext).dayStartsAtHour.first()
        val zone = ZoneId.systemDefault()
        val today = StreakEngine.readingDate(System.currentTimeMillis(), zone, dayStart).toEpochDay()
        val states = repository.observeReviewStates().first()
        val due = ReviewEngine.dailyQueue(
            items = ReviewEngine.noteItems(repository.observeAllNotes().first()) + ReviewEngine.wordItems(repository.observeWords().first()),
            states = states.associateBy { it.itemId },
            today = today,
            reviewedToday = ReviewEngine.reviewedOn(states, today) { StreakEngine.readingDate(it, zone, dayStart).toEpochDay() }
        ).size
        if (due > 0) {
            applicationContext.postNudge(
                Notifications.CHANNEL_REMINDERS, Notifications.ID_REVIEW_REMINDER, "review",
                "Daily review",
                "$due ${if (due == 1) "card is" else "cards are"} ready. A couple of minutes keeps them fresh."
            )
        }
        return Result.success()
    }
}

/** In the morning, when a wished-for book comes out today. */
class WishlistReleaseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.nudgePrefs() ?: return Result.success()
        if (!prefs.releaseReminders) return Result.success()
        val books = ServiceLocator.repository(applicationContext).observeBooks().first()
        val out = WishlistEngine.releasedOn(books, java.time.LocalDate.now())
        if (out.isNotEmpty()) {
            val titles = if (prefs.hideTitles) null else out.joinToString(", ") { it.title }
            applicationContext.postNudge(
                Notifications.CHANNEL_REMINDERS, Notifications.ID_WISHLIST_RELEASE, "wishlist",
                if (out.size == 1) "Out today from your wishlist" else "${out.size} wishlist books out today",
                titles ?: "A book you've been waiting for is out today."
            )
        }
        return Result.success()
    }
}

/**
 * "Pick up tomorrow?", asked for on the save screen: once, at the chosen time, unless
 * the book was read again since. It takes the daily reminder's notification slot, so
 * the two never stack when they land together.
 */
class PickUpReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.nudgePrefs() ?: return Result.success()
        val bookId = inputData.getString(KEY_BOOK_ID) ?: return Result.success()
        val setAt = inputData.getLong(KEY_SET_AT, 0L)
        val repository = ServiceLocator.repository(applicationContext)
        val book = repository.observeBooks().first().firstOrNull { it.id == bookId } ?: return Result.success()
        if (!book.status.equals(BookStatus.READING.name, ignoreCase = true)) return Result.success()
        val readSince = repository.observeCompletedSessions().first().any { it.bookId == bookId && it.endTime > setAt }
        if (readSince) return Result.success()
        val unit = com.example.booktracker.app.format.FormatAdaptabilityLayer.unitAbbrev(book)
        applicationContext.postNudge(
            Notifications.CHANNEL_REMINDERS, Notifications.ID_REMINDER, "book/$bookId",
            if (prefs.hideTitles) "Pick up where you left off" else "Pick up ${book.title}",
            "You're on $unit ${book.currentPage}. A few pages now keeps the story fresh."
        )
        return Result.success()
    }

    companion object {
        const val KEY_BOOK_ID = "bookId"
        const val KEY_SET_AT = "setAt"
    }
}

/** Keeps the nudge jobs in line with the reader's switches and chosen hours. */
object NudgeScheduler {
    private const val PICK_UP = "pick_up_reminder"

    /** One pick-up reminder at a time: a newer one replaces the last. */
    fun schedulePickUp(context: Context, bookId: String, at: LocalDateTime) {
        val now = LocalDateTime.now()
        val request = OneTimeWorkRequestBuilder<PickUpReminderWorker>()
            .setInitialDelay(delayTo(now, at).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    PickUpReminderWorker.KEY_BOOK_ID to bookId,
                    PickUpReminderWorker.KEY_SET_AT to System.currentTimeMillis()
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(PICK_UP, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * When to offer the pick-up reminder: the next [hour]:00 at least half a day away,
     * so a session just finished isn't followed by a reminder the same evening.
     */
    fun pickUpTime(now: LocalDateTime, hour: Int): LocalDateTime {
        var at = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        while (at.isBefore(now.plusHours(12))) at = at.plusDays(1)
        return at
    }

    private const val STREAK = "streak_nudge"
    private const val RECAP = "weekly_recap"
    private const val REVIEW = "review_reminder"
    private const val RELEASE = "wishlist_release"
    private const val RECAP_HOUR = 19
    private const val RELEASE_HOUR = 9

    fun apply(context: Context, prefs: AppPrefs) {
        val work = WorkManager.getInstance(context)
        val now = LocalDateTime.now()

        if (prefs.streakNudge) {
            work.enqueueUniquePeriodicWork(STREAK, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                PeriodicWorkRequestBuilder<StreakNudgeWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayTo(now, nextAt(now, prefs.streakNudgeHour)), TimeUnit.MILLISECONDS).build())
        } else work.cancelUniqueWork(STREAK)

        if (prefs.weeklyRecap) {
            var sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).withHour(RECAP_HOUR).withMinute(0).withSecond(0).withNano(0)
            if (!sunday.isAfter(now)) sunday = sunday.plusWeeks(1)
            work.enqueueUniquePeriodicWork(RECAP, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                PeriodicWorkRequestBuilder<WeeklyRecapWorker>(7, TimeUnit.DAYS)
                    .setInitialDelay(delayTo(now, sunday), TimeUnit.MILLISECONDS).build())
        } else work.cancelUniqueWork(RECAP)

        if (prefs.reviewReminder) {
            work.enqueueUniquePeriodicWork(REVIEW, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayTo(now, nextAt(now, prefs.reviewReminderHour)), TimeUnit.MILLISECONDS).build())
        } else work.cancelUniqueWork(REVIEW)

        if (prefs.releaseReminders) {
            work.enqueueUniquePeriodicWork(RELEASE, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                PeriodicWorkRequestBuilder<WishlistReleaseWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayTo(now, nextAt(now, RELEASE_HOUR)), TimeUnit.MILLISECONDS).build())
        } else work.cancelUniqueWork(RELEASE)
    }

    private fun nextAt(now: LocalDateTime, hour: Int): LocalDateTime {
        val today = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun delayTo(now: LocalDateTime, target: LocalDateTime) = Duration.between(now, target).toMillis()
}
