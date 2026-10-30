package com.example.booktracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.PeriodStats
import com.example.booktracker.app.ui.theme.AppShape
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Week · Month · Year · All, with ‹ › to step back through earlier ones. */
@Composable
fun PeriodSwitcher(
    period: PeriodStats.Period,
    label: String,
    onSpan: (PeriodStats.Span) -> Unit,
    onStep: (PeriodStats.Period) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PeriodStats.Span.entries.forEach { span ->
                TickFilterChip(
                    selected = period.span == span,
                    onClick = { onSpan(span) },
                    label = { Text(span.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    shape = CircleShape
                )
            }
        }
        if (period.span != PeriodStats.Span.ALL) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onStep(period.previous) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Earlier")
                }
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onStep(period.next) }, enabled = !period.isCurrent) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Later")
                }
            }
        }
    }
}

/**
 * Books, pages, time and active days for the period, each with its change against
 * the period before (left out for all time, or with nothing to compare against).
 */
@Composable
fun SummaryStrip(summary: PeriodStats.Summary, before: PeriodStats.Summary?, comparedWith: String?, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = AppShape.L,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryNumber("%,d".format(summary.books), if (summary.books == 1) "book" else "books",
                    before?.let { PeriodStats.change(summary.books, it.books) }, Modifier.weight(1f))
                SummaryNumber("%,d".format(summary.pages), "pages",
                    before?.let { PeriodStats.change(summary.pages, it.pages) }, Modifier.weight(1f))
                SummaryNumber(AnalyticsEngine.formatMinutes(summary.minutes), "read",
                    before?.let { PeriodStats.change(summary.minutes, it.minutes) }, Modifier.weight(1f))
                SummaryNumber("${summary.activeDays}", if (summary.activeDays == 1) "day" else "days",
                    before?.let { PeriodStats.change(summary.activeDays, it.activeDays) }, Modifier.weight(1f))
            }
            if (comparedWith != null && before != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Compared with $comparedWith",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryNumber(value: String, label: String, change: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (change != null) {
            Text(
                when {
                    change > 0 -> "▲ $change%"
                    change < 0 -> "▼ ${-change}%"
                    else -> "same"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (change >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pages or minutes as bars: days of a week or month, months of a year, years of all
 * time. Tapping a bar names it and its value in the title line.
 */
@Composable
fun ActivityChart(
    buckets: List<PeriodStats.Bucket>,
    span: PeriodStats.Span,
    showMinutes: Boolean,
    onShowMinutes: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var picked by remember(buckets) { mutableStateOf<Int?>(null) }
    val max = buckets.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    val unit = if (showMinutes) "min" else "pages"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = AppShape.L,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val bucket = picked?.let { buckets.getOrNull(it) }
                Text(
                    if (bucket != null) "${bucketName(bucket, span)} · ${formatValue(bucket.value, showMinutes)}"
                    else if (showMinutes) "Time read" else "Pages read",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TickFilterChip(selected = !showMinutes, onClick = { onShowMinutes(false) }, label = { Text("Pages") }, shape = CircleShape)
                Spacer(Modifier.width(6.dp))
                TickFilterChip(selected = showMinutes, onClick = { onShowMinutes(true) }, label = { Text("Minutes") }, shape = CircleShape)
            }
            Spacer(Modifier.height(16.dp))
            if (buckets.all { it.value == 0L }) {
                // A flat row of zeros reads as broken; say what will fill it instead.
                Text(
                    if (showMinutes) "No timed reading in this period. Time a session and your minutes show up here."
                    else "No pages logged in this period. Log a page or time a session and it shows up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Bars(buckets, max, picked, onPick = { picked = if (picked == it) null else it })
                Spacer(Modifier.height(6.dp))
                AxisLabels(buckets, span)
                Text(
                    "Total ${formatValue(buckets.sumOf { it.value }, showMinutes)}" +
                        if (buckets.size > 1) " · best ${formatValue(max, showMinutes)}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Bars(buckets: List<PeriodStats.Bucket>, max: Long, picked: Int?, onPick: (Int) -> Unit) {
    val strong = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(buckets) {
                detectTapGestures { offset ->
                    val slot = size.width / buckets.size.toFloat()
                    onPick((offset.x / slot).toInt().coerceIn(0, buckets.lastIndex))
                }
            }
    ) {
        val slot = size.width / buckets.size
        val gap = (slot * 0.25f).coerceAtMost(6.dp.toPx())
        val barWidth = (slot - gap).coerceAtLeast(1.dp.toPx())
        val radius = (barWidth / 2f).coerceAtMost(4.dp.toPx())
        buckets.forEachIndexed { i, bucket ->
            val left = slot * i + (slot - barWidth) / 2f
            val h = if (bucket.value > 0) (size.height * bucket.value / max).coerceAtLeast(radius * 2) else 3.dp.toPx()
            drawRoundRect(
                color = when {
                    bucket.value == 0L -> empty
                    picked == null || picked == i -> strong
                    else -> soft
                },
                topLeft = Offset(left, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

/** Axis labels under the bars; a month shows every seventh day so they don't crowd. */
@Composable
private fun AxisLabels(buckets: List<PeriodStats.Bucket>, span: PeriodStats.Span) {
    Row(modifier = Modifier.fillMaxWidth()) {
        buckets.forEachIndexed { i, bucket ->
            val show = span != PeriodStats.Span.MONTH || i % 7 == 0
            Text(
                if (show) bucket.label else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun bucketName(bucket: PeriodStats.Bucket, span: PeriodStats.Span): String = when (span) {
    PeriodStats.Span.WEEK, PeriodStats.Span.MONTH -> bucket.start.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    PeriodStats.Span.YEAR -> bucket.start.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
    PeriodStats.Span.ALL -> bucket.start.year.toString()
}

private fun formatValue(value: Long, minutes: Boolean): String =
    if (minutes) AnalyticsEngine.formatMinutes(value) else "%,d pages".format(value)
