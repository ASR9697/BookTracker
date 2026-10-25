package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.Badge
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.app.analytics.GoalEngine
import com.example.booktracker.app.util.MarkdownExporter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    userName: String,
    books: List<Book>,
    sessions: List<Session>,
    notes: List<MarginNote> = emptyList(),
    streak: Int,
    yearlyGoal: Int,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenFinished: () -> Unit = {},
    onOpenPlanner: () -> Unit = {},
    onOpenBook: (Book) -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onEditGoal: () -> Unit = {},
    unlockedBadges: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val completedBooks = books.filter { it.status == BookStatus.FINISHED.name }
    val pagesRead = completedBooks.sumOf { it.totalPages }
    val currentBook = books.firstOrNull { it.status == BookStatus.READING.name }
    val velocity = remember(sessions) { AnalyticsEngine.pagesPerHour(sessions) }
    val totalMinutes = remember(sessions) {
        AnalyticsEngine.timedSessions(sessions).sumOf { it.endTime - it.startTime } / 60_000L
    }
    val thisYear = remember { LocalDate.now(zone).year }
    val finishedThisYear = completedBooks.count {
        Instant.ofEpochMilli(it.lastUpdated).atZone(zone).toLocalDate().year == thisYear
    }
    val trackingSince = remember(books, sessions) {
        val earliest = (books.map { it.lastUpdated } + sessions.map { it.startTime })
            .filter { it > 0 }
            .minOrNull()
        earliest?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                .format(DateTimeFormatter.ofPattern("MMM yyyy"))
        }
    }

    var selectedBadge by remember { mutableStateOf<Badge?>(null) }
    selectedBadge?.let { badge ->
        BadgeDetailDialog(
            badge = badge,
            isUnlocked = unlockedBadges.contains(badge.id),
            onDismiss = { selectedBadge = null }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Section: User Info
        item {
            var showNameDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (showNameDialog) {
                var newName by remember { androidx.compose.runtime.mutableStateOf(userName) }
                AlertDialog(
                    onDismissRequest = { showNameDialog = false },
                    title = { Text("Edit Profile") },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Display Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            onUpdateName(newName.trim().takeIf { it.isNotEmpty() } ?: "Reader")
                            showNameDialog = false
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
                    }
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNameDialog = true }
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userName.firstOrNull()?.toString()?.uppercase() ?: "R",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        velocity?.let { "≈ ${it.roundToInt()} pages/hour reader" }
                            ?: "Building your reading profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (trackingSince != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Tracking since $trackingSince",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onOpenWrapped() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Your Wrapped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "See your reading summary",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }

        // Yearly goal progress with pacing badge
        item {
            val yearlyPace = remember(yearlyGoal, finishedThisYear) {
                GoalEngine.calculateYearlyPace(yearlyGoal, finishedThisYear, LocalDate.now(zone))
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = onEditGoal)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                            val target = (finishedThisYear.toFloat() / yearlyGoal.coerceAtLeast(1))
                                .coerceIn(0f, 1f)
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
                                "$finishedThisYear",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$thisYear Reading Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit Goal",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "$finishedThisYear of $yearlyGoal books finished",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                yearlyPace.message,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (yearlyPace.status) {
                                    GoalEngine.GoalPaceStatus.AHEAD -> MaterialTheme.colorScheme.primary
                                    GoalEngine.GoalPaceStatus.BEHIND -> MaterialTheme.colorScheme.error
                                    GoalEngine.GoalPaceStatus.ON_TRACK -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                ReadingHeatmap(
                    sessions = sessions,
                    books = books,
                    notes = notes,
                    onOpenBook = onOpenBook,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Current Book Card (if any)
        if (currentBook != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onOpenBook(currentBook) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "CURRENT BOOK",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                currentBook.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            val targetProgress = if (currentBook.totalPages > 0) currentBook.currentPage.toFloat() / currentBook.totalPages else 0f
                            val progress by androidx.compose.animation.core.animateFloatAsState(targetValue = targetProgress, label = "progress")
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        BookCover(currentBook.coverUrl, Modifier.size(width = 48.dp, height = 72.dp))
                    }
                }
            }
        }

        // Achievements Grid
        item {
            Column {
                Text(
                    "Achievements",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AchievementCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.LocalFireDepartment,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        iconBackground = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                        value = streak.toString(),
                        label = "Days Streak"
                    )
                    AchievementCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBackground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        value = completedBooks.size.toString(),
                        label = "Completed"
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AchievementCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Layers,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        value = pagesRead.toString(),
                        label = "Pages Read"
                    )
                    AchievementCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Schedule,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        value = AnalyticsEngine.formatMinutes(totalMinutes),
                        label = "Time Read"
                    )
                }
            }
        }

        // Reading Achievements & Badges Gallery
        item {
            var badgeFilter by remember { mutableStateOf("ALL") }
            val allBadges = BadgeEngine.BADGES
            val unlockedCount = allBadges.count { unlockedBadges.contains(it.id) }
            val displayedBadges = remember(badgeFilter, unlockedBadges) {
                when (badgeFilter) {
                    "UNLOCKED" -> allBadges.filter { unlockedBadges.contains(it.id) }
                    "LOCKED" -> allBadges.filter { !unlockedBadges.contains(it.id) }
                    else -> allBadges
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Reading Achievements",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$unlockedCount of ${allBadges.size} Unlocked",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Filter toggle chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL" to "All", "UNLOCKED" to "Earned", "LOCKED" to "Locked").forEach { (key, label) ->
                            val selected = badgeFilter == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.bounceClick { badgeFilter = key }
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val rows = displayedBadges.chunked(2)
                rows.forEach { rowBadges ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowBadges.forEach { badge ->
                            val isUnlocked = unlockedBadges.contains(badge.id)
                            val tierColor = getBadgeTierColor(badge.tier)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick { selectedBadge = badge },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUnlocked) tierColor.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isUnlocked) BorderStroke(1.5.dp, tierColor.copy(alpha = 0.45f)) else null,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Tier tag
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isUnlocked) tierColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            badge.tier,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(if (isUnlocked) tierColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.5.dp,
                                                if (isUnlocked) tierColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isUnlocked) {
                                            Icon(
                                                badge.icon,
                                                contentDescription = null,
                                                tint = tierColor,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Filled.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Text(
                                        badge.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        if (isUnlocked) badge.description else "Tap to view challenge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowBadges.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // Library Insights — genre split of the whole library.
        item {
            val genreShares = remember(books) {
                val counts = books.flatMap { it.genres }.groupingBy { it }.eachCount()
                val total = counts.values.sum().coerceAtLeast(1)
                counts.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .map { it.key to it.value.toFloat() / total }
            }
            Column {
                Text(
                    "Library Insights",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top Genres", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))

                        if (genreShares.isEmpty()) {
                            Text(
                                "Genres appear once you add books via search or the scanner.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val colorScheme = MaterialTheme.colorScheme
                            genreShares.forEachIndexed { index, (genre, pct) ->
                                val color = when (index) {
                                    0 -> colorScheme.primary
                                    1 -> colorScheme.secondary
                                    else -> colorScheme.tertiary
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(genre, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "${(pct * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Settings & Activity
        item {
            Column {
                Text(
                    "Settings & Activity",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Filled.History,
                            label = "Reading History",
                            onClick = onOpenHistory
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Filled.DoneAll,
                            label = "Finished & Abandoned",
                            onClick = onOpenFinished
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Filled.Flag,
                            label = "Goals & Settings",
                            onClick = onOpenSettings
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Filled.Event,
                            label = "Reading Planner",
                            onClick = onOpenPlanner
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Filled.Share,
                            label = "Export Knowledge Base (.md)",
                            onClick = { MarkdownExporter.exportToMarkdown(context, books, notes) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun getBadgeTierColor(tier: String): Color {
    return when (tier.uppercase()) {
        "PLATINUM" -> Color(0xFF00E5FF)
        "GOLD" -> Color(0xFFFFB300)
        "SILVER" -> Color(0xFFB0BEC5)
        else -> Color(0xFFCD7F32) // BRONZE
    }
}

@Composable
internal fun BadgeDetailDialog(
    badge: Badge,
    isUnlocked: Boolean,
    onDismiss: () -> Unit
) {
    val tierColor = getBadgeTierColor(badge.tier)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tierColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        "✦ ${badge.tier} ACHIEVEMENT ✦",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) tierColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            2.dp,
                            if (isUnlocked) tierColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isUnlocked) badge.icon else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    badge.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isUnlocked) "Unlocked & Earned!" else "Locked Challenge",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
