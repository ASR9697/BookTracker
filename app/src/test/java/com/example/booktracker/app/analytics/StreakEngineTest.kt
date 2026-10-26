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
    fun `a timer stopped within seconds with no progress is not a reading day`() {
        val blip = sessionOn(today, pages = 0).copy(durationSeconds = 20)
        val info = StreakEngine.compute(listOf(blip), zone = zone, now = now)
        assertEquals(0, info.currentStreak)
        assertEquals(false, info.goalMetToday)
        assertEquals(emptySet<LocalDate>(), info.activeDays)
    }

    @Test
    fun `a real timer run counts even without a page update`() {
        // 30 minutes of reading, page not updated: Bookmory counts this day, so do we.
        val info = StreakEngine.compute(listOf(sessionOn(today, pages = 0)), zone = zone, now = now)
        assertEquals(1, info.currentStreak)
        assertEquals(0, info.pagesToday)
    }

    @Test
    fun `a session keeps the day of the zone it was recorded in`() {
        // 23:30 in India is 14:00 in New York the same day, but 00:30 the next day would not be.
        val kolkata = ZoneId.of("Asia/Kolkata")
        val newYork = ZoneId.of("America/New_York")
        val lateInIndia = LocalDate.of(2026, 9, 10).atTime(23, 30).atZone(kolkata).toInstant().toEpochMilli()
        val earlyInIndia = LocalDate.of(2026, 9, 11).atTime(0, 30).atZone(kolkata).toInstant().toEpochMilli()
        fun s(at: Long) = Session(id = "s$at", bookId = "b", startTime = at, endTime = at + 600_000, durationSeconds = 600, pagesRead = 5, zoneId = "Asia/Kolkata")
        // Evaluated on a phone now set to New York, with days starting at midnight.
        assertEquals(LocalDate.of(2026, 9, 10), StreakEngine.sessionDay(s(lateInIndia), newYork, 0))
        assertEquals(LocalDate.of(2026, 9, 11), StreakEngine.sessionDay(s(earlyInIndia), newYork, 0))
        // Without a recorded zone the phone's zone decides: both land on Sep 10 in New York.
        assertEquals(LocalDate.of(2026, 9, 10), StreakEngine.sessionDay(s(earlyInIndia).copy(zoneId = ""), newYork, 0))
        // An unknown zone id falls back rather than crashing.
        assertEquals(LocalDate.of(2026, 9, 10), StreakEngine.sessionDay(s(earlyInIndia).copy(zoneId = "Mars/Base"), newYork, 0))
    }

    @Test
    fun `streak, heatmap, habit and monthly totals agree on active days`() {
        val sessions = listOf(
            sessionOn(today, pages = 12),
            sessionOn(today.minusDays(1), pages = 0).copy(durationSeconds = 20),
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
