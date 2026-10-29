package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import com.example.booktracker.app.ui.animations.bounceClick
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import com.example.booktracker.shared.models.BookStatus

enum class CalendarViewMode {
    PAGES,
    COVERS,
    YEAR_HEATMAP
}

private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dayDetailFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

/**
 * Bookmory-style Interactive Monthly Reading Calendar.
 * Shows days read with reading stamps/page counts or book covers mosaic,
 * month statistics, finished book badges (🏆), and detailed daily reading logs when a date is selected.
 */
@Composable
fun ReadingCalendarView(
    books: List<Book>,
    sessions: List<Session>,
    notes: List<MarginNote> = emptyList(),
    monthlyGoalPages: Int = 500,
    userName: String = "Reader",
    onOpenBook: (Book) -> Unit,
    onStartTimer: (Book) -> Unit,
    onEditGoal: () -> Unit = {},
    dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(dayStartsAtHour) { StreakEngine.readingDate(System.currentTimeMillis(), zone, dayStartsAtHour) }
    val haptic = LocalHapticFeedback.current
    var selectedMonth by remember { mutableStateOf(YearMonth.now(zone)) }
    var selectedDate by remember { mutableStateOf(today) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.PAGES) }
    var showMonthlyShareDialog by remember { mutableStateOf(false) }

    val booksById = remember(books) { books.associateBy { it.id } }

    // Map sessions by LocalDate
    val sessionsByDate = remember(sessions, dayStartsAtHour) {
        sessions.filter { it.endTime > 0 }
            .groupBy { StreakEngine.sessionDay(it, zone, dayStartsAtHour) }
    }

    // Map notes by LocalDate
    val notesByDate = remember(notes) {
        notes.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
    }

    // Map of dates when a book was finished
    val finishedDates = remember(books, sessions) {
        val dates = mutableSetOf<LocalDate>()
        books.filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) && it.completedAt > 0 }
            .forEach { book ->
                val date = Instant.ofEpochMilli(book.completedAt).atZone(zone).toLocalDate()
                dates.add(date)
            }
        sessions.filter { it.endTime > 0 }.forEach { session ->
            val b = booksById[session.bookId]
            if (b != null && b.totalPages > 0 && session.endPage >= b.totalPages) {
                val date = Instant.ofEpochMilli(session.endTime).atZone(zone).toLocalDate()
                dates.add(date)
            }
        }
        dates
    }

    // Monthly summary stats
    val monthSessions = remember(sessions, selectedMonth, dayStartsAtHour) {
        sessions.filter {
            it.endTime > 0 && YearMonth.from(StreakEngine.sessionDay(it, zone, dayStartsAtHour)) == selectedMonth
        }
    }
    val monthTotalPages = remember(monthSessions) { monthSessions.sumOf { it.pagesRead } }
    val monthTotalMinutes = remember(monthSessions) { monthSessions.sumOf { it.durationSeconds } / 60 }
    val monthActiveDays = remember(monthSessions) {
        monthSessions.filter(StreakEngine::isReading)
            .map { StreakEngine.sessionDay(it, zone, dayStartsAtHour) }.distinct().size
    }
    val monthFinishedBooks = remember(finishedDates, selectedMonth) {
        finishedDates.count { YearMonth.from(it) == selectedMonth }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Month Selector Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { selectedMonth = selectedMonth.minusMonths(1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                        }

                        TextButton(onClick = {
                            selectedMonth = YearMonth.now(zone)
                            selectedDate = today
                        }) {
                            Text(
                                text = selectedMonth.format(monthYearFormatter),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                        }
                    }

                    IconButton(
                        onClick = { showMonthlyShareDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share Monthly Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Month Stats Strip (Bookmory summary with finished count)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonthStatItem(
                        value = "$monthActiveDays",
                        unit = "days read",
                        icon = Icons.Filled.CalendarMonth,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    VerticalDivider(modifier = Modifier.height(24.dp))
                    MonthStatItem(
                        value = "$monthTotalPages",
                        unit = "pages",
                        icon = Icons.Filled.AutoStories,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    if (monthFinishedBooks > 0) {
                        VerticalDivider(modifier = Modifier.height(24.dp))
                        MonthStatItem(
                            value = "$monthFinishedBooks",
                            unit = "finished",
                            icon = Icons.Filled.EmojiEvents,
                            tint = Color(0xFFFFB300)
                        )
                    }
                    VerticalDivider(modifier = Modifier.height(24.dp))
                    MonthStatItem(
                        value = "${monthTotalMinutes / 60}h ${monthTotalMinutes % 60}m",
                        unit = "time",
                        icon = Icons.Filled.Timer,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Monthly Goal Progress Bar
                Spacer(Modifier.height(10.dp))
                val goalProgress = (monthTotalPages.toFloat() / monthlyGoalPages.coerceAtLeast(1)).coerceIn(0f, 1f)
                val animatedGoalProgress by animateFloatAsState(
                    targetValue = goalProgress,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "calendarGoalProgress"
                )
                val goalPct = (goalProgress * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditGoal)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "$monthTotalPages / $monthlyGoalPages pages ($goalPct%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (monthTotalPages >= monthlyGoalPages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { animatedGoalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Calendar Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Calendar mode toggle (Pages vs Covers)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Monthly View",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            val modes = listOf(
                                CalendarViewMode.PAGES to "Pages",
                                CalendarViewMode.COVERS to "Covers",
                                CalendarViewMode.YEAR_HEATMAP to "Year"
                            )
                            modes.forEach { (mode, label) ->
                                val isSelected = viewMode == mode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.bounceClick {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewMode = mode
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (mode) {
                                                CalendarViewMode.PAGES -> Icons.Filled.AutoStories
                                                CalendarViewMode.COVERS -> Icons.Filled.PhotoLibrary
                                                CalendarViewMode.YEAR_HEATMAP -> Icons.Filled.CalendarMonth
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewMode == CalendarViewMode.YEAR_HEATMAP) {
                    ReadingHeatmap(
                        sessions = sessions,
                        books = books,
                        notes = notes,
                        onOpenBook = onOpenBook,
                        dayStartsAtHour = dayStartsAtHour,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                } else {

                Spacer(Modifier.height(4.dp))

                // Days of week row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val weekStart = LocalAppPrefs.current.weekStart
                    weekDays(weekStart).forEach { dow ->
                        val day = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                        val isWeekend = dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isWeekend) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState = Pair(selectedMonth, viewMode),
                    transitionSpec = {
                        if (targetState.first > initialState.first) {
                            slideInHorizontally { width -> width } + fadeIn(tween(250)) togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut(tween(200))
                        } else if (targetState.first < initialState.first) {
                            slideInHorizontally { width -> -width } + fadeIn(tween(250)) togetherWith
                                slideOutHorizontally { width -> width } + fadeOut(tween(200))
                        } else {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        }
                    },
                    label = "calendar_month_animation"
                ) { (month, currentViewMode) ->
                    val firstDayOfMonth = month.atDay(1)
                    val daysInMonth = month.lengthOfMonth()
                    val firstDayOffset = leadingDays(firstDayOfMonth, LocalAppPrefs.current.weekStart)

                    val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until totalCells / 7) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - firstDayOffset + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val date = month.atDay(dayNum)
                                        val daySessions = sessionsByDate[date].orEmpty()
                                        val dayPages = daySessions.sumOf { it.pagesRead }
                                        val hasNotes = notesByDate[date]?.isNotEmpty() == true
                                        val isSelected = date == selectedDate
                                        val isCurrentDay = date == today
                                        val hasFinishedBook = finishedDates.contains(date)

                                        val dayBooks = daySessions.mapNotNull { booksById[it.bookId] }.distinctBy { it.id }
                                        val primaryBook = dayBooks.firstOrNull()

                                        CalendarDayCell(
                                            dayNumber = dayNum,
                                            pagesRead = dayPages,
                                            hasNotes = hasNotes,
                                            isSelected = isSelected,
                                            isToday = isCurrentDay,
                                            viewMode = currentViewMode,
                                            coverBook = primaryBook,
                                            hasFinished = hasFinishedBook,
                                            extraBooksCount = (dayBooks.size - 1).coerceAtLeast(0),
                                            onClick = { selectedDate = date }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(width = 44.dp, height = if (currentViewMode == CalendarViewMode.COVERS) 58.dp else 50.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(16.dp))

        // Selected Date Reading Log Sheet / List
        val selectedDaySessions = remember(selectedDate, sessionsByDate) {
            sessionsByDate[selectedDate].orEmpty().sortedByDescending { it.endTime }
        }
        val selectedDayNotes = remember(selectedDate, notesByDate) {
            notesByDate[selectedDate].orEmpty()
        }

        if (viewMode == CalendarViewMode.YEAR_HEATMAP) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Tap any pixel in the 52-week matrix to inspect that day's reading sessions, finished books, and quotes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = selectedDate.format(dayDetailFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

        if (selectedDaySessions.isEmpty() && selectedDayNotes.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No reading logged on this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(selectedDaySessions, key = { it.id }) { session ->
                    val book = booksById[session.bookId]
                    SessionHistoryCard(
                        session = session,
                        book = book,
                        onOpenBook = { book?.let(onOpenBook) },
                        onStartTimer = { book?.let(onStartTimer) },
                        modifier = Modifier.animateItem()
                    )
                }

                if (selectedDayNotes.isNotEmpty()) {
                    item {
                        Text(
                            "Notes & Quotes (${selectedDayNotes.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 4.dp)
                                .animateItem()
                        )
                    }
                    items(selectedDayNotes, key = { it.id }) { note ->
                        val book = booksById[note.bookId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = book?.title ?: "Note",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (note.pageNumber > 0) {
                                        Text(
                                            text = "Page ${note.pageNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "“${note.content}”",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showMonthlyShareDialog) {
        MonthlyReportDialog(
            yearMonth = selectedMonth,
            books = books,
            sessions = sessions,
            userName = userName,
            onDismiss = { showMonthlyShareDialog = false }
        )
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    pagesRead: Int,
    hasNotes: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    viewMode: CalendarViewMode = CalendarViewMode.PAGES,
    coverBook: Book? = null,
    hasFinished: Boolean = false,
    extraBooksCount: Int = 0,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val cellScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cellScale"
    )

    if (viewMode == CalendarViewMode.COVERS && coverBook != null) {
        // Bookmory Cover Thumbnail Cell
        val cellShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 58.dp)
                .graphicsLayer {
                    scaleX = cellScale
                    scaleY = cellScale
                }
                .clip(cellShape)
                .then(
                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, cellShape)
                    else if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, cellShape)
                    else Modifier
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
        ) {
            // Book cover image
            BookCover(
                url = coverBook.coverUrl,
                modifier = Modifier.fillMaxSize()
            )

            // Day number pill overlay (top-left)
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(bottomEnd = 6.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "$dayNumber",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            // Finished book trophy badge (top-right)
            if (hasFinished) {
                Text(
                    text = "🏆",
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 2.dp, top = 1.dp)
                )
            }

            // Extra books stack badge (bottom-right)
            if (extraBooksCount > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(topStart = 6.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "+$extraBooksCount",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }
    } else {
        // Bookmory Pages Mode Cell
        val targetContainerColor = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isToday -> MaterialTheme.colorScheme.surfaceContainerHighest
            pagesRead > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else -> Color.Transparent
        }
        val containerColor by animateColorAsState(
            targetValue = targetContainerColor,
            animationSpec = tween(150),
            label = "dayBg"
        )

        val contentColor = when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }

        val cellShape = RoundedCornerShape(14.dp)

        Box(
            modifier = Modifier
                .size(width = 44.dp, height = if (viewMode == CalendarViewMode.COVERS) 58.dp else 50.dp)
                .graphicsLayer {
                    scaleX = cellScale
                    scaleY = cellScale
                }
                .clip(cellShape)
                .background(containerColor)
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, cellShape)
                    else if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, cellShape)
                    else Modifier
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$dayNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday || isSelected || pagesRead > 0) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    )
                    if (hasFinished) {
                        Text(
                            text = "🏆",
                            fontSize = 9.sp,
                            modifier = Modifier.padding(start = 1.dp)
                        )
                    }
                }

                if (pagesRead > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "${pagesRead}p",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else if (hasNotes) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthStatItem(
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionHistoryCard(
    session: Session,
    book: Book?,
    onOpenBook: () -> Unit,
    onStartTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenBook()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                url = book?.coverUrl.orEmpty(),
                modifier = Modifier.size(width = 48.dp, height = 70.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book?.title ?: "Unknown Book",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book?.authors?.isNotEmpty() == true) {
                    Text(
                        text = book.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "+${session.pagesRead} pages",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (session.durationSeconds > 0) {
                        Text(
                            text = "${session.durationSeconds / 60}m read",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (session.environmentTag.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = session.environmentTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onStartTimer) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Read again",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
