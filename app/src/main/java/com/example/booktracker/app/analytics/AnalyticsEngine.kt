package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

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

    /** Reading days in a year before Wrapped has enough to summarize. */
    const val MIN_DAYS_FOR_WRAPPED = 7

    /**
     * Wrapped is a shareable card, so it stays locked until the year has a
     * finished book or [MIN_DAYS_FOR_WRAPPED] reading days, never a card of zeros.
     */
    fun hasYearToReview(
        books: List<Book>,
        sessions: List<Session>,
        year: Int,
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
    ): Boolean {
        val finished = books.any {
            it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) && it.completedAt > 0 &&
                Instant.ofEpochMilli(it.completedAt).atZone(zone).year == year
        }
        return finished ||
            StreakEngine.pagesByDay(sessions, zone, dayStartsAtHour).keys.count { it.year == year } >= MIN_DAYS_FOR_WRAPPED
    }

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

    /** Reading speed in pages per minute (ppm). */
    fun readingSpeedPpm(sessions: List<Session>): Float? {
        val pph = pagesPerHour(sessions) ?: return null
        return pph / 60f
    }

    /** Single session reading velocity in pages per minute (ppm). */
    fun sessionSpeedPpm(pagesRead: Int, durationSeconds: Int): Float? {
        if (durationSeconds < 60 || pagesRead <= 0) return null
        return pagesRead.toFloat() / (durationSeconds / 60f)
    }

    /** Formatted reading speed string, e.g. "1.2 p/m" or "45 p/h". */
    fun formatReadingSpeed(pph: Float?): String? {
        if (pph == null || pph <= 0f) return null
        val ppm = pph / 60f
        return if (ppm >= 0.1f) {
            String.format(java.util.Locale.US, "%.1f p/m", ppm)
        } else {
            "${pph.roundToInt()} p/h"
        }
    }

    /** Formatted single session speed string, e.g. "1.2 p/m". */
    fun formatSessionPpm(ppm: Float?): String? {
        if (ppm == null || ppm <= 0f) return null
        return String.format(java.util.Locale.US, "%.1f p/m", ppm)
    }

    /** Minutes left in [book] at the reader's measured velocity; null when unknowable. */
    fun estimatedMinutesLeft(book: Book, sessions: List<Session>): Long? {
        if (book.totalPages <= 0) return null
        val remaining = (book.totalPages - book.currentPage).coerceAtLeast(0)
        if (remaining == 0) return 0L
        val velocity = pagesPerHour(sessions) ?: return null
        return (remaining / velocity * 60f).toLong()
    }

    /**
     * Estimated minutes left with fallback: uses book-specific sessions if timed
     * sessions exist for this book, otherwise falls back to overall reader sessions.
     */
    fun bookEstimatedMinutesLeft(
        book: Book,
        bookSessions: List<Session>,
        allSessions: List<Session>
    ): Long? {
        val targetSessions = if (timedSessions(bookSessions).isNotEmpty()) bookSessions else allSessions
        return estimatedMinutesLeft(book, targetSessions)
    }

    /**
     * Velocity for this specific book, falling back to overall sessions if this book
     * has no timed sessions yet.
     */
    fun bookPagesPerHour(
        bookSessions: List<Session>,
        allSessions: List<Session>
    ): Float? {
        val targetSessions = if (timedSessions(bookSessions).isNotEmpty()) bookSessions else allSessions
        return pagesPerHour(targetSessions)
    }

    /** Formats estimated minutes into "45m left", "2h 15m left", or "Finished". */
    fun formatEstimatedTime(minutes: Long?): String? {
        if (minutes == null) return null
        if (minutes <= 0L) return "Finished"
        return when {
            minutes < 60L -> "${minutes}m left"
            else -> "${minutes / 60}h ${minutes % 60}m left"
        }
    }

    data class CompletionPrediction(
        val daysRemaining: Int,
        val estimatedDate: LocalDate,
        val formattedPrediction: String
    )

    /**
     * Estimates when the reader will finish [book] based on their recent reading volume.
     * Uses average daily reading minutes from the last 14 days or overall history.
     */
    fun predictCompletionDate(
        book: Book,
        bookSessions: List<Session>,
        allSessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault()
    ): CompletionPrediction? {
        if (book.totalPages <= 0 || book.currentPage >= book.totalPages) return null
        val minutesLeft = bookEstimatedMinutesLeft(book, bookSessions, allSessions) ?: return null
        if (minutesLeft <= 0L) return null

        val timed = timedSessions(allSessions)
        val today = LocalDate.now(zone)

        val last14Days = today.minusDays(13)
        val recentMinutesByDay = timed
            .filter {
                val date = StreakEngine.sessionDay(it, zone)
                !date.isBefore(last14Days) && !date.isAfter(today)
            }
            .groupBy { StreakEngine.sessionDay(it, zone) }
            .mapValues { (_, sList) -> sList.sumOf { it.endTime - it.startTime } / 60_000L }

        val avgDailyMinutes = if (recentMinutesByDay.isNotEmpty()) {
            recentMinutesByDay.values.average().coerceAtLeast(15.0)
        } else if (timed.isNotEmpty()) {
            val totalMins = timed.sumOf { it.endTime - it.startTime } / 60_000L
            val distinctDays = timed.map { StreakEngine.sessionDay(it, zone) }.distinct().size.coerceAtLeast(1)
            (totalMins.toDouble() / distinctDays).coerceAtLeast(15.0)
        } else {
            25.0
        }

        val daysRemaining = Math.ceil(minutesLeft.toDouble() / avgDailyMinutes).toInt().coerceAtLeast(1)
        val estimatedDate = today.plusDays(daysRemaining.toLong())

        val formatted = when (daysRemaining) {
            1 -> "Tomorrow"
            in 2..6 -> "In $daysRemaining days (${estimatedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))})"
            else -> "~$daysRemaining days (${estimatedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))})"
        }

        return CompletionPrediction(daysRemaining, estimatedDate, formatted)
    }

    /** Reading minutes per day (timed sessions only), keyed like pagesByDay. */
    fun minutesByDay(
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
    ): Map<LocalDate, Long> =
        timedSessions(sessions)
            .groupBy { StreakEngine.sessionDay(it, zone, dayStartsAtHour) }
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
            val day = StreakEngine.sessionDay(it, zone)
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

    private val FORMAT_TAGS = BookFormat.entries.map { it.name.lowercase() }.toSet() + "ebook"

    /**
     * Most common tag across [books], read as a genre. Tags double as genres, so a
     * format tag like "#Audiobook" is skipped: it says how, not what, you read.
     */
    fun topGenre(books: List<Book>): String? =
        books.flatMap { it.genres }
            .map { it.trim().removePrefix("#").trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in FORMAT_TAGS }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key

    /** Label of the slice [genreMix] folds the smaller genres into. */
    const val OTHER_GENRE = "Other"

    /**
     * Books per genre for the genre chart, largest first. Format tags are skipped
     * as in [topGenre]. Past the top [keep], genres fold into one [OTHER_GENRE]
     * slice so the chart stays readable; a single leftover genre keeps its own name.
     */
    fun genreMix(books: List<Book>, keep: Int = 6): List<Pair<String, Int>> {
        val counts = books
            .flatMap { it.genres }
            .filter { tag -> tag.trim().removePrefix("#").trim().let { it.isNotEmpty() && it.lowercase() !in FORMAT_TAGS } }
            .groupingBy { it }.eachCount()
            .toList()
            .sortedByDescending { it.second }
        if (counts.size <= keep + 1) return counts
        return counts.take(keep) + (OTHER_GENRE to counts.drop(keep).sumOf { it.second })
    }

    /**
     * Short traits for the profile header, e.g. ["Night reader", "32-min sessions",
     * "loves Fantasy"]. Each trait is left out until there is data behind it.
     */
    fun readerPersonality(
        books: List<Book>,
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<String> {
        val traits = mutableListOf<String>()
        bestTimeOfDay(sessions, zone)?.let { traits += "$it reader" }
        summarizeTime(sessions, zone).avgSessionMinutes.takeIf { it > 0 }?.let { traits += "$it-min sessions" }

        val shelf = books.filter {
            it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) ||
                it.status.equals(BookStatus.READING.name, ignoreCase = true)
        }
        // Paperback is the default format, so only a deliberate majority says anything.
        val topFormat = shelf.groupingBy { it.format }.eachCount().maxByOrNull { it.value }
        val genre = topGenre(shelf)
        when {
            topFormat != null && topFormat.key != BookFormat.Paperback &&
                shelf.size >= 3 && topFormat.value * 10 >= shelf.size * 6 ->
                traits += "mostly ${topFormat.key.name.lowercase()}s"
            genre != null -> traits += "loves $genre"
        }
        return traits
    }

    /** "2h 15m", "45m", "<1m" — for durations shown in stats and recaps. */
    fun formatMinutes(minutes: Long): String = when {
        minutes < 1L -> "<1m"
        minutes < 60L -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}
