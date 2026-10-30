package com.example.booktracker.wear

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * A reading session timed on the wrist, for when the phone's timer isn't running.
 * It lives in prefs as timestamps, so nothing needs to tick in the background and
 * it survives the process being killed. The whole session goes to the phone once,
 * when it ends.
 */
data class WristSession(
    val id: String,
    val bookId: String,
    val title: String,
    val startPage: Int,
    val startedAt: Long,
    /** Reading time before the current run, in ms. */
    val banked: Long,
    /** When the current run began; null while paused. */
    val runningSince: Long?,
    /** A countdown's length in ms; null to count up with no limit. */
    val targetMillis: Long? = null
) {
    val running: Boolean get() = runningSince != null
    val countdown: Boolean get() = targetMillis != null

    /** Time read. A countdown stops counting at its length, even if the buzz came late. */
    fun activeMillis(now: Long): Long {
        val raw = banked + (runningSince?.let { (now - it).coerceAtLeast(0) } ?: 0)
        return targetMillis?.let { raw.coerceAtMost(it) } ?: raw
    }

    /** What's left of a countdown; null when counting up. */
    fun remainingMillis(now: Long): Long? = targetMillis?.let { (it - activeMillis(now)).coerceAtLeast(0) }

    fun expired(now: Long): Boolean = remainingMillis(now) == 0L
}

object WristSessions {

    private const val KEY = "wrist_session"
    private const val LAST_MINUTES = "wrist_last_minutes"

    /** The countdowns the timer offers, in minutes; 0 counts up with no limit. */
    val CHOICES = listOf(0, 15, 25, 45, 60)

    fun current(context: Context): WristSession? {
        val parts = WatchStore.prefs(context).getString(KEY, null)?.split('\u001F') ?: return null
        if (parts.size < 7) return null
        return WristSession(
            id = parts[0],
            bookId = parts[1],
            title = parts[2],
            startPage = parts[3].toIntOrNull() ?: return null,
            startedAt = parts[4].toLongOrNull() ?: return null,
            banked = parts[5].toLongOrNull() ?: return null,
            runningSince = parts[6].toLongOrNull(),
            targetMillis = parts.getOrNull(7)?.toLongOrNull()
        )
    }

    /** The length picked last time, so it's offered first. */
    fun lastMinutes(context: Context): Int = WatchStore.prefs(context).getInt(LAST_MINUTES, 0)

    fun start(context: Context, book: ActiveBook, minutes: Int): WristSession {
        val now = System.currentTimeMillis()
        WatchStore.prefs(context).edit { putInt(LAST_MINUTES, minutes) }
        return WristSession(
            id = java.util.UUID.randomUUID().toString(),
            bookId = book.id,
            title = book.title,
            startPage = book.currentUnit,
            startedAt = now,
            banked = 0,
            runningSince = now,
            targetMillis = minutes.takeIf { it > 0 }?.let { it * 60_000L }
        ).also {
            save(context, it)
            SessionAlarm.schedule(context, it)
        }
    }

    fun setRunning(context: Context, running: Boolean): WristSession? {
        val session = current(context) ?: return null
        if (session.running == running) return session
        val now = System.currentTimeMillis()
        val next = when {
            !running -> session.copy(banked = session.activeMillis(now), runningSince = null)
            // Reading on past the end of a countdown just keeps counting up.
            session.expired(now) -> session.copy(runningSince = now, targetMillis = null)
            else -> session.copy(runningSince = now)
        }
        save(context, next)
        if (next.running) SessionAlarm.schedule(context, next) else SessionAlarm.cancel(context)
        return next
    }

    /**
     * A countdown has run out: stop the clock at its length and buzz. Called by the
     * alarm, or by the screen when it sees zero first; whichever comes second finds
     * the clock already stopped and does nothing.
     */
    fun expire(context: Context): WristSession? {
        val session = current(context) ?: return null
        val now = System.currentTimeMillis()
        if (!session.running || !session.countdown) return session
        if (!session.expired(now)) {
            SessionAlarm.schedule(context, session)
            return session
        }
        val stopped = session.copy(banked = session.targetMillis ?: session.banked, runningSince = null)
        save(context, stopped)
        SessionNotifier.timesUp(context)
        WatchStore.refreshSurfaces(context)
        return stopped
    }

    /** Sends the session, ending on [book]'s current page, to the phone. */
    suspend fun finish(context: Context, book: ActiveBook, focus: Int) {
        val session = current(context)?.takeIf { it.bookId == book.id } ?: return
        clear(context)
        WatchStore.publishSession(context, session, book.currentUnit, System.currentTimeMillis(), focus)
        WatchStore.refreshSurfaces(context)
    }

