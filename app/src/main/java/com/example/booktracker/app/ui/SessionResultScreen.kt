package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.app.analytics.BadgeStatus
import com.example.booktracker.app.analytics.FinishByPlanner
import com.example.booktracker.app.analytics.RereadMemories
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val resultDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")

/**
 * Post-save recap. Leads with what the session was (time, pages, pace), then what
 * it means for the book (how much is left and how long that takes), then what it
 * counted towards (today's goal, the streak, a deadline, the next badge).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionResultScreen(
    book: Book,
    result: BookTrackerViewModel.SessionResult,
    allSessions: List<Session>,
    onDone: () -> Unit,
    notes: List<MarginNote> = emptyList(),
    streak: StreakEngine.StreakInfo? = null,
    badges: List<BadgeStatus> = emptyList(),
    onAddNote: (page: Int, text: String, type: NoteType, images: List<String>) -> Unit = { _, _, _, _ -> }
) {
    val zone = remember { ZoneId.systemDefault() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unit = FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()
    val pageUnit = FormatAdaptabilityLayer.unitAbbrev(book)
    var showNoteSheet by remember { mutableStateOf(false) }

    // From the saved result, not the book row: the row can still be a frame behind the save.
    val endPage = result.endPage
    val pagesLeft = (book.totalPages - endPage).coerceAtLeast(0)
    val finished = book.totalPages > 0 && pagesLeft == 0
    val bookSessions = remember(book, allSessions) { allSessions.filter { it.bookId == book.id } }
    val minutesLeft = remember(book, endPage, bookSessions, allSessions) {
        AnalyticsEngine.bookEstimatedMinutesLeft(book.copy(currentPage = endPage), bookSessions, allSessions)
    }
    val sessionPages = (result.endPage - result.startPage).coerceAtLeast(0)
    val sessionPpm = remember(sessionPages, result.durationSeconds) {
        AnalyticsEngine.sessionSpeedPpm(sessionPages, result.durationSeconds)
    }
    // This book's pace without the session just saved, so it isn't compared with itself.
    val usualPph = remember(bookSessions, result.sessionId) {
        AnalyticsEngine.pagesPerHour(bookSessions.filter { it.id != result.sessionId })
    }
    val paceComparison = paceComparison(sessionPpm, usualPph, result.durationSeconds, sessionPages)

    val progress = if (book.totalPages > 0) endPage.toFloat() / book.totalPages else 0f
    val startProgress = if (book.totalPages > 0) result.startPage.toFloat() / book.totalPages else null

    val goalJustMet = streak != null && streak.goalReached &&
        streak.pagesToday - sessionPages < streak.dailyGoal
    val headline = when {
        finished -> "You finished it!"
        goalJustMet -> "Daily goal reached"
        sessionPages > 0 -> "Nice session"
        else -> "Session saved"
    }

    val shareLayer = rememberGraphicsLayer()
    // Once per result: a finished book gets the victory card's celebration instead.
    // Keyed on goalJustMet because the streak can catch up with the save a moment late.
    var celebrate by remember { mutableStateOf(false) }
    var celebrated by androidx.compose.runtime.saveable.rememberSaveable(result.savedAt) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(goalJustMet, finished) {
        if (goalJustMet && !finished && !celebrated) {
            celebrated = true
            celebrate = true
        }
    }

    val accent = rememberCoverAccent(book.coverUrl)
    val wide = LocalWidthTier.current.atLeastExpanded
    val contentWidth = if (wide) WideRecapWidth else ReadableWidth
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .coverGlow(accent, strength = 0.32f)
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Done")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .readableWidth(contentWidth)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showNoteSheet = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add a note")
                    }
                    OutlinedButton(
                        onClick = { scope.launch { shareLayerImage(context, shareLayer, "session.png", "Share this session") } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onDone,
                    shape = AppShape.M,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        // The session itself: the shareable card.
        val recap: @Composable ColumnScope.() -> Unit = {
            // The shareable part: recorded into a layer so Share sends exactly this card.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        shareLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(shareLayer)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.XL)
                    .padding(20.dp)
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(16.dp))
                ProgressRingCover(
                    coverUrl = book.coverUrl,
                    progress = progress,
                    size = 200.dp,
                    ringWidth = 14.dp,
                    startProgress = startProgress,
                    highlightFrom = startProgress,
                    ringColor = com.example.booktracker.app.ui.theme.ProgressAmber
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    buildString {
                        append(if (sessionPages > 0) "$pageUnit ${result.startPage} → $endPage" else "$pageUnit $endPage")
                        if (book.totalPages > 0) append(" · ${(progress * 100).toInt()}% through")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                val shownPages by animateCountUp(sessionPages, durationMillis = 1100)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(formatSessionTime(result.durationSeconds), "read")
                    StatTile(if (sessionPages > 0) "+$shownPages" else "0", if (sessionPages == 1) unit.trimEnd('s') else unit)
                    StatTile(
                        sessionPpm?.let { String.format(Locale.US, "%.1f", it) } ?: "–",
                        if (sessionPpm != null) "$unit/min" else "pace"
                    )
                }

                if (paceComparison != null || result.focus > 0) {
                    Spacer(Modifier.height(14.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        paceComparison?.let { Pill(Icons.Rounded.Speed, it) }
                        if (result.focus > 0) Pill(Icons.Rounded.CenterFocusStrong, "Focus: ${focusLabel(result.focus)}")
                    }
                }
            }
        }
        // What's left of the book after it.
        val bookLeft: @Composable ColumnScope.() -> Unit = {
            if (finished) {
                StatSentence { highlight("You finished this book.") }
            } else if (book.totalPages > 0 || minutesLeft != null) {
                StatSentence {
                    if (book.totalPages > 0) {
                        highlight("$pagesLeft $unit")
                        append(" left")
                    }
                    if (minutesLeft != null) {
                        append(if (book.totalPages > 0) ", about " else "About ")
                        highlight(AnalyticsEngine.formatMinutes(minutesLeft))
                        append(" at your pace")
                    }
                    append(".")
                }
            }
        }
        // What it counted towards, and on a re-read what you wrote last time.
        val countedTowards: @Composable ColumnScope.() -> Unit = {
            // What this session counted towards.
            val finishBy = remember(book, endPage, bookSessions, streak) {
                streak?.let {
                    finishByLine(FinishByPlanner.plan(book.copy(currentPage = endPage), bookSessions, it.today, zone, it.dayStartsAtHour))
                }
            }
            val nextBadge = remember(badges) {
                badges
                    .filter { !it.unlocked && it.badge.tier != BadgeEngine.MONTHLY_TIER && it.current > 0 }
                    .maxByOrNull { it.fraction }
            }
            if (streak != null || finishBy != null || nextBadge != null) {
                if (!wide) Spacer(Modifier.height(24.dp))
                Text(
                    "Counted towards",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.L)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (streak != null && streak.dailyGoal > 0) {
                        GoalRow(
                            icon = Icons.Rounded.TrackChanges,
                            title = if (streak.goalReached) "Daily goal met" else "Daily goal",
                            detail = "${streak.pagesToday} / ${streak.dailyGoal} $unit today",
                            fraction = streak.pagesToday.toFloat() / streak.dailyGoal
                        )
                    }
                    if (streak != null && streak.currentStreak > 0) {
                        GoalRow(
                            icon = Icons.Rounded.LocalFireDepartment,
                            title = "${streak.currentStreak}-day streak",
                            detail = if (streak.currentStreak >= streak.longestStreak && streak.currentStreak > 1) "Your longest yet"
                            else "Best: ${streak.longestStreak} days"
                        )
                    }
                    finishBy?.let { (line, good) ->
                        GoalRow(
                            icon = Icons.Rounded.EventAvailable,
                            title = "Finish-by plan",
                            detail = line,
                            detailColor = if (good) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                    nextBadge?.let {
                        GoalRow(
                            icon = it.badge.icon,
                            title = "Next badge: ${it.badge.title}",
                            detail = it.progressLabel,
                            fraction = it.fraction
                        )
                    }
                }
            }

            // On a re-read: what you wrote last time on the pages just read.
            val readStart = remember(book, allSessions) { RereadMemories.currentReadStart(book, allSessions) }
            val reached = remember(notes, readStart, result) {
                readStart?.let { RereadMemories.reached(notes, it, result.startPage, result.endPage) }.orEmpty()
            }
            if (reached.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                MemoriesReachedCard(reached)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .readableWidth(contentWidth)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(headline, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                Instant.ofEpochMilli(result.savedAt).atZone(zone).format(resultDateFormat),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) { recap() }
                    Column(Modifier.weight(1f)) {
                        bookLeft()
                        Spacer(Modifier.height(20.dp))
                        countedTowards()
                    }
                }
            } else {
                recap()
                Spacer(Modifier.height(12.dp))
                bookLeft()
                countedTowards()
            }
        }
    }
    if (celebrate) {
        com.example.booktracker.app.ui.animations.ConfettiExplosion(
            modifier = Modifier.fillMaxSize(),
            origin = androidx.compose.ui.geometry.Offset(0.5f, 0.35f),
            onFinished = { celebrate = false }
        )
    }
    }

    if (showNoteSheet) {
        NoteSheet(
            suggestedPage = endPage,
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

/** "8s" under a minute, "4m 30s" under ten (so it matches the save screen), otherwise "25m" / "1h 5m". */
private fun formatSessionTime(seconds: Int): String = when {
    seconds < 60 -> "${seconds.coerceAtLeast(0)}s"
    seconds < 600 && seconds % 60 != 0 -> "${seconds / 60}m ${seconds % 60}s"
    else -> AnalyticsEngine.formatMinutes(seconds / 60L)
}

