package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import com.example.booktracker.app.ui.animations.BurstStyle
import com.example.booktracker.app.ui.animations.ConfettiExplosion
import com.example.booktracker.app.ui.animations.EmberColors
import com.example.booktracker.app.ui.animations.LivingFlame
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.TimeOfDaySky
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.animations.bounceOnPress
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import com.example.booktracker.app.ui.animations.staggeredEntrance
import com.example.booktracker.app.ui.animations.swipeToAct
import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.GoalEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.analytics.RereadMemories
import com.example.booktracker.shared.models.ProgressUnit
import com.example.booktracker.app.analytics.WishlistEngine
import com.example.booktracker.app.data.HomeLayout
import com.example.booktracker.app.data.HomeSection
import com.example.booktracker.shared.models.Book
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val TileShape = AppShape.L

/** How recent the reading must be for reaching today's goal to get a celebration. */
private const val FRESH_LOG_WINDOW_MS = 5 * 60_000L

/**
 * Home: everything about your reading on one scroll. The books in progress sit
 * on top as a swipeable carousel; streak, today, the year and the to-read queue
 * follow as tiles, each opening the screen with the detail.
 */
/** How tall the sky behind the greeting is, below the app bar. */
private val SKY_HEIGHT = 200.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboard(
    books: List<Book>,
    readingBooks: List<Book>,
    sessions: List<Session>,
    openSession: Session?,
    streak: StreakEngine.StreakInfo,
    userName: String,
    yearlyGoal: Int,
    onOpenBook: (Book) -> Unit,
    onOpenTimer: (Book) -> Unit,
    onLogPages: (Book, Int) -> Unit,
    onAddBook: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBookshelf: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenFinished: () -> Unit,
    onOpenWrapped: () -> Unit,
    onEditGoal: (GoalType) -> Unit,
    modifier: Modifier = Modifier,
    layout: HomeLayout = HomeLayout.DEFAULT,
    onEditHome: () -> Unit = {},
    onOpenOwnership: (Ownership) -> Unit = {},
    onAddNote: (Book, page: Int, text: String, type: NoteType, images: List<String>) -> Unit = { _, _, _, _, _ -> },
    onChangeStatus: (Book, BookStatus) -> Unit = { _, _ -> },
    onOpenStreak: () -> Unit = {},
    reviewDue: Int = 0,
    reviewStarted: Boolean = false,
    onOpenReview: () -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
    headerState: TopAppBarState = rememberTopAppBarState(),
    onSearch: () -> Unit = {},
    banner: @Composable () -> Unit = {}
) {
    val zone = remember { ZoneId.systemDefault() }
    val year = streak.today.year
    var bookToLog by remember { mutableStateOf<Book?>(null) }
    var bookToNote by remember { mutableStateOf<Book?>(null) }
    var bookForActions by remember { mutableStateOf<Book?>(null) }
    val haptics = rememberHaptics()
    val entrance = rememberEntranceState()

    // Celebrate the moment today's goal is met, not every time Home opens after it.
    // Data streams in after launch (empty first), so a crossing only counts when
    // the reading behind it was logged in the last few minutes.
    val goalReached = streak.dailyGoal > 0 && streak.pagesToday >= streak.dailyGoal
    var goalWasReached by rememberSaveable { mutableStateOf(goalReached) }
    var celebrating by remember { mutableStateOf(false) }
    LaunchedEffect(goalReached) {
        val lastLogged = sessions.maxOfOrNull { maxOf(it.startTime, it.endTime) } ?: 0L
        val fresh = System.currentTimeMillis() - lastLogged < FRESH_LOG_WINDOW_MS
        if (goalReached && !goalWasReached && fresh) {
            celebrating = true
            haptics.celebrate()
        }
        goalWasReached = goalReached
    }

    val finishedThisYear = remember(books, year) {
        books.filter {
            it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) &&
                Instant.ofEpochMilli(it.completedAt).atZone(zone).year == year
        }
    }
    val trackingStart = remember(books, sessions) { GoalEngine.trackingStart(books, sessions, zone) }
    val hasYearToReview = remember(books, sessions, year, streak.dayStartsAtHour) {
        AnalyticsEngine.hasYearToReview(books, sessions, year, zone, streak.dayStartsAtHour)
    }
    val pagesByMonth = remember(sessions, year, streak.dayStartsAtHour) {
        IntArray(12).also { months ->
            StreakEngine.pagesByDay(sessions, zone, streak.dayStartsAtHour).forEach { (date, pages) ->
                if (date.year == year) months[date.monthValue - 1] += pages
            }
        }
    }
    val minutesToday = remember(sessions, streak.today, streak.dayStartsAtHour) {
        sessions.filter { StreakEngine.sessionDay(it, zone, streak.dayStartsAtHour) == streak.today }
            .sumOf { it.durationSeconds } / 60
    }
    val upNext = remember(books) {
        val order = listOf(BookStatus.UP_NEXT.name, BookStatus.SHORTLIST.name, BookStatus.TO_READ.name, "BACKLOG")
        WishlistEngine.onShelf(books).filter { b -> order.any { it.equals(b.status, ignoreCase = true) } }
            .sortedWith(compareBy<Book> { b -> order.indexOfFirst { it.equals(b.status, ignoreCase = true) } }
                .thenByDescending { it.lastUpdated })
            .take(12)
    }

    // Landscape tablets: the current book and sections in two columns instead of one long scroll.
    val twoColumns = LocalWidthTier.current.atLeastExpanded

    val currentBooks: @Composable () -> Unit = {
        if (readingBooks.isEmpty()) {
            NothingInProgressCard(
                hasBooks = books.isNotEmpty(),
                onAddBook = onAddBook,
                onOpenLibrary = onOpenLibrary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            val pagerState = rememberPagerState { readingBooks.size }
            Column {
                HorizontalPager(
                    state = pagerState,
                    // The next book peeks in from the edge when there is one.
                    contentPadding = PaddingValues(horizontal = if (readingBooks.size > 1) 24.dp else 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val book = readingBooks[page]
                    CurrentBookCard(
                        book = book,
                        sessions = remember(book.id, sessions) { sessions.filter { it.bookId == book.id } },
                        allSessions = sessions,
                        sessionOpen = openSession?.bookId == book.id,
                        today = streak.today,
                        dayStartsAtHour = streak.dayStartsAtHour,
                        modifier = Modifier.staggeredEntrance(entrance, 0),
                        onOpen = { onOpenBook(book) },
                        onOpenTimer = { onOpenTimer(book) },
                        onLogPages = { bookToLog = book },
                        onAddNote = { bookToNote = book }
                    )
                }
                if (readingBooks.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(readingBooks.size) { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(width = if (i == pagerState.currentPage) 18.dp else 6.dp, height = 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    // One of the reader-ordered sections; the greeting and current book above stay fixed.
    val sectionContent: @Composable (Int, HomeSection) -> Unit = { sectionIndex, section ->
            Box(modifier = Modifier.staggeredEntrance(entrance, sectionIndex + 1)) {
            when (section) {
                HomeSection.STREAK -> {
                    // Streak and this week; tap for the full history.
                    Tile(onClick = onOpenStreak, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivingFlame(streak = streak.currentStreak)
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val shownStreak by animateCountUp(streak.currentStreak)
                                Text(
                                    when (streak.currentStreak) {
                                        0 -> "No streak yet"
                                        1 -> "1-day streak"
                                        else -> "$shownStreak-day streak"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (streak.currentStreak == 0) "Read a page today to start one"
                                    else "Best: ${streak.longestStreak} day${if (streak.longestStreak == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        WeekBubbles(streak = streak, modifier = Modifier.fillMaxWidth())
                    }
                }
                HomeSection.GOALS -> {
                    // Today and this year, side by side.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val dailyFraction = if (streak.dailyGoal > 0) streak.pagesToday.toFloat() / streak.dailyGoal else 0f
                        Tile(
                            onClick = { onEditGoal(GoalType.DAILY_PAGES) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            RingHeader(title = "Today", fraction = dailyFraction)
                            Spacer(Modifier.height(10.dp))
                            val shownToday by animateCountUp(streak.pagesToday)
                            Text(
                                "$shownToday of ${streak.dailyGoal} pages",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (minutesToday > 0) formatMinutes(minutesToday) + " read" else "Nothing logged yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val yearFraction = if (yearlyGoal > 0) finishedThisYear.size.toFloat() / yearlyGoal else 0f
                        Tile(
                            onClick = { onEditGoal(GoalType.YEARLY_BOOKS) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            RingHeader(title = "$year", fraction = yearFraction)
                            Spacer(Modifier.height(10.dp))
                            val shownFinished by animateCountUp(finishedThisYear.size)
                            Text(
                                "$shownFinished of $yearlyGoal books",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                yearPace(finishedThisYear.size, yearlyGoal, streak.today, trackingStart),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HomeSection.REVIEW -> {
                    // Hidden until there is something to review; afterwards it says when you're caught up.
                    if (reviewDue > 0 || reviewStarted) {
                        Tile(onClick = onOpenReview, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (reviewDue > 0) "$reviewDue ${if (reviewDue == 1) "card" else "cards"} to keep your highlights and words fresh"
                                        else "All caught up. More tomorrow.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (reviewDue > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    FilledTonalButton(onClick = onOpenReview) { Text("Start") }
                                }
                            }
                        }
                    }
                }
                HomeSection.MONTHLY_PAGES -> {
                    // Pages per month this year.
                    Tile(onClick = onOpenStats, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "Pages read in $year",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${pagesByMonth.sum()} total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        MonthBars(pagesByMonth, currentMonth = streak.today.monthValue)
                    }
                }
                HomeSection.UP_NEXT -> {
                    if (upNext.isNotEmpty()) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Up next",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onOpenLibrary) { Text("See all") }
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(upNext, key = { it.id }) { book ->
                                    val press = remember { MutableInteractionSource() }
                                    Column(
                                        modifier = Modifier
                                            .width(84.dp)
                                            .bounceOnPress(press)
                                            .clip(AppShape.S)
                                            .onSecondaryClick { bookForActions = book }
                                            .combinedClickable(
                                                interactionSource = press,
                                                indication = null,
                                                onClick = { onOpenBook(book) },
                                                onLongClickLabel = "Book actions",
                                                onLongClick = {
                                                    haptics.longPress()
                                                    bookForActions = book
                                                }
                                            )
                                    ) {
                                        BookCover(
                                            book,
                                            Modifier.heroSharedElement("cover-${book.id}").size(width = 84.dp, height = 126.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            book.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HomeSection.OWNERSHIP -> {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(Ownership.WISHLIST, Ownership.OWNED, Ownership.GIFT).forEach { ownership ->
                            val count = books.count { it.ownership == ownership }
                            ShortcutTile(
                                icon = ownership.icon,
                                title = if (ownership == Ownership.GIFT) "Gifts" else ownership.label,
                                subtitle = "$count ${if (count == 1) "book" else "books"}",
                                onClick = { onOpenOwnership(ownership) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                HomeSection.SHORTCUTS -> {
                    // Shortcuts to the deeper views.
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val thisMonthPages = pagesByMonth[streak.today.monthValue - 1]
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShortcutTile(
                                icon = Icons.Rounded.CalendarMonth,
                                title = "Calendar",
                                subtitle = if (thisMonthPages > 0) "$thisMonthPages pages this month" else "Your reading by day",
                                onClick = onOpenCalendar,
                                modifier = Modifier.weight(1f)
                            )
                            val shelfCount = WishlistEngine.onShelf(books).size
                            ShortcutTile(
                                icon = Icons.Rounded.ViewWeek,
                                title = "Bookshelf",
                                subtitle = "$shelfCount ${if (shelfCount == 1) "book" else "books"}",
                                onClick = onOpenBookshelf,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val finishedCount = books.count { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
                            ShortcutTile(
                                icon = Icons.Rounded.EmojiEvents,
                                title = "Finished",
                                subtitle = "$finishedCount ${if (finishedCount == 1) "book" else "books"} read",
                                onClick = onOpenFinished,
                                modifier = Modifier.weight(1f)
                            )
                            ShortcutTile(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "Year in review",
                                subtitle = if (hasYearToReview) "Your $year so far"
                                else "Unlocks after ${AnalyticsEngine.MIN_DAYS_FOR_WRAPPED} reading days",
                                onClick = onOpenWrapped,
                                enabled = hasYearToReview,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            }
    }

    // The sky starts at the top of the screen, behind the header's slim bar.
    val skyTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + SECTION_BAR_HEIGHT
    val density = LocalDensity.current
    val skyGonePx = with(density) { (skyTop + SKY_HEIGHT).toPx() * 2f }
    val skyOnScreen by remember { derivedStateOf { scrollState.value < skyGonePx } }
    val homeSky = LocalAppPrefs.current.homeSky

    // What today looks like so far, in the words under the greeting.
    val goal = streak.dailyGoal
    val pagesToday = streak.pagesToday
    val todayLine = when {
        goal > 0 && pagesToday >= goal -> "Goal met · ${pages(pagesToday)} today"
        goal > 0 && pagesToday > 0 -> "${pages(goal - pagesToday)} to today's goal"
        goal > 0 -> "${pages(goal)} makes today count"
        pagesToday > 0 -> "${pages(pagesToday)} today"
        else -> null
    }

    Box(modifier = modifier.fillMaxSize()) {
    SectionPage(
        state = headerState,
        seeThrough = homeSky,
        backdrop = { collapsedPx ->
            // Behind the header and cards, drifting at half the scroll's speed and fading as it leaves.
            if (homeSky) TimeOfDaySky(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(skyTop + SKY_HEIGHT)
                    .graphicsLayer {
                        val moved = collapsedPx() + scrollState.value
                        translationY = -collapsedPx() - scrollState.value * 0.5f
                        alpha = 1f - (moved / skyGonePx).coerceIn(0f, 1f)
                    },
                bodyTop = skyTop + 24.dp,
                active = LocalHeroEnabled.current && skyOnScreen
            )
        },
        expanded = {
            Column {
                SectionTitle(greeting(userName), todayLine, Modifier.staggeredEntrance(entrance, 0))
                Text(
                    subtitle(readingBooks.isEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.staggeredEntrance(entrance, 1)
                )
            }
        },
        // Scrolled down, the greeting gives way to today at a glance: the streak and the day's goal.
        collapsed = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(onClick = onOpenStreak) {
                    LivingFlame(streak = streak.currentStreak, baseSize = 18.dp)
                    Text("${streak.currentStreak}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                if (goal > 0) {
                    StatusChip(onClick = { onEditGoal(GoalType.DAILY_PAGES) }) {
                        CircularProgressIndicator(
                            progress = { (pagesToday.toFloat() / goal).coerceIn(0f, 1f) },
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.5.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            gapSize = 0.dp
                        )
                        Text("$pagesToday / $goal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Search library")
            }
        }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 4.dp, bottom = 16.dp + LocalNavBarClearance.current),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = if (twoColumns) Modifier.fillMaxWidth() else Modifier.readableWidth()) { banner() }

        val sections = layout.visible.withIndex().toList()
        if (twoColumns) {
            // Alternate sections between the columns so neither runs much longer than the other.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    currentBooks()
                    sections.filter { it.index % 2 == 1 }.forEach { (i, section) -> key(section) { sectionContent(i, section) } }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    sections.filter { it.index % 2 == 0 }.forEach { (i, section) -> key(section) { sectionContent(i, section) } }
                }
            }
        } else {
            Column(modifier = Modifier.readableWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                currentBooks()
                sections.forEach { (i, section) -> key(section) { sectionContent(i, section) } }
            }
        }

        TextButton(
            onClick = onEditHome,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit home screen")
        }
    }
    }

        if (celebrating) {
            ConfettiExplosion(
                colors = EmberColors,
                origin = Offset(0.5f, 0.55f),
                particleCount = 60,
                style = BurstStyle.EMBERS,
                onFinished = { celebrating = false }
            )
        }
        AnimatedVisibility(
            visible = celebrating,
            enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { -it } + fadeIn(),
            exit = fadeOut(tween(Motion.MEDIUM)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = skyTop + 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Daily goal reached!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    bookForActions?.let { book ->
        BookQuickActionsSheet(
            book = book,
            onDismiss = { bookForActions = null },
            onOpen = { onOpenBook(book) },
            onStartReading = { onOpenTimer(book) },
            onAddNote = { bookToNote = book },
            onChangeStatus = { status -> onChangeStatus(book, status) }
        )
    }

    bookToNote?.let { book ->
        NoteSheet(
            suggestedPage = book.currentPage,
            totalPages = book.totalPages,
            pageLabel = FormatAdaptabilityLayer.getDisplayUnit(book).trimEnd('s'),
            draftKey = book.id,
            onDismiss = { bookToNote = null },
            onConfirm = { page, text, type, images ->
                onAddNote(book, page, text, type, images)
                bookToNote = null
            },
            onConfirmAndAddAnother = { page, text, type, images -> onAddNote(book, page, text, type, images) }
        )
    }

    bookToLog?.let { book ->
        ProgressPickerSheet(
            book = book,
            initialPage = book.currentPage,
            onDismiss = { bookToLog = null },
            onConfirm = { page ->
                if (page != book.currentPage) onLogPages(book, page - book.currentPage)
                bookToLog = null
            }
        )
    }
}

@Composable
private fun CurrentBookCard(
    book: Book,
    sessions: List<Session>,
    allSessions: List<Session>,
    sessionOpen: Boolean,
    onOpen: () -> Unit,
    onOpenTimer: () -> Unit,
    onLogPages: () -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR
) {
    val zone = remember { ZoneId.systemDefault() }
    val fraction = if (book.totalPages > 0) (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) else 0f
    // On a re-read, "Day N" counts from when this read began, not the first read's first session.
    val startedAt = remember(book, sessions) {
        val readStart = RereadMemories.currentReadStart(book, sessions)
        sessions.filter { it.startTime > 0 && (readStart == null || it.startTime >= readStart) }
            .minOfOrNull { it.startTime } ?: readStart
    }
    val dayLine = remember(startedAt, today) {
        startedAt?.let {
            val first = StreakEngine.readingDate(it, zone, dayStartsAtHour)
            val day = ChronoUnit.DAYS.between(first, today).toInt() + 1
            "Day $day · since ${first.format(DateTimeFormatter.ofPattern("MMM d"))}"
        }
    }
    val etaText = remember(book, sessions, allSessions) {
        AnalyticsEngine.formatEstimatedTime(AnalyticsEngine.bookEstimatedMinutesLeft(book, sessions, allSessions))
    }

    val press = remember { MutableInteractionSource() }
    val rail by animateFloatAsState(fraction, Motion.settle(), label = "rail")
    Surface(
        onClick = onOpen,
        shape = TileShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        interactionSource = press,
        modifier = modifier.fillMaxWidth().bounceOnPress(press)
    ) {
        val accent = rememberCoverAccent(book.coverUrl)
        Column(modifier = Modifier.coverGlow(accent, strength = 0.22f, fromLeft = true)) {
            // A thin progress rail along the top edge of every in-progress card.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rail)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Row(modifier = Modifier.padding(16.dp)) {
                // Grab the cover and fling it: right logs pages, left starts the timer.
                // Only the cover takes the drag, so swiping the card itself still pages between books.
                var drag by remember { mutableFloatStateOf(0f) }
                Box(modifier = Modifier.size(width = 88.dp, height = 132.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(AppShape.XS)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = (abs(drag) / 200f).coerceIn(0f, 1f)))
                    ) {
                        if (drag != 0f) {
                            Icon(
                                if (drag > 0) Icons.Rounded.Add else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .align(if (drag > 0) Alignment.CenterStart else Alignment.CenterEnd)
                                    .padding(horizontal = 8.dp)
                                    .alpha((abs(drag) / 150f).coerceIn(0f, 1f))
                            )
                        }
                    }
                    BookCover(
                        book,
                        Modifier
                            .heroSharedElement("cover-${book.id}")
                            .matchParentSize()
                            .swipeToAct(
                                onOffset = { drag = it },
                                onSwipeRight = onLogPages,
                                onSwipeLeft = onOpenTimer
                            )
                    )
                }
                Spacer(Modifier.width(16.dp))
                // At least the cover's height, but free to grow with large text.
                Column(modifier = Modifier.weight(1f).heightIn(min = 132.dp)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        dayLine ?: "Not started yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val byPercent = book.progressUnit == ProgressUnit.Percentage
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (byPercent) "${book.currentPage}%"
                            else "${FormatAdaptabilityLayer.unitAbbrev(book)} ${book.currentPage}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (book.totalPages > 0 && !byPercent) {
                            Text(
                                " / ${book.totalPages}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${(fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                    // A finish-by deadline says what today needs; otherwise the time left.
                    val planLine = remember(book, sessions, today, dayStartsAtHour) {
                        finishByLine(com.example.booktracker.app.analytics.FinishByPlanner.plan(book, sessions, today, zone, dayStartsAtHour))
                    }
                    if (planLine != null) {
                        Text(
                            planLine.first,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (planLine.second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else etaText?.takeIf { LocalAppPrefs.current.showTimeLeft }?.let {
                        Text(
                            "About $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenTimer,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(
                        if (sessionOpen) Icons.Rounded.Timer else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (sessionOpen) "Back to timer" else "Read", fontWeight = FontWeight.SemiBold)
                }
                // Labelled: a bare "+" next to Read was read as "add book".
                FilledTonalButton(
                    onClick = onLogPages,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pages", fontWeight = FontWeight.SemiBold)
                }
                FilledTonalIconButton(onClick = onAddNote, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = "Add a note")
                }
            }
        }
    }
}

@Composable
private fun NothingInProgressCard(
    hasBooks: Boolean,
    onAddBook: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tile(modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasBooks) "Nothing on the go" else "Start your library",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasBooks) "Pick a book from your library and press Read to start tracking."
            else "Add the book you're reading. Scan its barcode or search by title.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasBooks) {
                FilledTonalButton(onClick = onOpenLibrary) { Text("Choose a book") }
                OutlinedButton(onClick = onAddBook) { Text("Add book") }
            } else {
                FilledTonalButton(onClick = onAddBook) { Text("Add a book") }
            }
        }
    }
}

@Composable
private fun Tile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(18.dp)) { content() }
    }
    if (onClick != null) {
        val press = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            shape = TileShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            interactionSource = press,
            modifier = modifier.fillMaxWidth().bounceOnPress(press),
            content = body
        )
    } else {
        Surface(
            shape = TileShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier.fillMaxWidth(),
            content = body
        )
    }
}

@Composable
private fun ShortcutTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val press = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = TileShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        interactionSource = press,
        modifier = modifier.alpha(if (enabled) 1f else 0.6f).bounceOnPress(press)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Tile title with a small progress ring and its percentage on the right. */
@Composable
private fun RingHeader(title: String, fraction: Float) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = MaterialTheme.colorScheme.primary
    // Fills from empty on first sight, then springs to each new value with a little overshoot.
    val reduce = rememberReduceMotion()
    val ring = remember { Animatable(if (reduce) fraction else 0f) }
    LaunchedEffect(fraction) { ring.animateTo(fraction, Motion.settle()) }
    // A one-time pop when the ring closes.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(fraction >= 1f) {
        if (fraction >= 1f && !reduce) {
            pop.animateTo(1.15f, tween(Motion.SHORT))
            pop.animateTo(1f, Motion.press())
        }
    }
    val shownPercent by animateCountUp((fraction * 100).toInt())
    // The ring grows with the font scale so "140%" still fits inside it.
    val ringSize = 48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.size(ringSize).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (ring.value > 0f) {
                    drawArc(
                        fill, -90f, 360f * ring.value.coerceIn(0f, 1f), false, topLeft, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                "$shownPercent%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Twelve thin bars, one per month. Rounded tops sit on a shared baseline; the
 * current month is full strength and the others recede.
 */
@Composable
private fun MonthBars(pages: IntArray, currentMonth: Int) {
    val max = pages.maxOrNull()?.coerceAtLeast(1) ?: 1
    val strong = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val slot = size.width / 12f
            val barWidth = (slot - 2.dp.toPx() * 2).coerceAtMost(14.dp.toPx())
            val radius = 4.dp.toPx()
            pages.forEachIndexed { i, value ->
                val left = slot * i + (slot - barWidth) / 2f
                // Months with no reading still get a stub, so the year reads as a row.
                val h = if (value > 0) (size.height * value / max).coerceAtLeast(radius * 2) else 3.dp.toPx()
                drawRoundRect(
                    color = when {
                        value == 0 -> empty
                        i + 1 == currentMonth -> strong
                        else -> soft
                    },
                    topLeft = Offset(left, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            "JFMAMJJASOND".forEachIndexed { i, letter ->
                Text(
                    letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i + 1 == currentMonth) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i + 1 == currentMonth) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

internal fun greeting(userName: String): String {
    val first = userName.trim().split(" ").firstOrNull().orEmpty()
    if (first.isNotEmpty() && !first.equals("Reader", ignoreCase = true)) return "Hi, $first"
    val hour = LocalTime.now().hour
    return when {
        hour < 5 -> "Reading late?"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}

/** A line under the greeting that knows what time it is. */
private fun subtitle(nothingInProgress: Boolean): String {
    val hour = LocalTime.now().hour
    return when {
        nothingInProgress -> "What will you read next?"
        hour >= 22 || hour < 5 -> "The best pages are read after dark."
        hour < 9 -> "A few pages before the day begins?"
        hour >= 18 -> "Time to unwind with a chapter?"
        else -> "What will you read today?"
    }
}

private fun formatMinutes(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"

/** "On track", "2 ahead of pace" or "3 behind pace", from the day tracking began. */
private fun yearPace(finished: Int, goal: Int, today: LocalDate, trackingStart: LocalDate?): String {
    if (goal <= 0) return "Tap to set a goal"
    if (finished >= goal) return "Goal reached"
    val pace = GoalEngine.calculateYearlyPace(goal, finished, today, trackingStart)
    val diff = pace.difference.roundToInt().absoluteValue.coerceAtLeast(1)
    return when (pace.status) {
        GoalEngine.GoalPaceStatus.AHEAD -> "$diff ahead of pace"
        GoalEngine.GoalPaceStatus.BEHIND -> "$diff behind pace"
        GoalEngine.GoalPaceStatus.ON_TRACK -> "On track"
    }
}

private fun pages(n: Int) = if (n == 1) "1 page" else "$n pages"

/** A tappable capsule in Home's slim bar. */
@Composable
private fun StatusChip(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        content = content
    )
}
