package com.example.booktracker.app.analytics

import com.example.booktracker.app.analytics.FinishByPlanner.Status
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinishByPlannerTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 10, 27)

    private fun dayMillis(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun read(date: LocalDate, pages: Int) = Session(
        id = "s-$date-$pages",
        bookId = "b",
        startTime = dayMillis(date) + 12 * 3_600_000L,
        endTime = dayMillis(date) + 13 * 3_600_000L,
        durationSeconds = 3600,
        pagesRead = pages,
        zoneId = "UTC"
    )

    private fun book(current: Int, total: Int = 300, finishBy: LocalDate? = today.plusDays(9)) =
        Book(id = "b", title = "T", isbn = null, totalPages = total, currentPage = current, finishBy = finishBy?.let(::dayMillis))

    private fun plan(book: Book, sessions: List<Session>) =
        FinishByPlanner.plan(book, sessions, today, zone, dayStartsAtHour = 0)

    @Test
    fun `pages a day spread what was left this morning over the days remaining`() {
        // 100 pages left this morning, 10 days including today; 4 already read today.
        val result = plan(book(current = 204), listOf(read(today, 4)))!!
        assertEquals(10, result.daysLeft)
        assertEquals(10, result.pagesPerDay)
        assertEquals(4, result.readToday)
        assertEquals(6, result.leftToday)
        assertEquals(96, result.pagesLeft)
    }

    @Test
    fun `keeping up with the plan is on track and falling short is behind`() {
        val steady = (1L..5L).map { read(today.minusDays(it), 10) }
        assertEquals(Status.ON_TRACK, plan(book(current = 200), steady)!!.status)
        val slow = (1L..5L).map { read(today.minusDays(it), 4) }
        assertEquals(Status.BEHIND, plan(book(current = 200), slow)!!.status)
        val fast = (1L..5L).map { read(today.minusDays(it), 30) }
        assertEquals(Status.AHEAD, plan(book(current = 200), fast)!!.status)
    }

    @Test
    fun `pace counts from the first reading day in the window`() {
        // Nothing read yet today: the two days up to yesterday set the pace.
        assertEquals(15.0, FinishByPlanner.recentPace(mapOf(today.minusDays(2) to 30), today)!!, 0.001)
        // Once today has reading it joins the window.
        assertEquals(15.0, FinishByPlanner.recentPace(mapOf(today.minusDays(2) to 30, today to 15), today)!!, 0.001)
        assertNull(FinishByPlanner.recentPace(mapOf(today.minusDays(30) to 50), today))
    }

    @Test
    fun `past the deadline is overdue and the last page is done`() {
        assertEquals(Status.OVERDUE, plan(book(current = 100, finishBy = today.minusDays(1)), emptyList())!!.status)
        assertEquals(Status.DONE, plan(book(current = 300), emptyList())!!.status)
    }

    @Test
    fun `no deadline or no page count means no plan`() {
        assertNull(plan(book(current = 10, finishBy = null), emptyList()))
        assertNull(plan(book(current = 10, total = 0), emptyList()))
    }

    @Test
    fun `without any reading yet the plan still gives a daily target`() {
        val result = plan(book(current = 0), emptyList())!!
        assertEquals(Status.NO_PACE, result.status)
        assertEquals(30, result.pagesPerDay)
    }
}
