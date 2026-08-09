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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import java.time.format.DateTimeFormatter

private val headerFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

/** Every completed session, newest day first — the full reading log. */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    books: List<Book>,
    sessions: List<Session>,
    onDeleteSession: (Session) -> Unit,
    onUpdateSession: (Session) -> Unit,
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
        var sessionToEdit by remember { mutableStateOf<Session?>(null) }
        
        sessionToEdit?.let { session ->
            val book = books.find { it.id == session.bookId }
            EditSessionDialog(
                session = session,
                book = book,
                onDismiss = { sessionToEdit = null },
                onUpdate = { updated ->
                    onUpdateSession(updated)
                    sessionToEdit = null
                }
            )
        }

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
                    val pages = daySessions.sumOf { it.pagesRead }
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
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onDeleteSession(session)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    ) {
                        HistorySessionRow(
                            session = session,
                            bookTitle = titlesById[session.bookId] ?: "Removed book",
                            onClick = { sessionToEdit = session }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSessionDialog(
    session: Session,
    book: Book?,
    onDismiss: () -> Unit,
    onUpdate: (Session) -> Unit
) {
    var pagesReadText by remember { mutableStateOf(session.pagesRead.toString()) }
    val newUnits = pagesReadText.toIntOrNull()
    val isError = newUnits != null && (newUnits < 0 || (book != null && book.totalPages > 0 && newUnits > book.totalPages))
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session") },
        text = {
            Column {
                OutlinedTextField(
                    value = pagesReadText,
                    onValueChange = { pagesReadText = it.filter { char -> char.isDigit() } },
                    label = { Text("Pages read") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isError
                )
                if (isError) {
                    Text(
                        if (newUnits != null && newUnits < 0) "Pages cannot be negative"
                        else "Cannot exceed total pages (${book?.totalPages ?: "?"})",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalUnits = newUnits ?: session.pagesRead
                    onUpdate(session.copy(pagesRead = finalUnits))
                },
                enabled = newUnits != null && !isError
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun HistorySessionRow(session: Session, bookTitle: String, onClick: () -> Unit) {
    val durationMinutes = (session.endTime - session.startTime) / 60_000L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                add("${session.pagesRead} pages")
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
