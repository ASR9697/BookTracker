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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Place
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val saveDateFormat = DateTimeFormatter.ofPattern("M/d/yyyy")
private val saveTimeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/**
 * The one review step every finished session passes through, whether it came from
 * the focus timer or the home card. Everything shown here is editable before it is
 * committed — the reader confirms the page, the clock, and the resulting status
 * rather than inheriting whatever the timer happened to record.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSaveScreen(
    book: Book,
    initialStartTime: Long,
    initialDurationSeconds: Int,
    initialPage: Int,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
    onSave: (endPage: Int, tag: String, startTime: Long, endTime: Long, status: BookStatus?) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val haptic = LocalHapticFeedback.current

    var endPage by remember { mutableIntStateOf(initialPage.coerceAtMost(if (book.totalPages > 0) book.totalPages else initialPage)) }
    var durationSeconds by remember { mutableIntStateOf(initialDurationSeconds) }
    var status by remember { mutableStateOf<BookStatus?>(null) }
    var tag by remember { mutableStateOf("") }

    var endDateTime by remember {
        mutableStateOf(
            Instant.ofEpochMilli(initialStartTime + initialDurationSeconds * 1000L)
                .atZone(zone)
                .toLocalDateTime()
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPagePicker by remember { mutableStateOf(false) }
    var showDurationEditor by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }

    val unit = FormatAdaptabilityLayer.unitAbbrev(book)
    val complete = book.totalPages > 0 && endPage >= book.totalPages

    val effectiveStatus = status ?: if (complete) BookStatus.FINISHED else BookStatus.READING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showOptions = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                                .size(44.dp)
                        ) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = "Session options",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit read time") },
                                leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                                onClick = {
                                    showOptions = false
                                    showDurationEditor = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Where you read") },
                                leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                                onClick = {
                                    showOptions = false
                                    showTagPicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Discard session", color = MaterialTheme.colorScheme.error)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOptions = false
                                    showDiscardConfirm = true
                                }
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val end = endDateTime.atZone(zone).toInstant().toEpochMilli()
                            val start = end - durationSeconds * 1000L
                            onSave(endPage, tag, start, end, status)
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Save reading session",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Please check the contents to be saved.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditableStamp(endDateTime.toLocalDate().format(saveDateFormat)) { showDatePicker = true }
                Spacer(Modifier.width(12.dp))
                EditableStamp(endDateTime.toLocalTime().format(saveTimeFormat)) { showTimePicker = true }
            }

            Spacer(Modifier.height(20.dp))

            SaveRow(
                icon = { Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                label = "Read time",
                value = formatClock(durationSeconds),
                onClick = { showDurationEditor = true }
            )

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                SaveRow(
                    icon = { Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                    label = "Reading progress",
                    value = "$unit $endPage",
                    trailing = if (book.totalPages > 0) " / ${book.totalPages}" else null,
                    onClick = { showPagePicker = true }
                )

                // Outcome shortcuts, anchored to the progress row and revealed
                // while it is being edited — the same coupling as the reference.
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPagePicker,
                    enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.expandVertically(expandFrom = Alignment.Top),
                    exit = androidx.compose.animation.fadeOut() +
                        androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.offset(y = (-56).dp)
                    ) {
                        OutcomeButton("Give up", Icons.Filled.Flag, status == BookStatus.DNF) {
                            status = if (status == BookStatus.DNF) null else BookStatus.DNF
                        }
                        OutcomeButton("Pause", Icons.Filled.Pause, status == BookStatus.PAUSED) {
                            status = if (status == BookStatus.PAUSED) null else BookStatus.PAUSED
                        }
                        OutcomeButton("I've read it all!", Icons.Filled.MenuBook, status == BookStatus.FINISHED) {
                            if (status == BookStatus.FINISHED) {
                                status = null
                            } else {
                                status = BookStatus.FINISHED
                                if (book.totalPages > 0) endPage = book.totalPages
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SaveRow(
                icon = { Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                label = "Status",
                value = effectiveStatus.label,
                onClick = null
            )

            Spacer(Modifier.height(12.dp))

            SaveRow(
                icon = { Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                label = "Where you read",
                value = tag.ifBlank { "Not set" },
                onClick = { showTagPicker = true }
            )

            if (complete && status != BookStatus.DNF) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "You reached the last ${unit.trimEnd('.')} — this will finish the book.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = endDateTime.toLocalDate()
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        endDateTime = LocalDateTime.of(picked, endDateTime.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = endDateTime.hour,
            initialMinute = endDateTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Session time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    endDateTime = LocalDateTime.of(
                        endDateTime.toLocalDate(),
                        LocalTime.of(state.hour, state.minute)
                    )
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showPagePicker) {
        ProgressPickerSheet(
            book = book,
            initialPage = endPage,
            onDismiss = { showPagePicker = false },
            onConfirm = {
                endPage = it
                if (book.totalPages > 0 && it >= book.totalPages) status = BookStatus.FINISHED
                showPagePicker = false
            }
        )
    }

    if (showDurationEditor) {
        DurationEditorDialog(
            initialSeconds = durationSeconds,
            onDismiss = { showDurationEditor = false },
            onConfirm = {
                durationSeconds = it
                showDurationEditor = false
            }
        )
    }

    if (showTagPicker) {
        TagPickerDialog(
            initial = tag,
            onDismiss = { showTagPicker = false },
            onConfirm = {
                tag = it
                showTagPicker = false
            }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this session?") },
            text = { Text("The time you just read will not be recorded. Your reading progress stays where it is.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDiscard()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun EditableStamp(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SaveRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    trailing: String? = null,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(20.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (trailing != null) {
                    Text(
                        trailing,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OutcomeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Spacer(Modifier.width(10.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DurationEditorDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours by remember { mutableStateOf((initialSeconds / 3600).toString()) }
    var minutes by remember { mutableStateOf(((initialSeconds % 3600) / 60).toString()) }
    var seconds by remember { mutableStateOf((initialSeconds % 60).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Read time") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Hr", hours, { v: String -> hours = v }),
                    Triple("Min", minutes, { v: String -> minutes = v }),
                    Triple("Sec", seconds, { v: String -> seconds = v })
                ).forEach { (label, value, setter) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { setter(it.filter(Char::isDigit).take(3)) },
                        label = { Text(label) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val total = (hours.toIntOrNull() ?: 0) * 3600 +
                    (minutes.toIntOrNull() ?: 0) * 60 +
                    (seconds.toIntOrNull() ?: 0)
                onConfirm(total.coerceAtLeast(0))
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TagPickerDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val presets = listOf("Bed", "Coffee Shop", "Commute", "Couch", "Desk", "Tea")
    var custom by remember { mutableStateOf(if (initial in presets) "" else initial) }
    var selected by remember { mutableStateOf(initial.takeIf { it in presets }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Where or how were you reading?") },
        text = {
            Column {
                presets.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { preset ->
                            FilterChip(
                                selected = selected == preset,
                                onClick = {
                                    selected = if (selected == preset) null else preset
                                    if (selected != null) custom = ""
                                },
                                label = { Text(preset) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = {
                        custom = it
                        if (it.isNotEmpty()) selected = null
                    },
                    label = { Text("Custom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(custom.trim().ifBlank { selected.orEmpty() })
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

internal fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}
