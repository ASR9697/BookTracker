package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book?,
    sessions: List<Session>,
    notes: List<MarginNote>,
    onAddNote: (pageOrUnit: Int, text: String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddNote by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "This book is no longer in your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        val completed = sessions.filter { it.endTime > 0 }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { DetailHeader(book) }

            if (book.genres.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        book.genres.forEach { genre ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(
                                    genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { AboutCard(book) }
            item { BookStatsCard(book, completed) }

            if (book.status == BookStatus.FINISHED.name && book.rating.isNotEmpty()) {
                item { RatingCard(book) }
            }
            if (book.status == BookStatus.DNF.name && book.dnfData != null) {
                item {
                    DetailCard(title = "Did not finish") {
                        Text(
                            "Abandoned at ${book.dnfData!!.abandonedPercentage.roundToInt()}%" +
                                book.dnfData!!.reason
                                    .takeIf { it.isNotBlank() }
                                    ?.let { " · $it" }.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Margin notes", style = MaterialTheme.typography.titleLarge)
                    FilledTonalButton(onClick = { showAddNote = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Add note")
                    }
                }
            }
            if (notes.isEmpty()) {
                item {
                    Text(
                        "Capture thoughts, quotes, or questions tied to a page.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note, onDelete = { onDeleteNote(note.id) })
                }
            }

            item {
                Text("Session history", style = MaterialTheme.typography.titleLarge)
            }
            if (completed.isEmpty()) {
                item {
                    Text(
                        "No recorded sessions yet — start one from the Home tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(completed.take(30), key = { it.id }) { session ->
                    SessionRow(session)
                }
            }
        }
    }

    if (showAddNote && book != null) {
        AddNoteDialog(
            suggestedPage = book.currentUnit,
            onDismiss = { showAddNote = false },
            onConfirm = { page, text ->
                onAddNote(page, text)
                showAddNote = false
            }
        )
    }
}

@Composable
private fun DetailHeader(book: Book) {
    Row {
        BookCover(book.coverUrl, Modifier.size(width = 110.dp, height = 165.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                book.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (book.authors.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            val facts = buildList {
                if (book.totalUnits > 0) add("${book.totalUnits} pages")
                if (book.publishedDate.isNotBlank()) add(book.publishedDate.take(4))
            }
            if (facts.isNotEmpty()) {
                Text(
                    facts.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            StatusPill(book.status)
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val label = when (status) {
        BookStatus.BACKLOG.name -> "Backlog"
        BookStatus.SHORTLIST.name -> "Shortlist"
        BookStatus.UP_NEXT.name -> "Up Next"
        BookStatus.READING.name -> "Reading"
        BookStatus.FINISHED.name -> "Finished"
        BookStatus.DNF.name -> "Did not finish"
        else -> status
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AboutCard(book: Book) {
    var expanded by remember { mutableStateOf(false) }
    DetailCard(title = "About") {
        if (book.description.isBlank()) {
            Text(
                "No summary available for this edition.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                book.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis
            )
            if (book.description.length > 300) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (expanded) "Show less" else "Read more")
                }
            }
        }
    }
}

@Composable
private fun BookStatsCard(book: Book, completed: List<Session>) {
    val totalMinutes = AnalyticsEngine.timedSessions(completed)
        .sumOf { it.endTime - it.startTime } / 60_000L
    val velocity = AnalyticsEngine.pagesPerHour(completed)
    val minutesLeft = AnalyticsEngine.estimatedMinutesLeft(book, completed)
    val pagesFromSessions = completed.sumOf { it.unitsRead }

    DetailCard(title = "Your reading") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCell("Time read", AnalyticsEngine.formatMinutes(totalMinutes))
            StatCell("Pages logged", pagesFromSessions.toString())
            StatCell(
                "Speed",
                velocity?.let { "${it.roundToInt()} p/h" } ?: "—"
            )
        }
        if (book.status == BookStatus.READING.name && minutesLeft != null && minutesLeft > 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "≈ ${AnalyticsEngine.formatMinutes(minutesLeft)} left at your pace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RatingCard(book: Book) {
    DetailCard(title = "Your rating") {
        RatingAxis.ALL.forEach { axis ->
            val value = book.rating[axis] ?: return@forEach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(axis, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.1f / 5".format(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NoteCard(note: MarginNote, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(
                        "p. ${note.pageOrUnit}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    Instant.ofEpochMilli(note.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(dateFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete note",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(note.markdownContent, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SessionRow(session: Session) {
    val day = Instant.ofEpochMilli(session.endTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(dateFormatter)
    val durationMinutes = (session.endTime - session.startTime) / 60_000L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(day, style = MaterialTheme.typography.titleSmall)
                if (session.deviceSource == "watch") {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Watch,
                        contentDescription = "From watch",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        if (durationMinutes >= 1 && session.unitsRead > 0) {
            Icon(
                Icons.Filled.Speed,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${(session.unitsRead * 60L / durationMinutes)} p/h",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun AddNoteDialog(
    suggestedPage: Int,
    onDismiss: () -> Unit,
    onConfirm: (page: Int, text: String) -> Unit
) {
    var pageText by remember { mutableStateOf(if (suggestedPage > 0) "$suggestedPage" else "") }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add margin note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it.filter(Char::isDigit) },
                    label = { Text("Page") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = noteText.isNotBlank(),
                onClick = { onConfirm(pageText.toIntOrNull() ?: 0, noteText.trim()) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
