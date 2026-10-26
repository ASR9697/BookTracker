package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 5 Reading Intensity Levels for Annual Matrix (GitHub & Bookmory standard).
 */
enum class ReadingIntensityLevel(
    val title: String,
    val rangeDescription: String,
    val minPages: Int
) {
    NEUTRAL("Neutral", "0 pages", 0),
    SOFT_TINT("Soft Tint", "1–14 pages", 1),
    RADIANT_PRIMARY("Radiant Primary", "15–39 pages", 15),
    HIGH_INTENSITY("High-Intensity", "40–79 pages", 40),
    FIERY_FLAME("Fiery Flame", "80+ pages", 80)
}

fun getReadingIntensity(pages: Int): ReadingIntensityLevel {
    return when {
        pages <= 0 -> ReadingIntensityLevel.NEUTRAL
        pages < 15 -> ReadingIntensityLevel.SOFT_TINT
        pages < 40 -> ReadingIntensityLevel.RADIANT_PRIMARY
        pages < 80 -> ReadingIntensityLevel.HIGH_INTENSITY
        else -> ReadingIntensityLevel.FIERY_FLAME
    }
}

@Composable
fun getIntensityColor(level: ReadingIntensityLevel): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (level) {
        ReadingIntensityLevel.NEUTRAL -> colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ReadingIntensityLevel.SOFT_TINT -> colorScheme.primary.copy(alpha = 0.35f)
        ReadingIntensityLevel.RADIANT_PRIMARY -> colorScheme.primary.copy(alpha = 0.70f)
        ReadingIntensityLevel.HIGH_INTENSITY -> colorScheme.primary
        ReadingIntensityLevel.FIERY_FLAME -> colorScheme.tertiary
    }
}

