package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.journal.NoiseType
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.NoteType
import java.util.Locale

enum class TimerPhase { WORK, BREAK }
enum class TimerMode { COUNTDOWN, STOPWATCH }

private fun formatTimerClock(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}

/**
 * The reading timer. Deliberately quiet: a clock, one big play/pause button,
 * the book you're reading, and everything else (countdown, sounds, zen) one tap
 * away. Double-tap anywhere for zen mode.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FocusTimerScreen(
    book: Book,
    viewModel: BookTrackerViewModel,
    onBack: () -> Unit,
    onSaveSession: () -> Unit,
    onAddNote: (page: Int, text: String, type: NoteType, images: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val phase by viewModel.timerPhase.collectAsState()
    val mode by viewModel.timerMode.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val noiseType by viewModel.noiseType.collectAsState()
    val volume by viewModel.timerVolume.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()
    val openSession by viewModel.openSession.collectAsState()
    // Re-read when the phase or preset changes so the countdown ring has the right length.
    val workMinutes by viewModel.workMinutes.collectAsState()
    val countdownTotal = remember(phase, workMinutes, timeLeftSeconds == 0) { viewModel.countdownTotalSeconds() }
    val countdownDone = mode == TimerMode.COUNTDOWN && timeLeftSeconds <= 0
    val notStarted = !isRunning && !countdownDone &&
        (if (mode == TimerMode.STOPWATCH) elapsedSeconds == 0 else timeLeftSeconds >= countdownTotal)

    // The timer runs as a foreground service; on Android 13+ its notification
    // needs this permission. Asked here, when the reader first uses the timer.
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (!com.example.booktracker.app.notifications.Notifications.canPost(context) &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val bookSessions = remember(book.id, completedSessions) {
        completedSessions.filter { it.bookId == book.id }
    }
    val speedStr = remember(bookSessions, completedSessions) {
        AnalyticsEngine.formatReadingSpeed(AnalyticsEngine.bookPagesPerHour(bookSessions, completedSessions))
    }
    val etaStr = remember(book, bookSessions, completedSessions) {
        AnalyticsEngine.formatEstimatedTime(AnalyticsEngine.bookEstimatedMinutesLeft(book, bookSessions, completedSessions))
    }

    // Live session progress. Taps are saved to the book after a short pause, so the
    // page survives leaving this screen, the save screen sees it, and a burst of
    // taps is one write rather than many.
    var sessionCurrentPage by remember(book.id) { mutableIntStateOf(book.currentPage) }
    val maxPage = if (book.totalPages > 0) book.totalPages else Int.MAX_VALUE
    LaunchedEffect(sessionCurrentPage) {
        kotlinx.coroutines.delay(600)
        if (sessionCurrentPage != book.currentPage) viewModel.setProgress(book, sessionCurrentPage)
    }
    val sessionStartPage = openSession?.takeIf { it.bookId == book.id }?.startPage ?: book.currentPage
    val sessionDelta = (sessionCurrentPage - sessionStartPage).coerceAtLeast(0)

    var isZenMode by remember { mutableStateOf(false) }
    var showSoundscapeSheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showCountdownSheet by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val accent = rememberCoverAccent(book.coverUrl)
    val background = accent.immersiveBackground(MaterialTheme.colorScheme.surfaceContainerLowest)
    val onBackground = if (background.luminance() > 0.45f) Color.Black else Color.White

    // Taken before startFocus promotes the book to Reading, so a visit that never
    // presses Start can put everything back the way it was.
    val statusOnOpen = remember(book.id) { book.status }
    LaunchedEffect(book.id) { viewModel.startFocus(book) }

    val leave = {
        if (notStarted) viewModel.leaveUnstartedFocus(book, statusOnOpen)
        onBack()
    }
    val finish = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.setTimerRunning(false)
        if (sessionCurrentPage != book.currentPage) viewModel.setProgress(book, sessionCurrentPage)
        onSaveSession()
    }
    androidx.activity.compose.BackHandler {
        if (isZenMode) isZenMode = false else leave()
    }

    // Slow breathing glow behind the play button while the clock runs.
    val infiniteTransition = rememberInfiniteTransition(label = "focus_breath")
    val breath by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val displaySeconds = if (mode == TimerMode.STOPWATCH) elapsedSeconds else timeLeftSeconds
    val chrome = onBackground.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isZenMode) Color(0xFF090D14) else background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isZenMode = !isZenMode
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: minimise on the left; sounds, zen and Done on the right.
            AnimatedVisibility(visible = !isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerIconButton(
                        icon = Icons.Filled.KeyboardArrowDown,
                        description = if (notStarted) "Close timer" else "Minimize timer",
                        tint = onBackground,
                        container = chrome,
                        onClick = leave
                    )
                    Spacer(Modifier.weight(1f))
                    TimerIconButton(
                        icon = if (noiseType == NoiseType.NONE) Icons.Filled.Headphones else Icons.Filled.GraphicEq,
                        description = "Ambient sounds",
                        tint = onBackground,
                        container = if (noiseType == NoiseType.NONE) chrome else onBackground.copy(alpha = 0.24f),
                        onClick = { showSoundscapeSheet = true }
                    )
                    Spacer(Modifier.width(8.dp))
                    TimerIconButton(
                        icon = Icons.Filled.Bedtime,
                        description = "Zen mode",
                        tint = onBackground,
                        container = chrome,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isZenMode = true
                        }
                    )
                    AnimatedVisibility(visible = !notStarted) {
                        Row {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = finish,
                                shape = RoundedCornerShape(50),
                                color = onBackground.copy(alpha = 0.18f),
                                contentColor = onBackground,
                                modifier = Modifier.height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Done", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = formatTimerClock(displaySeconds),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (displaySeconds >= 3600) 56.sp else 68.sp,
                    fontWeight = FontWeight.Medium,
                    // Fixed-width digits, so the clock doesn't jiggle every second.
                    fontFeatureSettings = "tnum"
                ),
                color = onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    countdownDone -> "Time's up. Tap to save your session"
                    phase == TimerPhase.BREAK && isRunning -> "Break. Rest your eyes"
                    isRunning && speedStr != null -> "Reading · $speedStr"
                    isRunning -> "Reading…"
                    notStarted -> "Ready when you are"
                    else -> "Paused"
                },
                style = MaterialTheme.typography.titleSmall,
                color = onBackground.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(36.dp))

            AnimatedVisibility(visible = !isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayButton(
                        isRunning = isRunning,
                        countdownDone = countdownDone,
                        // Empty until the clock has actually moved.
                        progress = when {
                            notStarted -> 0f
                            mode == TimerMode.COUNTDOWN -> 1f - timeLeftSeconds.toFloat() / countdownTotal
                            else -> (elapsedSeconds % 3600) / 3600f
                        }.coerceIn(0f, 1f),
                        ringColor = if (phase == TimerPhase.BREAK) Color(0xFF6EE7A0) else onBackground,
                        foreground = onBackground,
                        background = background,
                        glow = if (isRunning) breath else 0f,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (countdownDone) finish() else viewModel.setTimerRunning(!isRunning)
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    // The countdown can only be changed before the clock starts;
                    // after that it's shown for reference.
                    val countdownLabel = when {
                        mode == TimerMode.STOPWATCH -> "Set countdown"
                        phase == TimerPhase.BREAK -> "Break · ${countdownTotal / 60} min"
                        else -> "${countdownTotal / 60} min countdown"
                    }
                    AnimatedVisibility(visible = notStarted || mode == TimerMode.COUNTDOWN) {
                        Surface(
                            onClick = { showCountdownSheet = true },
                            enabled = notStarted,
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            contentColor = onBackground.copy(alpha = if (notStarted) 0.9f else 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, onBackground.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (mode == TimerMode.STOPWATCH) Icons.Filled.Timer else Icons.Filled.HourglassBottom,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(countdownLabel, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(visible = !isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = onBackground.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, onBackground.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCover(book.coverUrl, Modifier.size(width = 64.dp, height = 96.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (book.authors.isNotEmpty()) {
                                Text(
                                    book.authors.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onBackground.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            // Page stepper: the page is saved as you go.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        sessionCurrentPage -= 1
                                    },
                                    enabled = sessionCurrentPage > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Page back", tint = onBackground, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    if (book.totalPages > 0) "p. $sessionCurrentPage / ${book.totalPages}" else "p. $sessionCurrentPage",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = onBackground,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        sessionCurrentPage = (sessionCurrentPage + 1).coerceAtMost(maxPage)
                                    },
                                    enabled = sessionCurrentPage < maxPage,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Page forward", tint = onBackground, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                when {
                                    sessionDelta > 0 -> "+$sessionDelta pages this session"
                                    etaStr != null -> "About $etaStr left in this book"
                                    else -> "Tap + as you turn pages"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = onBackground.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { showNoteSheet = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = onBackground.copy(alpha = 0.16f),
                                contentColor = onBackground
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add a note")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "Double-tap to leave zen mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }

    if (showCountdownSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCountdownSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Countdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Read for a set time. You'll get an alert when it's up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 25, 30, 45, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = mode == TimerMode.COUNTDOWN && countdownTotal == mins * 60,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setTimerMode(TimerMode.COUNTDOWN)
                                viewModel.setWorkMinutes(mins)
                                showCountdownSheet = false
                            },
                            label = { Text("$mins min") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ListItem(
                    headlineContent = { Text("No countdown") },
                    supportingContent = { Text("Count up until you stop") },
                    leadingContent = { Icon(Icons.Filled.Timer, contentDescription = null) },
                    trailingContent = {
                        if (mode == TimerMode.STOPWATCH) Icon(Icons.Filled.Check, contentDescription = "Selected")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.setTimerMode(TimerMode.STOPWATCH)
                            showCountdownSheet = false
                        }
                )
            }
        }
    }

    if (showSoundscapeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSoundscapeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Ambient sounds", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Plays while the timer runs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))

                val sounds = listOf(
                    NoiseType.NONE to ("Off" to "Silence"),
                    NoiseType.RAIN to ("Rain" to "Gentle rain on a window"),
                    NoiseType.FIRE to ("Fireplace" to "A warm crackling hearth"),
                    NoiseType.OCEAN to ("Ocean" to "Slow rolling waves"),
                    NoiseType.BROWN to ("Brown noise" to "Deep, steady rumble"),
                    NoiseType.WHITE to ("White noise" to "Masks background chatter")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sounds.forEach { (type, meta) ->
                        val isSelected = noiseType == type
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setNoiseType(type)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        meta.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        meta.second,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = volume,
                    onValueChange = { viewModel.setTimerVolume(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showNoteSheet) {
        NoteSheet(
            suggestedPage = sessionCurrentPage,
            totalPages = book.totalPages,
            pageLabel = FormatAdaptabilityLayer.getDisplayUnit(book).trimEnd('s'),
            draftKey = book.id,
            onDismiss = { showNoteSheet = false },
            onConfirm = { page, text, type, images ->
                onAddNote(page, text, type, images)
                showNoteSheet = false
            },
            onConfirmAndAddAnother = onAddNote
        )
    }
}

@Composable
private fun TimerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    container: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .background(container, CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

/** The big round play/pause button, ringed by the session's progress. */
@Composable
private fun PlayButton(
    isRunning: Boolean,
    countdownDone: Boolean,
    progress: Float,
    ringColor: Color,
    foreground: Color,
    background: Color,
    glow: Float,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            if (glow > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(foreground.copy(alpha = 0.10f + 0.12f * glow), Color.Transparent)
                    ),
                    radius = size.minDimension / 2f
                )
            }
            drawArc(
                color = foreground.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (progress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = foreground,
            contentColor = background,
            shadowElevation = 8.dp,
            modifier = Modifier.size(112.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        countdownDone -> Icons.Filled.Check
                        isRunning -> Icons.Filled.Pause
                        else -> Icons.Filled.PlayArrow
                    },
                    contentDescription = when {
                        countdownDone -> "Save session"
                        isRunning -> "Pause"
                        else -> "Start"
                    },
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}
