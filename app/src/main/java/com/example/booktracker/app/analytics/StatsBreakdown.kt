package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.Session
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * The cards under Stats' period summary, for one period: when you read (rhythm),
 * how fast (pace) and what (the shelf you read from). Pure, like [PeriodStats].
 */
object StatsBreakdown {

    /** Parts of the day, in the order the rhythm grid shows them. */
    enum class DayPart(val label: String) { MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening"), NIGHT("Night") }

    fun dayPart(hour: Int): DayPart = when (hour) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        in 17..21 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }

    /** Sessions whose reading day falls inside [range]. */
    fun sessionsIn(sessions: List<Session>, range: ClosedRange<LocalDate>, zone: ZoneId, dayStartsAtHour: Int): List<Session> =
        sessions.filter { it.endTime > 0 && StreakEngine.sessionDay(it, zone, dayStartsAtHour) in range }

    /**
     * Minutes read by weekday and part of day: rows Monday..Sunday, columns in [DayPart]
     * order. Untimed progress has no minutes, so it counts a page as two minutes rather
     * than vanishing from the grid.
     */
    fun rhythm(sessions: List<Session>, zone: ZoneId): List<List<Int>> {
        val grid = List(7) { IntArray(DayPart.entries.size) }
        sessions.forEach { s ->
            val start = Instant.ofEpochMilli(s.startTime.takeIf { it > 0 } ?: s.endTime).atZone(zone)
            val minutes = if (s.durationSeconds > 0) s.durationSeconds / 60 else s.pagesRead * 2
            grid[start.dayOfWeek.value - 1][dayPart(start.hour).ordinal] += minutes.coerceAtLeast(0)
        }
        return grid.map { it.toList() }
    }

    /** The busiest cell of [rhythm], e.g. Sunday evening; null with no reading. */
    fun peak(grid: List<List<Int>>): Pair<DayOfWeek, DayPart>? {
        var best: Pair<DayOfWeek, DayPart>? = null
        var most = 0
        grid.forEachIndexed { d, row ->
            row.forEachIndexed { p, minutes ->
                if (minutes > most) { most = minutes; best = DayOfWeek.of(d + 1) to DayPart.entries[p] }
            }
        }
        return best
    }

    data class Pace(
        val pagesPerHour: Float?,
        val averageSessionMinutes: Int?,
        val longestSessionMinutes: Int?,
        /** Average days from first to last day of a read finished in the period. */
        val averageDaysToFinish: Float?
    )

    fun pace(sessions: List<Session>, finishedReads: List<FinishedShelf.Read>): Pace {
        val timed = AnalyticsEngine.timedSessions(sessions).map { ((it.endTime - it.startTime) / 60_000L).toInt() }
        return Pace(
            pagesPerHour = AnalyticsEngine.pagesPerHour(sessions),
            averageSessionMinutes = timed.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            longestSessionMinutes = timed.maxOrNull(),
            averageDaysToFinish = finishedReads.mapNotNull { it.days }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        )
    }

    /** Short, medium and long books by page count. */
    enum class Length(val label: String) { SHORT("Under 200 pages"), MEDIUM("200–399"), LONG("400+") }

    fun length(pages: Int): Length? = when {
        pages <= 0 -> null
        pages < 200 -> Length.SHORT
        pages < 400 -> Length.MEDIUM
        else -> Length.LONG
    }

    data class Shelf(
        val genres: List<Pair<String, Int>>,
        val formats: List<Pair<BookFormat, Int>>,
        val lengths: Map<Length, Int>,
        val averageRating: Float?,
        /** How many finished books got 1, 2, 3, 4 and 5 stars (rounded). */
        val ratingSpread: List<Int>,
        val finished: Int,
        val abandoned: Int
    ) {
        val isEmpty: Boolean get() = finished + abandoned == 0 && genres.isEmpty() && formats.isEmpty()
    }

    /**
     * What was read in the period: genres, formats and lengths across [read] (books
     * finished or read in it); ratings over [finished]; and finished against abandoned.
     */
    fun shelf(read: List<Book>, finished: List<Book>, abandoned: Int): Shelf {
        val ratings = finished.mapNotNull { it.overallRating }
        return Shelf(
            genres = AnalyticsEngine.genreMix(read),
            formats = read.groupingBy { it.format }.eachCount().toList().sortedByDescending { it.second },
            lengths = read.mapNotNull { length(it.totalPages) }.groupingBy { it }.eachCount(),
            averageRating = ratings.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            ratingSpread = (1..5).map { stars -> ratings.count { it.roundToInt().coerceIn(1, 5) == stars } },
            finished = finished.size,
            abandoned = abandoned
        )
    }
}