/**
 * 52-Week Annual Reading Heatmap ("Year in Pixels").
 * A GitHub-style horizontal matrix visualizing reading consistency and intensity across 365 days,
 * with comprehensive tap-to-inspect day sheet detailing sessions, completed books, and margin notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHeatmap(
    sessions: List<Session>,
    books: List<Book> = emptyList(),
    notes: List<MarginNote> = emptyList(),
    onOpenBook: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val haptic = LocalHapticFeedback.current

    // Aggregate pages by LocalDate
    val dailyPages = remember(sessions) {
        val map = mutableMapOf<LocalDate, Int>()
        for (session in sessions) {
            val date = Instant.ofEpochMilli(session.startTime).atZone(zone).toLocalDate()
            map[date] = (map[date] ?: 0) + session.pagesRead
        }
        map
    }

    val sessionsByDate = remember(sessions) {
        sessions.filter { it.startTime > 0 }
            .groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
    }

    val booksById = remember(books) { books.associateBy { it.id } }

    val finishedBooksByDate = remember(books) {
        books.filter { it.status == BookStatus.FINISHED.name && it.completedAt > 0 }
            .groupBy { Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }
    }

    val notesByDate = remember(notes) {
        notes.filter { it.timestamp > 0 }
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
    }

    // 52-week horizontal calendar geometry (aligned to Monday)
    val totalWeeks = 52
    val currentMonday = remember(today) { today.minusDays((today.dayOfWeek.value - 1).toLong()) }
    val startMonday = remember(currentMonday) { currentMonday.minusWeeks((totalWeeks - 1).toLong()) }

    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        // Auto-scroll to current week so reader sees latest activity immediately
        scrollState.scrollTo(scrollState.maxValue)
    }

    var inspectingDate by remember { mutableStateOf<LocalDate?>(null) }

    // Summary Headline Stats
    val activeDaysPastYear = remember(dailyPages, startMonday, today) {
        dailyPages.count { (date, pages) -> !date.isBefore(startMonday) && !date.isAfter(today) && pages > 0 }
    }
    val totalPagesPastYear = remember(dailyPages, startMonday, today) {
        dailyPages.filter { (date, _) -> !date.isBefore(startMonday) && !date.isAfter(today) }.values.sum()
    }
    val fieryDaysCount = remember(dailyPages, startMonday, today) {
        dailyPages.count { (date, pages) -> !date.isBefore(startMonday) && !date.isAfter(today) && pages >= 80 }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header with title and key stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Year in Pixels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$totalPagesPastYear pages across $activeDaysPastYear active days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (fieryDaysCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$fieryDaysCount Fiery Days",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 52-Week Horizontal Matrix Container
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Day of week labels on left (M, W, F)
            Column(
                modifier = Modifier
                    .padding(top = 18.dp, end = 6.dp)
                    .width(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dayLabels = listOf("M", "", "W", "", "F", "", "")
                dayLabels.forEach { label ->
                    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                        if (label.isNotEmpty()) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Horizontally Scrollable 52-Week Matrix with Month Headers
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.horizontalScroll(scrollState)
                ) {
                    // Month Label Row
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        var lastMonth = -1
                        for (col in 0 until totalWeeks) {
                            val weekStartDate = startMonday.plusWeeks(col.toLong())
                            val month = weekStartDate.monthValue
                            val showMonth = month != lastMonth
                            if (showMonth) lastMonth = month

                            Box(
                                modifier = Modifier.width(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (showMonth) {
                                    Text(
                                        weekStartDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // 7 Rows of Weeks
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (col in 0 until totalWeeks) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                for (row in 0 until 7) {
                                    val date = startMonday.plusWeeks(col.toLong()).plusDays(row.toLong())
                                    val isFuture = date.isAfter(today)
                                    val pages = if (isFuture) 0 else (dailyPages[date] ?: 0)
                                    val intensity = if (isFuture) null else getReadingIntensity(pages)
                                    val cellColor = if (isFuture) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    } else {
                                        getIntensityColor(intensity!!)
                                    }

                                    val isSelected = inspectingDate == date

                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(cellColor)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(2.5.dp))
                                                } else Modifier
                                            )
                                            .then(
                                                if (!isFuture) {
                                                    Modifier.clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        inspectingDate = date
                                                    }
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Legend at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Less",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            ReadingIntensityLevel.entries.forEach { level ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(getIntensityColor(level))
                )
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(3.dp))
            Text(
                "More",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Modal Sheet for Inspecting Selected Date
    inspectingDate?.let { date ->
        val dateSessions = sessionsByDate[date] ?: emptyList()
        val finishedBooks = finishedBooksByDate[date] ?: emptyList()
        val dateNotes = notesByDate[date] ?: emptyList()
        val pagesOnDate = dailyPages[date] ?: 0
        val intensity = getReadingIntensity(pagesOnDate)

        ModalBottomSheet(
            onDismissRequest = { inspectingDate = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Date Header & Intensity Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$pagesOnDate pages read",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = getIntensityColor(intensity).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, getIntensityColor(intensity))
                    ) {
                        Text(
                            "${intensity.title} (${intensity.rangeDescription})",
                            style = MaterialTheme.typography.labelSmall,
                            color = getIntensityColor(intensity),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Finished Books on this day
                if (finishedBooks.isNotEmpty()) {
                    Text(
                        "Finished Books 🏆",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                    Spacer(Modifier.height(8.dp))
                    finishedBooks.forEach { book ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .bounceClick { onOpenBook?.invoke(book) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        book.authors.firstOrNull() ?: "Unknown Author",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Reading Sessions on this day
                Text(
                    "Logged Reading Sessions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                if (dateSessions.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            "No reading sessions logged on this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    dateSessions.forEach { session ->
                        val book = booksById[session.bookId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .then(
                                    if (book != null && onOpenBook != null) Modifier.bounceClick { onOpenBook(book) }
                                    else Modifier
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        book?.title ?: "Reading Session",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "+${session.pagesRead} pages",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (session.durationSeconds > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Timer,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "${session.durationSeconds / 60} mins",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    session.environmentTag?.let { tag ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Tag,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                tag,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Margin Notes recorded on this day
                if (dateNotes.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Captured Margin Notes (${dateNotes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    dateNotes.forEach { note ->
                        val noteBook = booksById[note.bookId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (note.content.isNotBlank()) {
                                    Text(
                                        "“${note.content}”",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        noteBook?.title ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (note.pageNumber > 0) {
                                        Text(
                                            "Page ${note.pageNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
