package com.example.booktracker.app.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Everything a badge can be measured against. */
data class BadgeInput(
    val books: List<Book>,
    val sessions: List<Session>,
    /** Longest streak ever, so a badge stays earned after the streak breaks. */
    val longestStreak: Int,
    val notesCount: Int
)

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tier: String = "BRONZE", // BRONZE, SILVER, GOLD, PLATINUM
    /** Earned once [measure] reaches this. */
    val target: Int = 1,
    /** Shown after progress numbers, e.g. "20 / 30 days". */
    val unit: String = "",
    val measure: (BadgeInput) -> Int
)

data class BadgeStatus(
    val badge: Badge,
    val current: Int,
    /** When the badge was first earned, worked out from history; null while locked. */
    val earnedAt: Long? = null
) {
    val unlocked: Boolean get() = current >= badge.target
    val fraction: Float get() = (current.toFloat() / badge.target).coerceIn(0f, 1f)
    val progressLabel: String
        get() = "${current.coerceAtMost(badge.target)} / ${badge.target}" +
            if (badge.unit.isNotEmpty()) " ${badge.unit}" else ""
}

object BadgeEngine {

    val TIERS = listOf("BRONZE", "SILVER", "GOLD", "PLATINUM")
    const val MONTHLY_TIER = "MONTHLY"

    private fun finishedCount(books: List<Book>) =
        books.count { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }

    val BADGES = listOf(
        Badge(
            id = "first_chapter",
            title = "First Step",
            description = "Complete your first reading session.",
            icon = Icons.Filled.PlayArrow,
            tier = "BRONZE",
            unit = "session",
            measure = { input -> if (input.sessions.any(StreakEngine::isReading)) 1 else 0 }
        ),
        Badge(
            id = "finisher",
            title = "Book Finisher",
            description = "Finish your very first book.",
            icon = Icons.Filled.EmojiEvents,
            tier = "BRONZE",
            unit = "book",
            measure = { input -> finishedCount(input.books).coerceAtMost(1) }
        ),
        Badge(
            id = "night_owl",
            title = "Night Owl",
            description = "Read between midnight and 4 AM.",
            icon = Icons.Filled.Bedtime,
            tier = "BRONZE",
            unit = "late session",
            measure = { input ->
                val zone = ZoneId.systemDefault()
                if (input.sessions.any { Instant.ofEpochMilli(it.startTime).atZone(zone).hour in 0..3 }) 1 else 0
            }
        ),
        Badge(
            id = "streak_keeper",
            title = "Streak Keeper",
            description = "Read 7 days in a row.",
            icon = Icons.Filled.LocalFireDepartment,
            tier = "SILVER",
            target = 7,
            unit = "days",
            measure = { it.longestStreak }
        ),
        Badge(
            id = "century_streak",
            title = "Fire Walker",
            description = "Read 30 days in a row.",
            icon = Icons.Filled.Whatshot,
            tier = "GOLD",
            target = 30,
            unit = "days",
            measure = { it.longestStreak }
        ),
        Badge(
            id = "bookworm",
            title = "Bookworm",
            description = "Finish 5 or more books.",
            icon = Icons.Filled.AutoStories,
            tier = "SILVER",
            target = 5,
            unit = "books",
            measure = { finishedCount(it.books) }
        ),
        Badge(
            id = "bibliophile",
            title = "Bibliophile",
            description = "Finish 20 or more books.",
            icon = Icons.Filled.WorkspacePremium,
            tier = "PLATINUM",
            target = 20,
            unit = "books",
            measure = { finishedCount(it.books) }
        ),
        Badge(
            id = "page_master",
            title = "Page Master",
            description = "Read over 1,000 pages in total.",
            icon = Icons.Filled.MenuBook,
            tier = "SILVER",
            target = 1000,
            unit = "pages",
            measure = { input -> input.sessions.sumOf { it.pagesRead } }
        ),
        Badge(
            id = "marathoner",
            title = "Marathoner",
            description = "Read for 3 or more hours in a single session.",
            icon = Icons.Filled.DirectionsRun,
            tier = "GOLD",
            target = 180,
            unit = "min",
            measure = { input ->
                (input.sessions.filter { it.endTime > 0 }.maxOfOrNull { it.endTime - it.startTime } ?: 0L)
                    .div(60_000L).toInt()
            }
        ),
        Badge(
            id = "speed_reader",
            title = "Speed Reader",
            description = "Achieve a reading pace of 50+ pages per hour.",
            icon = Icons.Filled.Speed,
            tier = "GOLD",
            target = 50,
            unit = "p/h",
            measure = { input ->
                input.sessions.filter { it.durationSeconds >= 600 }
                    .maxOfOrNull { it.pagesRead.toFloat() / (it.durationSeconds / 3600f) }
                    ?.toInt() ?: 0
            }
        ),
        Badge(
            id = "zen_focus",
            title = "Zen Master",
            description = "Complete 10 focused reading sessions of 15+ minutes.",
            icon = Icons.Filled.SelfImprovement,
            tier = "SILVER",
            target = 10,
            unit = "sessions",
            measure = { input -> input.sessions.count { it.durationSeconds >= 900 } }
        ),
        Badge(
            id = "polyglot_shelf",
            title = "Polyglot Shelf",
            description = "Read books spanning 5 or more distinct genres.",
            icon = Icons.Filled.Category,
            tier = "GOLD",
            target = 5,
            unit = "genres",
            measure = { input ->
                input.books
                    .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) || it.status == BookStatus.READING.name }
                    .flatMap { it.genres }
                    .toSet()
                    .size
            }
        ),
        Badge(
            id = "marginalia",
            title = "Marginalia",
            description = "Write 25 or more margin notes or quotes.",
            icon = Icons.Filled.EditNote,
            tier = "SILVER",
            target = 25,
            unit = "notes",
            measure = { it.notesCount }
        ),
        Badge(
            id = "early_bird",
            title = "Early Bird",
            description = "Read between 5 and 7 AM.",
            icon = Icons.Filled.WbTwilight,
            tier = "BRONZE",
            unit = "early session",
            measure = { input ->
                val zone = ZoneId.systemDefault()
                if (input.sessions.any { Instant.ofEpochMilli(it.startTime).atZone(zone).hour in 5..6 }) 1 else 0
            }
        ),
        Badge(
            id = "let_it_go",
            title = "Let It Go",
            description = "Set aside a book that isn't working for you. Life's too short.",
            icon = Icons.Filled.DoNotDisturbOn,
            tier = "BRONZE",
            unit = "book",
            measure = { input ->
                if (input.books.any { it.status == BookStatus.DNF.name || it.status == BookStatus.ABANDONED.name }) 1 else 0
            }
        ),
        Badge(
            id = "re_reader",
            title = "Encore",
            description = "Start a second read of a book you've finished.",
            icon = Icons.Filled.Replay,
            tier = "SILVER",
            unit = "re-read",
            measure = { input -> if (input.books.any { it.readCount >= 1 }) 1 else 0 }
        ),
        Badge(
            id = "series_completer",
            title = "Saga Seeker",
            description = "Finish 3 books from the same series.",
            icon = Icons.Filled.CollectionsBookmark,
            tier = "GOLD",
            target = 3,
            unit = "books",
            measure = { input ->
                input.books
                    .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
                    .mapNotNull { book -> book.seriesInfo?.takeIf { it.isPartOfSeries }?.seriesName?.trim()?.lowercase() }
                    .filter { it.isNotEmpty() }
                    .groupingBy { it }.eachCount()
                    .maxOfOrNull { it.value } ?: 0
            }
        ),
        Badge(
            id = "hundred_hours",
            title = "Deep Diver",
            description = "Spend 100 hours reading.",
            icon = Icons.Filled.HourglassBottom,
            tier = "PLATINUM",
            target = 100,
            unit = "hours",
            measure = { input ->
                (AnalyticsEngine.timedSessions(input.sessions).sumOf { it.endTime - it.startTime } / 3_600_000L).toInt()
            }
        ),
        Badge(
            id = "ten_thousand_pages",
            title = "Ten Thousand",
            description = "Read 10,000 pages in total.",
            icon = Icons.Filled.Landscape,
            tier = "PLATINUM",
            target = 10_000,
            unit = "pages",
            measure = { input -> input.sessions.sumOf { it.pagesRead } }
        )
    )

    fun statuses(input: BadgeInput): List<BadgeStatus> =
        BADGES.map { BadgeStatus(it, it.measure(input)) }

    fun evaluate(input: BadgeInput): Set<String> =
        statuses(input).filter { it.unlocked }.map { it.badge.id }.toSet()

    /** The locked badge closest to unlocking, by share of its target reached. */
    fun nextUp(statuses: List<BadgeStatus>): BadgeStatus? =
        statuses.filter { !it.unlocked }.maxByOrNull { it.fraction }

    /**
     * Stamps each earned badge with the moment it was first earned, by replaying
     * history: every badge only grows as sessions, finished books and notes pile
     * up, so a binary search over those event times finds the first one at which
     * the badge was met. Derived rather than stored, so badges earned before this
     * existed, or restored from a backup, still get a real date.
     */
    fun withEarnedDates(
        statuses: List<BadgeStatus>,
        books: List<Book>,
        sessions: List<Session>,
        noteTimes: List<Long>,
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<BadgeStatus> {
        if (statuses.none { it.unlocked }) return statuses
        val times = (sessions.map { it.startTime } + books.map { it.completedAt } + noteTimes)
            .filter { it > 0 }.distinct().sorted()
        if (times.isEmpty()) return statuses

        val inputs = HashMap<Int, BadgeInput>()
        fun inputAt(index: Int): BadgeInput = inputs.getOrPut(index) {
            val t = times[index]
            val before = sessions.filter { it.startTime in 1..t }
            BadgeInput(
                books = books.filter { it.completedAt <= t },
                sessions = before,
                longestStreak = StreakEngine.longestStreak(StreakEngine.pagesByDay(before, zone, dayStartsAtHour).keys),
                notesCount = noteTimes.count { it <= t }
            )
        }

        return statuses.map { status ->
            if (!status.unlocked) return@map status
            var lo = 0
            var hi = times.lastIndex
            var first = -1
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                if (status.badge.measure(inputAt(mid)) >= status.badge.target) {
                    first = mid
                    hi = mid - 1
                } else {
                    lo = mid + 1
                }
            }
            status.copy(earnedAt = times.getOrNull(first))
        }
    }

    /** One month's reading, the scope a monthly challenge is measured over. */
    data class MonthInput(
        val sessions: List<Session>,
        val booksFinished: Int,
        val notes: Int,
        val zone: ZoneId,
        val dayStartsAtHour: Int
    )

    private class Challenge(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector,
        val target: Int,
        val unit: String,
        val measure: (MonthInput) -> Int
    )

    private val CHALLENGES = listOf(
        Challenge("days", "Every Day Counts", "Read on 20 different days this month.", Icons.Filled.EventAvailable, 20, "days") {
            StreakEngine.pagesByDay(it.sessions, it.zone, it.dayStartsAtHour).size
        },
        Challenge("pages", "Page Turner", "Read 600 pages this month.", Icons.Filled.AutoStories, 600, "pages") { month ->
            month.sessions.filter(StreakEngine::isReading).sumOf { it.pagesRead }
        },
        Challenge("books", "Double Feature", "Finish 2 books this month.", Icons.Filled.LibraryBooks, 2, "books") {
            it.booksFinished
        },
        Challenge("hours", "Ten Hour Club", "Spend 10 hours reading this month.", Icons.Filled.Schedule, 10, "hours") { month ->
            (AnalyticsEngine.timedSessions(month.sessions).sumOf { it.endTime - it.startTime } / 3_600_000L).toInt()
        },
        Challenge("notes", "Note Taker", "Write 10 notes or quotes this month.", Icons.Filled.EditNote, 10, "notes") {
            it.notes
        },
        Challenge("streak", "Week of Fire", "Read 7 days in a row this month.", Icons.Filled.LocalFireDepartment, 7, "days") {
            StreakEngine.longestStreak(StreakEngine.pagesByDay(it.sessions, it.zone, it.dayStartsAtHour).keys)
        }
    )

    /**
     * This month's challenge. It rotates through [CHALLENGES] by calendar month
     * and resets on the 1st, so there is always something fresh to chase.
     */
    fun monthlyChallenge(
        books: List<Book>,
        sessions: List<Session>,
        noteTimes: List<Long>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
    ): BadgeStatus {
        val month = YearMonth.from(today)
        val challenge = CHALLENGES[Math.floorMod(month.year * 12 + month.monthValue - 1, CHALLENGES.size)]
        val inMonth = { millis: Long -> YearMonth.from(Instant.ofEpochMilli(millis).atZone(zone)) == month }
        val input = MonthInput(
            sessions = sessions.filter {
                it.startTime > 0 && YearMonth.from(StreakEngine.sessionDay(it, zone, dayStartsAtHour)) == month
            },
            booksFinished = books.count {
                it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) && it.completedAt > 0 && inMonth(it.completedAt)
            },
            notes = noteTimes.count { it > 0 && inMonth(it) },
            zone = zone,
            dayStartsAtHour = dayStartsAtHour
        )
        val badge = Badge(
            id = "monthly_${challenge.id}",
            title = challenge.title,
            description = challenge.description,
            icon = challenge.icon,
            tier = MONTHLY_TIER,
            target = challenge.target,
            unit = challenge.unit,
            measure = { 0 }
        )
        return BadgeStatus(badge, challenge.measure(input))
    }
}