    /** Drops the timing but keeps the pages: they were read either way. */
    suspend fun discard(context: Context, book: ActiveBook?) {
        val session = current(context) ?: return
        clear(context)
        if (book != null && book.id == session.bookId && book.currentUnit != session.startPage) {
            WatchStore.publishProgress(context, book.id, book.currentUnit, book.updatedAt)
        }
        WatchStore.refreshSurfaces(context)
    }

    private fun save(context: Context, session: WristSession) {
        WatchStore.prefs(context).edit {
            putString(
                KEY,
                listOf(
                    session.id, session.bookId, session.title, session.startPage, session.startedAt,
                    session.banked, session.runningSince ?: "", session.targetMillis ?: ""
                ).joinToString("\u001F")
            )
        }
        SessionNotifier.show(context, session)
    }

    private fun clear(context: Context) {
        WatchStore.prefs(context).edit { remove(KEY) }
        SessionAlarm.cancel(context)
        SessionNotifier.hide(context)
    }
}

/**
 * Wakes the watch when a countdown ends. On time when the reader has allowed
 * "Alarms & reminders"; otherwise it hops toward the end and lands within about a
 * minute (the screen still ends it on the dot while it's showing).
 */
object SessionAlarm {

    private const val ASKED = "exact_alarm_asked"

    /** True the first time only, so the setting is offered once and not on every countdown. */
    fun firstAsk(context: Context): Boolean {
        val prefs = WatchStore.prefs(context)
        if (prefs.getBoolean(ASKED, false)) return false
        prefs.edit { putBoolean(ASKED, true) }
        return true
    }

    fun canBeOnTime(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    /** The settings page where the reader can allow on-time alarms. */
    fun permissionIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
        } else null

    fun schedule(context: Context, session: WristSession) {
        val remaining = session.remainingMillis(System.currentTimeMillis()) ?: return
        if (!session.running) return
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        // An inexact alarm can land up to ~75% of its wait late, so aim at the halfway
        // point and re-aim from there (expire() does): each hop halves the error, and
        // the last, short hop ends within about a minute.
        val hop = if (remaining > LAST_HOP_MS) remaining / 2 else remaining
        try {
            if (canBeOnTime(context)) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + remaining, pending(context))
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + hop, pending(context))
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + hop, pending(context))
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context))
    }

    private const val LAST_HOP_MS = 90_000L

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, SessionAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

class SessionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WristSessions.expire(context)
    }
}

/**
 * The ongoing notification behind a wrist session, which puts a ticking chip on
 * the watch face (Ongoing Activity) and a way back into the app.
 */
object SessionNotifier {

    private const val CHANNEL = "wrist_session"
    private const val ALERT_CHANNEL = "wrist_session_alerts"
    private const val ID = 7
    private const val ALERT_ID = 8

    fun show(context: Context, session: WristSession) {
        if (!canPost(context)) return
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.session_channel), NotificationManager.IMPORTANCE_LOW)
        )

        val now = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val active = session.activeMillis(now)
        val remaining = session.remainingMillis(now)
        // The chip's clock counts in elapsed-realtime, which doesn't jump with the wall clock.
        val clock: Status.Part = when {
            remaining == 0L -> Status.TextPart(context.getString(R.string.times_up))
            remaining != null && session.running -> Status.TimerPart(nowElapsed + remaining)
            remaining != null -> Status.TimerPart(nowElapsed + remaining, nowElapsed, session.targetMillis ?: 0)
            session.running -> Status.StopwatchPart(nowElapsed - active)
            else -> Status.StopwatchPart(nowElapsed - active, nowElapsed)
        }
        val text = context.getString(
            if (session.running) R.string.session_reading else R.string.session_paused,
            session.title
        )

        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_book)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        OngoingActivity.Builder(context, ID, builder)
            .setStaticIcon(R.drawable.ic_book)
            .setTouchIntent(openApp(context))
            .setStatus(Status.Builder().addTemplate("#time#").addPart("time", clock).build())
            .build()
            .apply(context)
        post(context, ID, builder.build())
    }

    /** A countdown ran out: a buzz on the wrist, and a notice that opens the app to save it. */
    fun timesUp(context: Context) {
        val pattern = VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250, 120, 450), -1)
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(pattern)
        } catch (e: Exception) {
            Log.w("SessionNotifier", "Could not vibrate", e)
        }
        if (!canPost(context)) return
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, context.getString(R.string.session_alert_channel), NotificationManager.IMPORTANCE_HIGH)
                // The buzz above is the alert; the channel's own would double it.
                .apply { enableVibration(false) }
        )
        val alert = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_book)
            .setContentTitle(context.getString(R.string.times_up))
            .setContentText(context.getString(R.string.times_up_text))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        post(context, ALERT_ID, alert)
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).apply {
            cancel(ID)
            cancel(ALERT_ID)
        }
    }

    // Callers check canPost first; the catch covers the permission being revoked in between.
    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w("SessionNotifier", "Notifications not allowed", e)
        }
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, WearActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
