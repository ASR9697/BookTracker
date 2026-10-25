package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.journal.NoiseType
import com.example.booktracker.shared.models.Book
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
 * Flagship Ambient Zen Reading Timer & Focus Companion.
 * Features reactive breathing cover aura, halo progress ring, live page stepper,
 * Pomodoro quick presets, procedural soundscape dock with frequency visualizer,
 * and distraction-free OLED Zen mode.
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
    val completedSessions by viewModel.completedSessions.collectAsState()

    val bookSessions = remember(book.id, completedSessions) {
        completedSessions.filter { it.bookId == book.id }
    }
    val speedPph = remember(bookSessions, completedSessions) {
        AnalyticsEngine.bookPagesPerHour(bookSessions, completedSessions)
    }
    val speedStr = remember(speedPph) {
        AnalyticsEngine.formatReadingSpeed(speedPph)
    }
    val minutesLeft = remember(book, bookSessions, completedSessions) {
        AnalyticsEngine.bookEstimatedMinutesLeft(book, bookSessions, completedSessions)
    }
    val etaStr = remember(minutesLeft) {
        AnalyticsEngine.formatEstimatedTime(minutesLeft)
    }

    // Live session progress
    var sessionCurrentPage by remember(book.currentPage) { mutableIntStateOf(book.currentPage) }
    var isZenMode by remember { mutableStateOf(false) }
    var showSoundscapeSheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val accent = rememberCoverAccent(book.coverUrl)
    val baseVibrant = accent.vibrant ?: MaterialTheme.colorScheme.primary
    val baseDominant = accent.dominant ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val background = accent.immersiveBackground(MaterialTheme.colorScheme.surfaceContainerLowest)
    val onBackground = if (background.luminance() > 0.45f) Color.Black else Color.White

    // Auto-start session immediately without modal friction
    LaunchedEffect(book.id, book.title) {
        viewModel.setTimerBook(book.id, book.title)
        if (openSession == null) {
            onStartSession(book, book.currentPage)
        }
    }

    LaunchedEffect(Unit) {
        com.example.booktracker.app.data.ServiceLocator.timerExpiredEvents.collect { onSaveSession() }
    }

    // Breathing light animation for active focus
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_focus_aura")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = if (isRunning) 0.94f else 1.0f,
        targetValue = if (isRunning) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = if (isRunning) 0.35f else 0.18f,
        targetValue = if (isRunning) 0.65f else 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    val displaySeconds = if (mode == TimerMode.STOPWATCH) elapsedSeconds else timeLeftSeconds

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
        // Ambient breathing radial glow behind center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(320.dp * auraScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            baseVibrant.copy(alpha = auraAlpha),
                            baseDominant.copy(alpha = auraAlpha * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            AnimatedVisibility(
                visible = !isZenMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(onBackground.copy(alpha = 0.12f), CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit timer", tint = onBackground)
                    }

                    // Mode segmented pill: Stopwatch vs Pomodoro
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = onBackground.copy(alpha = 0.12f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (mode == TimerMode.STOPWATCH) onBackground else Color.Transparent,
                                modifier = Modifier.clickable {
                                    viewModel.setTimerMode(TimerMode.STOPWATCH)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (mode == TimerMode.STOPWATCH) background else onBackground
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Stopwatch",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mode == TimerMode.STOPWATCH) background else onBackground
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (mode == TimerMode.COUNTDOWN) onBackground else Color.Transparent,
                                modifier = Modifier.clickable {
                                    viewModel.setTimerMode(TimerMode.COUNTDOWN)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🍅",
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Pomodoro",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mode == TimerMode.COUNTDOWN) background else onBackground
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showNoteSheet = true },
                            modifier = Modifier
                                .background(onBackground.copy(alpha = 0.12f), CircleShape)
                                .size(42.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add quick note", tint = onBackground)
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isZenMode = true
                            },
                            modifier = Modifier
                                .background(onBackground.copy(alpha = 0.12f), CircleShape)
                                .size(42.dp)
                        ) {
                            Icon(Icons.Filled.Bedtime, contentDescription = "Enter Zen mode", tint = onBackground)
                        }
                    }
                }
            }

            // Pomodoro Quick Presets Row
            AnimatedVisibility(
                visible = !isZenMode && mode == TimerMode.COUNTDOWN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val presets = listOf(15 to "15m", 25 to "25m Focus", 30 to "30m", 45 to "45m", 60 to "60m Deep")
                    presets.forEach { (mins, label) ->
                        val isCurrent = timeLeftSeconds == mins * 60 && !isRunning
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) onBackground else onBackground.copy(alpha = 0.12f),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setWorkMinutes(mins)
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) background else onBackground,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.4f))

            // Central Luminous Halo Ring & 3D Book Showcase
            Box(
                modifier = Modifier
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Luminous Halo Arc Canvas
                val haloProgress = when (mode) {
                    TimerMode.STOPWATCH -> ((elapsedSeconds % 1800) / 1800f).coerceIn(0f, 1f)
                    TimerMode.COUNTDOWN -> {
                        val totalSecs = (com.example.booktracker.app.data.ServiceLocator.timeLeftSeconds.value.coerceAtLeast(1))
                        (timeLeftSeconds.toFloat() / totalSecs).coerceIn(0f, 1f)
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                    // Track ring
                    drawArc(
                        color = onBackground.copy(alpha = 0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Animated progress halo
                    val sweep = if (mode == TimerMode.COUNTDOWN) (360f * haloProgress) else (360f * haloProgress.coerceAtLeast(0.04f))
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                baseVibrant,
                                baseVibrant.copy(alpha = 0.7f),
                                baseVibrant
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Centered Book Cover Card with Progress Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    shadowElevation = 16.dp,
                    modifier = Modifier.size(width = 114.dp, height = 168.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BookCover(
                            url = book.coverUrl,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )

                        // Bottom page tag pill
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "p. $sessionCurrentPage${if (book.totalPages > 0) " / ${book.totalPages}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Digital Clock Face
            Text(
                text = formatTimerClock(displaySeconds),
                fontSize = if (displaySeconds >= 3600) 48.sp else 58.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                color = onBackground
            )

            Spacer(Modifier.height(6.dp))

            // Status & Velocity Subtitle
            Text(
                text = when {
                    phase == TimerPhase.BREAK -> "☕ Rest your eyes & take a break"
                    isRunning && speedStr != null -> "📖 Deep Reading · ⚡ $speedStr"
                    isRunning -> "📖 Deep Reading…"
                    else -> "⏸ Session Paused"
                },
                style = MaterialTheme.typography.titleSmall,
                color = onBackground.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium
            )

            if (etaStr != null && isRunning) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⏱ ~$etaStr to finish book",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBackground.copy(alpha = 0.55f)
                )
            }

            Spacer(Modifier.height(18.dp))

            // In-Session Live Page Stepper Capsule
            AnimatedVisibility(
                visible = !isZenMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = onBackground.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, onBackground.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (sessionCurrentPage > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sessionCurrentPage -= 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Page back", tint = onBackground, modifier = Modifier.size(18.dp))
                        }

                        val sessionDelta = (sessionCurrentPage - book.currentPage).coerceAtLeast(0)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                "Page $sessionCurrentPage",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = onBackground
                            )
                            if (sessionDelta > 0) {
                                Text(
                                    "+$sessionDelta pages read",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = baseVibrant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sessionCurrentPage += 1
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Page forward", tint = onBackground, modifier = Modifier.size(18.dp))
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp))

                        // Quick +5 button
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = onBackground.copy(alpha = 0.15f),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sessionCurrentPage += 5
                            }
                        ) {
                            Text(
                                "+5",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = onBackground,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.6f))

            // Fluid Capsule Main Controls (Play/Pause & Finish)
            AnimatedVisibility(
                visible = !isZenMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause Capsule Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setTimerRunning(!isRunning)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(58.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) onBackground.copy(alpha = 0.18f) else onBackground,
                            contentColor = if (isRunning) onBackground else background
                        )
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Resume",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Pause" else "Resume",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Finish / Done Capsule Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setTimerRunning(false)
                            // Advance current page in book model before moving to session review
                            if (sessionCurrentPage != book.currentPage) {
                                viewModel.setProgress(book, sessionCurrentPage)
                            }
                            onSaveSession()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(58.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Done reading", modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Finish",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Ambient Soundscape Floating Bar (Equalizer Visualizer)
            AnimatedVisibility(
                visible = !isZenMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = onBackground.copy(alpha = 0.10f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSoundscapeSheet = true }
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val soundEmoji = when (noiseType) {
                                NoiseType.RAIN -> "🌧️"
                                NoiseType.FIRE -> "🔥"
                                NoiseType.OCEAN -> "🌊"
                                NoiseType.WHITE -> "⚪"
                                NoiseType.BROWN -> "🤎"
                                NoiseType.NONE -> "🎧"
                            }
                            Text(soundEmoji, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (noiseType == NoiseType.NONE) "Ambient Soundscapes" else "${noiseType.name.lowercase().replaceFirstChar { it.uppercase() }} Sound",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onBackground
                                )
                                Text(
                                    text = if (noiseType == NoiseType.NONE) "Tap to play relaxing background audio" else "Volume ${(volume * 100).toInt()}% · Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // 3-Bar animated equalizer simulation when active
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bar1 by infiniteTransition.animateFloat(
                                initialValue = 6f, targetValue = 18f,
                                animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                                label = "eq1"
                            )
                            val bar2 by infiniteTransition.animateFloat(
                                initialValue = 16f, targetValue = 8f,
                                animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                                label = "eq2"
                            )
                            val bar3 by infiniteTransition.animateFloat(
                                initialValue = 8f, targetValue = 20f,
                                animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                                label = "eq3"
                            )

                            if (noiseType != NoiseType.NONE && isRunning) {
                                Box(modifier = Modifier.width(3.dp).height(bar1.dp).background(baseVibrant, CircleShape))
                                Box(modifier = Modifier.width(3.dp).height(bar2.dp).background(baseVibrant, CircleShape))
                                Box(modifier = Modifier.width(3.dp).height(bar3.dp).background(baseVibrant, CircleShape))
                            } else {
                                Icon(Icons.Filled.Tune, contentDescription = "Soundscape settings", tint = onBackground.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Zen Mode exit helper indicator
            AnimatedVisibility(
                visible = isZenMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    "Double-tap screen to exit Zen Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }

    // Ambient Soundscape Modal Drawer
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
                Text("Ambient Soundscapes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Procedural relaxing audio to enhance concentration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))

                val sounds = listOf(
                    NoiseType.NONE to ("Off" to "Mute audio"),
                    NoiseType.RAIN to ("Rain on Window" to "Cozy gentle rain droplets"),
                    NoiseType.FIRE to ("Crackling Hearth" to "Warm cozy fireplace rumble"),
                    NoiseType.OCEAN to ("Ocean Waves" to "Rhythmic rolling sea swells"),
                    NoiseType.BROWN to ("Deep Brown Noise" to "Low-frequency steady warmth"),
                    NoiseType.WHITE to ("White Noise" to "Broadband sound isolation")
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

                // Sound Volume Slider
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

    // Quick Quote / Memo Sheet
    if (showNoteSheet) {
        InSessionNoteSheet(
            book = book,
            defaultPage = sessionCurrentPage,
            onDismiss = { showNoteSheet = false },
            onConfirm = { page, text ->
                onAddNote(page, text)
                showNoteSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InSessionNoteSheet(
    book: Book,
    defaultPage: Int,
    onDismiss: () -> Unit,
    onConfirm: (page: Int, text: String) -> Unit
) {
    var pageText by remember { mutableStateOf(defaultPage.toString()) }
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
            Text("Quick Thought or Quote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                label = { Text("What stood out in this passage?") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(page ?: defaultPage, noteText.trim()) },
                enabled = noteText.isNotBlank() && pageValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Save Memo", fontWeight = FontWeight.Bold)
            }
        }
    }
}