/** Shortest sitting, and fewest pages, a pace comparison means anything for. */
private const val PACE_MIN_SECONDS = 5 * 60
private const val PACE_MIN_PAGES = 3

/** On a wide window the recap and what it counted towards sit side by side, no scrolling. */
private val WideRecapWidth = 1120.dp

/**
 * This session's pace against the book's usual; null until both are known, and for
 * sessions too short to say anything (a two-minute sitting logged as +20 pages read
 * as "1602% faster"). Beyond double or half it says so in words, not a percentage.
 */
private fun paceComparison(sessionPpm: Float?, usualPph: Float?, seconds: Int, pages: Int): String? {
    if (sessionPpm == null || usualPph == null || usualPph <= 0f) return null
    if (seconds < PACE_MIN_SECONDS || pages < PACE_MIN_PAGES) return null
    val ratio = sessionPpm * 60f / usualPph
    val diff = (ratio - 1f) * 100f
    return when {
        ratio >= 2f -> "More than twice your usual pace in this book"
        ratio <= 0.5f -> "Under half your usual pace in this book"
        diff >= 5f -> "${diff.roundToInt()}% faster than your usual in this book"
        diff <= -5f -> "${(-diff).roundToInt()}% slower than your usual in this book"
        else -> "Right on your usual pace for this book"
    }
}

