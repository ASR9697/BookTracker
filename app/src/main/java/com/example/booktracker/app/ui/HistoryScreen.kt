package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val headerFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

/** Every completed session, newest day first — the full reading log. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    books: List<Book>,
    sessions: List<Session>,
    onBack: () -> Unit
) {
    val titlesById = remember(books) { books.associate { it.id to it.title } }
    val zone = remember { ZoneId.systemDefault() }
    val byDay: List<Pair<LocalDate, List<Session>>> = remember(sessions) {
        sessions
            .filter { it.endTime > 0 }
            .groupBy { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (day, list) -> day to list.sortedByDescending { it.endTime } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (byDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Your reading sessions will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            byDay.forEach { (day, daySessions) ->
                item(key = "header-$day") {
                    val pages = daySessions.sumOf { it.unitsRead }
                    val minutes = AnalyticsEngine.timedSessions(daySessions)
                        .sumOf { it.endTime - it.startTime } / 60_000L
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day.format(headerFormatter), style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                append("$pages pages")
                                if (minutes >= 1) append(" · ${AnalyticsEngine.formatMinutes(minutes)}")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(daySessions, key = { it.id }) { session ->
                    HistorySessionRow(
                        session = session,
                        bookTitle = titlesById[session.bookId] ?: "Removed book"
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySessionRow(session: Session, bookTitle: String) {
    val durationMinutes = (session.endTime - session.startTime) / 60_000L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                bookTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val parts = buildList {
                add("${session.unitsRead} pages")
                if (durationMinutes >= 1) add(AnalyticsEngine.formatMinutes(durationMinutes))
                if (session.environmentTag.isNotBlank()) add(session.environmentTag)
            }
            Text(
                parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (session.deviceSource == "watch") {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Watch,
                contentDescription = "From watch",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
