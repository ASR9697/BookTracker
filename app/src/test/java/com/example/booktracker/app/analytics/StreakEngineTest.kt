package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakEngineTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 7, 28)
    private val now: Instant = today.atStartOfDay(zone).toInstant().plusSeconds(12 * 3600)

    private fun sessionOn(date: LocalDate, pages: Int = 10): Session {
        val start = date.atStartOfDay(zone).toInstant().plusSeconds(9 * 3600).toEpochMilli()
        return Session(
            id = "s-$date-$pages",
            bookId = "b",
            startTime = start,
            endTime = start + 1_800_000L,
            durationSeconds = 1800,
            startPage = 0,
            endPage = pages,
            pagesRead = pages
        )
    }

    @Test
    fun `longest streak is zero with no reading days`() {
        assertEquals(0, StreakEngine.longestStreak(emptySet()))
    }

    @Test
    fun `longest streak counts a single day`() {
        assertEquals(1, StreakEngine.longestStreak(setOf(today)))
    }

    @Test
    fun `longest streak spans consecutive days only`() {
        val days = setOf(
            today.minusDays(10),
            // 4-day run
            today.minusDays(8),
            today.minusDays(7),
            today.minusDays(6),
            today.minusDays(5),
            // 2-day run
            today.minusDays(1),
            today
        )
        assertEquals(4, StreakEngine.longestStreak(days))
    }

    @Test
    fun `longest streak ignores duplicate and unordered input`() {
        val days = setOf(today, today.minusDays(2), today.minusDays(1))
        assertEquals(3, StreakEngine.longestStreak(days))
    }

    @Test
    fun `longest streak can exceed the current streak`() {
        // A 5-day run last month, then a gap, then today alone.
        val past = (20L..24L).map { today.minusDays(it) }
        val info = StreakEngine.compute(
            sessions = (past + today).map { sessionOn(it) },
            zone = zone,
            now = now
        )
        assertEquals(1, info.currentStreak)
        assertEquals(5, info.longestStreak)
    }

    @Test
    fun `current streak counts back from today when today has a session`() {
        val days = listOf(today, today.minusDays(1), today.minusDays(2))
        val info = StreakEngine.compute(days.map { sessionOn(it) }, zone = zone, now = now)
        assertEquals(3, info.currentStreak)
        assertEquals(3, info.longestStreak)
    }

    @Test
    fun `current streak survives a day that has not been read yet`() {
        // Nothing today, but yesterday and the day before — the streak is still alive.
        val days = listOf(today.minusDays(1), today.minusDays(2))
        val info = StreakEngine.compute(days.map { sessionOn(it) }, zone = zone, now = now)
        assertEquals(2, info.currentStreak)
    }

    @Test
    fun `a session with no progress is not a reading day`() {
        // Timer started and stopped without moving the bookmark.
        val info = StreakEngine.compute(listOf(sessionOn(today, pages = 0)), zone = zone, now = now)
        assertEquals(0, info.currentStreak)
        assertEquals(false, info.goalMetToday)
        assertEquals(emptySet<LocalDate>(), info.activeDays)
    }

    @Test
    fun `streak, heatmap, habit and monthly totals agree on active days`() {
        val sessions = listOf(
            sessionOn(today, pages = 12),
            sessionOn(today.minusDays(1), pages = 0),
            sessionOn(today.minusDays(2), pages = 8)
        )
        val streak = StreakEngine.compute(sessions, zone = zone, now = now)
        val heatmap = ReadingCalendar.pagesByDay(sessions, zone)
        val habit = GoalEngine.calculateHabitConsistency(sessions, 30, today, zone)
        val month = GoalEngine.calculateMonthlySummary(emptyList(), sessions, 500, 2, today, zone)

        assertEquals(setOf(today, today.minusDays(2)), streak.activeDays)
        assertEquals(streak.activeDays, heatmap.keys)
        assertEquals(2, habit.activeDays)
        assertEquals(2, month.activeDays)
        assertEquals(20, month.pagesRead)
        assertEquals(1, streak.currentStreak)
    }

    @Test
    fun `a 2 AM session belongs to the previous reading day everywhere`() {
        val twoAm = today.atStartOfDay(zone).toInstant().plusSeconds(2 * 3600).toEpochMilli()
        val late = Session(id = "late", bookId = "b", startTime = twoAm, endTime = twoAm + 600_000L, pagesRead = 5)
        val yesterday = today.minusDays(1)
        assertEquals(setOf(yesterday), StreakEngine.pagesByDay(listOf(late), zone).keys)
        assertEquals(setOf(yesterday), ReadingCalendar.pagesByDay(listOf(late), zone).keys)
        assertEquals(1, GoalEngine.calculateHabitConsistency(listOf(late), 30, today, zone).activeDays)
    }
}
