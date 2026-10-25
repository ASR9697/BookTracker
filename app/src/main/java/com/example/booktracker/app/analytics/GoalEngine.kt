package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Pure logic engine for reading targets: yearly goal pacing, monthly page/book
 * targets, and on-track / ahead / behind schedule calculations.
 */
object GoalEngine {

    enum class GoalPaceStatus {
        AHEAD,
        ON_TRACK,
        BEHIND
    }

    data class YearlyGoalPace(
        val targetBooks: Int,
        val completedBooks: Int,
        val dayOfYear: Int,
        val totalDaysInYear: Int,
        val expectedBooks: Float,
        val difference: Float,
        val status: GoalPaceStatus,
        val message: String
    )

    data class MonthlyGoalSummary(
        val year: Int,
        val month: Int,
        val targetPages: Int,
        val pagesRead: Int,
        val targetBooks: Int,
        val booksCompleted: Int,
        val activeDays: Int,
        val daysInMonth: Int
    )

    /**
     * Calculates whether the reader is ahead, on track, or behind schedule for
     * their yearly reading target based on the current day of the year.
     */
    fun calculateYearlyPace(
        yearlyGoal: Int,
        completedBooksThisYear: Int,
        date: LocalDate = LocalDate.now()
    ): YearlyGoalPace {
        val safeGoal = yearlyGoal.coerceAtLeast(1)
        val daysInYear = date.lengthOfYear()
        val dayOfYear = date.dayOfYear
        val expected = (dayOfYear.toFloat() / daysInYear) * safeGoal
        val diff = completedBooksThisYear - expected

        val status = when {
            diff >= 0.75f -> GoalPaceStatus.AHEAD
            diff <= -0.75f -> GoalPaceStatus.BEHIND
            else -> GoalPaceStatus.ON_TRACK
        }

        val message = when (status) {
            GoalPaceStatus.AHEAD -> {
                val aheadCount = diff.roundToInt().coerceAtLeast(1)
                "+$aheadCount book${if (aheadCount > 1) "s" else ""} ahead of schedule"
            }
            GoalPaceStatus.BEHIND -> {
                val behindCount = (-diff).roundToInt().coerceAtLeast(1)
                "$behindCount book${if (behindCount > 1) "s" else ""} behind schedule"
            }
            GoalPaceStatus.ON_TRACK -> "On track for your yearly goal"
        }

        return YearlyGoalPace(
            targetBooks = safeGoal,
            completedBooks = completedBooksThisYear,
            dayOfYear = dayOfYear,
            totalDaysInYear = daysInYear,
            expectedBooks = expected,
            difference = diff,
            status = status,
            message = message
        )
    }

    /**
     * Aggregates monthly reading volume and completed books against monthly targets.
     */
    fun calculateMonthlySummary(
        books: List<Book>,
        sessions: List<Session>,
        monthlyGoalPages: Int,
        monthlyGoalBooks: Int,
        date: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): MonthlyGoalSummary {
        val daysInMonth = date.lengthOfMonth()
        val year = date.year
        val month = date.monthValue

        val pagesThisMonth = sessions
            .filter {
                val d = Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate()
                d.year == year && d.monthValue == month
            }
            .sumOf { it.pagesRead }

        val booksThisMonth = books
            .filter { it.status == BookStatus.FINISHED.name }
            .count {
                val d = Instant.ofEpochMilli(it.lastUpdated).atZone(zone).toLocalDate()
                d.year == year && d.monthValue == month
            }

        val activeDays = sessions
            .filter { it.pagesRead > 0 }
            .map { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }
            .filter { it.year == year && it.monthValue == month }
            .distinct()
            .count()

        return MonthlyGoalSummary(
            year = year,
            month = month,
            targetPages = monthlyGoalPages.coerceAtLeast(1),
            pagesRead = pagesThisMonth,
            targetBooks = monthlyGoalBooks.coerceAtLeast(1),
            booksCompleted = booksThisMonth,
            activeDays = activeDays,
            daysInMonth = daysInMonth
        )
    }
}
