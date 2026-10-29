package com.example.booktracker.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.FinishedShelf
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Every fifth finished book gets a numbered marker on the path. */
private const val MILESTONE_EVERY = 5

private val CoverWidth = 104.dp
private val CoverHeight = 156.dp
private val RowHeight = CoverHeight + 36.dp
private val EdgePadding = 24.dp

/** One stop on the path, newest first; [number] counts finished books from the very first. */
private sealed interface Stop {
    data class Now(val count: Int) : Stop
    data class Year(val year: Int) : Stop
    data class Finished(val read: FinishedShelf.Read, val number: Int, val coverOnRight: Boolean) : Stop
    data class Start(val read: FinishedShelf.Read) : Stop
}

/**
 * Finished books as a winding path, newest at the top. Each row draws its own
 * stretch of the path, so the line always meets the covers; a stretch traces
 * itself in as it first scrolls into view, and the cover slides in from its side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingJourneyMap(
    books: List<Book>,
    onBack: () -> Unit,
    onOpenBook: (Book) -> Unit,
    sessions: List<Session> = emptyList()
) {
    val zone = remember { ZoneId.systemDefault() }
    val reads = remember(books, sessions) { FinishedShelf.reads(books, sessions, zone, FinishedShelf::isFinished) }
    val stops = remember(reads) { journeyStops(reads, zone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Reading journey", fontWeight = FontWeight.Bold)
                        reads.lastOrNull()?.let { first ->
                            Text(
                                "${reads.size} ${if (reads.size == 1) "book" else "books"} since ${monthLabel(first.endedAt, zone)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (reads.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Timeline,
                title = "Your journey starts with one book",
                message = "Finish a book and it becomes the first stop on your path.",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = windowCentredPadding(), end = windowCentredPadding(), top = 8.dp, bottom = 48.dp)
        ) {
            itemsIndexed(stops, key = { _, stop -> stopKey(stop) }) { _, stop ->
                when (stop) {
                    is Stop.Now -> NowStop(stop.count)
                    is Stop.Year -> YearStop(stop.year)
                    is Stop.Finished -> BookStop(stop, zone, onOpen = { onOpenBook(stop.read.book) })
                    is Stop.Start -> StartStop(stop.read, zone)
                }
            }
        }
    }
}

private fun journeyStops(reads: List<FinishedShelf.Read>, zone: ZoneId): List<Stop> {
    if (reads.isEmpty()) return emptyList()
    val stops = mutableListOf<Stop>(Stop.Now(reads.size))
    var shownYear: Int? = null
    reads.forEachIndexed { i, read ->
        // A banner above each year's books, newest year first.
        val year = yearOf(read.endedAt, zone)
        if (year != shownYear) {
            stops += Stop.Year(year)
            shownYear = year
        }
        stops += Stop.Finished(read, number = reads.size - i, coverOnRight = i % 2 == 0)
    }
    stops += Stop.Start(reads.last())
    return stops
}

private fun stopKey(stop: Stop): String = when (stop) {
    is Stop.Now -> "now"
    is Stop.Year -> "year-${stop.year}"
    is Stop.Finished -> "book-${stop.read.book.id}"
    is Stop.Start -> "start"
}

private fun yearOf(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).year

private val MonthYear = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
private val DayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private fun monthLabel(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).format(MonthYear)

/**
 * 0 -> 1 the first time a row appears, remembered across scrolling so a row
 * scrolled back into view doesn't trace itself again.
 */
