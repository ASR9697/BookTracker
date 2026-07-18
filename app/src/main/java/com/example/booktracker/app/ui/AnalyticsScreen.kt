package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.ReadingCalendar
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    books: List<Book>,
    sessions: List<Session>,
    dailyGoal: Int,
    yearlyGoal: Int,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }
    val pagesByDay = remember(sessions) { ReadingCalendar.pagesByDay(sessions) }
    val minutesByDay = remember(sessions) { AnalyticsEngine.minutesByDay(sessions) }
    val timeSummary = remember(sessions) { AnalyticsEngine.summarizeTime(sessions) }
    val velocity = remember(sessions) { AnalyticsEngine.pagesPerHour(sessions) }
    val bestTime = remember(sessions) { AnalyticsEngine.bestTimeOfDay(sessions) }
    val environments = remember(sessions) { AnalyticsEngine.environmentStats(sessions) }

    var showMinutes by remember { mutableStateOf(false) }

    val last7Days = remember(today, pagesByDay, minutesByDay, showMinutes) {
        (0..6).map { i ->
            val date = today.minusDays((6 - i).toLong())
            val value =
                if (showMinutes) (minutesByDay[date] ?: 0L)
                else (pagesByDay[date] ?: 0).toLong()
            date to value
        }
    }
    val maxIn7Days = last7Days.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

    val finishedBooks = books
        .filter { it.status == BookStatus.FINISHED.name }
        .sortedByDescending { it.lastUpdated }
    val finishedThisMonth = remember(finishedBooks, today) {
        finishedBooks.count {
            val day = Instant.ofEpochMilli(it.lastUpdated).atZone(zone).toLocalDate()
            day.year == today.year && day.month == today.month
        }
    }
    val monthlyGoal = (yearlyGoal / 12).coerceAtLeast(1)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Monthly goal, derived from the yearly books goal in Settings.
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp).fillMaxWidth()
                    ) {
                        Text("Monthly Goal", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                            val target = (finishedThisMonth.toFloat() / monthlyGoal).coerceIn(0f, 1f)
                            val progress by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = target,
                                label = "progress"
                            )
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 12.dp,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$finishedThisMonth",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "of $monthlyGoal book${if (monthlyGoal > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            when {
                                finishedThisMonth >= monthlyGoal -> "Goal reached — pick your next read!"
                                finishedThisMonth == monthlyGoal - 1 -> "You're almost there!"
                                else -> "Based on your ${yearlyGoal}-book yearly goal."
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Last-7-days activity, switchable between pages and reading time.
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (showMinutes) "Time Read" else "Pages Read",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !showMinutes,
                                    onClick = { showMinutes = false },
                                    label = { Text("Pages") },
                                    shape = CircleShape
                                )
                                FilterChip(
                                    selected = showMinutes,
                                    onClick = { showMinutes = true },
                                    label = { Text("Minutes") },
                                    shape = CircleShape
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7Days.forEachIndexed { index, (date, value) ->
                                val heightFrac = (value.toFloat() / maxIn7Days).coerceIn(0.1f, 1f)
                                val color =
                                    if (index == last7Days.size - 1) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (value > 0) {
                                        Text(
                                            value.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .fillMaxHeight(heightFrac)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(color)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        date.dayOfWeek.name.take(1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reading-time stats from recorded session durations.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reading Time", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    if (timeSummary.timedSessionCount == 0) {
                        Text(
                            "Start and end sessions from the Home tab to unlock time " +
                                "stats and your reading speed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TimeStat(
                                Icons.Filled.Schedule,
                                AnalyticsEngine.formatMinutes(timeSummary.minutesLast7Days),
                                "This week"
                            )
                            TimeStat(
                                Icons.Filled.Schedule,
                                AnalyticsEngine.formatMinutes(timeSummary.avgSessionMinutes),
                                "Avg session"
                            )
                            TimeStat(
                                Icons.Filled.Speed,
                                velocity?.let { "${it.roundToInt()} p/h" } ?: "—",
                                "Speed"
                            )
                            TimeStat(
                                Icons.Filled.WbTwilight,
                                bestTime ?: "—",
                                "Best time"
                            )
                        }
                    }
                }
            }
        }

        // Where you read best — volume and pace per environment tag.
        if (environments.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Environments", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tagged when you end a session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        environments.take(6).forEach { env ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    env.tag,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    buildString {
                                        append("${env.totalPages} pages")
                                        env.pagesPerHour?.let {
                                            append(" · ${it.roundToInt()} p/h")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top genres from the metadata fetched with each book.
        item {
            val genreCounts = remember(books) {
                books.flatMap { it.genres }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
            }
            Column {
                Text("Top Genres", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (genreCounts.isEmpty()) {
                    Text(
                        "Add books via search or the ISBN scanner and their genres " +
                            "will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chipColors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.primary
                        )
                        genreCounts.forEachIndexed { index, (genre, _) ->
                            GenreChip(genre, chipColors[index % chipColors.size])
                        }
                    }
                }
            }
        }

        if (finishedBooks.isNotEmpty()) {
            item {
                Text(
                    "Recently Finished",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(finishedBooks) { book ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCover(url = book.coverUrl, modifier = Modifier.size(width = 48.dp, height = 72.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                book.authors.joinToString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (book.rating.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "%.1f".format(book.rating.values.average()),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GenreChip(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}
