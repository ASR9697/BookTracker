package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.booktracker.app.data.AvatarStore
import java.time.Instant
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.app.analytics.BadgeStatus
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.ProgressUnit
import com.example.booktracker.shared.models.Session
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.app.ui.animations.LivingFlame
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.staggeredEntrance
import com.example.booktracker.app.analytics.GoalEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.util.MarkdownExporter
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val MAX_READING_ROWS = 3
private const val MAX_TEASER_MEDALS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    books: List<Book>,
    sessions: List<Session>,
    notes: List<MarginNote> = emptyList(),
    streak: Int,
    longestStreak: Int = streak,
    dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR,
    onOpenSettings: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenFinished: () -> Unit = {},
    onOpenPlanner: () -> Unit = {},
    onOpenWishlist: () -> Unit = {},
    onOpenBook: (Book) -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenStreak: () -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    badgeStatuses: List<BadgeStatus> = emptyList(),
    monthlyChallenge: BadgeStatus? = null,
    yearlyGoal: Int = 0,
    avatar: String = "",
    onSetAvatarPhoto: (Uri) -> Unit = {},
    onSetAvatarCover: (Book) -> Unit = {},
    onClearAvatar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val completedBooks = books.filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
    // Logged reading, plus whole books finished before they were ever tracked (imports).
    val pagesRead = remember(books, sessions) {
        val trackedBookIds = sessions.filter(StreakEngine::isReading).map { it.bookId }.toSet()
        StreakEngine.pagesByDay(sessions).values.sum() +
            completedBooks.filter { it.id !in trackedBookIds }.sumOf { it.totalPages }
    }
    val readingBooks = remember(books) {
        books.filter { it.status.equals(BookStatus.READING.name, ignoreCase = true) }
            .sortedByDescending { it.lastUpdated }
    }
    val velocity = remember(sessions) { AnalyticsEngine.pagesPerHour(sessions) }
    val personality = remember(books, sessions) {
        listOfNotNull(velocity?.let { "≈ ${it.roundToInt()} pages/hour" }) +
            AnalyticsEngine.readerPersonality(books, sessions, zone)
    }
    val today = LocalDate.now(zone)
    val finishedThisYear = completedBooks.count {
        it.completedAt > 0 && Instant.ofEpochMilli(it.completedAt).atZone(zone).year == today.year
    }
    val totalMinutes = remember(sessions) {
        AnalyticsEngine.timedSessions(sessions).sumOf { it.endTime - it.startTime } / 60_000L
    }
    val trackingStart = remember(books, sessions) { GoalEngine.trackingStart(books, sessions, zone) }
    val trackingSince = trackingStart?.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    val goalPace = when {
        yearlyGoal <= 0 -> null
        finishedThisYear >= yearlyGoal -> "goal reached"
        else -> when (GoalEngine.calculateYearlyPace(yearlyGoal, finishedThisYear, today, trackingStart).status) {
            GoalEngine.GoalPaceStatus.AHEAD -> "ahead"
            GoalEngine.GoalPaceStatus.ON_TRACK -> "on track"
            GoalEngine.GoalPaceStatus.BEHIND -> "behind"
        }
    }
    val hasYearToReview = remember(books, sessions, dayStartsAtHour) {
        AnalyticsEngine.hasYearToReview(books, sessions, LocalDate.now(zone).year, zone, dayStartsAtHour)
    }

    val entrance = rememberEntranceState()
    SectionPage(
        modifier = modifier,
        // Avatar inside the yearly-goal ring, name, personality: the page's own big header.
        expanded = {
            Column {
            var showNameDialog by remember { mutableStateOf(false) }
            var showAvatarSheet by remember { mutableStateOf(false) }
            if (showNameDialog) {
                var newName by remember { mutableStateOf(userName) }
                AlertDialog(
                    onDismissRequest = { showNameDialog = false },
                    title = { Text("Edit profile") },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Display name") },
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
            if (showAvatarSheet) {
                AvatarPickerSheet(
                    avatar = avatar,
                    books = books,
                    onPickPhoto = { onSetAvatarPhoto(it); showAvatarSheet = false },
                    onPickCover = { onSetAvatarCover(it); showAvatarSheet = false },
                    onUseInitial = { onClearAvatar(); showAvatarSheet = false },
                    onDismiss = { showAvatarSheet = false }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).staggeredEntrance(entrance, 0)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                    if (yearlyGoal > 0) {
                        CircularProgressIndicator(
                            progress = { (finishedThisYear.toFloat() / yearlyGoal).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            gapSize = 0.dp
                        )
                    }
                    ProfileAvatar(
                        avatar = avatar,
                        userName = userName,
                        books = books,
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { showAvatarSheet = true }
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 6.dp, bottom = 6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { showAvatarSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            contentDescription = "Change profile picture",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showNameDialog = true }
                    ) {
                        Text(
                            userName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Edit name",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        personality.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                            ?: "Building your reading profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (yearlyGoal > 0) {
                        Spacer(Modifier.height(4.dp))
                        HeaderLine(
                            icon = Icons.Rounded.Flag,
                            text = "$finishedThisYear / $yearlyGoal books in ${today.year}" +
                                (goalPace?.let { " · $it" } ?: "")
                        )
                    }
                    if (trackingSince != null) {
                        Spacer(Modifier.height(2.dp))
                        HeaderLine(icon = Icons.Rounded.CalendarToday, text = "Tracking since $trackingSince")
                    }
                }
            }
            }
        },
        collapsed = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(avatar, userName, books, Modifier.size(32.dp), initialStyle = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(10.dp))
                CollapsedTitle(userName)
            }
        },
        actions = {
            IconButton(onClick = onOpenAppSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
            }
        }
    ) {
    // A wider column than text screens: the profile is tiles and rows, not paragraphs.
    val side = 16.dp + windowCentredPadding(max = 840.dp, chrome = navChromeWidth())
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = side, top = 8.dp, end = side, bottom = 16.dp + LocalNavBarClearance.current),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Lifetime numbers, one compact card instead of four big tiles.
        item {
            Card(
                shape = AppShape.L,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().staggeredEntrance(entrance, 1)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row {
                        StatTile(
                            icon = Icons.Rounded.LocalFireDepartment,
                            tint = MaterialTheme.colorScheme.tertiary,
                            count = streak,
                            flameStreak = streak,
                            label = "day streak · best $longestStreak",
                            modifier = Modifier.weight(1f).bounceClick { onOpenStreak() }
                        )
                        StatTile(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            tint = MaterialTheme.colorScheme.secondary,
                            count = completedBooks.size,
                            label = if (completedBooks.size == 1) "book finished" else "books finished",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row {
                        StatTile(
                            icon = Icons.Rounded.Layers,
                            tint = MaterialTheme.colorScheme.primary,
                            count = pagesRead,
                            format = { "%,d".format(it) },
                            label = "pages read",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            icon = Icons.Rounded.Schedule,
                            tint = MaterialTheme.colorScheme.primary,
                            count = totalMinutes.toInt(),
                            format = { AnalyticsEngine.formatMinutes(it.toLong()) },
                            label = "time reading",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Wrapped stays hidden until there is a year worth sharing.
        if (hasYearToReview) item {
            Card(
                shape = AppShape.M,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(entrance, 2)
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
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Your ${LocalDate.now(zone).year} Wrapped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your year in books, ready to share",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }

        if (badgeStatuses.isNotEmpty()) item {
            Box(Modifier.staggeredEntrance(entrance, 3)) {
                AchievementsTeaser(statuses = badgeStatuses, challenge = monthlyChallenge, onClick = onOpenAchievements)
            }
        }

        item {
            Card(
                shape = AppShape.M,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().staggeredEntrance(entrance, 4)
            ) {
                ReadingHeatmap(
                    sessions = sessions,
                    books = books,
                    notes = notes,
                    onOpenBook = onOpenBook,
                    dayStartsAtHour = dayStartsAtHour,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (readingBooks.isNotEmpty()) item {
            Card(
                shape = AppShape.M,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().staggeredEntrance(entrance, 5)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "CURRENTLY READING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    readingBooks.take(MAX_READING_ROWS).forEach { book ->
                        ReadingRow(book, sessions, onClick = { onOpenBook(book) })
                    }
                    if (readingBooks.size > MAX_READING_ROWS) {
                        Text(
                            "+${readingBooks.size - MAX_READING_ROWS} more in your library",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Settings & Activity
        item {
            Column(modifier = Modifier.staggeredEntrance(entrance, 6)) {
                Text(
                    "Your reading",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = AppShape.M
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Rounded.History,
                            label = "Reading history",
                            onClick = onOpenHistory
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Rounded.DoneAll,
                            label = "Finished & abandoned",
                            onClick = onOpenFinished
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Rounded.Flag,
                            label = "Reading goals",
                            onClick = onOpenSettings
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Rounded.Event,
                            label = "Reading planner",
                            onClick = onOpenPlanner
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Rounded.BookmarkAdd,
                            label = "Wishlist",
                            onClick = onOpenWishlist
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRow(
                            icon = Icons.Rounded.Share,
                            label = "Export notes as Markdown",
                            onClick = { MarkdownExporter.exportToMarkdown(context, books, notes) }
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    tint: Color,
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { it.toString() },
    /** When set, the icon is the living streak flame instead. */
    flameStreak: Int? = null
) {
    val shown by animateCountUp(count)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (flameStreak != null) {
                LivingFlame(streak = flameStreak, baseSize = 20.dp, tint = tint)
            } else {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(format(shown), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Closed summary of the badge gallery; the full grid lives on the Achievements screen. */
@Composable
private fun AchievementsTeaser(statuses: List<BadgeStatus>, challenge: BadgeStatus?, onClick: () -> Unit) {
    val earned = statuses.filter { it.unlocked }
        .sortedByDescending { BadgeEngine.TIERS.indexOf(it.badge.tier) }
    val nextUp = remember(statuses) { BadgeEngine.nextUp(statuses) }

    Card(
        shape = AppShape.L,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().bounceClick(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${earned.size} / ${statuses.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (earned.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    earned.take(MAX_TEASER_MEDALS).forEach { BadgeMedallion(it, size = 40.dp) }
                    if (earned.size > MAX_TEASER_MEDALS) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+${earned.size - MAX_TEASER_MEDALS}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (nextUp != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Next up",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        nextUp.badge.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = getBadgeTierColor(nextUp.badge.tier)
                    )
                }
                Spacer(Modifier.height(6.dp))
                BadgeProgressBar(nextUp)
            }

            if (challenge != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        challenge.badge.icon,
                        contentDescription = null,
                        tint = getBadgeTierColor(challenge.badge.tier),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault())} challenge",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (challenge.unlocked) "Done ✓" else challenge.progressLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getBadgeTierColor(challenge.badge.tier)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Photo, chosen book cover, or the name's initial, in that order of preference. */
@Composable
internal fun ProfileAvatar(
    avatar: String,
    userName: String,
    books: List<Book>,
    modifier: Modifier = Modifier,
    initialStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    initialColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val coverUrl = remember(avatar, books) {
        avatar.takeIf { it.startsWith(AvatarStore.COVER_PREFIX) }
            ?.removePrefix(AvatarStore.COVER_PREFIX)
            ?.let { id -> books.firstOrNull { it.id == id }?.coverUrl?.takeIf { url -> url.isNotBlank() } }
    }
    val photo = avatar.takeIf { it.startsWith(AvatarStore.PHOTO_PREFIX) }?.removePrefix(AvatarStore.PHOTO_PREFIX)
    Box(
        modifier = modifier.clip(CircleShape).background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        val image = photo ?: coverUrl
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                // Covers carry the title at the top, which reads best in a circle.
                alignment = if (photo != null) Alignment.Center else Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                userName.firstOrNull()?.toString()?.uppercase() ?: "R",
                style = initialStyle,
                color = initialColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarPickerSheet(
    avatar: String,
    books: List<Book>,
    onPickPhoto: (Uri) -> Unit,
    onPickCover: (Book) -> Unit,
    onUseInitial: () -> Unit,
    onDismiss: () -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickPhoto(uri)
    }
    // Books the reader has lived with come first: finished, then in progress, then the rest.
    val covers = remember(books) {
        books.filter { it.coverUrl.isNotBlank() }.sortedBy {
            when (it.status) {
                BookStatus.FINISHED.name -> 0
                BookStatus.READING.name -> 1
                else -> 2
            }
        }
    }
    val chosenCover = avatar.takeIf { it.startsWith(AvatarStore.COVER_PREFIX) }?.removePrefix(AvatarStore.COVER_PREFIX)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Profile picture",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            SettingsRow(
                icon = Icons.Rounded.PhotoLibrary,
                label = "Choose a photo",
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            if (avatar.isNotBlank()) {
                SettingsRow(icon = Icons.Rounded.TextFields, label = "Use my initial", onClick = onUseInitial)
            }
            if (covers.isNotEmpty()) {
                Text(
                    "Or a cover from your shelf",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(covers, key = { it.id }) { book ->
                        val selected = book.id == chosenCover
                        BookCover(
                            book,
                            Modifier
                                .size(width = 64.dp, height = 96.dp)
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                    else Modifier
                                )
                                .clickable { onPickCover(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingRow(book: Book, sessions: List<Session>, onClick: () -> Unit) {
    val fraction = if (book.totalPages > 0) (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) else 0f
    val progress by androidx.compose.animation.core.animateFloatAsState(targetValue = fraction, label = "progress")
    // Time left is page velocity, so only meaningful for books tracked in pages.
    val timeLeft = remember(book, sessions) {
        if (book.progressUnit != ProgressUnit.Page) null
        else AnalyticsEngine.formatEstimatedTime(
            AnalyticsEngine.bookEstimatedMinutesLeft(book, sessions.filter { it.bookId == book.id }, sessions)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookCover(book, Modifier.heroSharedElement("cover-${book.id}").size(width = 40.dp, height = 60.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(6.dp))
            Text(
                listOfNotNull("${(fraction * 100).toInt()}%", timeLeft).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
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
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
