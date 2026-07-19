package com.example.booktracker.app.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
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
    val predicate: (books: List<Book>, sessions: List<Session>, currentStreak: Int, notesCount: Int) -> Boolean
)

object BadgeEngine {

    val BADGES = listOf(
        Badge(
            id = "night_owl",
            title = "Night Owl",
            description = "Read between midnight and 4 AM.",
            icon = Icons.Filled.Bedtime,
            predicate = { _, sessions, _, _ ->
                val zone = ZoneId.systemDefault()
                sessions.any { session ->
                    val hour = Instant.ofEpochMilli(session.startTime).atZone(zone).hour
                    hour in 0..3
                }
            }
        ),
        Badge(
            id = "marathoner",
            title = "Marathoner",
            description = "Read for 3 or more hours in a single session.",
            icon = Icons.Filled.DirectionsRun,
            predicate = { _, sessions, _, _ ->
                sessions.any { it.endTime > 0 && (it.endTime - it.startTime) >= 180 * 60_000L }
            }
        ),
        Badge(
            id = "finisher",
            title = "Finisher",
            description = "Finish your first book.",
            icon = Icons.Filled.EmojiEvents,
            predicate = { books, _, _, _ ->
                books.any { it.status == BookStatus.FINISHED.name }
            }
        ),
        Badge(
            id = "streak_keeper",
            title = "Streak Keeper",
            description = "Hit your daily reading goal for 7 consecutive days.",
            icon = Icons.Filled.LocalFireDepartment,
            predicate = { _, _, currentStreak, _ ->
                currentStreak >= 7
            }
        ),
        Badge(
            id = "polyglot_shelf",
            title = "Polyglot Shelf",
            description = "Read books spanning 5 or more distinct genres.",
            icon = Icons.Filled.Category,
            predicate = { books, _, _, _ ->
                val distinctGenres = books
                    .filter { it.status == BookStatus.FINISHED.name || it.status == BookStatus.READING.name }
                    .flatMap { it.genres }
                    .toSet()
                distinctGenres.size >= 5
            }
        ),
        Badge(
            id = "marginalia",
            title = "Marginalia",
            description = "Write 25 or more margin notes.",
            icon = Icons.Filled.EditNote,
            predicate = { _, _, _, notesCount ->
                notesCount >= 25
            }
        )
    )

    fun evaluate(books: List<Book>, sessions: List<Session>, currentStreak: Int, notesCount: Int): Set<String> {
        return BADGES.filter { it.predicate(books, sessions, currentStreak, notesCount) }.map { it.id }.toSet()
    }
}
