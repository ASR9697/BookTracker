package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.analytics.WrappedEngine
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import kotlin.math.roundToInt

private enum class Slide { INTRO, VOLUME, TIME, TOP_BOOK, TASTE, RHYTHM, QUOTE, SUMMARY }

private const val MAX_STACKED_COVERS = 6

/** True for the slide on screen, so its numbers roll up as you arrive rather than off-screen. */
private val LocalSlideActive = compositionLocalOf { true }

/**
 * Wrapped as a story: one idea per slide, tap the right side (or swipe) to go
 * on, and share whichever slide is showing. Slides with nothing to say are skipped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(
    books: List<Book>,
    sessions: List<Session>,
    notes: List<MarginNote> = emptyList(),
    userName: String = "Reader",
    dayStartsAtHour: Int = StreakEngine.DEFAULT_DAY_START_HOUR,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isYearly by remember { mutableStateOf(true) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(dayStartsAtHour) { StreakEngine.readingDate(System.currentTimeMillis(), zone, dayStartsAtHour) }

    val summary = remember(books, sessions, notes, isYearly, today) {
        WrappedEngine.summarize(books, sessions, notes, isYearly, today, zone, dayStartsAtHour)
    }
    val slides = remember(summary) {
        buildList {
            add(Slide.INTRO)
            if (!summary.isEmpty) {
                if (summary.booksFinished.isNotEmpty() || summary.pagesRead > 0) add(Slide.VOLUME)
                if (summary.minutesRead > 0 || summary.activeDays > 0) add(Slide.TIME)
                if (summary.topBook != null) add(Slide.TOP_BOOK)
                if (summary.topAuthor != null || summary.topGenre != null) add(Slide.TASTE)
                if (summary.busiest != null) add(Slide.RHYTHM)
                if (summary.quote != null) add(Slide.QUOTE)
                add(Slide.SUMMARY)
            }
        }
    }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    LaunchedEffect(isYearly) { pagerState.scrollToPage(0) }
    val graphicsLayer = rememberGraphicsLayer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Wrapped") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(end = 8.dp)) {
                        TickFilterChip(selected = !isYearly, onClick = { isYearly = false }, label = { Text("Month") })
                        TickFilterChip(selected = isYearly, onClick = { isYearly = true }, label = { Text("Year") })
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Story progress: one segment per slide.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                slides.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index <= pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                val recording = page == pagerState.currentPage
                SlideFrame(
                    slide = slides[page],
                    active = recording,
                    layer = graphicsLayer.takeIf { recording },
                    onTapSide = { forward ->
                        val target = (pagerState.currentPage + if (forward) 1 else -1).coerceIn(0, slides.lastIndex)
                        scope.launch { pagerState.animateScrollToPage(target) }
                    }
                ) {
                    when (slides[page]) {
                        Slide.INTRO -> IntroSlide(summary, userName)
                        Slide.VOLUME -> VolumeSlide(summary)
                        Slide.TIME -> TimeSlide(summary)
                        Slide.TOP_BOOK -> TopBookSlide(summary)
                        Slide.TASTE -> TasteSlide(summary)
                        Slide.RHYTHM -> RhythmSlide(summary)
                        Slide.QUOTE -> QuoteSlide(summary)
                        Slide.SUMMARY -> SummarySlide(summary)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    "${pagerState.currentPage + 1} / ${slides.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    scope.launch { shareLayerImage(context, graphicsLayer, "wrapped.png", "Share your wrapped") }
                }) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share this slide")
                }
            }
        }
    }
}

/** Shares whatever [layer] last recorded as a PNG through the app's file provider. */
internal suspend fun shareLayerImage(
    context: android.content.Context,
    layer: GraphicsLayer,
    fileName: String,
    chooserTitle: String
) {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    val imageFile = File(context.cacheDir, fileName)
    FileOutputStream(imageFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/** Container colour and its matching text colour, so every slide stays readable in both themes. */
@Composable
private fun slideColors(slide: Slide): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (slide) {
        Slide.INTRO, Slide.SUMMARY -> scheme.primaryContainer to scheme.onPrimaryContainer
        Slide.VOLUME, Slide.TASTE -> scheme.secondaryContainer to scheme.onSecondaryContainer
        Slide.TIME, Slide.QUOTE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        Slide.TOP_BOOK, Slide.RHYTHM -> scheme.surfaceContainerHigh to scheme.onSurface
    }
}

@Composable
private fun SlideFrame(
    slide: Slide,
    active: Boolean,
    layer: GraphicsLayer?,
    onTapSide: (forward: Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val (container, onContainer) = slideColors(slide)
    // A story is 9:16. On a tablet the slide keeps that shape in the middle instead
    // of stretching into a landscape banner (and the shared image keeps it too).
    val storyShaped = LocalWidthTier.current.atLeastMedium
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Box(
        modifier = Modifier
            .then(
                if (storyShaped) Modifier.fillMaxHeight().aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                else Modifier.fillMaxSize()
            )
            .drawWithContent {
                if (layer != null) {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                } else {
                    drawContent()
                }
            }
            .clip(AppShape.XL)
            // Opaque at both ends, so a shared PNG has no see-through corners.
            .background(Brush.verticalGradient(listOf(container, lerp(container, MaterialTheme.colorScheme.surface, 0.35f))))
            .pointerInput(Unit) {
                detectTapGestures { offset -> onTapSide(offset.x > size.width / 3f) }
            }
    ) {
        CompositionLocalProvider(LocalContentColor provides onContainer, LocalSlideActive provides active) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                content = content
            )
            Text(
                "Nocturnal Reader",
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            )
        }
    }
    }
}

