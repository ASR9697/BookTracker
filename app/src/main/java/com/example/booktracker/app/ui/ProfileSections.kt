package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.ui.animations.LivingFlame
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.theme.AppShape

/** Profile's ways into your record, two by two, each with a live count. */
@Composable
fun ProfileShortcuts(
    sessions: Int,
    finished: Int,
    setAside: Int,
    planned: Int,
    onOpenHistory: () -> Unit,
    onOpenFinished: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenJourneyMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutTile(
                Icons.Rounded.History, "History",
                if (sessions == 0) "Every session you log" else "$sessions ${if (sessions == 1) "session" else "sessions"}",
                onOpenHistory, Modifier.weight(1f)
            )
            ShortcutTile(
                Icons.Rounded.DoneAll, "Finished",
                "$finished read" + if (setAside > 0) " · $setAside set aside" else "",
                onOpenFinished, Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutTile(
                Icons.Rounded.Event, "Planner",
                if (planned == 0) "Plan what to read" else "$planned planned",
                onOpenPlanner, Modifier.weight(1f)
            )
            ShortcutTile(
                Icons.Rounded.Route, "Journey map",
                if (finished == 0) "Your books as a path" else "$finished ${if (finished == 1) "stop" else "stops"} so far",
                onOpenJourneyMap, Modifier.weight(1f)
            )
        }
    }
}

/**
 * Lifetime numbers: the streak as the headline (it's alive; tap for its history),
 * then books, pages and time as one quiet line.
 */
@Composable
fun LifetimeCard(
    streak: Int,
    longestStreak: Int,
    books: Int,
    pages: Int,
    minutes: Long,
    onOpenStreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpenStreak,
        shape = AppShape.L,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivingFlame(streak = streak)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val shown by animateCountUp(streak)
                    Text(
                        when (streak) {
                            0 -> "No streak right now"
                            1 -> "1-day streak"
                            else -> "$shown-day streak"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Best: $longestStreak ${if (longestStreak == 1) "day" else "days"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "${"%,d".format(books)} ${if (books == 1) "book" else "books"} · " +
                    "${"%,d".format(pages)} pages · ${AnalyticsEngine.formatMinutes(minutes)} reading",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
