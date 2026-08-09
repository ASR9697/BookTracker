package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.journal.NoiseType
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.NoteType
import java.util.Locale

enum class TimerPhase { WORK, BREAK }
enum class TimerMode { COUNTDOWN, STOPWATCH }

/**
 * The reading screen. Everything on it is in service of one thing — staying in
 * the book — so the surface takes its colour from the cover, the only large
 * control is play/pause, and capturing a note never leaves the timer running
 * behind a different screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    book: Book,
    viewModel: BookTrackerViewModel,
    onBack: () -> Unit,
    onStartSession: (Book, Int) -> Unit,
    onSaveSession: () -> Unit,
    onAddNote: (page: Int, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val phase by viewModel.timerPhase.collectAsState()
    val mode by viewModel.timerMode.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val noiseType by viewModel.noiseType.collectAsState()
    val volume by viewModel.timerVolume.collectAsState()
    val openSession by viewModel.openSession.collectAsState()

    var showStartDialog by remember { mutableStateOf(false) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var showSoundscape by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val accent = rememberCoverAccent(book.coverUrl)
    val background = accent.immersiveBackground(MaterialTheme.colorScheme.surfaceContainerHighest)
    val onBackground = if (background.luminance() > 0.4f) Color.Black else Color.White

    LaunchedEffect(book.id) { viewModel.setTimerBook(book.id) }

    LaunchedEffect(openSession) {
        if (openSession == null && !isRunning) showStartDialog = true
    }

    LaunchedEffect(Unit) {
        com.example.booktracker.app.data.ServiceLocator.timerExpiredEvents.collect { onSaveSession() }
    }

    val displaySeconds = if (mode == TimerMode.STOPWATCH) elapsedSeconds else timeLeftSeconds

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.Close, contentDescription = "Leave timer", tint = onBackground)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSoundscape = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Soundscape", tint = onBackground)
                }
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = onBackground.copy(alpha = 0.18f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setTimerRunning(false)
                        onSaveSession()
                    },
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = onBackground, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Done", color = onBackground, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.weight(0.6f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    formatClock(displaySeconds),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Light,
                    color = onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        phase == TimerPhase.BREAK -> "Take a break…"
                        isRunning -> "Reading…"
                        else -> "Paused"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackground.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(onBackground.copy(alpha = 0.25f), CircleShape)
                        .padding(10.dp)
                        .background(onBackground, CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isRunning) {
                                viewModel.setTimerRunning(false)
                            } else if (openSession == null) {
                                showStartDialog = true
                            } else {
                                viewModel.setTimerRunning(true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Resume",
                        modifier = Modifier.size(56.dp),
                        tint = background
                    )
                }

                Spacer(Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, onBackground.copy(alpha = 0.4f)),
                    onClick = { showCountdownDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = onBackground, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (mode == TimerMode.COUNTDOWN) "Change countdown" else "Set countdown",
                            color = onBackground,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            NowReadingCard(
                book = book,
                tint = onBackground,
                onAddNote = { showNoteSheet = true }
            )
        }
    }

    if (showStartDialog) {
        StartSessionDialog(
            currentPage = book.currentPage,
            totalPages = book.totalPages,
            unitName = if (book.totalPages > 0) "Page" else "Unit",
            onDismiss = {
                showStartDialog = false
                if (openSession == null) onBack()
            },
            onConfirm = { startPage ->
                onStartSession(book, startPage)
                viewModel.setTimerRunning(true)
                showStartDialog = false
            }
        )
    }

    if (showCountdownDialog) {
        CountdownDialog(
            currentMode = mode,
            onDismiss = { showCountdownDialog = false },
            onStopwatch = {
                viewModel.setTimerMode(TimerMode.STOPWATCH)
                showCountdownDialog = false
            },
            onMinutes = { minutes ->
                viewModel.setTimerMode(TimerMode.COUNTDOWN)
                viewModel.setCustomWorkMinutes(minutes)
                showCountdownDialog = false
            }
        )
    }

    if (showSoundscape) {
        ModalBottomSheet(
            onDismissRequest = { showSoundscape = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Soundscape", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                val options = NoiseType.entries.toTypedArray()
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { type ->
                        androidx.compose.material3.FilterChip(
                            selected = noiseType == type,
                            onClick = { viewModel.setNoiseType(type) },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Volume", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = volume,
                    onValueChange = { viewModel.setTimerVolume(it) },
                    valueRange = 0f..1f
                )
            }
        }
    }

    if (showNoteSheet) {
        InSessionNoteSheet(
            book = book,
            onDismiss = { showNoteSheet = false },
            onConfirm = { page, text ->
                onAddNote(page, text)
                showNoteSheet = false
            }
        )
    }
}

@Composable
private fun NowReadingCard(book: Book, tint: Color, onAddNote: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookCover(book.coverUrl, Modifier.size(width = 62.dp, height = 92.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (book.authors.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                buildString {
                    append("${FormatAdaptabilityLayer.unitAbbrev(book)} ${book.currentPage}")
                    if (book.totalPages > 0) append(" / ${book.totalPages}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tint.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.2f),
            onClick = onAddNote
        ) {
            Icon(
                Icons.AutoMirrored.Filled.NoteAdd,
                contentDescription = "Add a note",
                tint = tint,
                modifier = Modifier.padding(12.dp).size(24.dp)
            )
        }
    }
}

@Composable
private fun CountdownDialog(
    currentMode: TimerMode,
    onDismiss: () -> Unit,
    onStopwatch: () -> Unit,
    onMinutes: (Int) -> Unit
) {
    var custom by remember { mutableStateOf("") }
    val presets = listOf(10, 15, 25, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Countdown") },
        text = {
            Column {
                Text(
                    "Switching now keeps the time you have already read.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                presets.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { minutes ->
                            Button(
                                onClick = { onMinutes(minutes) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("${minutes}m") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(3) },
                    label = { Text("Custom minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { custom.toIntOrNull()?.takeIf { it > 0 }?.let(onMinutes) },
                enabled = (custom.toIntOrNull() ?: 0) > 0
            ) { Text("Use custom") }
        },
        dismissButton = {
            TextButton(onClick = if (currentMode == TimerMode.COUNTDOWN) onStopwatch else onDismiss) {
                Text(if (currentMode == TimerMode.COUNTDOWN) "Count up instead" else "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InSessionNoteSheet(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (page: Int, text: String) -> Unit
) {
    var pageText by remember { mutableStateOf(book.currentPage.toString()) }
    var noteText by remember { mutableStateOf("") }
    val page = pageText.toIntOrNull()
    val pageValid = page != null && page >= 0 && (book.totalPages <= 0 || page <= book.totalPages)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Quick note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = pageText,
                onValueChange = { pageText = it.filter(Char::isDigit).take(6) },
                label = { Text(FormatAdaptabilityLayer.getDisplayUnit(book).trimEnd('s')) },
                isError = pageText.isNotEmpty() && !pageValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("What stood out?") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(page ?: 0, noteText.trim()) },
                enabled = noteText.isNotBlank() && pageValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Save note", fontWeight = FontWeight.Bold)
            }
        }
    }
}
