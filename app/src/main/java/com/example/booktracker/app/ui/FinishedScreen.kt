package com.example.booktracker.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.FinishedShelf
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.staggeredEntrance
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private enum class ShelfTab(val label: String) { FINISHED("Finished"), SET_ASIDE("Did not finish") }

/**
 * Every ended read: a summary of the shelf on top, then the books month by month.
 * Finished books and set-aside ones sit on separate tabs, and the year chips
 * narrow both the summary and the list.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinishedScreen(
    books: List<Book>,
    sessions: List<Session> = emptyList(),
    notes: List<MarginNote> = emptyList(),
    onBack: () -> Unit,
    onOpenBook: (Book) -> Unit = {},
    onOpenJourneyMap: () -> Unit = {}
) {
    val zone = remember { ZoneId.systemDefault() }
    var bookForVictoryCard by remember { mutableStateOf<Book?>(null) }
    var tab by rememberSaveable { mutableStateOf(ShelfTab.FINISHED) }
    var year by rememberSaveable { mutableStateOf<Int?>(null) }

    val finished = remember(books, sessions) { FinishedShelf.reads(books, sessions, zone, FinishedShelf::isFinished) }
    val setAside = remember(books, sessions) { FinishedShelf.reads(books, sessions, zone, FinishedShelf::isAbandoned) }
    val onTab = if (tab == ShelfTab.FINISHED) finished else setAside
    val years = remember(onTab) { FinishedShelf.years(onTab, zone) }
    val shown = remember(onTab, year) { year?.let { FinishedShelf.inYear(onTab, it, zone) } ?: onTab }
    val months = remember(shown) { FinishedShelf.byMonth(shown, zone) }
    val summary = remember(shown) { FinishedShelf.summary(shown) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Finished books") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        if (finished.isEmpty() && setAside.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.EmojiEvents,
                title = "Your trophy shelf is waiting",
                message = "Finish or set aside a book and it will show here.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val listEntrance = rememberEntranceState()
            val sidePadding = 16.dp + windowCentredPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary") {
                    ShelfSummaryCard(
                        summary = summary,
                        tab = tab,
                        year = year,
                        onOpenJourney = onOpenJourneyMap,
                        modifier = Modifier
                            .padding(horizontal = sidePadding)
                            .staggeredEntrance(listEntrance, 0)
                    )
                }
                item(key = "filters") {
                    Column(
                        modifier = Modifier.staggeredEntrance(listEntrance, 1),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = sidePadding)
                        ) {
                            ShelfTab.entries.forEachIndexed { i, entry ->
                                val count = if (entry == ShelfTab.FINISHED) finished.size else setAside.size
                                SegmentedButton(
                                    selected = tab == entry,
                                    onClick = {
                                        tab = entry
                                        year = null
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(i, ShelfTab.entries.size)
                                ) { Text("${entry.label} ($count)") }
                            }
                        }
                        if (years.size > 1) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = sidePadding),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    TickFilterChip(selected = year == null, onClick = { year = null }, label = { Text("All time") })
                                }
                                items(years) { y ->
                                    TickFilterChip(selected = year == y, onClick = { year = y }, label = { Text("$y") })
                                }
                            }
                        }
                    }
                }
                if (shown.isEmpty()) {
                    item(key = "none") {
                        Text(
                            if (tab == ShelfTab.FINISHED) "No finished books here yet." else "Nothing set aside. Every book you started, you finished.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = sidePadding, vertical = 24.dp)
                        )
                    }
                }
                var index = 2
                months.forEach { (month, reads) ->
                    stickyHeader(key = "${tab.name}-$month") {
                        Text(
                            month.format(MonthHeader),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = sidePadding + 4.dp, vertical = 6.dp)
                        )
                    }
                    itemsIndexed(reads, key = { _, read -> "${tab.name}-${read.book.id}" }) { _, read ->
                        val modifier = Modifier
                            .padding(horizontal = sidePadding)
                            .animateItem()
                            .staggeredEntrance(listEntrance, index)
                        if (read.finished) {
                            FinishedRow(read, zone, onClick = { onOpenBook(read.book) }, onShare = { bookForVictoryCard = read.book }, modifier = modifier)
                        } else {
                            SetAsideRow(read, zone, onClick = { onOpenBook(read.book) }, modifier = modifier)
                        }
                    }
                    index += reads.size
                }
            }
        }
    }

    bookForVictoryCard?.let { victoryBook ->
        BookVictoryCardDialog(
            book = victoryBook,
            sessions = sessions,
            notes = notes,
            onDismiss = { bookForVictoryCard = null }
        )
    }
}

private val MonthHeader = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val DayLabel = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

@Composable
private fun ShelfSummaryCard(
    summary: FinishedShelf.Summary,
    tab: ShelfTab,
    year: Int?,
    onOpenJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer.copy(alpha = 0.7f))))
                .padding(18.dp)
        ) {
            Text(
                when {
                    tab == ShelfTab.SET_ASIDE -> "Set aside ${year?.let { "in $it" } ?: "so far"}"
                    year != null -> "Your $year shelf"
                    else -> "Your shelf so far"
                },
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(summary.books, if (summary.books == 1) "book" else "books", Modifier.weight(1f))
                SummaryStat(summary.pages, "pages", Modifier.weight(1f))
                SummaryStat(((summary.minutes + 30) / 60), "hours", Modifier.weight(1f))
            }
            if (tab == ShelfTab.FINISHED) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val average = summary.averageRating
                    if (average != null) {
                        StarRatingDisplay(average, starSize = 16.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("%.1f average".format(average), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = onOpenJourney, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                        Icon(Icons.Filled.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Journey")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: Int, label: String, modifier: Modifier = Modifier) {
    val shown by animateCountUp(value)
    Column(modifier = modifier) {
        Text("%,d".format(shown), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ShelfRowCard(onClick: () -> Unit, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun ShelfCover(book: Book, dimmed: Boolean = false) {
    val shape = MaterialTheme.shapes.small
    BookCover(
        book.coverUrl,
        Modifier
            .heroSharedElement("cover-${book.id}")
            .size(width = 60.dp, height = 90.dp)
            .shadow(6.dp, shape)
            .graphicsLayer { alpha = if (dimmed) 0.6f else 1f }
    )
}

@Composable
private fun FinishedRow(read: FinishedShelf.Read, zone: ZoneId, onClick: () -> Unit, onShare: () -> Unit, modifier: Modifier) {
    val book = read.book
    ShelfRowCard(onClick, modifier) {
        ShelfCover(book)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    "Finished ${dayLabel(read.endedAt, zone)}",
                    read.days?.let { if (it == 1) "in a day" else "$it days" },
                    read.minutes.takeIf { it > 0 }?.let { AnalyticsEngine.formatMinutes(it.toLong()) }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!book.review.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "“${book.review}”",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val rating = book.overallRating
                if (rating != null) {
                    StarRatingDisplay(rating, starSize = 15.dp)
                } else {
                    Text("Not rated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share card", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SetAsideRow(read: FinishedShelf.Read, zone: ZoneId, onClick: () -> Unit, modifier: Modifier) {
    val book = read.book
    val percent = book.dnfData?.abandonedPercentage
        ?: if (book.totalPages > 0) book.currentPage * 100f / book.totalPages else 0f
    ShelfRowCard(onClick, modifier) {
        ShelfCover(book, dimmed = true)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    "Set aside ${dayLabel(read.endedAt, zone)}",
                    read.pages.takeIf { it > 0 }?.let { "at p. $it" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val reason = book.dnfData?.reason
            if (!reason.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    reason,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.align(Alignment.CenterVertically).size(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 4.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text("${percent.roundToInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun dayLabel(millis: Long, zone: ZoneId): String = Instant.ofEpochMilli(millis).atZone(zone).format(DayLabel)
