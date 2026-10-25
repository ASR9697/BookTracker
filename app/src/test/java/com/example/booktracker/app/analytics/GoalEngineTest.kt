package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GoalEngineTest {

    @Test
    fun `calculate yearly pace ahead of schedule`() {
        // Day 180 of 365 (mid-year, ~50%), goal 24 books.
        // Expected ~11.8 books. Completed 15 books -> ahead!
        val date = LocalDate.of(2026, 6, 29)
        val pace = GoalEngine.calculateYearlyPace(
            yearlyGoal = 24,
            completedBooksThisYear = 15,
            date = date
        )
        assertEquals(GoalEngine.GoalPaceStatus.AHEAD, pace.status)
        assertTrue(pace.message.contains("ahead of schedule"))
    }

    @Test
    fun `calculate yearly pace behind schedule`() {
        // Day 270 of 365 (~74%), goal 24 books.
        // Expected ~17.7 books. Completed 10 books -> behind!
        val date = LocalDate.of(2026, 9, 27)
        val pace = GoalEngine.calculateYearlyPace(
            yearlyGoal = 24,
            completedBooksThisYear = 10,
            date = date
        )
        assertEquals(GoalEngine.GoalPaceStatus.BEHIND, pace.status)
        assertTrue(pace.message.contains("behind schedule"))
    }

    @Test
    fun `calculate yearly pace on track`() {
        // Day 182 of 365 (mid-year, ~50%), goal 24 books.
        // Expected 12 books. Completed 12 books -> on track!
        val date = LocalDate.of(2026, 7, 1)
        val pace = GoalEngine.calculateYearlyPace(
            yearlyGoal = 24,
            completedBooksThisYear = 12,
            date = date
        )
        assertEquals(GoalEngine.GoalPaceStatus.ON_TRACK, pace.status)
        assertEquals("On track for your yearly goal", pace.message)
    }

    @Test
    fun `calculate monthly summary aggregates pages and books`() {
        val zone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 10, 15)
        val timeOct10 = LocalDate.of(2026, 10, 10).atStartOfDay(zone).toInstant().toEpochMilli()
        val timeSep15 = LocalDate.of(2026, 9, 15).atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = listOf(
            Session(id = "s1", bookId = "b1", startTime = timeOct10, endTime = timeOct10 + 3600_000L, pagesRead = 50),
            Session(id = "s2", bookId = "b1", startTime = timeOct10 + 7200_000L, endTime = timeOct10 + 9000_000L, pagesRead = 30),
            Session(id = "s3", bookId = "b2", startTime = timeSep15, endTime = timeSep15 + 3600_000L, pagesRead = 40)
        )

        val books = listOf(
            Book(id = "b1", title = "B1", isbn = null, status = BookStatus.FINISHED.name, lastUpdated = timeOct10),
            Book(id = "b2", title = "B2", isbn = null, status = BookStatus.FINISHED.name, lastUpdated = timeSep15),
            Book(id = "b3", title = "B3", isbn = null, status = BookStatus.READING.name, lastUpdated = timeOct10)
        )

        val summary = GoalEngine.calculateMonthlySummary(
            books = books,
            sessions = sessions,
            monthlyGoalPages = 500,
            monthlyGoalBooks = 2,
            date = date,
            zone = zone
        )

        assertEquals(80, summary.pagesRead)
        assertEquals(1, summary.booksCompleted)
        assertEquals(1, summary.activeDays)
        assertEquals(31, summary.daysInMonth)
        assertEquals(500, summary.targetPages)
        assertEquals(2, summary.targetBooks)
    }
}
