package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import com.example.booktracker.app.analytics.GoalEngine
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.ReadingCalendar
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Books with genre info needed before the genre split is worth showing. */
private const val MIN_BOOKS_FOR_GENRES = 3

@Composable
fun AnalyticsScreen(
    books: List<Book>,
    sessions: List<Session>,
    dailyGoal: Int,
    yearlyGoal: Int,
    monthlyGoalPages: Int = 500,
    monthlyGoalBooks: Int = 2,
    dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR,
    onEditGoal: (GoalType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(dayStartsAtHour) { StreakEngine.readingDate(System.currentTimeMillis(), zone, dayStartsAtHour) }
    val pagesByDay = remember(sessions, dayStartsAtHour) { ReadingCalendar.pagesByDay(sessions, zone, dayStartsAtHour) }
    val minutesByDay = remember(sessions, dayStartsAtHour) { AnalyticsEngine.minutesByDay(sessions, zone, dayStartsAtHour) }
    val timeSummary = remember(sessions) { AnalyticsEngine.summarizeTime(sessions) }
    val velocity = remember(sessions) { AnalyticsEngine.pagesPerHour(sessions) }
    val bestTime = remember(sessions) { AnalyticsEngine.bestTimeOfDay(sessions) }
    val environments = remember(sessions) { AnalyticsEngine.environmentStats(sessions) }
    val focus = remember(sessions) { com.example.booktracker.app.analytics.FocusInsights.summarize(sessions) }

    var showMinutes by remember { mutableStateOf(false) }

    val last7Days = remember(today, pagesByDay, minutesByDay, showMinutes) {
        (0..6).map { i ->
            val date = today.minusDays((6 - i).toLong())
            val value =
                if (showMinutes) (minutesByDay[date] ?: 0L)
                else (pagesByDay[date] ?: 0).toLong()
            date to value
        }
    }
    val maxIn7Days = last7Days.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

    val finishedBooks = books
        .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
        .sortedByDescending { it.completedAt }

    val thisYear = remember(today) { today.year }
    val finishedThisYear = remember(finishedBooks, thisYear) {
        finishedBooks.count {
            Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate().year == thisYear
        }
    }
    val trackingStart = remember(books, sessions) { GoalEngine.trackingStart(books, sessions, zone) }
    val yearlyPace = remember(yearlyGoal, finishedThisYear, today, trackingStart) {
        GoalEngine.calculateYearlyPace(yearlyGoal, finishedThisYear, today, trackingStart)
    }

    val habitConsistency = remember(sessions, today, dayStartsAtHour) {
        GoalEngine.calculateHabitConsistency(sessions, 30, today, zone, dayStartsAtHour)
    }

    val seasonalSprint = remember(books, today) {
        GoalEngine.calculateSeasonalSprint(books, today, zone, 5)
    }

    val monthlySummary = remember(books, sessions, monthlyGoalPages, monthlyGoalBooks, today, dayStartsAtHour) {
        GoalEngine.calculateMonthlySummary(
            books = books,
            sessions = sessions,
            monthlyGoalPages = monthlyGoalPages,
            monthlyGoalBooks = monthlyGoalBooks,
            date = today,
            zone = zone,
            dayStartsAtHour = dayStartsAtHour
        )
    }
    var monthlyTargetMode by remember { mutableStateOf(GoalType.MONTHLY_PAGES) }

    val genreDistribution = remember(books) {
        val distribution = mutableMapOf<String, Int>()
        books.forEach { book ->
            val tags = book.genres.ifEmpty { book.classification.tags }
            tags.forEach { tag ->
                distribution[tag] = (distribution[tag] ?: 0) + 1
            }
        }
        distribution.toList().sortedByDescending { it.second }
    }
    val booksWithGenres = remember(books) { books.count { it.genres.isNotEmpty() || it.classification.tags.isNotEmpty() } }
    
    val formatDistribution = remember(books) {
        val distribution = mutableMapOf<String, Int>()
        books.forEach { book ->
            val format = book.format.name
            distribution[format] = (distribution[format] ?: 0) + 1
        }
        distribution.toList().sortedByDescending { it.second }
    }

    // One column on a phone; on a tablet the cards sit side by side, two or three across.
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 360.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 24.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Bookmory Yearly Reading Target with Pace
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("$thisYear Reading Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onEditGoal(GoalType.YEARLY_BOOKS) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit goal", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Pacing banner
                    val paceBg = when (yearlyPace.status) {
                        GoalEngine.GoalPaceStatus.AHEAD -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        GoalEngine.GoalPaceStatus.BEHIND -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        GoalEngine.GoalPaceStatus.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    }
                    val paceContentColor = when (yearlyPace.status) {
                        GoalEngine.GoalPaceStatus.AHEAD -> MaterialTheme.colorScheme.onPrimaryContainer
                        GoalEngine.GoalPaceStatus.BEHIND -> MaterialTheme.colorScheme.onErrorContainer
                        GoalEngine.GoalPaceStatus.ON_TRACK -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    val paceIcon = when (yearlyPace.status) {
                        GoalEngine.GoalPaceStatus.AHEAD -> Icons.AutoMirrored.Filled.TrendingUp
                        GoalEngine.GoalPaceStatus.BEHIND -> Icons.Filled.Schedule
                        GoalEngine.GoalPaceStatus.ON_TRACK -> Icons.Filled.CheckCircle
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = paceBg,
                        contentColor = paceContentColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(paceIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                yearlyPace.message,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                            val target = (finishedThisYear.toFloat() / yearlyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val progress by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = target,
                                label = "yearlyProgress"
                            )
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 8.dp,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                "${(target * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "$finishedThisYear of $yearlyGoal books",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Day ${yearlyPace.dayOfYear} of ${yearlyPace.totalDaysInYear} · Expected ~${yearlyPace.expectedBooks.roundToInt()} books",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (yearlyPace.projectionMessage.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    yearlyPace.projectionMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

        }

        item {
            // Bookmory Monthly Goal with Pages / Books Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${today.month.name.lowercase().replaceFirstChar { it.uppercase() }} Target",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TickFilterChip(
                                selected = monthlyTargetMode == GoalType.MONTHLY_PAGES,
                                onClick = { monthlyTargetMode = GoalType.MONTHLY_PAGES },
                                label = { Text("Pages") },
                                shape = CircleShape
                            )
                            TickFilterChip(
                                selected = monthlyTargetMode == GoalType.MONTHLY_BOOKS,
                                onClick = { monthlyTargetMode = GoalType.MONTHLY_BOOKS },
                                label = { Text("Books") },
                                shape = CircleShape
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val isPages = monthlyTargetMode == GoalType.MONTHLY_PAGES
                    val currentCount = if (isPages) monthlySummary.pagesRead else monthlySummary.booksCompleted
                    val targetCount = if (isPages) monthlySummary.targetPages else monthlySummary.targetBooks
                    val monthlyFraction = (currentCount.toFloat() / targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                        val progress by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = monthlyFraction,
                            label = "monthlyProgress"
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 10.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$currentCount",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "of $targetCount ${if (isPages) "pages" else "books"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${monthlySummary.activeDays} active reading day${if (monthlySummary.activeDays == 1) "" else "s"} this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onEditGoal(monthlyTargetMode) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Adjust Target")
                    }
                }
            }

        }

        item {
            // Seasonal Reading Sprint Challenge
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                seasonalSprint.season.emoji,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    seasonalSprint.season.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val seasonDateRange = when (seasonalSprint.season) {
                                    GoalEngine.Season.SPRING -> "Mar 1 – May 31"
                                    GoalEngine.Season.SUMMER -> "Jun 1 – Aug 31"
                                    GoalEngine.Season.AUTUMN -> "Sep 1 – Nov 30"
                                    GoalEngine.Season.WINTER -> "Dec 1 – Feb 28"
                                }
                                Text(
                                    seasonDateRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (seasonalSprint.isCompleted)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (seasonalSprint.isCompleted)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                if (seasonalSprint.isCompleted) "Completed 🎉" else "${seasonalSprint.daysRemaining}d left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val progressFraction = seasonalSprint.progress.coerceIn(0f, 1f)
                    val animatedSprintProgress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = progressFraction,
                        label = "sprintProgress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedSprintProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${seasonalSprint.completedBooks} of ${seasonalSprint.targetBooks} books completed",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        seasonalSprint.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }

        item {
            // Reading Habit Consistency Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Reading Habit Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                habitConsistency.rating,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                            val habitTarget = (habitConsistency.consistencyPercent / 100f).coerceIn(0f, 1f)
                            val habitProgress by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = habitTarget,
                                label = "habitProgress"
                            )
                            CircularProgressIndicator(
                                progress = { habitProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 7.dp,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                "${habitConsistency.consistencyPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${habitConsistency.activeDays} of ${habitConsistency.totalDays} days active (30d)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                habitConsistency.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

        }

        item {
            // Last-7-days activity, switchable between pages and reading time.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (showMinutes) "Time Read" else "Pages Read",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TickFilterChip(
                                selected = !showMinutes,
                                onClick = { showMinutes = false },
                                label = { Text("Pages") },
                                shape = CircleShape
                            )
                            TickFilterChip(
                                selected = showMinutes,
                                onClick = { showMinutes = true },
                                label = { Text("Minutes") },
                                shape = CircleShape
                            )
                        }
                    }
                    if (last7Days.all { it.second == 0L }) {
                        // A flat line of zeros reads as broken; say what will fill it instead.
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (showMinutes) "No timed reading in the last 7 days. Use the timer on Home and your minutes show up here."
                            else "No pages logged in the last 7 days. Update your page on Home and your week shows up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(Modifier.height(24.dp))
                        SmoothLineChart(
                            data = last7Days,
                            maxValue = maxIn7Days,
                            modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }
            }
        }

        // Reading-time stats from recorded session durations.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reading Time", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    if (timeSummary.timedSessionCount == 0) {
                        Text(
                            "Start and end sessions from the Home tab to unlock time " +
                                "stats and your reading speed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TimeStat(
                                Icons.Filled.Schedule,
                                AnalyticsEngine.formatMinutes(timeSummary.minutesLast7Days),
                                "This week"
                            )
                            TimeStat(
                                Icons.Filled.Schedule,
                                AnalyticsEngine.formatMinutes(timeSummary.avgSessionMinutes),
                                "Avg session"
                            )
                            TimeStat(
                                Icons.Filled.Speed,
                                velocity?.let { "${it.roundToInt()} p/h" } ?: "—",
                                "Speed"
                            )
                            TimeStat(
                                Icons.Filled.WbTwilight,
                                bestTime ?: "—",
                                "Best time"
                            )
                        }
                    }
                }
            }
        }

        // When and where focus is best, from the focus rated on the save screen.
        if (focus != null) {
            item { FocusInsightsCard(focus) }
        }

        // Where you read best — volume and pace per environment tag.
        if (environments.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Environments", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tagged when you end a session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        environments.take(6).forEach { env ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    env.tag,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    buildString {
                                        append("${env.totalPages} pages")
                                        env.pagesPerHour?.let {
                                            append(" · ${it.roundToInt()} p/h")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shares of one or two books are noise, so the split waits for a small library.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Genres", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    if (booksWithGenres < MIN_BOOKS_FOR_GENRES) {
                        Text(
                            "Your genre mix appears once $MIN_BOOKS_FOR_GENRES books have genre info. " +
                                "Books added via search or the ISBN scanner include it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        PieChart(
                            data = genreDistribution,
                            modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)
                        )
                    }
                }
            }
        }
        
        if (formatDistribution.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Format Distribution", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        PieChart(
                            data = formatDistribution,
                            modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)
                        )
                    }
                }
            }
        }



        if (finishedBooks.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    "Recently Finished",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(finishedBooks, key = { it.id }) { book ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .animateItem()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCover(url = book.coverUrl, modifier = Modifier.size(width = 48.dp, height = 72.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                book.authors.joinToString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (book.rating.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "%.1f".format(book.overallRating ?: 0f),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Average focus per time of day and per place, with the best of each called out. */
@Composable
private fun FocusInsightsCard(summary: com.example.booktracker.app.analytics.FocusInsights.Summary) {
    val headline = com.example.booktracker.app.analytics.FocusInsights.headline(summary)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Focus", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                headline ?: "Average ${String.format(java.util.Locale.US, "%.1f", summary.average)} of 5 across ${summary.rated} rated sessions. Rate a few more at different times to see a pattern.",
                style = if (headline != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                fontWeight = if (headline != null) FontWeight.SemiBold else null,
                color = if (headline != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            (summary.byTime + summary.byPlace.take(4)).forEachIndexed { i, group ->
                if (i == summary.byTime.size && summary.byPlace.isNotEmpty()) Spacer(Modifier.height(10.dp))
                val best = group == summary.bestTime || group == summary.bestPlace
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        group.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (best) FontWeight.Bold else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(96.dp)
                    )
                    LinearProgressIndicator(
                        progress = { group.average / 5f },
                        color = if (best) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        String.format(java.util.Locale.US, "%.1f", group.average),
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SmoothLineChart(
    data: List<Pair<java.time.LocalDate, Long>>,
    maxValue: Long,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
    )
) {
    if (data.isEmpty()) return
    
    var animationPlayed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    val animationProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "chartAnimation"
    )
    
    val textStyle = MaterialTheme.typography.labelSmall
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height - 30.dp.toPx() // Leave room for labels
        
        val pointSpacing = if (data.size > 1) width / (data.size - 1) else width
        
        val points = data.mapIndexed { index, (_, value) ->
            val normalizedValue = if (maxValue > 0) value.toFloat() / maxValue else 0f
            // Y is inverted in canvas (0 is top), animate the Y growth
            val x = index * pointSpacing
            val y = height - ((normalizedValue * animationProgress) * height)
            Offset(x, y)
        }
        
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                
                // Calculate control points for smooth bezier curve
                val controlPoint1 = Offset((p0.x + p1.x) / 2, p0.y)
                val controlPoint2 = Offset((p0.x + p1.x) / 2, p1.y)
                
                path.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
            }
        }
        
        // Draw the gradient fill
        val fillPath = Path().apply {
            addPath(path)
            if (points.isNotEmpty()) {
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = 0f,
                endY = height
            )
        )
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            val (_, value) = data[index]
            val date = data[index].first
            val dayName = date.dayOfWeek.name.take(1)
            
            // Draw a circle for each data point
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = point
            )
            
            // Draw value text above the point if > 0
            if (value > 0) {
                val textLayoutResult = textMeasurer.measure(
                    text = value.toString(),
                    style = textStyle
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    color = onSurfaceVariant,
                    topLeft = Offset(
                        x = point.x - (textLayoutResult.size.width / 2f),
                        y = point.y - textLayoutResult.size.height - 8.dp.toPx()
                    )
                )
            }
            
            // Draw day label below
            val dayLayoutResult = textMeasurer.measure(
                text = dayName,
                style = textStyle
            )
            drawText(
                textLayoutResult = dayLayoutResult,
                color = onSurfaceVariant,
                topLeft = Offset(
                    x = point.x - (dayLayoutResult.size.width / 2f),
                    y = height + 8.dp.toPx()
                )
            )
        }
    }
}

@Composable
fun PieChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
    )
    
    val total = data.sumOf { it.second }.toFloat().coerceAtLeast(1f) // never NaN slices
    val proportions = data.map { it.second.toFloat() / total }
    val onSurface = MaterialTheme.colorScheme.onSurface
    
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    val animationProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "pieAnimation"
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                proportions.forEachIndexed { index, proportion ->
                    val sweepAngle = proportion * 360f * animationProgress
                    val color = colors[index % colors.size]
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }
        }
        
        Spacer(Modifier.width(24.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, (label, count) ->
                val color = colors[index % colors.size]
                val percentage = ((count.toFloat() / total) * 100).roundToInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$label ($percentage%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
