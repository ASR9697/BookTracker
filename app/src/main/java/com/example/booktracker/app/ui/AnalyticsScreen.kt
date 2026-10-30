package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.staggeredItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.FinishedShelf
import com.example.booktracker.app.analytics.FocusInsights
import com.example.booktracker.app.analytics.GoalEngine
import com.example.booktracker.app.analytics.PeriodStats
import com.example.booktracker.app.analytics.ReadingCalendar
import com.example.booktracker.app.analytics.StatsBreakdown
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Stats: your reading over a period you pick. A week, month, year or all time sets
 * everything below: totals against the period before, activity as bars, when you
 * read, how fast, where you focus, and what you read. Targets live on Goals; this
 * page links there and doesn't repeat them.
 */
@Composable
fun AnalyticsScreen(
    books: List<Book>,
    sessions: List<Session>,
    yearlyGoal: Int,
    dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR,
    onTidyTags: () -> Unit = {},
    /** Remembers the period the reader picked, for next time. */
    onSpanChange: (PeriodStats.Span) -> Unit = {},
    onOpenGoals: () -> Unit = {},
    onOpenFinished: () -> Unit = {},
    onOpenBook: (Book) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(dayStartsAtHour) { StreakEngine.readingDate(System.currentTimeMillis(), zone, dayStartsAtHour) }
    val pagesByDay = remember(sessions, dayStartsAtHour) { ReadingCalendar.pagesByDay(sessions, zone, dayStartsAtHour) }
    val minutesByDay = remember(sessions, dayStartsAtHour) { AnalyticsEngine.minutesByDay(sessions, zone, dayStartsAtHour) }
    var showMinutes by remember { mutableStateOf(false) }

    val finishedBooks = remember(books) { books.filter(FinishedShelf::isFinished).sortedByDescending { it.completedAt } }
    val finishDays = remember(finishedBooks, dayStartsAtHour) {
        finishedBooks.map { StreakEngine.readingDate(it.completedAt, zone, dayStartsAtHour) }
    }

    // The period everything below follows; the span is remembered, stepping back isn't.
    val prefs = LocalAppPrefs.current
    var period by remember(prefs.statsSpan) {
        mutableStateOf(PeriodStats.Period(PeriodStats.Span.entries.firstOrNull { it.name == prefs.statsSpan } ?: PeriodStats.Span.MONTH))
    }
    val firstDay = remember(pagesByDay, minutesByDay, finishDays) { (pagesByDay.keys + minutesByDay.keys + finishDays).minOrNull() }
    val range = PeriodStats.bounds(period, today, prefs.weekStartsSunday, firstDay)
    val periodLabel = PeriodStats.label(period, today, prefs.weekStartsSunday)
    val periodSummary = remember(range, pagesByDay, minutesByDay, finishDays) {
        PeriodStats.summary(range, pagesByDay, minutesByDay, finishDays)
    }
    val before = if (period.span == PeriodStats.Span.ALL) null else remember(period, today, pagesByDay, minutesByDay, finishDays) {
        PeriodStats.summary(PeriodStats.bounds(period.previous, today, prefs.weekStartsSunday), pagesByDay, minutesByDay, finishDays)
    }
    val comparedWith = PeriodStats.label(period.previous, today, prefs.weekStartsSunday)
        .let { if (it.startsWith("Last") || it.startsWith("This")) it.lowercase() else it }
    val buckets = remember(period, range, showMinutes, pagesByDay, minutesByDay) {
        PeriodStats.buckets(period, range, if (showMinutes) minutesByDay else pagesByDay.mapValues { it.value.toLong() })
    }

    // The breakdowns, all for the same period.
    val periodSessions = remember(sessions, range, dayStartsAtHour) { StatsBreakdown.sessionsIn(sessions, range, zone, dayStartsAtHour) }
    val rhythm = remember(periodSessions) { StatsBreakdown.rhythm(periodSessions, zone) }
    val peak = remember(rhythm) {
        StatsBreakdown.peak(rhythm)?.let { (day, part) ->
            "${day.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${part.label.lowercase()}s"
        }
    }
    val daysSoFar = (ChronoUnit.DAYS.between(range.start, minOf(range.endInclusive, today)) + 1).toInt().coerceAtLeast(1)
    val finishedInPeriod = remember(finishedBooks, range) {
        finishedBooks.filter { StreakEngine.readingDate(it.completedAt, zone, dayStartsAtHour) in range }
    }
    val abandonedInPeriod = remember(books, range) {
        books.count { book ->
            FinishedShelf.isAbandoned(book) &&
                book.finishedAt?.let { StreakEngine.readingDate(it, zone, dayStartsAtHour) in range } == true
        }
    }
    val pace = remember(periodSessions, finishedInPeriod) {
        StatsBreakdown.pace(periodSessions, FinishedShelf.reads(finishedInPeriod, sessions, zone) { true })
    }
    val shelf = remember(books, periodSessions, finishedInPeriod, abandonedInPeriod) {
        val readIds = periodSessions.map { it.bookId }.toSet()
        val read = (finishedInPeriod + books.filter { it.id in readIds }).distinctBy { it.id }
        StatsBreakdown.shelf(read, finishedInPeriod, abandonedInPeriod)
    }
    val focus = remember(periodSessions) { FocusInsights.summarize(periodSessions, zone) }
    val places = remember(periodSessions) { AnalyticsEngine.environmentStats(periodSessions) }

    // One line about the year's target; the target itself lives on Goals.
    val goalLine = remember(finishedBooks, yearlyGoal, today) {
        val thisYear = finishedBooks.count { StreakEngine.readingDate(it.completedAt, zone, dayStartsAtHour).year == today.year }
        when {
            yearlyGoal <= 0 -> "Set a reading goal"
            thisYear >= yearlyGoal -> "$thisYear / $yearlyGoal books this year · goal reached"
            else -> {
                val status = GoalEngine.calculateYearlyPace(yearlyGoal, thisYear, today, GoalEngine.trackingStart(books, sessions, zone)).status
                "$thisYear / $yearlyGoal books this year · " + when (status) {
                    GoalEngine.GoalPaceStatus.AHEAD -> "ahead"
                    GoalEngine.GoalPaceStatus.ON_TRACK -> "on track"
                    GoalEngine.GoalPaceStatus.BEHIND -> "behind"
                }
            }
        }
    }

    val entrance = rememberEntranceState()
    SectionPage(
        title = "Stats",
        line = if (periodSummary.pages > 0) String.format(Locale.getDefault(), "%,d pages · %s", periodSummary.pages, periodLabel)
        else "Your reading, in numbers",
        modifier = modifier
    ) {
    // One column on a phone; on a tablet the cards sit side by side, two or three across.
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 360.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + LocalNavBarClearance.current),
        verticalItemSpacing = 20.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            PeriodSwitcher(
                period = period,
                label = periodLabel,
                onSpan = { span ->
                    period = PeriodStats.Period(span)
                    onSpanChange(span)
                },
                onStep = { period = it }
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) { SummaryStrip(periodSummary, before, comparedWith) }
        item(span = StaggeredGridItemSpan.FullLine) {
            ActivityChart(buckets, period.span, showMinutes, onShowMinutes = { showMinutes = it })
        }
        staggeredItem(entrance, 0) { RhythmCard(rhythm, periodSummary.activeDays, daysSoFar, peak) }
        staggeredItem(entrance, 1) { PaceCard(pace) }
        if (focus != null || places.isNotEmpty()) {
            staggeredItem(entrance, 2) { FocusPlacesCard(focus, places) }
        }
        if (!shelf.isEmpty) {
            staggeredItem(entrance, 3) { WhatYouReadCard(shelf, onTidyTags) }
        }
        if (finishedInPeriod.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { FinishedInPeriod(finishedInPeriod, onOpenBook, onOpenFinished) }
        }
        item(span = StaggeredGridItemSpan.FullLine) { GoalsLink(goalLine, onOpenGoals) }
    }
    }
}
