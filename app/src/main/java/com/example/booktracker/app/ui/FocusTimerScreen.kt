package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

enum class TimerMode { COUNTDOWN, STOPWATCH }

private const val STILL_READING_AFTER_SECONDS = 30 * 60
private const val STILL_READING_GRACE_SECONDS = 5 * 60
private const val ZEN_DIM_AFTER_MS = 10_000L
private const val ZEN_DIM_ALPHA = 0.22f
private const val EXTEND_MINUTES = 5

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
    /** Carries the live page, since the save screen can open before that write lands. */
    onSaveSession: (endPage: Int) -> Unit,
    onAddNote: (page: Int, text: String, type: NoteType, images: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val mode by viewModel.timerMode.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    // Paper-book readers set the phone down; keep the display awake while the timer is open.
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val timerView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(keepScreenOn) {
        timerView.keepScreenOn = keepScreenOn
        onDispose { timerView.keepScreenOn = false }
    }
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val noiseType by viewModel.noiseType.collectAsState()
    val volume by viewModel.timerVolume.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()
    val openSession by viewModel.openSession.collectAsState()
    // Re-read when the preset changes so the countdown ring has the right length.
    val workMinutes by viewModel.workMinutes.collectAsState()
    val countdownTotal = remember(workMinutes, timeLeftSeconds == 0) { viewModel.countdownTotalSeconds() }
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
    // Only this book's own history: another book's pace says little about this one.
    val bookSpeedStr = remember(bookSessions) {
        AnalyticsEngine.formatReadingSpeed(AnalyticsEngine.pagesPerHour(bookSessions))
    }
    val activeReadingSeconds by com.example.booktracker.app.data.ServiceLocator.activeReadingSeconds.collectAsState()
    val etaStr = remember(book, bookSessions, completedSessions) {
        AnalyticsEngine.formatEstimatedTime(AnalyticsEngine.bookEstimatedMinutesLeft(book, bookSessions, completedSessions))
    }

    // Live session progress. Taps are saved to the book after a short pause, so the
    // page survives leaving this screen, the save screen sees it, and a burst of
    // taps is one write rather than many.
    var sessionCurrentPage by remember(book.id) { mutableIntStateOf(book.currentPage) }
    // The page this screen last wrote, so a change made elsewhere (notification, widget,
    // watch) can be told apart from our own write landing, and taken up instead of overwritten.
    var lastWrittenPage by remember(book.id) { mutableIntStateOf(book.currentPage) }
    val maxPage = if (book.totalPages > 0) book.totalPages else Int.MAX_VALUE
    LaunchedEffect(sessionCurrentPage) {
        kotlinx.coroutines.delay(600)
        if (sessionCurrentPage != book.currentPage) {
            lastWrittenPage = sessionCurrentPage
            viewModel.setProgress(book, sessionCurrentPage)
        }
    }
    LaunchedEffect(book.currentPage) {
        if (book.currentPage != lastWrittenPage) {
            lastWrittenPage = book.currentPage
            sessionCurrentPage = book.currentPage
        }
    }
    val sessionStartPage = openSession?.takeIf { it.bookId == book.id }?.startPage ?: book.currentPage
    val sessionDelta = (sessionCurrentPage - sessionStartPage).coerceAtLeast(0)
    // Measured once a minute has passed and a page has turned; until then, the book's usual pace.
    val livePaceStr = AnalyticsEngine.formatSessionPpm(
        AnalyticsEngine.sessionSpeedPpm(sessionDelta, activeReadingSeconds)
    )
    val pageWord = FormatAdaptabilityLayer.getDisplayUnit(book).lowercase().let { if (sessionDelta == 1) it.trimEnd('s') else it }

    // Page goal: how far this session means to get, counted from where it began.
    val pageTarget by com.example.booktracker.app.data.ServiceLocator.sessionPageTarget.collectAsState()
    val goalPages = pageTarget?.let { (it - sessionStartPage).coerceAtLeast(1) }
    val goalReached = pageTarget?.let { sessionCurrentPage >= it } == true
    var goalWasReached by remember(book.id) { mutableStateOf(goalReached) }
    val goalHaptic = LocalHapticFeedback.current
    LaunchedEffect(goalReached) {
        if (goalReached && !goalWasReached) goalHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        goalWasReached = goalReached
    }
    var showGoalSheet by remember { mutableStateOf(false) }

    // "Still reading?" after a long stretch with no touch; any touch on the screen answers it.
    // Measured in reading seconds, so time spent paused never counts towards it.
    val prefs = LocalAppPrefs.current
    val checkInHaptic = LocalHapticFeedback.current
    var lastTouchSecond by remember(book.id) { mutableIntStateOf(activeReadingSeconds) }
    var askingSince by remember(book.id) { mutableStateOf<Int?>(null) }
    var pausedUnattended by remember(book.id) { mutableStateOf(false) }
    val markTouched = {
        lastTouchSecond = activeReadingSeconds
        askingSince = null
    }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            markTouched()
            pausedUnattended = false
        }
    }
    LaunchedEffect(activeReadingSeconds, isRunning, prefs.stillReadingCheck) {
        if (!prefs.stillReadingCheck || !isRunning) {
            askingSince = null
            return@LaunchedEffect
        }
        val asked = askingSince
        if (asked == null) {
            if (activeReadingSeconds - lastTouchSecond >= STILL_READING_AFTER_SECONDS) {
                askingSince = activeReadingSeconds
                checkInHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else if (activeReadingSeconds - asked >= STILL_READING_GRACE_SECONDS) {
            askingSince = null
            pausedUnattended = true
            viewModel.pauseUnattended(activeReadingSeconds - asked)
        }
    }

    // How long the current pause has lasted, ticking while paused.
    val pausedAt by com.example.booktracker.app.data.ServiceLocator.timerPausedAt.collectAsState()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pausedAt, isRunning) {
        while (pausedAt != null && !isRunning) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val pausedForSeconds = pausedAt?.takeIf { !isRunning }?.let { ((nowMillis - it) / 1000).toInt().coerceAtLeast(0) }

    var isZenMode by remember { mutableStateOf(false) }
    // In zen the clock fades to a dim glow after a while; a tap anywhere brings it back.
    var zenWake by remember { mutableIntStateOf(0) }
    var zenDimmed by remember { mutableStateOf(false) }
    LaunchedEffect(isZenMode, zenWake) {
        zenDimmed = false
        if (isZenMode) {
            kotlinx.coroutines.delay(ZEN_DIM_AFTER_MS)
            zenDimmed = true
        }
    }
    var showSoundscapeSheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showCountdownSheet by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val haptics = com.example.booktracker.app.ui.animations.rememberHaptics()
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
        haptics.confirm()
        viewModel.setTimerRunning(false)
        if (sessionCurrentPage != book.currentPage) {
            lastWrittenPage = sessionCurrentPage
            viewModel.setProgress(book, sessionCurrentPage)
        }
        onSaveSession(sessionCurrentPage)
    }
    androidx.activity.compose.BackHandler {
        if (isZenMode) isZenMode = false else leave()
    }

    // Slow breathing glow behind the play button while the clock runs; still under reduce motion.
    val reduceMotion = com.example.booktracker.app.ui.animations.rememberReduceMotion()
    val breath = if (reduceMotion) 0f else {
        val infiniteTransition = rememberInfiniteTransition(label = "focus_breath")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
        pulse
    }
    val zenClockAlpha by animateFloatAsState(
        targetValue = if (isZenMode && zenDimmed) ZEN_DIM_ALPHA else 1f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 1500),
        label = "zenDim"
    )
    var showProgressPicker by remember { mutableStateOf(false) }

    val displaySeconds = if (mode == TimerMode.STOPWATCH) elapsedSeconds else timeLeftSeconds
    val chrome = onBackground.copy(alpha = 0.12f)

    // Tablets: this book's notes sit beside the clock.
    val showNotesPane = LocalWidthTier.current.atLeastExpanded
    val bookNotes by remember(book.id) { viewModel.notesFor(book.id) }.collectAsState(initial = emptyList())
    // Half-open foldables: the clock above the hinge, the controls below it.
    val hinge = rememberTabletopHinge()
    var columnTop by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val hingeTop = hinge?.let { with(density) { (it.top - columnTop).toDp() } }?.takeIf { it > 160.dp }
    val hingeHeight = hinge?.let { with(density) { it.height.toDp() } } ?: 0.dp
    // Space starts and pauses the clock from a keyboard.
    val keyFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { keyFocus.requestFocus() } }

    Row(
        modifier = modifier
            .fillMaxSize()
            // Watches every touch without taking it, so a touch anywhere answers "still reading?".
            .pointerInput(book.id) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        markTouched()
                    }
                }
            }
    ) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (isZenMode) com.example.booktracker.app.ui.theme.ZenNight else background)
            .focusRequester(keyFocus)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Spacebar) {
                    if (countdownDone) finish() else viewModel.setTimerRunning(!isRunning)
                    true
                } else {
                    false
                }
            }
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { zenWake++ },
                    onDoubleTap = {
                        haptics.confirm()
                        isZenMode = !isZenMode
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .onGloballyPositioned { columnTop = it.positionInWindow().y },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val topPart: @Composable ColumnScope.() -> Unit = {
            // Top bar: minimise on the left; sounds, zen and Done on the right.
            AnimatedVisibility(visible = !isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerIconButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        description = if (notStarted) "Close timer" else "Minimize timer",
                        tint = onBackground,
                        container = chrome,
                        onClick = leave
                    )
                    Spacer(Modifier.weight(1f))
                    TimerIconButton(
                        icon = if (noiseType == NoiseType.NONE) Icons.Rounded.Headphones else Icons.Rounded.GraphicEq,
                        description = "Ambient sounds",
                        tint = onBackground,
                        container = if (noiseType == NoiseType.NONE) chrome else onBackground.copy(alpha = 0.24f),
                        onClick = { showSoundscapeSheet = true }
                    )
                    Spacer(Modifier.width(8.dp))
                    TimerIconButton(
                        icon = Icons.Rounded.Bedtime,
                        description = "Zen mode",
                        tint = onBackground,
                        container = chrome,
                        onClick = {
                            haptics.confirm()
                            isZenMode = true
                        }
                    )
                    AnimatedVisibility(visible = !notStarted) {
                        Row {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = finish,
                                shape = CircleShape,
                                color = onBackground.copy(alpha = 0.18f),
                                contentColor = onBackground,
                                modifier = Modifier.height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
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
                color = onBackground,
                modifier = Modifier.alpha(zenClockAlpha)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier.alpha(zenClockAlpha),
                text = when {
                    countdownDone -> "Time's up. Save, or keep going for 5 more minutes"
                    isRunning && goalReached -> "Page goal reached · keep going if you like"
                    isRunning && livePaceStr != null -> "Reading · $livePaceStr"
                    isRunning && bookSpeedStr != null -> "Reading · usually $bookSpeedStr"
                    isRunning -> "Reading…"
                    notStarted -> "Ready when you are"
                    pausedUnattended && pausedForSeconds != null -> "Paused while you were away · ${formatTimerClock(pausedForSeconds)}"
                    pausedForSeconds != null -> "Paused for ${formatTimerClock(pausedForSeconds)}"
                    else -> "Paused"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = onBackground.copy(alpha = 0.7f)
            )
            AnimatedVisibility(visible = sessionDelta > 0 && !isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = CircleShape,
                    color = onBackground.copy(alpha = 0.12f),
                    contentColor = onBackground,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        "+$sessionDelta $pageWord this session",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Before starting: where the last session ended and the last thing written in this book.
            val lastSession = remember(bookSessions) { bookSessions.maxByOrNull { it.endTime } }
            val lastNote = remember(bookNotes) { bookNotes.filter { it.content.isNotBlank() }.maxByOrNull { it.timestamp } }
            AnimatedVisibility(
                visible = notStarted && !isZenMode && (lastSession != null || lastNote != null),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PickUpCard(
                    lastSessionEnd = lastSession?.endTime,
                    currentPage = book.currentPage,
                    unit = FormatAdaptabilityLayer.unitAbbrev(book),
                    lastNote = lastNote,
                    tint = onBackground,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            }

            val bottomPart: @Composable ColumnScope.() -> Unit = {
            AnimatedVisibility(visible = !isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayButton(
                        isRunning = isRunning,
                        countdownDone = countdownDone,
                        // The ring fills towards an end: the countdown's, or the page goal's.
                        // Counting up with no page goal has no end, so no ring.
                        progress = when {
                            mode == TimerMode.COUNTDOWN && notStarted -> 0f
                            mode == TimerMode.COUNTDOWN -> (1f - timeLeftSeconds.toFloat() / countdownTotal).coerceIn(0f, 1f)
                            goalPages != null -> (sessionDelta.toFloat() / goalPages).coerceIn(0f, 1f)
                            else -> null
                        },
                        ringColor = onBackground,
                        foreground = onBackground,
                        background = background,
                        glow = if (isRunning) breath else 0f,
                        onClick = {
                            haptics.confirm()
                            if (countdownDone) finish() else viewModel.setTimerRunning(!isRunning)
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    // The countdown can only be changed before the clock starts;
                    // after that it's shown for reference.
                    val countdownLabel = when {
                        mode == TimerMode.STOPWATCH -> "Set countdown"
                        else -> "${countdownTotal / 60} min countdown"
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (countdownDone) {
                            // Time's up: keep going in the same session rather than starting over.
                            TimerChip(
                                icon = Icons.Rounded.MoreTime,
                                label = "+$EXTEND_MINUTES min",
                                tint = onBackground,
                                filled = true,
                                onClick = {
                                    haptics.confirm()
                                    viewModel.extendCountdown(EXTEND_MINUTES)
                                }
                            )
                        } else if (notStarted || mode == TimerMode.COUNTDOWN) {
                            TimerChip(
                                icon = if (mode == TimerMode.STOPWATCH) Icons.Rounded.Timer else Icons.Rounded.HourglassBottom,
                                label = countdownLabel,
                                tint = onBackground,
                                enabled = notStarted,
                                onClick = { showCountdownSheet = true }
                            )
                        }
                        // A page goal can be set or changed at any point in the session.
                        TimerChip(
                            icon = Icons.Rounded.Flag,
                            label = when {
                                pageTarget == null -> "Page goal"
                                goalReached -> "Goal reached"
                                else -> "$sessionDelta / $goalPages ${FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()}"
                            },
                            tint = onBackground,
                            filled = goalReached,
                            onClick = { showGoalSheet = true }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(visible = !isZenMode, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = AppShape.L,
                    color = onBackground.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, onBackground.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCover(book, Modifier.heroSharedElement("cover-${book.id}").size(width = 64.dp, height = 96.dp))
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
                            // Page stepper: the page is saved as you go. +/- for one page;
                            // tap the number to jump (or scan the page) after reading a stretch.
                            val unit = FormatAdaptabilityLayer.unitAbbrev(book)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        sessionCurrentPage -= 1
                                    },
                                    enabled = sessionCurrentPage > 0,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Page back", tint = onBackground, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    if (book.totalPages > 0) "$unit $sessionCurrentPage / ${book.totalPages}" else "$unit $sessionCurrentPage",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = onBackground,
                                    modifier = Modifier
                                        .clip(AppShape.XS)
                                        .clickable(onClickLabel = "Choose page") { showProgressPicker = true }
                                        .padding(horizontal = 6.dp, vertical = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        sessionCurrentPage = (sessionCurrentPage + 1).coerceAtMost(maxPage)
                                    },
                                    enabled = sessionCurrentPage < maxPage,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Page forward", tint = onBackground, modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                when {
                                    etaStr != null -> "About $etaStr in this book"
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
                            Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = "Add a note")
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

            if (hingeTop != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().height(hingeTop),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    topPart()
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(hingeHeight + 12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { bottomPart() }
            } else {
                topPart()
                Spacer(Modifier.height(36.dp))
                bottomPart()
            }
        }

        StillReadingCard(
            secondsLeft = askingSince?.let { STILL_READING_GRACE_SECONDS - (activeReadingSeconds - it) },
            onPause = { viewModel.setTimerRunning(false) },
            onStillReading = markTouched,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 20.dp, end = 20.dp)
        )
    }
    AnimatedVisibility(visible = showNotesPane && !isZenMode && hingeTop == null) {
        TimerNotesPane(
            notes = bookNotes,
            currentPage = sessionCurrentPage,
            pageLabel = FormatAdaptabilityLayer.unitAbbrev(book),
            onQuickNote = { page, text, type -> onAddNote(page, text, type, emptyList()) },
            onOpenNoteSheet = { showNoteSheet = true },
            modifier = Modifier.width(380.dp).fillMaxHeight()
        )
    }
    }

    if (showProgressPicker) {
        ProgressPickerSheet(
            book = book,
            initialPage = sessionCurrentPage,
            sessionStartPage = sessionStartPage,
            sessionSeconds = activeReadingSeconds,
            onDismiss = { showProgressPicker = false },
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sessionCurrentPage = it.coerceIn(0, maxPage)
                showProgressPicker = false
            }
        )
    }

    if (showCountdownSheet) {
        ActionSheet(
            onDismiss = { showCountdownSheet = false },
            icon = Icons.Rounded.HourglassBottom,
            title = "Countdown",
            message = "Read for a set time. You'll get an alert when it's up."
        ) {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 25, 30, 45, 60, 90).forEach { mins ->
                        TickFilterChip(
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
                    leadingContent = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                    trailingContent = {
                        if (mode == TimerMode.STOPWATCH) Icon(Icons.Rounded.Check, contentDescription = "Selected")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clip(AppShape.M)
                        .clickable {
                            viewModel.setTimerMode(TimerMode.STOPWATCH)
                            showCountdownSheet = false
                        }
                )
            }
        }
    }

    if (showGoalSheet) {
        PageGoalSheet(
            book = book,
            startPage = sessionStartPage,
            currentTarget = pageTarget,
            onDismiss = { showGoalSheet = false },
            onSet = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setPageTarget(it)
                showGoalSheet = false
            }
        )
    }

    if (showSoundscapeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSoundscapeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                            shape = AppShape.M,
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
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

/** An outlined pill under the play button; [filled] marks one that's done. */
@Composable
private fun TimerChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    filled: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (filled) tint.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = tint.copy(alpha = if (enabled) 0.9f else 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (filled) Icons.Rounded.Check else icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** How far this session means to get: a few steps from where it began, or a page typed in. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PageGoalSheet(
    book: Book,
    startPage: Int,
    currentTarget: Int?,
    onDismiss: () -> Unit,
    onSet: (Int?) -> Unit
) {
    val unit = FormatAdaptabilityLayer.unitAbbrev(book)
    val word = FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()
    val maxPage = if (book.totalPages > 0) book.totalPages else Int.MAX_VALUE
    var typed by remember { mutableStateOf(currentTarget?.toString().orEmpty()) }
    val typedPage = typed.toIntOrNull()?.takeIf { it in (startPage + 1)..maxPage }
    ActionSheet(
        onDismiss = onDismiss,
        icon = Icons.Rounded.Flag,
        title = "Page goal",
        message = "How far do you want to get? This session started on $unit $startPage.",
        confirmLabel = "Set goal",
        onConfirm = { onSet(typedPage) },
        confirmEnabled = typedPage != null
    ) {
        Column {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30, 50).filter { startPage + it <= maxPage }.forEach { step ->
                    TickFilterChip(
                        selected = currentTarget == startPage + step,
                        onClick = { onSet(startPage + step) },
                        label = { Text("+$step $word") }
                    )
                }
                if (book.totalPages > startPage) {
                    TickFilterChip(
                        selected = currentTarget == book.totalPages,
                        onClick = { onSet(book.totalPages) },
                        label = { Text("To the end") }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // A typed page is set with the sheet's main button.
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.filter(Char::isDigit).take(6) },
                label = { Text("Read to ${word.trimEnd('s')}") },
                singleLine = true,
                isError = typed.isNotEmpty() && typedPage == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (currentTarget != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onSet(null) }) { Text("No page goal") }
            }
        }
    }
}

/** Before the clock starts: when the book was last read and the last note in it. */
@Composable
private fun PickUpCard(
    lastSessionEnd: Long?,
    currentPage: Int,
    unit: String,
    lastNote: com.example.booktracker.shared.models.MarginNote?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = AppShape.L,
        color = tint.copy(alpha = 0.08f),
        contentColor = tint,
        modifier = modifier.widthIn(max = 420.dp).fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                "Pick up where you left off",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tint.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    lastSessionEnd?.let { append("Last read ${relativeDay(it)} · ") }
                    append("you're on $unit $currentPage")
                }.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (lastNote != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "“${lastNote.content.trim()}”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Your note on $unit ${lastNote.pageNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tint.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** "today", "yesterday", or a short date. */
private fun relativeDay(millis: Long): String {
    val zone = java.time.ZoneId.systemDefault()
    val day = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = java.time.LocalDate.now(zone)
    return when (day) {
        today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> day.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
}

/** "Still reading?" check-in; shown while [secondsLeft] (time until the clock pauses itself) is set. */
@Composable
private fun StillReadingCard(
    secondsLeft: Int?,
    onPause: () -> Unit,
    onStillReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = secondsLeft != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Surface(
            shape = AppShape.L,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Still reading?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "The clock pauses in ${formatTimerClock((secondsLeft ?: 0).coerceAtLeast(0))} unless you tap",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onPause) { Text("Pause") }
                Button(onClick = onStillReading) { Text("Yes") }
            }
        }
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

/** The big round play/pause button, ringed by the countdown's progress when there is one. */
@Composable
private fun PlayButton(
    isRunning: Boolean,
    countdownDone: Boolean,
    progress: Float?,
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
            if (progress == null) return@Canvas
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
                        countdownDone -> Icons.Rounded.Check
                        isRunning -> Icons.Rounded.Pause
                        else -> Icons.Rounded.PlayArrow
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
