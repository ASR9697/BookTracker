package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Daily-goal streak over completed sessions. A day counts when its summed
 * unitsRead meets the goal; the streak is the run of consecutive qualifying
 * days ending today (or yesterday, while today is still in progress).
 */
object StreakEngine {

    // Blueprint default; becomes user-configurable when a settings screen exists.
    const val DEFAULT_DAILY_GOAL_PAGES = 20

    data class StreakInfo(
        val currentStreak: Int,
        val pagesToday: Int,
        val dailyGoal: Int,
        val goalMetToday: Boolean
    )

    fun compute(
        sessions: List<Session>,
        dailyGoal: Int = DEFAULT_DAILY_GOAL_PAGES,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): StreakInfo {
        val pagesPerDay = sessions
            .filter { it.endTime > 0 }
            .groupBy { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.unitsRead } }

        val pagesToday = pagesPerDay[today] ?: 0
        val goalMetToday = pagesToday >= dailyGoal

        var streak = 0
        var day = if (goalMetToday) today else today.minusDays(1)
        while ((pagesPerDay[day] ?: 0) >= dailyGoal) {
            streak++
            day = day.minusDays(1)
        }
        return StreakInfo(
            currentStreak = streak,
            pagesToday = pagesToday,
            dailyGoal = dailyGoal,
            goalMetToday = goalMetToday
        )
    }
}
