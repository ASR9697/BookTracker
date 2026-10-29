package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import com.example.booktracker.shared.models.Session
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.clip
import com.example.booktracker.app.ui.animations.staggeredEntrance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.booktracker.app.analytics.AnalyticsEngine
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.rounded.Delete
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

private val saveDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

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
    /** Where this session began; the focus timer moves the bookmark live, so book.currentPage isn't it. */
    sessionStartPage: Int,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
    onSave: (SessionDraft) -> Unit,
    /** A session of this book that ended a little earlier; offered as one to join onto. */
    previousSession: Session? = null,
    /** Shorter than this with no pages read asks before saving; 0 never asks. */
    minSessionSeconds: Int = 0,
    /** Open the camera on the page number rather than the page wheel. */
    scanFirst: Boolean = false
) {
    val zone = remember { ZoneId.systemDefault() }
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val saveTimeFormat = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
    }
    var showStatusPicker by remember { mutableStateOf(false) }

    var endPage by remember { mutableIntStateOf(initialPage.coerceAtMost(if (book.totalPages > 0) book.totalPages else initialPage)) }
    val saveHaptics = com.example.booktracker.app.ui.animations.rememberHaptics()
    var durationSeconds by remember { mutableIntStateOf(initialDurationSeconds) }
    var status by remember { mutableStateOf<BookStatus?>(null) }
    var tag by remember { mutableStateOf("") }
    var focus by remember { mutableIntStateOf(0) }
    var quote by remember { mutableStateOf("") }
    var joinPrevious by remember { mutableStateOf(false) }
    var showTooShort by remember { mutableStateOf(false) }
    var scanOnOpen by remember { mutableStateOf(false) }

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
    // The page is the one thing the timer can't know, so ask for it on arrival, once.
    var askedForPage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!askedForPage) {
            askedForPage = true
            kotlinx.coroutines.delay(250)
            scanOnOpen = scanFirst
            showPagePicker = true
        }
    }
    var showDurationEditor by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }

    val unit = FormatAdaptabilityLayer.unitAbbrev(book)
    val complete = book.totalPages > 0 && endPage >= book.totalPages

    val effectiveStatus = status ?: if (complete) BookStatus.FINISHED else BookStatus.READING

    val commit = {
        saveHaptics.confirm()
        val end = endDateTime.atZone(zone).toInstant().toEpochMilli()
        val start = end - durationSeconds * 1000L
        onSave(
            SessionDraft(
                endPage = endPage,
                tag = tag,
                startTime = start,
                endTime = end,
                status = effectiveStatus,
                focus = focus,
                quote = quote,
                mergeWithId = previousSession?.id?.takeIf { joinPrevious }
            )
        )
    }
    val tooShort = minSessionSeconds > 0 && durationSeconds < minSessionSeconds &&
        endPage <= sessionStartPage && !joinPrevious

    val accent = rememberCoverAccent(book.coverUrl)
    val entrance = com.example.booktracker.app.ui.animations.rememberEntranceState()
    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .coverGlow(accent),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                                Icons.Rounded.Tune,
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
                                leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                                onClick = {
                                    showOptions = false
                                    showDurationEditor = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Where you read") },
                                leadingIcon = { Icon(Icons.Rounded.Place, contentDescription = null) },
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
                                        Icons.Rounded.Delete,
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
                        onClick = { if (tooShort) showTooShort = true else commit() },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
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
                .readableWidth()
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
                "Check the page you stopped on, then save.",
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

                modifier = Modifier.staggeredEntrance(entrance, 1),
                icon = { Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                label = "Read time",
                value = formatClock(durationSeconds),
                onClick = { showDurationEditor = true }
            )

            Spacer(Modifier.height(12.dp))

            SaveRow(

                modifier = Modifier.staggeredEntrance(entrance, 2),
                icon = { Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                label = "Reading progress",
                value = "$unit $endPage",
                trailing = if (book.totalPages > 0) " / ${book.totalPages}" else null,
                onClick = { showPagePicker = true }
            )

            val deltaPages = (endPage - sessionStartPage).coerceAtLeast(0)
            val sessionPpm = remember(deltaPages, durationSeconds) {
                AnalyticsEngine.sessionSpeedPpm(deltaPages, durationSeconds)
            }
            val sessionSpeedText = remember(sessionPpm) { AnalyticsEngine.formatSessionPpm(sessionPpm) }
            val remainingPages = if (book.totalPages > 0) (book.totalPages - endPage).coerceAtLeast(0) else 0
            val remainingMinutes = if (remainingPages > 0 && sessionPpm != null && sessionPpm > 0f) {
                (remainingPages / sessionPpm).toLong()
            } else null
            val remainingEtaText = remember(remainingMinutes) { AnalyticsEngine.formatEstimatedTime(remainingMinutes) }

            if (deltaPages > 0) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = AppShape.M,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (sessionSpeedText != null) "+$deltaPages $unit at $sessionSpeedText"
                                else "+$deltaPages $unit this session",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (remainingPages > 0 && remainingEtaText != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "~${remainingEtaText.removeSuffix(" left")} to finish the book at this pace",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SaveRow(

                modifier = Modifier.staggeredEntrance(entrance, 3),
                icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                label = "Status",
                value = effectiveStatus.label,
                onClick = { showStatusPicker = true }
            )

            Spacer(Modifier.height(12.dp))

            SaveRow(

                modifier = Modifier.staggeredEntrance(entrance, 4),
                icon = { Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                label = "Where you read",
                value = tag.ifBlank { "Not set" },
                onClick = { showTagPicker = true }
            )

            if (previousSession != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.L)
                        .clickable { joinPrevious = !joinPrevious }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val endedAt = Instant.ofEpochMilli(previousSession.endTime).atZone(zone).toLocalTime().format(saveTimeFormat)
                        Text("Join your $endedAt session", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${AnalyticsEngine.formatMinutes(previousSession.durationSeconds / 60L)}, $unit ${previousSession.startPage} → ${previousSession.endPage}. Saved as one session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(checked = joinPrevious, onCheckedChange = { joinPrevious = it })
                }
            }

            Spacer(Modifier.height(12.dp))
            FocusPicker(focus, Modifier.staggeredEntrance(entrance, 5)) { focus = if (focus == it) 0 else it }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = quote,
                onValueChange = { quote = it.take(1000) },
                label = { Text("A line worth keeping (optional)") },
                supportingText = { Text("Saved as a note on $unit $endPage") },
                leadingIcon = { Icon(Icons.Rounded.FormatQuote, contentDescription = null) },
                minLines = 2,
                shape = AppShape.L,
                modifier = Modifier.fillMaxWidth()
            )

            if (complete && status != BookStatus.DNF) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
            is24Hour = is24Hour
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
            sessionStartPage = sessionStartPage,
            sessionSeconds = durationSeconds,
            startWithScan = scanOnOpen,
            onDismiss = {
                showPagePicker = false
                scanOnOpen = false
            },
            onConfirm = {
                endPage = it
                if (book.totalPages > 0 && it >= book.totalPages) status = BookStatus.FINISHED
                showPagePicker = false
                scanOnOpen = false
            }
        )
    }

    if (showTooShort) {
        ActionSheet(
            onDismiss = { showTooShort = false },
            icon = Icons.Rounded.Timer,
            title = "Only ${formatShort(durationSeconds)}, with no pages",
            message = "Sessions this short are usually started by accident. Keep it anyway, or let it go?",
            confirmLabel = "Let it go",
            onConfirm = {
                showTooShort = false
                onDiscard()
            },
            destructive = true,
            dismissLabel = "Save anyway",
            onDismissAction = {
                showTooShort = false
                commit()
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

    if (showStatusPicker) {
        ActionSheet(
            onDismiss = { showStatusPicker = false },
            icon = Icons.Rounded.MenuBook,
            title = "After this session",
            message = "Where does this book stand now?",
            dismissLabel = "Close"
        ) {
            OutcomeButton("Keep reading", Icons.Rounded.Bookmark, effectiveStatus == BookStatus.READING) {
                status = BookStatus.READING
                showStatusPicker = false
            }
            OutcomeButton("Pause this book", Icons.Rounded.Pause, effectiveStatus == BookStatus.PAUSED) {
                status = BookStatus.PAUSED
                showStatusPicker = false
            }
            OutcomeButton("I've finished it", Icons.Rounded.Check, effectiveStatus == BookStatus.FINISHED) {
                status = BookStatus.FINISHED
                if (book.totalPages > 0) endPage = book.totalPages
                showStatusPicker = false
            }
            OutcomeButton("Give up on it", Icons.Rounded.Flag, effectiveStatus == BookStatus.DNF) {
                status = BookStatus.DNF
                showStatusPicker = false
            }
        }
    }

    if (showDiscardConfirm) {
        ActionSheet(
            onDismiss = { showDiscardConfirm = false },
            icon = Icons.Rounded.Delete,
            title = "Discard this session?",
            message = "The time you just read won't be recorded. Your page stays where it is.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardConfirm = false
                onDiscard()
            },
            destructive = true,
            dismissLabel = "Keep it"
        )
    }
}

/** What the reader confirmed on the save screen. */
data class SessionDraft(
    val endPage: Int,
    val tag: String,
    val startTime: Long,
    val endTime: Long,
    val status: BookStatus?,
    val focus: Int,
    val quote: String,
    /** The earlier session to join this one onto, when the reader chose to. */
    val mergeWithId: String?
)

private fun formatShort(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${seconds / 60} min"

internal fun focusLabel(level: Int): String = when (level) {
    1 -> "Distracted"
    2 -> "Restless"
    3 -> "Steady"
    4 -> "Focused"
    5 -> "Deep focus"
    else -> "Not rated"
}

/** Five steps from distracted to deep focus; tapping the chosen one clears it. */
@Composable
private fun FocusPicker(focus: Int, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.L)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(20.dp))
            Column {
                Text("How focused were you?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(focusLabel(focus), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { level ->
                val on = level <= focus
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPick(level)
                    },
                    shape = AppShape.S,
                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .semantics { contentDescription = focusLabel(level) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableStamp(text: String, onClick: () -> Unit) {
    Surface(
        shape = AppShape.XS,
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
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Clipped first, so the press ripple follows the rounded corners.
            .clip(AppShape.L)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                // A changed value rolls in from below rather than snapping.
                androidx.compose.animation.AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (androidx.compose.animation.slideInVertically { it / 2 } + androidx.compose.animation.fadeIn()) togetherWith
                            (androidx.compose.animation.slideOutVertically { -it / 2 } + androidx.compose.animation.fadeOut())
                    },
                    label = "saveValue"
                ) { shown ->
                    Text(
                        shown,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
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
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
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

    ActionSheet(
        onDismiss = onDismiss,
        icon = Icons.Rounded.Timer,
        title = "Read time",
        message = "How long you actually read, pauses left out.",
        confirmLabel = "Save",
        onConfirm = {
            val total = (hours.toIntOrNull() ?: 0) * 3600 +
                (minutes.toIntOrNull() ?: 0) * 60 +
                (seconds.toIntOrNull() ?: 0)
            onConfirm(total.coerceAtLeast(0))
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("Hours", hours, { v: String -> hours = v }),
                Triple("Minutes", minutes, { v: String -> minutes = v }),
                Triple("Seconds", seconds, { v: String -> seconds = v })
            ).forEach { (label, value, setter) ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { setter(it.filter(Char::isDigit).take(3)) },
                    label = { Text(label) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        }
    }
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

    ActionSheet(
        onDismiss = onDismiss,
        icon = Icons.Rounded.Place,
        title = "Where were you reading?",
        message = "Stats show where you read best.",
        confirmLabel = "Save",
        onConfirm = { onConfirm(custom.trim().ifBlank { selected.orEmpty() }) }
    ) {
        presets.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { preset ->
                    TickFilterChip(
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
        }
        OutlinedTextField(
            value = custom,
            onValueChange = {
                custom = it
                if (it.isNotEmpty()) selected = null
            },
            label = { Text("Somewhere else") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

internal fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}
