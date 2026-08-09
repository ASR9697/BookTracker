package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StreakEngine {

    const val DEFAULT_DAILY_GOAL_PAGES = 20

    data class StreakInfo(
        val currentStreak: Int,
        val longestStreak: Int,
        val pagesToday: Int,
        val dailyGoal: Int,
        val goalMetToday: Boolean,
        val activeDays: Set<LocalDate> // To support the 7-day bubble bar
    )

    fun compute(
        sessions: List<Session>,
        dailyGoal: Int = DEFAULT_DAILY_GOAL_PAGES,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
        dayStartsAtHour: Int = 3
    ): StreakInfo {
        val today = readingDate(now.toEpochMilli(), zone, dayStartsAtHour)

        val pagesPerDay = sessions
            .filter { it.startTime > 0 }
            .groupBy { readingDate(it.startTime, zone, dayStartsAtHour) }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.pagesRead.coerceAtLeast(0) } }

        val activeDays = sessions
            .filter { it.startTime > 0 }
            .map { readingDate(it.startTime, zone, dayStartsAtHour) }
            .toSet()

        val pagesToday = pagesPerDay[today] ?: 0
        val goalMetToday = activeDays.contains(today) // "Fill bubble if >= 1 ReadingSession exists"

        var streak = 0
        var day = if (goalMetToday) today else today.minusDays(1)
        while (activeDays.contains(day)) {
            streak++
            day = day.minusDays(1)
        }

        return StreakInfo(
            currentStreak = streak,
            longestStreak = longestStreak(activeDays),
            pagesToday = pagesToday,
            dailyGoal = dailyGoal,
            goalMetToday = goalMetToday,
            activeDays = activeDays
        )
    }

    /**
     * Longest run of consecutive reading days ever recorded. Walking the sorted
     * days once is enough: a run only breaks when the gap to the previous day is
     * more than one, so we never need to re-scan from each start.
     */
    fun longestStreak(activeDays: Set<LocalDate>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i - 1].plusDays(1) == sorted[i]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    // Reading day = the local calendar day in the phone's current timezone.
    // By subtracting dayStartsAtHour, a session at 2:00 AM (when dayStartsAtHour is 3)
    // effectively evaluates as 11:00 PM of the previous day, which assigns it to the correct Streak date.
    fun readingDate(timestamp: Long, zone: ZoneId, dayStartsAtHour: Int = 3): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).minusHours(dayStartsAtHour.toLong()).toLocalDate()
}
