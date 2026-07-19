package com.example.booktracker.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
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
    streak: Int,
    yearlyGoal: Int,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenFinished: () -> Unit = {},
    onOpenBook: (Book) -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    unlockedBadges: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val completedBooks = books.filter { it.status == BookStatus.FINISHED.name }
    val pagesRead = completedBooks.sumOf { it.totalUnits }
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
                    .clickable { onOpenWrapped() }
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

        // Yearly goal progress (goal set in Settings).
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$thisYear Reading Goal", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$finishedThisYear of $yearlyGoal books finished",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (finishedThisYear >= yearlyGoal) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Goal complete — raise it in Settings?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
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
                        .clickable { onOpenBook(currentBook) }
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
                            val targetProgress = if (currentBook.totalUnits > 0) currentBook.currentUnit.toFloat() / currentBook.totalUnits else 0f
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

        // Secret Badges Grid
        item {
            Column {
                Text(
                    "Secret Badges",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Use a lazy grid equivalent using simple rows because LazyColumn can't easily nest LazyVerticalGrid
                val badges = BadgeEngine.BADGES
                val rows = badges.chunked(2)
                rows.forEach { rowBadges ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowBadges.forEach { badge ->
                            val isUnlocked = unlockedBadges.contains(badge.id)
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (isUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isUnlocked) {
                                            Icon(badge.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(24.dp))
                                        } else {
                                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (isUnlocked) badge.title else "???",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (isUnlocked) badge.description else "Keep reading to discover.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        // Handle odd number of badges in the last row
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
                            genreShares.forEachIndexed { index, (genre, pct) ->
                                val color = when (index) {
                                    0 -> MaterialTheme.colorScheme.primary
                                    1 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.tertiary
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
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
