package com.example.booktracker.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.ui.animations.ConfettiExplosion
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive Single Book "Finished Wrap-Up" Victory Card Dialog.
 * Features:
 * - Triumphant celebratory Confetti explosion on enter
 * - Interactive 3D Card Flip (Front: Trophy Cover & Stats; Back: Reading Journey Chronicles)
 * - 5 Luxury Themes and 1:1 Square vs 9:16 Story formats
 * - HD Canvas Graphic generator export for sharing
 */
@Composable
fun BookVictoryCardDialog(
    book: Book,
    sessions: List<Session>,
    notes: List<MarginNote> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var selectedTheme by remember { mutableStateOf(VictoryCardTheme.ROYAL_AMBER) }
    var selectedFormat by remember { mutableStateOf(QuoteCardFormat.SQUARE) }
    var isSharing by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(true) }

    // 3D Flip Animation Spec
    val cardRotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "victoryCardFlip"
    )

    val bookSessions = remember(book, sessions) { sessions.filter { it.bookId == book.id } }
    val timed = remember(bookSessions) { AnalyticsEngine.timedSessions(bookSessions) }
    val totalMinutes = remember(timed) { timed.sumOf { it.endTime - it.startTime } / 60_000L }
    val formattedTime = remember(totalMinutes) { if (totalMinutes < 1) "<1h" else AnalyticsEngine.formatMinutes(totalMinutes) }
    val velocity = remember(bookSessions) { AnalyticsEngine.pagesPerHour(bookSessions) }
    val formattedSpeed = remember(velocity) { velocity?.let { "${it.roundToInt()} p/h" } ?: "—" }
    val totalPagesRead = remember(book, bookSessions) {
        if (book.totalPages > 0) book.totalPages else bookSessions.sumOf { it.pagesRead }
    }
    val sessionCount = remember(bookSessions) { bookSessions.size.coerceAtLeast(1) }

    val topNote = remember(notes, book) {
        val bookNotes = notes.filter { it.bookId == book.id }
        bookNotes.firstOrNull { it.isFavorite } ?: bookNotes.firstOrNull()
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }
    val finishTimestamp = if (book.lastUpdated > 0L) book.lastUpdated else System.currentTimeMillis()
    val finishedDateStr = remember(finishTimestamp) {
        "Finished on ${Instant.ofEpochMilli(finishTimestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)}"
    }

    val startTimestamp = remember(bookSessions, book) {
        val earliestSession = bookSessions.minByOrNull { it.startTime }
        when {
            earliestSession != null && earliestSession.startTime > 0L -> earliestSession.startTime
            book.dateAdded > 0L -> book.dateAdded
            else -> finishTimestamp - (7 * 24 * 3600 * 1000L)
        }
    }
    val startedDateStr = remember(startTimestamp) {
        Instant.ofEpochMilli(startTimestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }
    val daysReading = remember(startTimestamp, finishTimestamp) {
        maxOf(1, ((finishTimestamp - startTimestamp) / (24 * 3600 * 1000L)).toInt())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Victory Wrap-Up",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Share your reading triumph",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Format Switcher (Square 1:1 vs Story 9:16)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        QuoteCardFormat.entries.forEachIndexed { index, format ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = QuoteCardFormat.entries.size),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFormat = format
                                },
                                selected = selectedFormat == format
                            ) {
                                Text(
                                    format.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedFormat == format) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Interactive 3D Flippable Live Preview Card
                    val previewBrush = Brush.linearGradient(
                        colors = selectedTheme.gradientColors.map { Color(it) }
                    )
                    val cardBgColor = Color(selectedTheme.cardColor)
                    val textColor = Color(selectedTheme.textColor)
                    val accentColor = Color(selectedTheme.accentColor)
                    val goldColor = Color(selectedTheme.goldColor)
                    val previewAspect = if (selectedFormat == QuoteCardFormat.SQUARE) 1f else 0.68f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(previewAspect)
                            .clip(RoundedCornerShape(24.dp))
                            .background(previewBrush)
                            .padding(if (selectedFormat == QuoteCardFormat.STORY) 12.dp else 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val isShowingBack = cardRotationY > 90f

                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = cardRotationY
                                    cameraDistance = 20f * density.density
                                }
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isFlipped = !isFlipped
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = cardBgColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            if (!isShowingBack) {
                                // Front of the Victory Card
                                VictoryCardFrontContent(
                                    book = book,
                                    selectedFormat = selectedFormat,
                                    topNote = topNote,
                                    finishedDateStr = finishedDateStr,
                                    formattedTime = formattedTime,
                                    formattedSpeed = formattedSpeed,
                                    totalPagesRead = totalPagesRead,
                                    textColor = textColor,
                                    accentColor = accentColor,
                                    goldColor = goldColor
                                )
                            } else {
                                // Back of the Victory Card (Reading Chronicles)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                ) {
                                    VictoryCardBackContent(
                                        book = book,
                                        selectedFormat = selectedFormat,
                                        startedDateStr = startedDateStr,
                                        finishedDateStr = finishedDateStr,
                                        daysReading = daysReading,
                                        sessionCount = sessionCount,
                                        formattedTime = formattedTime,
                                        formattedSpeed = formattedSpeed,
                                        totalPagesRead = totalPagesRead,
                                        noteCount = notes.filter { it.bookId == book.id }.size,
                                        textColor = textColor,
                                        accentColor = accentColor,
                                        goldColor = goldColor
                                    )
                                }
                            }
                        }

                        // Flip pill indicator badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isFlipped = !isFlipped
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Flip Card",
                                    tint = goldColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    if (isFlipped) "Front" else "Chronicles",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Theme Selection Swatches
                    Text(
                        "Card Theme (${VictoryCardTheme.entries.size} aesthetics)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VictoryCardTheme.entries.forEach { theme ->
                            val isSelected = selectedTheme == theme
                            val themeBrush = Brush.horizontalGradient(theme.gradientColors.map { Color(it) })

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .bounceClick {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTheme = theme
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(themeBrush)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    theme.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Action Button: Share Victory Card
                    Button(
                        onClick = {
                            if (!isSharing) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSharing = true
                                scope.launch {
                                    try {
                                        BookVictoryShareHelper.shareVictoryCard(
                                            context = context,
                                            book = book,
                                            sessions = sessions,
                                            topNote = topNote,
                                            theme = selectedTheme,
                                            format = selectedFormat
                                        )
                                    } finally {
                                        isSharing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Rendering HD Card...", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Share Victory Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Celebratory Confetti Rain on Opening
            if (showConfetti) {
                ConfettiExplosion(
                    modifier = Modifier.fillMaxSize(),
                    colors = listOf(
                        Color(0xFFFFD700), // Pure Gold
                        Color(0xFFF59E0B), // Vibrant Amber
                        Color(0xFF38BDF8), // Radiant Sky
                        Color(0xFF34D399), // Emerald
                        Color(0xFFF43F5E), // Rose
                        Color(0xFF818CF8)  // Indigo
                    ),
                    onFinished = { showConfetti = false }
                )
            }
        }
    }
}

@Composable
private fun VictoryCardFrontContent(
    book: Book,
    selectedFormat: QuoteCardFormat,
    topNote: MarginNote?,
    finishedDateStr: String,
    formattedTime: String,
    formattedSpeed: String,
    totalPagesRead: Int,
    textColor: Color,
    accentColor: Color,
    goldColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Banner
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "✦ READING VICTORY ✦",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Black
                ),
                color = goldColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                finishedDateStr,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = textColor.copy(alpha = 0.7f)
            )
        }

        // Book Centerpiece
        if (selectedFormat == QuoteCardFormat.SQUARE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCover(
                    url = book.coverUrl,
                    modifier = Modifier.size(width = 68.dp, height = 100.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.authors.isNotEmpty()) {
                        Text(
                            "by ${book.authors.first()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val avgRating = if (book.rating.isNotEmpty()) book.rating.values.average().toFloat() else 0f
                    if (avgRating > 0f) {
                        Spacer(Modifier.height(4.dp))
                        val stars = (avgRating).roundToInt().coerceIn(1, 5)
                        Text(
                            "${"★".repeat(stars)}${"☆".repeat(5 - stars)}  ${String.format(Locale.getDefault(), "%.1f", avgRating)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = goldColor
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BookCover(
                    url = book.coverUrl,
                    modifier = Modifier.size(width = 90.dp, height = 135.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.authors.isNotEmpty()) {
                    Text(
                        "by ${book.authors.first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                val avgRating = if (book.rating.isNotEmpty()) book.rating.values.average().toFloat() else 0f
                if (avgRating > 0f) {
                    Spacer(Modifier.height(4.dp))
                    val stars = (avgRating).roundToInt().coerceIn(1, 5)
                    Text(
                        "${"★".repeat(stars)}${"☆".repeat(5 - stars)}  ${String.format(Locale.getDefault(), "%.1f", avgRating)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = goldColor
                    )
                }
            }
        }

        // Stats Pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PreviewStatPill(Modifier.weight(1f), "TIME", formattedTime, textColor)
            PreviewStatPill(Modifier.weight(1f), "PACE", formattedSpeed, textColor)
            PreviewStatPill(Modifier.weight(1f), "PAGES", "$totalPagesRead p", textColor)
        }

        // Review or Top Note Snippet
        val snippetText = when {
            !book.review.isNullOrBlank() -> "“${book.review}”"
            topNote != null && topNote.content.isNotBlank() -> "“${topNote.content}”"
            else -> "“A reader lives a thousand lives before he dies.”"
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                snippetText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                ),
                color = textColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = if (selectedFormat == QuoteCardFormat.STORY) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Card Footer Watermark
        Text(
            "BookTracker • Tap to flip card",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = textColor.copy(alpha = 0.50f)
        )
    }
}

@Composable
private fun VictoryCardBackContent(
    book: Book,
    selectedFormat: QuoteCardFormat,
    startedDateStr: String,
    finishedDateStr: String,
    daysReading: Int,
    sessionCount: Int,
    formattedTime: String,
    formattedSpeed: String,
    totalPagesRead: Int,
    noteCount: Int,
    textColor: Color,
    accentColor: Color,
    goldColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "READING CHRONICLES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = goldColor
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "Completed in $daysReading days • $sessionCount sessions",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = textColor.copy(alpha = 0.75f)
            )
        }

        // Timeline Ribbon Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "STARTED",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    Text(
                        startedDateStr,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = textColor
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = goldColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "COMPLETED",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    Text(
                        finishedDateStr.replace("Finished on ", ""),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = textColor
                    )
                }
            }
        }

        // 4-Quadrant Journey Matrix
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                JourneyStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    label = "TOTAL TIME",
                    value = formattedTime,
                    textColor = textColor,
                    accentColor = accentColor
                )
                JourneyStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "AVERAGE PACE",
                    value = formattedSpeed,
                    textColor = textColor,
                    accentColor = accentColor
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                JourneyStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "VOLUME",
                    value = "$totalPagesRead pages",
                    textColor = textColor,
                    accentColor = accentColor
                )
                JourneyStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Verified,
                    label = "MARGIN NOTES",
                    value = "$noteCount saved",
                    textColor = textColor,
                    accentColor = accentColor
                )
            }
        }

        // Authenticated Reader Wax Seal Stamp
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = goldColor.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, goldColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .wrapContentSize()
                .graphicsLayer { rotationZ = -2.5f }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        "OFFICIAL READING TRIUMPH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        ),
                        color = goldColor
                    )
                    Text(
                        "Verified by Nocturnal Journal",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Tap to flip watermark
        Text(
            "Tap anywhere to flip back",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = textColor.copy(alpha = 0.50f)
        )
    }
}

@Composable
private fun JourneyStatTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    textColor: Color,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = textColor,
                    maxLines = 1
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = textColor.copy(alpha = 0.65f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PreviewStatPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = textColor,
                maxLines = 1
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = textColor.copy(alpha = 0.65f),
                maxLines = 1
            )
        }
    }
}
