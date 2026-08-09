package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Aggregates completed sessions into per-day page counts for the calendar
 * heatmap (blueprint §3A). Pure logic — no Android or Compose types.
 */
object ReadingCalendar {

    const val WEEKS = 52
    const val DAYS_PER_WEEK = 7

    data class Summary(
        val totalPages: Int,
        val activeDays: Int,
        val bestDayPages: Int
    )

    fun pagesByDay(
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, Int> =
        sessions
            .filter { it.endTime > 0 && it.pagesRead > 0 }
            .groupBy { StreakEngine.readingDate(it.endTime, zone) }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.pagesRead } }

    /** Sunday of the earliest visible week, so the grid is 7 whole rows tall. */
    fun gridStart(today: LocalDate): LocalDate {
        val sundayThisWeek = today.minusDays((today.dayOfWeek.value % 7).toLong())
        return sundayThisWeek.minusWeeks((WEEKS - 1).toLong())
    }

    fun summarize(pagesByDay: Map<LocalDate, Int>, start: LocalDate, today: LocalDate): Summary {
        val visible = pagesByDay.filterKeys { !it.isBefore(start) && !it.isAfter(today) }
        return Summary(
            totalPages = visible.values.sum(),
            activeDays = visible.count { it.value > 0 },
            bestDayPages = visible.values.maxOrNull() ?: 0
        )
    }

    /**
     * Sequential intensity level 0..4 for a day's page count, bucketed against
     * the daily goal. Level 0 = no reading; higher = darker in the single-hue ramp.
     */
    fun level(pages: Int, dailyGoal: Int): Int = when {
        pages <= 0 -> 0
        pages <= dailyGoal / 2 -> 1
        pages <= dailyGoal -> 2
        pages <= dailyGoal * 2 -> 3
        else -> 4
    }
}
