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
        val message: String,
        val projectedFinishBooks: Int = completedBooks,
        val paceRatio: Float = 1f,
        val projectionMessage: String = ""
    )

    data class HabitConsistency(
        val activeDays: Int,
        val totalDays: Int,
        val consistencyPercent: Int,
        val rating: String,
        val description: String
    )

    enum class Season(
        val title: String,
        val emoji: String,
        val startMonth: Int,
        val startDay: Int,
        val endMonth: Int,
        val endDay: Int,
        val defaultSprintGoal: Int = 5
    ) {
        SPRING("Spring Blossom Sprint", "🌸", 3, 1, 5, 31, 5),
        SUMMER("Summer Solstice Sprint", "☀️", 6, 1, 8, 31, 5),
        AUTUMN("Autumn Equinox Sprint", "🍂", 9, 1, 11, 30, 5),
        WINTER("Winter Frost Sprint", "❄️", 12, 1, 2, 28, 5)
    }

    data class SeasonalSprint(
        val season: Season,
        val targetBooks: Int,
        val completedBooks: Int,
        val progress: Float,
        val daysRemaining: Int,
        val isCompleted: Boolean,
        val statusMessage: String
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

    /** Days of tracking needed before the year-end projection is shown. */
    const val MIN_DAYS_FOR_PROJECTION = 14

    /** First day the reader added a book or logged a session, or null with no data. */
    fun trackingStart(books: List<Book>, sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        (books.map { it.dateAdded.takeIf { d -> d > 0 } ?: it.lastUpdated } + sessions.map { it.startTime })
            .filter { it > 0 }
            .minOrNull()
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    /**
     * Calculates whether the reader is ahead, on track, or behind schedule for
     * their yearly reading target, including a velocity-based year-end forecast.
     *
     * Pace runs from [trackingStart] when that falls inside [date]'s year, so a
     * reader who starts in October is not judged on January to September.
     */
    fun calculateYearlyPace(
        yearlyGoal: Int,
        completedBooksThisYear: Int,
        date: LocalDate = LocalDate.now(),
        trackingStart: LocalDate? = null
    ): YearlyGoalPace {
        val safeGoal = yearlyGoal.coerceAtLeast(1)
        val daysInYear = date.lengthOfYear()
        val dayOfYear = date.dayOfYear
        val startDay = trackingStart
            ?.takeIf { it.year == date.year && !it.isAfter(date) }
            ?.dayOfYear ?: 1
        val windowDays = daysInYear - startDay + 1
        val elapsedDays = dayOfYear - startDay + 1
        val expected = (elapsedDays.toFloat() / windowDays) * safeGoal
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

        // Books before tracking began still count; the rate comes from the tracked window.
        val projected = ((completedBooksThisYear.toFloat() / elapsedDays) * windowDays)
            .roundToInt().coerceAtLeast(completedBooksThisYear)

        val paceRatio = if (expected > 0f) (completedBooksThisYear / expected).coerceIn(0f, 3f) else 1f

        val projectionMessage = when {
            elapsedDays < MIN_DAYS_FOR_PROJECTION && completedBooksThisYear < safeGoal ->
                "Year-end forecast appears after ${MIN_DAYS_FOR_PROJECTION} days of reading"
            projected > safeGoal -> "Projected to finish ${bookCount(projected)} by Dec 31 (+${projected - safeGoal} above target)"
            projected == safeGoal -> "Projected to reach exactly ${bookCount(projected)} by Dec 31"
            // Within the on-track band a small gap gets magnified by the projection; don't call it short.
            status == GoalPaceStatus.ON_TRACK -> "Projected to finish ${bookCount(projected)}, just shy of your ${bookCount(safeGoal)}"
            else -> "Projected to finish ${bookCount(projected)} (${safeGoal - projected} short of target)"
        }

        return YearlyGoalPace(
            targetBooks = safeGoal,
            completedBooks = completedBooksThisYear,
            dayOfYear = dayOfYear,
            totalDaysInYear = daysInYear,
            expectedBooks = expected,
            difference = diff,
            status = status,
            message = message,
            projectedFinishBooks = projected,
            paceRatio = paceRatio,
            projectionMessage = projectionMessage
        )
    }

    private fun bookCount(n: Int) = "$n book${if (n == 1) "" else "s"}"

    /**
     * Evaluates daily reading habit consistency over a rolling window (e.g. 30 days or year-to-date).
     */
    fun calculateHabitConsistency(
        sessions: List<Session>,
        daysWindow: Int = 30,
        date: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
    ): HabitConsistency {
        val safeWindow = daysWindow.coerceAtLeast(7)
        val startDate = date.minusDays((safeWindow - 1).toLong())

        val activeDates = StreakEngine.pagesByDay(sessions, zone, dayStartsAtHour).keys
            .count { !it.isBefore(startDate) && !it.isAfter(date) }

        val percent = ((activeDates.toFloat() / safeWindow) * 100f).roundToInt().coerceIn(0, 100)

        val (rating, description) = when {
            percent >= 80 -> "Elite Daily Habit" to "Outstanding consistency! Reading is an ingrained daily ritual."
            percent >= 55 -> "Consistent Reader" to "Strong reading rhythm. You read on most days of the week."
            percent >= 30 -> "Active Explorer" to "Good habit foundation. Steady reading progress on weekends and quiet evenings."
            else -> "Casual Reader" to "Spontaneous reader. Dive in for 10 minutes today to boost your habit score!"
        }

        return HabitConsistency(
            activeDays = activeDates,
            totalDays = safeWindow,
            consistencyPercent = percent,
            rating = rating,
            description = description
        )
    }

    /**
     * Determines current season based on local calendar date.
     */
    fun currentSeason(date: LocalDate = LocalDate.now()): Season {
        val month = date.monthValue
        return when (month) {
            in 3..5 -> Season.SPRING
            in 6..8 -> Season.SUMMER
            in 9..11 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    /**
     * Evaluates active seasonal reading sprint challenge.
     */
    fun calculateSeasonalSprint(
        books: List<Book>,
        date: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        sprintGoal: Int = 5
    ): SeasonalSprint {
        val season = currentSeason(date)
        val year = date.year

        // Determine start and end date for season
        val (startDate, endDate) = when (season) {
            Season.SPRING -> LocalDate.of(year, 3, 1) to LocalDate.of(year, 5, 31)
            Season.SUMMER -> LocalDate.of(year, 6, 1) to LocalDate.of(year, 8, 31)
            Season.AUTUMN -> LocalDate.of(year, 9, 1) to LocalDate.of(year, 11, 30)
            Season.WINTER -> {
                val startYear = if (date.monthValue == 12) year else year - 1
                val endYear = startYear + 1
                val febDays = LocalDate.of(endYear, 2, 1).lengthOfMonth()
                LocalDate.of(startYear, 12, 1) to LocalDate.of(endYear, 2, febDays)
            }
        }

        val completedThisSeason = books
            .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) && it.completedAt > 0L }
            .count {
                val finishedDate = Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate()
                !finishedDate.isBefore(startDate) && !finishedDate.isAfter(endDate)
            }

        val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(date, endDate).toInt().coerceAtLeast(0)
        val progress = (completedThisSeason.toFloat() / sprintGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
        val isCompleted = completedThisSeason >= sprintGoal

        val message = when {
            isCompleted -> "Sprint Completed! 🏆 You finished $completedThisSeason of $sprintGoal books!"
            completedThisSeason == 0 -> "$sprintGoal books to finish · $daysRemaining days left in season"
            else -> "$completedThisSeason/$sprintGoal completed · ${sprintGoal - completedThisSeason} books to go in $daysRemaining days"
        }

        return SeasonalSprint(
            season = season,
            targetBooks = sprintGoal,
            completedBooks = completedThisSeason,
            progress = progress,
            daysRemaining = daysRemaining,
            isCompleted = isCompleted,
            statusMessage = message
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
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
    ): MonthlyGoalSummary {
        val daysInMonth = date.lengthOfMonth()
        val year = date.year
        val month = date.monthValue

        val pagesByDayThisMonth = StreakEngine.pagesByDay(sessions, zone, dayStartsAtHour)
            .filterKeys { it.year == year && it.monthValue == month }
        val pagesThisMonth = pagesByDayThisMonth.values.sum()

        val booksThisMonth = books
            .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
            .count {
                val d = Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate()
                d.year == year && d.monthValue == month
            }

        val activeDays = pagesByDayThisMonth.size

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
