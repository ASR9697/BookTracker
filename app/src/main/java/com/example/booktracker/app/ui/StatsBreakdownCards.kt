package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.FocusInsights
import com.example.booktracker.app.analytics.StatsBreakdown
import com.example.booktracker.app.ui.theme.AppShape
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** A card with a title line, the shell every Stats breakdown sits in. */
@Composable
private fun StatsCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = AppShape.L,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * When you read: weekdays across, parts of the day down, darker where more time
 * went. Above it, how many days had reading and the busiest slot.
 */
@Composable
fun RhythmCard(grid: List<List<Int>>, activeDays: Int, days: Int, peak: String?, modifier: Modifier = Modifier) {
    val most = grid.flatten().maxOrNull()?.coerceAtLeast(1) ?: 1
    val ink = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    StatsCard("When you read", modifier) {
        Text(
            "Active $activeDays of $days ${if (days == 1) "day" else "days"}" + (peak?.let { " · most on $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(76.dp))
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        StatsBreakdown.DayPart.entries.forEach { part ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    part.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(76.dp)
                )
                DayOfWeek.entries.forEach { day ->
                    val minutes = grid[day.value - 1][part.ordinal]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .height(22.dp)
                            .clip(AppShape.XS)
                            .background(if (minutes == 0) empty else ink.copy(alpha = 0.25f + 0.75f * minutes / most))
                    )
                }
            }
        }
    }
}

/** How fast: pages an hour, a typical and the longest sitting, and days to finish a book. */
@Composable
fun PaceCard(pace: StatsBreakdown.Pace, modifier: Modifier = Modifier) {
    StatsCard("Pace", modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            BigNumber(pace.pagesPerHour?.let { "${it.roundToInt()}" } ?: "—", "pages an hour", Modifier.weight(1f))
            BigNumber(pace.averageSessionMinutes?.let { AnalyticsEngine.formatMinutes(it.toLong()) } ?: "—", "average sitting", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            BigNumber(pace.longestSessionMinutes?.let { AnalyticsEngine.formatMinutes(it.toLong()) } ?: "—", "longest sitting", Modifier.weight(1f))
            val days = pace.averageDaysToFinish?.roundToInt()
            BigNumber(days?.let { "$it ${if (it == 1) "day" else "days"}" } ?: "—", "to finish a book", Modifier.weight(1f))
        }
    }
}

@Composable
private fun BigNumber(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Where and when you read best, in one card: focus by time of day and place (from
 * the focus rated on the save screen), then pages and pace per place.
 */
@Composable
fun FocusPlacesCard(focus: FocusInsights.Summary?, places: List<AnalyticsEngine.EnvironmentStat>, modifier: Modifier = Modifier) {
    StatsCard("Focus & places", modifier) {
        if (focus != null) {
            val headline = FocusInsights.headline(focus)
            Text(
                headline ?: "Average ${String.format(Locale.US, "%.1f", focus.average)} of 5 across ${focus.rated} rated sessions.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (headline != null) FontWeight.SemiBold else null,
                color = if (headline != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            (focus.byTime + focus.byPlace.take(4)).forEach { group ->
                MeterRow(group.label, group.average / 5f, String.format(Locale.US, "%.1f", group.average),
                    strong = group == focus.bestTime || group == focus.bestPlace)
            }
        }
        if (places.isNotEmpty()) {
            if (focus != null) Spacer(Modifier.height(12.dp))
            Text("Where you read", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val most = places.maxOf { it.totalPages }.coerceAtLeast(1)
            places.take(5).forEach { place ->
                MeterRow(
                    place.tag,
                    place.totalPages.toFloat() / most,
                    "${place.totalPages} p" + (place.pagesPerHour?.let { " · ${it.roundToInt()} p/h" } ?: "")
                )
            }
        }
    }
}

/** A label, a bar for [fraction] and a value on the right. */
@Composable
private fun MeterRow(label: String, fraction: Float, value: String, strong: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (strong) FontWeight.Bold else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(108.dp)
        )
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = if (strong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * What you read in the period: finished against set aside, genres as bars, formats
 * as one split bar, book lengths and ratings.
 */
@Composable
fun WhatYouReadCard(shelf: StatsBreakdown.Shelf, onTidyTags: () -> Unit, modifier: Modifier = Modifier) {
    StatsCard("What you read", modifier, action = { TextButton(onClick = onTidyTags) { Text("Tidy tags") } }) {
        Text(
            "${shelf.finished} finished" + if (shelf.abandoned > 0) " · ${shelf.abandoned} set aside" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (shelf.genres.isNotEmpty()) {
            SubHeading("Genres")
            // "Other" sums many small genres, so it's a line of text rather than a bar that dwarfs the rest.
            val named = shelf.genres.filter { it.first != AnalyticsEngine.OTHER_GENRE }
            val most = named.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            named.forEach { (genre, count) -> MeterRow(genre.removePrefix("#"), count.toFloat() / most, "$count") }
            shelf.genres.firstOrNull { it.first == AnalyticsEngine.OTHER_GENRE }?.let { (_, count) ->
                Text(
                    "+ $count in other genres",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (shelf.formats.isNotEmpty()) {
            SubHeading("Formats")
            FormatSplit(shelf.formats)
        }
        if (shelf.lengths.isNotEmpty()) {
            SubHeading("Length")
            val most = shelf.lengths.values.max().coerceAtLeast(1)
            StatsBreakdown.Length.entries.forEach { length ->
                val count = shelf.lengths[length] ?: 0
                MeterRow(length.label, count.toFloat() / most, "$count")
            }
        }
        shelf.averageRating?.let { average ->
            SubHeading("Ratings")
            Text(
                "${String.format(Locale.US, "%.1f", average)} ★ on average",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val most = shelf.ratingSpread.max().coerceAtLeast(1)
            (5 downTo 1).forEach { stars ->
                val count = shelf.ratingSpread[stars - 1]
                MeterRow("$stars ★", count.toFloat() / most, "$count")
            }
        }
    }
}

@Composable
private fun SubHeading(text: String) {
    Spacer(Modifier.height(14.dp))
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
}

/** One bar split by format, with a legend underneath. */
@Composable
private fun FormatSplit(formats: List<Pair<BookFormat, Int>>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.outline
    )
    val total = formats.sumOf { it.second }.coerceAtLeast(1)
    Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
        formats.forEachIndexed { i, (_, count) ->
            Box(Modifier.weight(count.toFloat()).height(12.dp).background(colors[i % colors.size]))
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        formats.forEachIndexed { i, (format, count) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors[i % colors.size]))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${format.name} ${count * 100 / total}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The books finished in the period as covers, with a way to the full Finished shelf. */
@Composable
fun FinishedInPeriod(books: List<Book>, onOpenBook: (Book) -> Unit, onSeeAll: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Finished", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onSeeAll) { Text("See all") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(books, key = { it.id }) { book ->
                BookCover(
                    book,
                    Modifier
                        .size(width = 72.dp, height = 108.dp)
                        .clip(AppShape.XS)
                        .clickable { onOpenBook(book) }
                )
            }
        }
    }
}

/** One row to the Goals screen, saying how the year's target stands. */
@Composable
fun GoalsLink(text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = AppShape.L,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