@Composable
private fun rememberTrace(delayMillis: Int = 0, durationMillis: Int = 700): Animatable<Float, *> {
    val reduce = rememberReduceMotion()
    var traced by rememberSaveable { mutableStateOf(reduce) }
    val progress = remember { Animatable(if (traced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!traced) {
            kotlinx.coroutines.delay(delayMillis.toLong())
            progress.animateTo(1f, tween(durationMillis, easing = Motion.EmphasizedDecelerate))
            traced = true
        }
    }
    return progress
}

/**
 * Draws this row's stretch of path from the top centre to the bottom centre,
 * bending toward [bendTo] (a share of the width; 0.5 is straight). The travelled
 * part is the primary colour over a faint track, and grows with [progress].
 */
private fun Modifier.pathStretch(bendTo: Float, progress: () -> Float, track: Color, travelled: Color, width: Dp): Modifier =
    drawWithCache {
        val stroke = width.toPx()
        val mid = size.width / 2f
        val bend = size.width * bendTo
        val path = Path().apply {
            moveTo(mid, 0f)
            cubicTo(bend, size.height * 0.25f, bend, size.height * 0.75f, mid, size.height)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        onDrawBehind {
            drawPath(path, track, style = Stroke(stroke, cap = StrokeCap.Round))
            val p = progress()
            if (p > 0f) {
                partial.reset()
                measure.getSegment(0f, measure.length * p, partial, true)
                drawPath(partial, travelled, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }

@Composable
private fun trackColor() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

@Composable
private fun travelledColor() = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)

@Composable
private fun BookStop(stop: Stop.Finished, zone: ZoneId, onOpen: () -> Unit) {
    val density = LocalDensity.current
    val trace = rememberTrace()
    val cardIn = rememberTrace(delayMillis = 120, durationMillis = Motion.LONG)
    val milestone = stop.number % MILESTONE_EVERY == 0
    val read = stop.read
    val book = read.book

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(RowHeight)) {
        val w = maxWidth
        // The path bends toward the cover so its marker sits just inside it.
        val coverInner = if (stop.coverOnRight) w - EdgePadding - CoverWidth else EdgePadding + CoverWidth
        val apex = if (stop.coverOnRight) coverInner - 22.dp else coverInner + 22.dp
        // A cubic through (mid, c, c, mid) peaks three quarters of the way to its control points.
        val bendTo = ((w / 2) + (apex - w / 2) / 0.75f) / w
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pathStretch(bendTo, { trace.value }, trackColor(), travelledColor(), 6.dp)
        )

        // Marker on the path at the bend.
        val markerSize = if (milestone) 30.dp else 14.dp
        Box(
            modifier = Modifier
                .offset(x = apex - markerSize / 2, y = RowHeight / 2 - markerSize / 2)
                .size(markerSize)
                .graphicsLayer {
                    val s = ((trace.value - 0.4f) / 0.3f).coerceIn(0f, 1f)
                    scaleX = s
                    scaleY = s
                }
        ) {
            if (milestone) MilestoneMarker(stop.number) else Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize()) {}
        }

        // The cover, sliding in from its own side.
        val slide = with(density) { 40.dp.toPx() } * if (stop.coverOnRight) 1f else -1f
        Box(
            modifier = Modifier
                .align(if (stop.coverOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = EdgePadding)
                .graphicsLayer {
                    alpha = cardIn.value
                    translationX = (1f - cardIn.value) * slide
                }
        ) {
            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(CoverWidth, CoverHeight)
                    .shadow(8.dp, shape)
                    .bounceClick(onClick = onOpen)
            ) {
                BookCover(book.coverUrl, Modifier.heroSharedElement("cover-${book.id}").fillMaxSize())
                if (book.coverUrl.isBlank()) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(10.dp)
                    )
                }
            }
        }

        // When and what, on the open side of the path.
        val labelWidth = if (stop.coverOnRight) apex - EdgePadding - 22.dp else w - apex - EdgePadding - 22.dp
        Column(
            modifier = Modifier
                .align(if (stop.coverOnRight) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = EdgePadding)
                .width(labelWidth.coerceAtLeast(80.dp))
                .graphicsLayer { alpha = cardIn.value },
            horizontalAlignment = if (stop.coverOnRight) Alignment.End else Alignment.Start
        ) {
            val align = if (stop.coverOnRight) TextAlign.End else TextAlign.Start
            Text(
                if (milestone) "${FinishedShelf.ordinal(stop.number)} book" else Instant.ofEpochMilli(read.endedAt).atZone(zone).format(DayMonth),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = align
            )
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = align
            )
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = align
                )
            }
            val detail = listOfNotNull(
                read.days?.let { if (it == 1) "1 day" else "$it days" },
                book.overallRating?.let { "★ %.1f".format(it) }
            ).joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = align)
            }
        }
    }
}

/** A numbered badge with one soft pulse of its halo, held still when motion is reduced. */
@Composable
private fun MilestoneMarker(number: Int) {
    val reduce = rememberReduceMotion()
    val pulse = rememberInfiniteTransition(label = "milestone")
    val halo by pulse.animateFloat(1f, 1.45f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "halo")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (reduce) 1.25f else halo
                    scaleX = s
                    scaleY = s
                }
        ) {}
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Text("$number", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** A straight stretch with something pinned to its middle. */
@Composable
private fun StraightStop(height: Dp, content: @Composable BoxScope.() -> Unit) {
    val trace = rememberTrace(durationMillis = Motion.MEDIUM)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .pathStretch(0.5f, { trace.value }, trackColor(), travelledColor(), 6.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun NowStop(count: Int) {
    val reduce = rememberReduceMotion()
    val pulse = rememberInfiniteTransition(label = "now")
    val glow by pulse.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "glow")
    Box(modifier = Modifier.fillMaxWidth().height(108.dp)) {
        StraightStopHalf(modifier = Modifier.align(Alignment.BottomCenter))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (reduce) 0.4f else glow),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) {}
            }
            Spacer(Modifier.height(4.dp))
            Text("You are here", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${FinishedShelf.ordinal(count + 1)} book next",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The path's opening stretch below the "You are here" marker. */
@Composable
private fun StraightStopHalf(modifier: Modifier) {
    val trace = rememberTrace(durationMillis = Motion.MEDIUM)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pathStretch(0.5f, { trace.value }, trackColor(), travelledColor(), 6.dp)
    )
}

@Composable
private fun YearStop(year: Int) {
    StraightStop(56.dp) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 2.dp
        ) {
            Text(
                "$year",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun StartStop(first: FinishedShelf.Read, zone: ZoneId) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        StraightStopHalf(modifier = Modifier)
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp)) {}
        Spacer(Modifier.height(8.dp))
        Text("Where it began", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(
            monthLabel(first.endedAt, zone),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
