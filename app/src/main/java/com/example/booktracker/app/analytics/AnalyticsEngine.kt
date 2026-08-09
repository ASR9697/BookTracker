package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Time-based reading analytics over completed sessions. Pure logic — no Android
 * or Compose types.
 *
 * Progress made with no session open is stored as an instant "mini-session"
 * (startTime == endTime), which counts for streaks/volume but carries no real
 * duration — so everything speed-related here first filters to [timedSessions].
 */
object AnalyticsEngine {

    /** Sessions shorter than this can't produce a meaningful pages/hour. */
    const val MIN_TIMED_SESSION_MILLIS = 60_000L

    fun timedSessions(sessions: List<Session>): List<Session> =
        sessions.filter {
            it.endTime - it.startTime >= MIN_TIMED_SESSION_MILLIS && it.pagesRead > 0
        }

    /**
     * Overall reading velocity, weighted by time (total pages / total hours),
     * so one short sprint doesn't dominate the average. Null until at least one
     * timed session exists.
     */
    fun pagesPerHour(sessions: List<Session>): Float? {
        val timed = timedSessions(sessions)
        if (timed.isEmpty()) return null
        val totalMillis = timed.sumOf { it.endTime - it.startTime }
        val totalPages = timed.sumOf { it.pagesRead }
        if (totalMillis <= 0L || totalPages <= 0) return null
        return totalPages / (totalMillis / 3_600_000f)
    }

    /** Minutes left in [book] at the reader's measured velocity; null when unknowable. */
    fun estimatedMinutesLeft(book: Book, sessions: List<Session>): Long? {
        if (book.totalPages <= 0) return null
        val remaining = (book.totalPages - book.currentPage).coerceAtLeast(0)
        if (remaining == 0) return 0L
        val velocity = pagesPerHour(sessions) ?: return null
        return (remaining / velocity * 60f).toLong()
    }

    /** Reading minutes per day (timed sessions only), keyed like pagesByDay. */
    fun minutesByDay(
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, Long> =
        timedSessions(sessions)
            .groupBy { StreakEngine.readingDate(it.endTime, zone) }
            .mapValues { (_, day) -> day.sumOf { it.endTime - it.startTime } / 60_000L }

    data class TimeSummary(
        val minutesLast7Days: Long,
        val avgSessionMinutes: Long,
        val timedSessionCount: Int
    )

    fun summarizeTime(
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): TimeSummary {
        val timed = timedSessions(sessions)
        val weekStart = today.minusDays(6)
        val last7 = timed.filter {
            val day = StreakEngine.readingDate(it.endTime, zone)
            !day.isBefore(weekStart) && !day.isAfter(today)
        }
        val totalMinutes = { list: List<Session> ->
            list.sumOf { it.endTime - it.startTime } / 60_000L
        }
        return TimeSummary(
            minutesLast7Days = totalMinutes(last7),
            avgSessionMinutes = if (timed.isEmpty()) 0L else totalMinutes(timed) / timed.size,
            timedSessionCount = timed.size
        )
    }

    /** Coarse day-part with the most pages read, e.g. "Evening"; null with no data. */
    fun bestTimeOfDay(
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault()
    ): String? =
        timedSessions(sessions)
            .groupBy { dayPart(Instant.ofEpochMilli(it.startTime).atZone(zone).hour) }
            .mapValues { (_, part) -> part.sumOf { it.pagesRead } }
            .maxByOrNull { it.value }
            ?.key

    private fun dayPart(hour: Int): String = when (hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..21 -> "Evening"
        else -> "Night"
    }

    data class EnvironmentStat(
        val tag: String,
        val sessionCount: Int,
        val totalPages: Int,
        val pagesPerHour: Float?
    )

    /** Per-environment volume and velocity, most-read tags first. */
    fun environmentStats(sessions: List<Session>): List<EnvironmentStat> =
        sessions
            .filter { it.environmentTag.isNotBlank() && it.endTime > 0 }
            .groupBy { it.environmentTag }
            .map { (tag, tagged) ->
                EnvironmentStat(
                    tag = tag,
                    sessionCount = tagged.size,
                    totalPages = tagged.sumOf { it.pagesRead },
                    pagesPerHour = pagesPerHour(tagged)
                )
            }
            .sortedByDescending { it.totalPages }

    /** "2h 15m", "45m", "<1m" — for durations shown in stats and recaps. */
    fun formatMinutes(minutes: Long): String = when {
        minutes < 1L -> "<1m"
        minutes < 60L -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}
