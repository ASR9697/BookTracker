package com.example.booktracker.app.analytics

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Stats over a period the reader picks: a week, a month, a year or all time, now
 * or stepped back. Works over per-day totals the app already keeps (pages, minutes,
 * finish days), so it stays pure and the screen just picks a [Period].
 */
object PeriodStats {

    enum class Span { WEEK, MONTH, YEAR, ALL }

    /** [offset] 0 is the current week/month/year, -1 the one before, and so on. */
    data class Period(val span: Span, val offset: Int = 0) {
        val previous: Period get() = copy(offset = offset - 1)
        val next: Period get() = copy(offset = (offset + 1).coerceAtMost(0))
        val isCurrent: Boolean get() = offset == 0
    }

    /** First and last day of [period]; all time runs from [firstDay] (or today) to today. */
    fun bounds(period: Period, today: LocalDate, weekStartsSunday: Boolean = false, firstDay: LocalDate? = null): ClosedRange<LocalDate> =
        when (period.span) {
            Span.WEEK -> {
                val first = if (weekStartsSunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
                val start = today.with(TemporalAdjusters.previousOrSame(first)).plusWeeks(period.offset.toLong())
                start..start.plusDays(6)
            }
            Span.MONTH -> {
                val start = today.withDayOfMonth(1).plusMonths(period.offset.toLong())
                start..start.withDayOfMonth(start.lengthOfMonth())
            }
            Span.YEAR -> {
                val start = today.withDayOfYear(1).plusYears(period.offset.toLong())
                start..start.withDayOfYear(start.lengthOfYear())
            }
            Span.ALL -> (firstDay?.takeIf { it <= today } ?: today)..today
        }

    /** "This week", "Last week", "Oct 12 – 18"; "October", "September 2025"; "2026"; "All time". */
    fun label(period: Period, today: LocalDate, weekStartsSunday: Boolean = false, locale: Locale = Locale.getDefault()): String {
        val range = bounds(period, today, weekStartsSunday)
        return when (period.span) {
            Span.WEEK -> when (period.offset) {
                0 -> "This week"
                -1 -> "Last week"
                else -> {
                    val day = DateTimeFormatter.ofPattern("MMM d", locale)
                    "${range.start.format(day)} – ${range.endInclusive.format(day)}"
                }
            }
            Span.MONTH -> {
                val month = range.start.month.getDisplayName(TextStyle.FULL, locale)
                if (range.start.year == today.year) month else "$month ${range.start.year}"
            }
            Span.YEAR -> range.start.year.toString()
            Span.ALL -> "All time"
        }
    }

    data class Summary(val books: Int, val pages: Int, val minutes: Long, val activeDays: Int)

    /** Totals inside [range]: books finished, pages, minutes read and days with any reading. */
    fun summary(
        range: ClosedRange<LocalDate>,
        pagesByDay: Map<LocalDate, Int>,
        minutesByDay: Map<LocalDate, Long>,
        finishDays: List<LocalDate>
    ): Summary {
        val pages = pagesByDay.filterKeys { it in range }
        val minutes = minutesByDay.filterKeys { it in range }
        return Summary(
            books = finishDays.count { it in range },
            pages = pages.values.sum(),
            minutes = minutes.values.sum(),
            activeDays = (pages.filterValues { it > 0 }.keys + minutes.filterValues { it > 0 }.keys).size
        )
    }

    /** Change from [before] to [now] as a whole percent; null when there's nothing to compare with. */
    fun change(now: Number, before: Number): Int? {
        val b = before.toDouble()
        if (b <= 0.0) return null
        return Math.round((now.toDouble() - b) / b * 100).toInt()
    }

    /** How much time one bar of the activity chart covers. */
    enum class Grain { DAY, MONTH, YEAR }

    /** One bar of the activity chart: its first day, a short axis label, the total, and what it covers. */
    data class Bucket(val start: LocalDate, val label: String, val value: Long, val grain: Grain = Grain.DAY)

    /**
     * The activity chart's bars: days for a week or a month, months for a year. All time
     * picks whatever fits how long you've been reading: days within a month, months up
     * to three years, years beyond. [values] is per-day pages or minutes.
     */
    fun buckets(
        period: Period,
        range: ClosedRange<LocalDate>,
        values: Map<LocalDate, Long>,
        locale: Locale = Locale.getDefault()
    ): List<Bucket> = when (period.span) {
        Span.WEEK, Span.MONTH -> generateSequence(range.start) { it.plusDays(1) }
            .takeWhile { it <= range.endInclusive }
            .map { day ->
                val label = if (period.span == Span.WEEK) day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
                else day.dayOfMonth.toString()
                Bucket(day, label, values[day] ?: 0L)
            }.toList()
        Span.YEAR -> months(range.start.withMonth(1), range.start.withMonth(12), values, locale, yearMarks = false, TextStyle.NARROW)
        Span.ALL -> {
            val first = range.start.withDayOfMonth(1)
            val last = range.endInclusive.withDayOfMonth(1)
            val monthCount = (last.year - first.year) * 12 + last.monthValue - first.monthValue + 1
            when {
                monthCount <= 1 -> buckets(Period(Span.MONTH), range, values, locale)
                monthCount <= MAX_MONTH_BARS -> months(
                    first, last, values, locale, yearMarks = first.year != last.year,
                    // A few wide bars have room for "Jul", which a lone "J" can't tell from June.
                    style = if (monthCount <= 6) TextStyle.SHORT else TextStyle.NARROW
                )
                else -> (range.start.year..range.endInclusive.year).map { y ->
                    Bucket(LocalDate.of(y, 1, 1), yearLabel(y), values.filterKeys { it.year == y }.values.sum(), Grain.YEAR)
                }
            }
        }
    }

    /** All time shows months until it would need more than this many bars. */
    private const val MAX_MONTH_BARS = 36

    /** One bar per month from [first] to [last]; with [yearMarks], each January is labelled with its year. */
    private fun months(first: LocalDate, last: LocalDate, values: Map<LocalDate, Long>, locale: Locale, yearMarks: Boolean, style: TextStyle) =
        generateSequence(first) { it.plusMonths(1) }.takeWhile { it <= last }.map { start ->
            val end = start.withDayOfMonth(start.lengthOfMonth())
            val label = if (yearMarks && start.monthValue == 1) yearLabel(start.year) else start.month.getDisplayName(style, locale)
            Bucket(start, label, values.filterKeys { it in start..end }.values.sum(), Grain.MONTH)
        }.toList()

    private fun yearLabel(year: Int) = "'${(year % 100).toString().padStart(2, '0')}"
}
