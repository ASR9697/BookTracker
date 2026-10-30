package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StreakEngine {

    const val DEFAULT_DAILY_GOAL_PAGES = 20
    const val DEFAULT_DAY_START_HOUR = 3

    /** A timer run at least this long counts as reading even without a page update. */
    const val MIN_TIMED_READING_SECONDS = 60

    data class StreakInfo(
        val currentStreak: Int,
        val longestStreak: Int,
        val pagesToday: Int,
        val dailyGoal: Int,
        /** Whether today counts toward the streak, i.e. any reading at all. For the page goal use [goalReached]. */
        val goalMetToday: Boolean,
        val activeDays: Set<LocalDate>, // To support the 7-day bubble bar
        /** "Today" under the day-start setting, so UI week bars agree with the streak. */
        val today: LocalDate = LocalDate.now(),
        /** Hour the reading day rolls over, so other screens bucket days the same way. */
        val dayStartsAtHour: Int = DEFAULT_DAY_START_HOUR
    ) {
        /** Whether today's pages have reached the daily page goal. False with no goal set. */
        val goalReached: Boolean get() = dailyGoal > 0 && pagesToday >= dailyGoal
    }

    fun compute(
        sessions: List<Session>,
        dailyGoal: Int = DEFAULT_DAILY_GOAL_PAGES,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
        dayStartsAtHour: Int = DEFAULT_DAY_START_HOUR
    ): StreakInfo {
        val today = readingDate(now.toEpochMilli(), zone, dayStartsAtHour)

        val pagesPerDay = pagesByDay(sessions, zone, dayStartsAtHour)
        val activeDays = pagesPerDay.keys

        val pagesToday = pagesPerDay[today] ?: 0
        val goalMetToday = activeDays.contains(today)

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
            activeDays = activeDays,
            today = today,
            dayStartsAtHour = dayStartsAtHour
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

    /** One unbroken run of reading days, both ends inclusive. */
    data class StreakRun(val start: LocalDate, val end: LocalDate) {
        val length: Int get() = (end.toEpochDay() - start.toEpochDay()).toInt() + 1
    }

    /** Every run of consecutive reading days, most recent first. */
    fun runs(activeDays: Set<LocalDate>): List<StreakRun> {
        if (activeDays.isEmpty()) return emptyList()
        val sorted = activeDays.sorted()
        val result = mutableListOf<StreakRun>()
        var start = sorted.first()
        var previous = start
        for (day in sorted.drop(1)) {
            if (day != previous.plusDays(1)) {
                result += StreakRun(start, previous)
                start = day
            }
            previous = day
        }
        result += StreakRun(start, previous)
        return result.reversed()
    }

    /**
     * The one rule for "the reader read": the session moved the bookmark forward,
     * or the timer ran for at least [MIN_TIMED_READING_SECONDS]. Streaks, the
     * heatmap, habit health and monthly totals all count through this, so a timer
     * stopped after a few seconds never counts on one screen and not another.
     */
    fun isReading(session: Session): Boolean =
        session.startTime > 0 && (session.pagesRead > 0 || session.durationSeconds >= MIN_TIMED_READING_SECONDS)

    /**
     * Reading day a session belongs to; every per-day aggregation keys on this.
     * Uses the zone the session was recorded in, so reading done abroad keeps its
     * day after the reader travels home; [zone] covers sessions without one.
     */
    fun sessionDay(session: Session, zone: ZoneId, dayStartsAtHour: Int = DEFAULT_DAY_START_HOUR): LocalDate =
        readingDate(session.startTime, recordedZone(session) ?: zone, dayStartsAtHour)

    private fun recordedZone(session: Session): ZoneId? =
        session.zoneId.takeIf { it.isNotBlank() }?.let { runCatching { ZoneId.of(it) }.getOrNull() }

    /** Pages read per reading day, for every day with reading activity (timer-only days map to 0). */
    fun pagesByDay(
        sessions: List<Session>,
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = DEFAULT_DAY_START_HOUR
    ): Map<LocalDate, Int> =
        sessions
            .filter(::isReading)
            .groupBy { sessionDay(it, zone, dayStartsAtHour) }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.pagesRead } }

    // Reading day = the local calendar day in the phone's current timezone.
    // By subtracting dayStartsAtHour, a session at 2:00 AM (when dayStartsAtHour is 3)
    // effectively evaluates as 11:00 PM of the previous day, which assigns it to the correct Streak date.
    fun readingDate(timestamp: Long, zone: ZoneId, dayStartsAtHour: Int = DEFAULT_DAY_START_HOUR): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).minusHours(dayStartsAtHour.toLong()).toLocalDate()
}
