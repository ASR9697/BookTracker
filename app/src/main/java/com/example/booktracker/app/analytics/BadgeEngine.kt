package com.example.booktracker.app.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.ZoneId

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tier: String = "BRONZE", // BRONZE, SILVER, GOLD, PLATINUM
    val predicate: (books: List<Book>, sessions: List<Session>, currentStreak: Int, notesCount: Int) -> Boolean
)

object BadgeEngine {

    val BADGES = listOf(
        Badge(
            id = "first_chapter",
            title = "First Step",
            description = "Complete your first reading session.",
            icon = Icons.Filled.PlayArrow,
            tier = "BRONZE",
            predicate = { _, sessions, _, _ -> sessions.any(StreakEngine::isReading) }
        ),
        Badge(
            id = "finisher",
            title = "Book Finisher",
            description = "Finish your very first book.",
            icon = Icons.Filled.EmojiEvents,
            tier = "BRONZE",
            predicate = { books, _, _, _ ->
                books.any { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
            }
        ),
        Badge(
            id = "night_owl",
            title = "Night Owl",
            description = "Read between midnight and 4 AM.",
            icon = Icons.Filled.Bedtime,
            tier = "BRONZE",
            predicate = { _, sessions, _, _ ->
                val zone = ZoneId.systemDefault()
                sessions.any { session ->
                    val hour = Instant.ofEpochMilli(session.startTime).atZone(zone).hour
                    hour in 0..3
                }
            }
        ),
        Badge(
            id = "streak_keeper",
            title = "Streak Keeper",
            description = "Hit your daily reading goal for 7 consecutive days.",
            icon = Icons.Filled.LocalFireDepartment,
            tier = "SILVER",
            predicate = { _, _, currentStreak, _ -> currentStreak >= 7 }
        ),
        Badge(
            id = "century_streak",
            title = "Fire Walker",
            description = "Maintain a reading streak of 30 days or more.",
            icon = Icons.Filled.Whatshot,
            tier = "GOLD",
            predicate = { _, _, currentStreak, _ -> currentStreak >= 30 }
        ),
        Badge(
            id = "bookworm",
            title = "Bookworm",
            description = "Finish 5 or more books.",
            icon = Icons.Filled.AutoStories,
            tier = "SILVER",
            predicate = { books, _, _, _ ->
                books.count { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) } >= 5
            }
        ),
        Badge(
            id = "bibliophile",
            title = "Bibliophile",
            description = "Finish 20 or more books.",
            icon = Icons.Filled.WorkspacePremium,
            tier = "PLATINUM",
            predicate = { books, _, _, _ ->
                books.count { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) } >= 20
            }
        ),
        Badge(
            id = "page_master",
            title = "Page Master",
            description = "Read over 1,000 pages in total.",
            icon = Icons.Filled.MenuBook,
            tier = "SILVER",
            predicate = { _, sessions, _, _ ->
                sessions.sumOf { it.pagesRead } >= 1000
            }
        ),
        Badge(
            id = "marathoner",
            title = "Marathoner",
            description = "Read for 3 or more hours in a single session.",
            icon = Icons.Filled.DirectionsRun,
            tier = "GOLD",
            predicate = { _, sessions, _, _ ->
                sessions.any { it.endTime > 0 && (it.endTime - it.startTime) >= 180 * 60_000L }
            }
        ),
        Badge(
            id = "speed_reader",
            title = "Speed Reader",
            description = "Achieve a reading pace of 50+ pages per hour.",
            icon = Icons.Filled.Speed,
            tier = "GOLD",
            predicate = { _, sessions, _, _ ->
                sessions.any { it.durationSeconds >= 600 && (it.pagesRead.toFloat() / (it.durationSeconds / 3600f)) >= 50f }
            }
        ),
        Badge(
            id = "zen_focus",
            title = "Zen Master",
            description = "Complete 10 focused reading timer sessions.",
            icon = Icons.Filled.SelfImprovement,
            tier = "SILVER",
            predicate = { _, sessions, _, _ ->
                sessions.count { it.durationSeconds >= 900 } >= 10
            }
        ),
        Badge(
            id = "polyglot_shelf",
            title = "Polyglot Shelf",
            description = "Read books spanning 5 or more distinct genres.",
            icon = Icons.Filled.Category,
            tier = "GOLD",
            predicate = { books, _, _, _ ->
                val distinctGenres = books
                    .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) || it.status == BookStatus.READING.name }
                    .flatMap { it.genres }
                    .toSet()
                distinctGenres.size >= 5
            }
        ),
        Badge(
            id = "marginalia",
            title = "Marginalia",
            description = "Write 25 or more margin notes or quotes.",
            icon = Icons.Filled.EditNote,
            tier = "SILVER",
            predicate = { _, _, _, notesCount -> notesCount >= 25 }
        )
    )

    fun evaluate(books: List<Book>, sessions: List<Session>, currentStreak: Int, notesCount: Int): Set<String> {
        return BADGES.filter { it.predicate(books, sessions, currentStreak, notesCount) }.map { it.id }.toSet()
    }
}