@Composable
private fun RowScope.StatTile(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, AppShape.M)
            .padding(vertical = 14.dp, horizontal = 6.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun Pill(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun GoalRow(
    icon: ImageVector,
    title: String,
    detail: String,
    fraction: Float? = null,
    detailColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = detailColor)
            if (fraction != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

/**
 * Builder scope for the mixed-weight recap sentences — plain body text with the
 * number itself pulled out in the accent colour.
 */
class StatSentenceScope internal constructor() {
    internal val parts = mutableListOf<Pair<String, Boolean>>()
    fun append(text: String) { parts += text to false }
    fun highlight(text: String) { parts += text to true }
}

@Composable
private fun StatSentence(content: StatSentenceScope.() -> Unit) {
    // Built every composition: a lambda that reads animated state is the same
    // instance each time, so remembering on it froze count-ups at their first frame.
    val scope = StatSentenceScope().apply(content)
    val accent = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.L)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            buildAnnotatedString {
                scope.parts.forEach { (text, isHighlight) ->
                    if (isHighlight) {
                        withStyle(
                            SpanStyle(
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize
                            )
                        ) { append(text) }
                    } else {
                        withStyle(SpanStyle(color = base)) { append(text) }
                    }
                }
            },
            style = MaterialTheme.typography.titleMedium
        )
    }
}
