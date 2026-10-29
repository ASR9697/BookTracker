package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

/**
 * "Finish by" deadlines: how many pages a day it takes to be done on time, and
 * whether the pace the reader actually keeps gets them there. Today's target is
 * worked out from where the day started, so it holds steady while you read and
 * only adjusts tomorrow, after real progress (or a missed day) is known.
 */
object FinishByPlanner {

    /** Days of this book's recent reading that set "your pace". */
    const val PACE_WINDOW_DAYS = 14

    /** Finishing this many days early counts as ahead rather than merely on track. */
    const val AHEAD_MARGIN_DAYS = 2

    enum class Status { DONE, ON_TRACK, AHEAD, BEHIND, OVERDUE, NO_PACE }

    data class Plan(
        val target: LocalDate,
        /** Reading days left including today; 0 once the deadline has passed. */
        val daysLeft: Int,
        val pagesLeft: Int,
        /** Pages a day needed from today on, counted from where today started. */
        val pagesPerDay: Int,
        val readToday: Int,
        /** Pages still to read today to stay on plan. */
        val leftToday: Int,
        /** Pages a day in this book lately; null before there is any reading to go on. */
        val recentPace: Double?,
        /** When the book ends at [recentPace], if it holds. */
        val projectedFinish: LocalDate?,
        val status: Status
    )

    /** Null when the book has no page count or no deadline. */
    fun plan(
        book: Book,
        sessions: List<Session>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
    ): Plan? {
        val finishBy = book.finishBy ?: return null
        if (book.totalPages <= 0) return null
        val target = java.time.Instant.ofEpochMilli(finishBy).atZone(zone).toLocalDate()

        val bookSessions = sessions.filter { it.bookId == book.id && StreakEngine.isReading(it) }
        val pagesByDay = bookSessions
            .groupBy { StreakEngine.sessionDay(it, zone, dayStartsAtHour) }
            .mapValues { (_, list) -> list.sumOf { it.pagesRead } }
        val readToday = pagesByDay[today] ?: 0
        val pagesLeft = (book.totalPages - book.currentPage).coerceAtLeast(0)
        val leftAtDayStart = (pagesLeft + readToday).coerceAtMost(book.totalPages)

        val daysLeft = (target.toEpochDay() - today.toEpochDay() + 1).toInt().coerceAtLeast(0)
        val pagesPerDay = if (daysLeft > 0) ceil(leftAtDayStart.toDouble() / daysLeft).toInt() else leftAtDayStart
        val leftToday = (pagesPerDay - readToday).coerceIn(0, pagesLeft)

        val pace = recentPace(pagesByDay, today)
        val projected = pace?.let { perDay ->
            if (pagesLeft == 0) today
            else {
                // Today's reading counts toward today, so only the rest needs new days.
                val stillToday = (perDay - readToday).coerceAtLeast(0.0)
                val afterToday = (pagesLeft - stillToday).coerceAtLeast(0.0)
                today.plusDays(ceil(afterToday / perDay).toLong())
            }
        }

        val status = when {
            pagesLeft == 0 -> Status.DONE
            today.isAfter(target) -> Status.OVERDUE
            projected == null -> Status.NO_PACE
            projected.isAfter(target) -> Status.BEHIND
            projected.isBefore(target.minusDays(AHEAD_MARGIN_DAYS.toLong())) -> Status.AHEAD
            else -> Status.ON_TRACK
        }

        return Plan(
            target = target,
            daysLeft = daysLeft,
            pagesLeft = pagesLeft,
            pagesPerDay = pagesPerDay,
            readToday = readToday,
            leftToday = leftToday,
            recentPace = pace,
            projectedFinish = projected,
            status = status
        )
    }

    /**
     * Pages a day over the last [PACE_WINDOW_DAYS], counted from the first day of
     * reading inside that window so a book started three days ago isn't diluted
     * by eleven days it didn't exist. Today only counts once something is read,
     * so the pace doesn't sag every morning.
     */
    fun recentPace(pagesByDay: Map<LocalDate, Int>, today: LocalDate): Double? {
        val end = if ((pagesByDay[today] ?: 0) > 0) today else today.minusDays(1)
        val windowStart = today.minusDays(PACE_WINDOW_DAYS - 1L)
        val recent = pagesByDay.filterKeys { !it.isBefore(windowStart) && !it.isAfter(end) }
        val first = recent.filterValues { it > 0 }.keys.minOrNull() ?: return null
        val days = end.toEpochDay() - first.toEpochDay() + 1
        val pages = recent.values.sum()
        return if (pages > 0) pages.toDouble() / days else null
    }
}
