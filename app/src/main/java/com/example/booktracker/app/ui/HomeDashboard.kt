package com.example.booktracker.app.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.GoalEngine
import com.example.booktracker.app.analytics.StreakEngine
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

private val TileShape = RoundedCornerShape(24.dp)

/**
 * Home: everything about your reading on one scroll. The books in progress sit
 * on top as a swipeable carousel; streak, today, the year and the to-read queue
 * follow as tiles, each opening the screen with the detail.
 */
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
    scrollState: ScrollState = rememberScrollState(),
    banner: @Composable () -> Unit = {}
) {
    val zone = remember { ZoneId.systemDefault() }
    val year = streak.today.year
    var bookToLog by remember { mutableStateOf<Book?>(null) }
    var bookToNote by remember { mutableStateOf<Book?>(null) }

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
        books.filter { b -> order.any { it.equals(b.status, ignoreCase = true) } }
            .sortedWith(compareBy<Book> { b -> order.indexOfFirst { it.equals(b.status, ignoreCase = true) } }
                .thenByDescending { it.lastUpdated })
            .take(12)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
            Text(
                greeting(userName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (readingBooks.isEmpty()) "What will you read next?" else "What will you read today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        banner()

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

        // Reader-ordered sections; the greeting and current book above stay fixed.
        for (section in layout.visible) key(section) {
            when (section) {
                HomeSection.STREAK -> {
                    // Streak and this week.
                    Tile(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = if (streak.currentStreak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    when (streak.currentStreak) {
                                        0 -> "No streak yet"
                                        1 -> "1-day streak"
                                        else -> "${streak.currentStreak}-day streak"
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
                            Text(
                                "${streak.pagesToday} of ${streak.dailyGoal} pages",
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
                            Text(
                                "${finishedThisYear.size} of $yearlyGoal books",
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
                                    Column(
                                        modifier = Modifier
                                            .width(84.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onOpenBook(book) }
                                    ) {
                                        BookCover(book.coverUrl, Modifier.size(width = 84.dp, height = 126.dp))
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
                                icon = Icons.Filled.CalendarMonth,
                                title = "Calendar",
                                subtitle = if (thisMonthPages > 0) "$thisMonthPages pages this month" else "Your reading by day",
                                onClick = onOpenCalendar,
                                modifier = Modifier.weight(1f)
                            )
                            ShortcutTile(
                                icon = Icons.Filled.ViewWeek,
                                title = "Bookshelf",
                                subtitle = "${books.size} ${if (books.size == 1) "book" else "books"}",
                                onClick = onOpenBookshelf,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val finishedCount = books.count { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
                            ShortcutTile(
                                icon = Icons.Filled.EmojiEvents,
                                title = "Finished",
                                subtitle = "$finishedCount ${if (finishedCount == 1) "book" else "books"} read",
                                onClick = onOpenFinished,
                                modifier = Modifier.weight(1f)
                            )
                            ShortcutTile(
                                icon = Icons.Filled.AutoAwesome,
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

        TextButton(
            onClick = onEditHome,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit home screen")
        }
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
        CustomProgressDialog(
            unitName = if (book.totalPages > 0) "Pages" else "Units",
            onDismiss = { bookToLog = null },
            onConfirm = { delta ->
                onLogPages(book, delta)
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
    onAddNote: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val fraction = if (book.totalPages > 0) (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) else 0f
    val startedAt = remember(sessions) { sessions.filter { it.startTime > 0 }.minOfOrNull { it.startTime } }
    val dayLine = remember(startedAt) {
        startedAt?.let {
            val first = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            val day = ChronoUnit.DAYS.between(first, LocalDate.now(zone)).toInt() + 1
            "Day $day · since ${first.format(DateTimeFormatter.ofPattern("MMM d"))}"
        }
    }
    val etaText = remember(book, sessions, allSessions) {
        AnalyticsEngine.formatEstimatedTime(AnalyticsEngine.bookEstimatedMinutesLeft(book, sessions, allSessions))
    }

    Surface(
        onClick = onOpen,
        shape = TileShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // A thin progress rail along the top edge of every in-progress card.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Row(modifier = Modifier.padding(16.dp)) {
                BookCover(
                    book.coverUrl,
                    Modifier
                        .heroSharedElement("cover-${book.id}")
                        .size(width = 88.dp, height = 132.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f).height(132.dp)) {
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
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "p. ${book.currentPage}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (book.totalPages > 0) {
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
                    etaText?.let {
                        Text(
                            "About $it left",
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
                        if (sessionOpen) Icons.Filled.Timer else Icons.Filled.PlayArrow,
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
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pages", fontWeight = FontWeight.SemiBold)
                }
                FilledTonalIconButton(onClick = onAddNote, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add a note")
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
            Icons.AutoMirrored.Filled.MenuBook,
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
        Surface(
            onClick = onClick,
            shape = TileShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier.fillMaxWidth(),
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
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = TileShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.alpha(if (enabled) 1f else 0.6f)
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (fraction > 0f) {
                    drawArc(
                        fill, -90f, 360f * fraction.coerceAtMost(1f), false, topLeft, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                "${(fraction * 100).toInt()}%",
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

private fun greeting(userName: String): String {
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
