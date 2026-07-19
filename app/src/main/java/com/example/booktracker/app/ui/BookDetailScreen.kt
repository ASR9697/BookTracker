package com.example.booktracker.app.ui

import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.format.rememberReadAloud
import com.example.booktracker.app.hardware.StylusScratchpad
import com.example.booktracker.app.journal.JournalingEngine
import com.example.booktracker.app.journal.SmartPlanningEngine
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

@Composable
private fun SectionItem(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book?,
    sessions: List<Session>,
    notes: List<MarginNote>,
    onAddNote: (pageOrUnit: Int, text: String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSetFormat: (String) -> Unit,
    onDelete: () -> Unit,
    onStartReading: () -> Unit,
    onPauseReading: () -> Unit,
    onReadAgain: () -> Unit,
    onEditSession: (Session, Int) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    var showAddNote by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<Session?>(null) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var showScratchpad by remember { mutableStateOf(false) }
    var showOcr by remember { mutableStateOf(false) }
    var ocrDraft by remember { mutableStateOf<String?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    if (book == null) {
        Scaffold { innerPadding ->
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
        }
        return
    }

    // Full-screen OCR camera takes over while active, then hands the recognized
    // text back into the note sheet (via ocrDraft) for editing before saving.
    if (showOcr) {
        androidx.activity.compose.BackHandler { showOcr = false }
        OcrCaptureScreen(
            onClose = { showOcr = false },
            onTextConfirmed = { text ->
                showOcr = false
                ocrDraft = text
                showAddNote = true
            }
        )
        return
    }

    val completed = sessions.filter { it.endTime > 0 }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Parallax Cover Background
        val firstItemOffset by remember {
            derivedStateOf {
                if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 0
            }
        }
        
        BookCover(
            url = book.coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .graphicsLayer {
                    translationY = firstItemOffset * 0.5f
                    alpha = 1f - (firstItemOffset / 800f).coerceIn(0f, 1f)
                }
        )
        
        // Gradient overlay to make text readable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 100f
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(300.dp))
            }
            
            item {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(16.dp)
                ) {
                    DetailHeader(
                        book = book, 
                        onStartReading = { 
                            if (book.status != BookStatus.READING.name) onStartReading() 
                        },
                        onPauseReading = onPauseReading,
                        onReadAgain = onReadAgain
                    )
                }
            }

            if (book.genres.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
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

            item { SectionItem { AboutCard(book) } }
            item { SectionItem { BookStatsCard(book, completed) } }
            item { SectionItem { FormatCard(book, onSetFormat) } }
            if (book.status == BookStatus.READING.name) {
                item { SectionItem { PlannerCard(book, completed) } }
            }

            if (book.status == BookStatus.FINISHED.name && book.rating.isNotEmpty()) {
                item { SectionItem { RatingCard(book) } }
            }
            if (book.status == BookStatus.DNF.name && book.dnfData != null) {
                item {
                    SectionItem {
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
            }

            item {
                SectionItem {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Margin notes", style = MaterialTheme.typography.titleLarge)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showOcr = true }) {
                                Icon(
                                    Icons.Filled.DocumentScanner,
                                    contentDescription = "Scan text from a page",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { showScratchpad = true }) {
                                Icon(
                                    Icons.Filled.Draw,
                                    contentDescription = "Handwrite a note",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                }
            }
            if (notes.isEmpty()) {
                item {
                    Text(
                        "Capture thoughts, quotes, or questions tied to a page.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    SectionItem {
                        NoteCard(note, format = book.format, onDelete = { onDeleteNote(note.id) })
                    }
                }
            }

            item {
                SectionItem {
                    Text("Session history", style = MaterialTheme.typography.titleLarge)
                }
            }
            if (completed.isEmpty()) {
                item {
                    Text(
                        "No recorded sessions yet — start one from the Home tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(completed.take(30), key = { it.id }) { session ->
                    SectionItem {
                        SessionRow(
                            session = session,
                            onEdit = { sessionToEdit = session },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        }
        
        // Top App Bar Glassmorphism Overlay
        val surfaceColor = MaterialTheme.colorScheme.surface
        val topBarAlpha = {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 500f).coerceIn(0f, 1f)
        }
        
        CenterAlignedTopAppBar(
            modifier = Modifier.drawBehind {
                drawRect(color = surfaceColor, alpha = topBarAlpha() * 0.9f)
            },
            title = {
                Text(
                    book.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        alpha = (topBarAlpha() - 0.5f).coerceAtLeast(0f) * 2f
                    }
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (book.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (book.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete from Library")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }

    if (showAddNote) {
        AddNoteBottomSheet(
            suggestedPage = book.currentUnit,
            initialText = ocrDraft.orEmpty(),
            onDismiss = {
                showAddNote = false
                ocrDraft = null
            },
            onConfirm = { page, text ->
                onAddNote(page, text)
                showAddNote = false
                ocrDraft = null
            }
        )
    }

    if (showScratchpad) {
        StylusScratchpad(
            onDismiss = { showScratchpad = false },
            onRecognized = { text ->
                onAddNote(book.currentUnit, text)
                showScratchpad = false
            }
        )
    }

    sessionToEdit?.let { session ->
        EditSessionDialog(
            session = session,
            onDismiss = { sessionToEdit = null },
            onConfirm = { newUnits ->
                onEditSession(session, newUnits)
                sessionToEdit = null
            }
        )
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session") },
            text = { Text("Are you sure you want to delete this reading session? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(session)
                    sessionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailHeader(
    book: Book, 
    onStartReading: () -> Unit,
    onPauseReading: () -> Unit,
    onReadAgain: () -> Unit
) {
    Column {
        Text(
            book.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        if (book.authors.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                book.authors.joinToString(", "),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        val facts = buildList {
            if (book.totalUnits > 0) add(FormatAdaptabilityLayer.countLabel(book.format, book.totalUnits))
            if (book.publishedDate.isNotBlank()) add(book.publishedDate.take(4))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (facts.isNotEmpty()) {
                Text(
                    facts.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
)
                Spacer(Modifier.width(12.dp))
            }
            StatusPill(book.status)
        }
        
        if (book.status != BookStatus.READING.name && book.status != BookStatus.FINISHED.name && book.status != BookStatus.DNF.name) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartReading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (book.status == BookStatus.PAUSED.name) "Resume Reading" else "Start Reading")
            }
        } else if (book.status == BookStatus.READING.name) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPauseReading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Pause, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pause Reading")
            }
        } else if (book.status == BookStatus.FINISHED.name || book.status == BookStatus.DNF.name) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onReadAgain,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Read Again")
            }
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
        BookStatus.PAUSED.name -> "Paused"
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
    val readAloud = rememberReadAloud()
    val speaking by readAloud.isSpeaking
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
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = { readAloud.toggle(FormatAdaptabilityLayer.readAloudText(book)) }
        ) {
            Icon(
                if (speaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (speaking) "Stop" else "Listen")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatCard(book: Book, onSetFormat: (String) -> Unit) {
    DetailCard(title = "Tracking format") {
        Text(
            "Track this book in ${FormatAdaptabilityLayer.getDisplayUnit(book.format).lowercase()}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            FormatAdaptabilityLayer.FORMATS.forEachIndexed { index, format ->
                SegmentedButton(
                    selected = book.format.equals(format, ignoreCase = true),
                    onClick = { onSetFormat(format) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = FormatAdaptabilityLayer.FORMATS.size
                    )
                ) {
                    Text(FormatAdaptabilityLayer.getDisplayUnit(format))
                }
            }
        }
    }
}

@Composable
private fun PlannerCard(book: Book, completed: List<Session>) {
    val velocity = AnalyticsEngine.pagesPerHour(completed)
    val unit = FormatAdaptabilityLayer.getDisplayUnit(book.format).lowercase()
    DetailCard(title = "Reading planner") {
        if (velocity == null) {
            Text(
                "Log a few timed sessions and this will estimate how much you can read in a spare moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "At your pace of ${velocity.roundToInt()} $unit/hour you could read about:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartPlanningEngine.PLAN_WINDOWS_MINUTES.forEach { minutes ->
                    val units = SmartPlanningEngine.unitsInWindow(minutes, velocity) ?: 0
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("${minutes}m · ~$units") },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    )
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
            StatCell("${FormatAdaptabilityLayer.getDisplayUnit(book.format)} logged", pagesFromSessions.toString())
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
private fun NoteCard(note: MarginNote, format: String, onDelete: () -> Unit) {
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
                        "${FormatAdaptabilityLayer.unitAbbrev(format)} ${note.pageOrUnit}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (note.isVoiceDictated) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Dictated on watch",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(JournalingEngine.render(note.markdownContent), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SessionRow(
    session: Session,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditSessionDialog(
    session: Session,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var units by remember { mutableStateOf(session.unitsRead.toString()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session") },
        text = {
            Column {
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it },
                    label = { Text("Pages read") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    units.toIntOrNull()?.let { onConfirm(it) } 
                },
                enabled = units.toIntOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteBottomSheet(
    suggestedPage: Int,
    initialText: String = "",
    onDismiss: () -> Unit,
    onConfirm: (page: Int, text: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pageText by remember { mutableStateOf(if (suggestedPage > 0) "$suggestedPage" else "") }
    var noteText by remember { mutableStateOf(initialText) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add margin note", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = pageText,
                onValueChange = { pageText = it.filter(Char::isDigit) },
                label = { Text("Page") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Your thoughts...") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                enabled = noteText.isNotBlank(),
                onClick = { onConfirm(pageText.toIntOrNull() ?: 0, noteText.trim()) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Note")
            }
        }
    }
}
