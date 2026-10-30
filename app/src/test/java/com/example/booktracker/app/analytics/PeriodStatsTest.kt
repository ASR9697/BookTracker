package com.example.booktracker.app.analytics

import com.example.booktracker.app.analytics.PeriodStats.Period
import com.example.booktracker.app.analytics.PeriodStats.Span
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class PeriodStatsTest {

    private val today = LocalDate.of(2026, 10, 30) // a Friday
    private val en = Locale.ENGLISH

    @Test
    fun `bounds follow the span, the offset and the week start`() {
        assertEquals(LocalDate.of(2026, 10, 26)..LocalDate.of(2026, 11, 1), PeriodStats.bounds(Period(Span.WEEK), today))
        assertEquals(LocalDate.of(2026, 10, 25)..LocalDate.of(2026, 10, 31), PeriodStats.bounds(Period(Span.WEEK), today, weekStartsSunday = true))
        assertEquals(LocalDate.of(2026, 9, 1)..LocalDate.of(2026, 9, 30), PeriodStats.bounds(Period(Span.MONTH, -1), today))
        assertEquals(LocalDate.of(2025, 1, 1)..LocalDate.of(2025, 12, 31), PeriodStats.bounds(Period(Span.YEAR, -1), today))
        assertEquals(LocalDate.of(2024, 3, 2)..today, PeriodStats.bounds(Period(Span.ALL), today, firstDay = LocalDate.of(2024, 3, 2)))
    }

    @Test
    fun `labels read naturally`() {
        assertEquals("This week", PeriodStats.label(Period(Span.WEEK), today, locale = en))
        assertEquals("Last week", PeriodStats.label(Period(Span.WEEK, -1), today, locale = en))
        assertEquals("Oct 12 – Oct 18", PeriodStats.label(Period(Span.WEEK, -2), today, locale = en))
        assertEquals("October", PeriodStats.label(Period(Span.MONTH), today, locale = en))
        assertEquals("December 2025", PeriodStats.label(Period(Span.MONTH, -10), today, locale = en))
        assertEquals("2025", PeriodStats.label(Period(Span.YEAR, -1), today, locale = en))
    }

    @Test
    fun `next never steps into the future`() {
        assertEquals(Period(Span.MONTH, 0), Period(Span.MONTH, 0).next)
        assertEquals(Period(Span.MONTH, -1), Period(Span.MONTH, 0).previous)
    }

    @Test
    fun `summary counts only days inside the range`() {
        val range = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        val summary = PeriodStats.summary(
            range,
            pagesByDay = mapOf(LocalDate.of(2026, 10, 3) to 20, LocalDate.of(2026, 10, 4) to 0, LocalDate.of(2026, 9, 30) to 99),
            minutesByDay = mapOf(LocalDate.of(2026, 10, 3) to 30L, LocalDate.of(2026, 10, 9) to 15L),
            finishDays = listOf(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 11, 1))
        )
        // Active: the 3rd (pages and minutes) and the 9th (minutes only); the 4th had no reading.
        assertEquals(PeriodStats.Summary(books = 1, pages = 20, minutes = 45, activeDays = 2), summary)
    }

    @Test
    fun `change is a percent, or nothing to compare`() {
        assertEquals(50, PeriodStats.change(150, 100))
        assertEquals(-25, PeriodStats.change(30L, 40L))
        assertNull(PeriodStats.change(10, 0))
    }

    @Test
    fun `buckets are days, months or years`() {
        val values = mapOf(LocalDate.of(2026, 10, 27) to 12L, LocalDate.of(2026, 3, 5) to 7L, LocalDate.of(2025, 6, 1) to 3L)
        val week = PeriodStats.buckets(Period(Span.WEEK), PeriodStats.bounds(Period(Span.WEEK), today), values, en)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), week.map { it.label })
        assertEquals(12L, week[1].value)
        val year = PeriodStats.buckets(Period(Span.YEAR), PeriodStats.bounds(Period(Span.YEAR), today), values, en)
        assertEquals(12, year.size)
        assertEquals(7L, year[2].value)
        assertEquals(12L, year[9].value)
    }

    @Test
    fun `all time picks days, months or years by how long you've read`() {
        val values = mapOf(LocalDate.of(2026, 10, 27) to 12L, LocalDate.of(2026, 3, 5) to 7L, LocalDate.of(2025, 6, 1) to 3L)
        fun all(first: LocalDate) = PeriodStats.buckets(Period(Span.ALL), PeriodStats.bounds(Period(Span.ALL), today, firstDay = first), values, en)

        val days = all(LocalDate.of(2026, 10, 3))
        assertEquals(PeriodStats.Grain.DAY, days.first().grain)
        assertEquals(28, days.size)

        val summer = all(LocalDate.of(2026, 7, 9))
        assertEquals(listOf("Jul", "Aug", "Sep", "Oct"), summer.map { it.label })

        val thisYear = all(LocalDate.of(2026, 3, 5))
        assertEquals(listOf("M", "A", "M", "J", "J", "A", "S", "O"), thisYear.map { it.label })
        assertEquals(listOf(7L, 0L, 0L, 0L, 0L, 0L, 0L, 12L), thisYear.map { it.value })

        val twoYears = all(LocalDate.of(2025, 6, 1))
        assertEquals(17, twoYears.size)
        assertEquals("'26", twoYears[7].label)
        assertEquals(3L, twoYears.first().value)

        val years = all(LocalDate.of(2022, 1, 1))
        assertEquals(listOf("'22", "'23", "'24", "'25", "'26"), years.map { it.label })
        assertEquals(PeriodStats.Grain.YEAR, years.last().grain)
        assertEquals(19L, years.last().value)
    }
}