@Composable
private fun Kicker(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = LocalContentColor.current.copy(alpha = 0.75f)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Hero(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Black,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/** A [Hero] number that counts up the first time its slide is shown. */
@Composable
private fun HeroCount(value: Int, format: (Int) -> String = { "%,d".format(it) }) {
    val active = LocalSlideActive.current
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(active) { if (active) arrived = true }
    val shown by animateCountUp(if (arrived) value else 0, durationMillis = 1200)
    Hero(format(shown))
}

@Composable
private fun IntroSlide(summary: WrappedEngine.Summary, userName: String) {
    Kicker(if (summary.isYear) "Your year in books" else "Your month in books")
    Text(summary.periodLabel, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(16.dp))
    Text(
        if (summary.isEmpty) "Not much reading logged yet, $userName. Come back after a few sessions."
        else "$userName, here's how your ${if (summary.isYear) "year" else "month"} of reading went.",
        style = MaterialTheme.typography.titleMedium
    )
    if (!summary.isEmpty) {
        Spacer(Modifier.height(32.dp))
        Text("Tap to continue →", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.7f))
    }
}

@Composable
private fun VolumeSlide(summary: WrappedEngine.Summary) {
    val finished = summary.booksFinished
    if (finished.isNotEmpty()) {
        Kicker("You finished")
        HeroCount(finished.size)
        Text(if (finished.size == 1) "book" else "books", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.height(96.dp)) {
            finished.takeLast(MAX_STACKED_COVERS).forEachIndexed { index, book ->
                BookCover(
                    book,
                    Modifier.padding(start = (index * 36).dp).size(width = 64.dp, height = 96.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("and turned ${"%,d".format(summary.pagesRead)} pages", style = MaterialTheme.typography.titleLarge)
    } else {
        Kicker("You read")
        HeroCount(summary.pagesRead)
        Text("pages", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun TimeSlide(summary: WrappedEngine.Summary) {
    Kicker("You spent")
    HeroCount(summary.minutesRead.toInt()) { AnalyticsEngine.formatMinutes(it.toLong()) }
    Text("reading", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(32.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        MiniStat(summary.activeDays.toString(), if (summary.activeDays == 1) "day with reading" else "days with reading")
        MiniStat(summary.longestStreak.toString(), "day best streak")
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.8f))
    }
}

@Composable
private fun TopBookSlide(summary: WrappedEngine.Summary) {
    val book = summary.topBook ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Kicker(if (summary.isYear) "Book of the year" else "Book of the month")
        Spacer(Modifier.height(8.dp))
        BookCover(book, Modifier.size(width = 140.dp, height = 210.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (book.authors.isNotEmpty()) {
            Text(book.authors.joinToString(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        book.overallRating?.let { rating ->
            Spacer(Modifier.height(12.dp))
            val stars = rating.roundToInt().coerceIn(0, 5)
            Text("★".repeat(stars) + "☆".repeat(5 - stars), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun TasteSlide(summary: WrappedEngine.Summary) {
    summary.topAuthor?.let { author ->
        Kicker("Your top author")
        Text(author.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, maxLines = 2)
        Text(
            buildString {
                append(if (author.books == 1) "1 book" else "${author.books} books")
                if (author.pages > 0) append(" · ${"%,d".format(author.pages)} pages")
            },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(32.dp))
    }
    summary.topGenre?.let {
        Kicker("Your top genre")
        Text(it, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, maxLines = 2)
        Spacer(Modifier.height(32.dp))
    }
    summary.topVibe?.let {
        Kicker("Favourite place to read")
        Text(it, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RhythmSlide(summary: WrappedEngine.Summary) {
    val busiest = summary.busiest ?: return
    val monthName = summary.periodLabel.substringBefore(' ')
    Kicker(if (summary.isYear) "Your busiest month" else "Your best day")
    Text(
        if (summary.isYear) busiest.label else "$monthName ${busiest.label}",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Black
    )
    Text("${"%,d".format(busiest.pages)} pages", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(28.dp))
    PagesBarChart(summary.buckets, busiest, labelEvery = summary.isYear)
}

/**
 * One series, one hue: every bar in the slide's text colour, faded, with the
 * busiest bar at full strength and labelled directly, so no legend is needed.
 */
@Composable
private fun PagesBarChart(buckets: List<WrappedEngine.Bucket>, busiest: WrappedEngine.Bucket, labelEvery: Boolean) {
    val ink = LocalContentColor.current
    val max = buckets.maxOf { it.pages }.coerceAtLeast(1)
    val chartHeight = 140.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(chartHeight + 20.dp)
    ) {
        buckets.forEach { bucket ->
            val isPeak = bucket == busiest
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                if (isPeak) {
                    Text(
                        bucket.pages.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight * (bucket.pages.toFloat() / max))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (isPeak) ink else ink.copy(alpha = 0.3f))
                )
            }
        }
    }
    if (labelEvery) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            buckets.forEach { bucket ->
                Text(
                    bucket.label.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = if (bucket == busiest) 1f else 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuoteSlide(summary: WrappedEngine.Summary) {
    val quote = summary.quote ?: return
    Kicker(if (summary.isYear) "Quote of the year" else "Quote of the month")
    Text("“", style = MaterialTheme.typography.displayLarge, fontFamily = FontFamily.Serif)
    Text(
        quote.content.trim(),
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic
    )
    summary.quoteBook?.let { book ->
        Spacer(Modifier.height(20.dp))
        Text(
            "— ${book.title}" + if (quote.pageNumber > 0) ", p. ${quote.pageNumber}" else "",
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummarySlide(summary: WrappedEngine.Summary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (summary.isYear) "My Reading Year" else "My Reading Month",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(summary.periodLabel, style = MaterialTheme.typography.titleMedium, color = LocalContentColor.current.copy(alpha = 0.75f))
        Spacer(Modifier.height(24.dp))
        val cells = buildList {
            add(summary.booksFinished.size.toString() to "Books finished")
            add("%,d".format(summary.pagesRead) to "Pages read")
            add((summary.minutesRead / 60).toString() to "Hours spent")
            if (summary.longestStreak > 0) add("${summary.longestStreak}d" to "Longest streak")
            summary.topGenre?.let { add(it to "Top genre") }
            summary.topBook?.let { add(it.title to "Top book") }
        }
        cells.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                row.forEach { (value, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
