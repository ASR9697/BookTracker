package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.ReadingCalendar
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.shared.models.Session
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CELL = 13.dp
private val CELL_GAP = 3.dp
private val DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    sessions: List<Session>,
    dailyGoal: Int,
    onBack: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val start = remember { ReadingCalendar.gridStart(today) }
    val pagesByDay = remember(sessions) { ReadingCalendar.pagesByDay(sessions) }
    val summary = remember(pagesByDay) { ReadingCalendar.summarize(pagesByDay, start, today) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            SummaryRow(summary)
            Spacer(Modifier.height(24.dp))
            Text("Last 52 weeks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Heatmap(
                start = start,
                today = today,
                pagesByDay = pagesByDay,
                dailyGoal = dailyGoal
            )
        }
    }
}

@Composable
private fun SummaryRow(summary: ReadingCalendar.Summary) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("Pages", summary.totalPages.toString(), Modifier.weight(1f))
        StatTile("Active days", summary.activeDays.toString(), Modifier.weight(1f))
        StatTile("Best day", summary.bestDayPages.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Heatmap(
    start: LocalDate,
    today: LocalDate,
    pagesByDay: Map<LocalDate, Int>,
    dailyGoal: Int
) {
    val ramp = rememberRamp()
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val scrollState = rememberScrollState()

    // Reveal the most recent weeks first (newest is at the right edge).
    LaunchedEffect(scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(CELL_GAP)
    ) {
        for (week in 0 until ReadingCalendar.WEEKS) {
            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                for (day in 0 until ReadingCalendar.DAYS_PER_WEEK) {
                    val date = start.plusDays((week * 7 + day).toLong())
                    if (date.isAfter(today)) {
                        Spacer(Modifier.size(CELL)) // future day: keep the grid rectangular
                    } else {
                        val pages = pagesByDay[date] ?: 0
                        val level = ReadingCalendar.level(pages, dailyGoal)
                        Box(
                            modifier = Modifier
                                .size(CELL)
                                .clip(RoundedCornerShape(3.dp))
                                .clickable { selected = date }
                                .background(ramp[level])
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    SelectedDayLabel(selected, pagesByDay)
    Spacer(Modifier.height(12.dp))
    Legend(ramp)
}

@Composable
private fun SelectedDayLabel(selected: LocalDate?, pagesByDay: Map<LocalDate, Int>) {
    val text = when {
        selected == null -> "Tap a day to see its pages"
        else -> {
            val pages = pagesByDay[selected] ?: 0
            val date = selected.format(DATE_FORMAT)
            if (pages > 0) "$date · $pages pages" else "$date · no reading"
        }
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Legend(ramp: List<Color>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        ramp.forEach { color ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(CELL)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Sequential single-hue ramp derived from the Material color scheme, so it
 * tracks dynamic color and light/dark automatically. Level 0 is the empty
 * (no-reading) neutral; 1..4 darken from primaryContainer to primary.
 */
@Composable
private fun rememberRamp(): List<Color> {
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val low = MaterialTheme.colorScheme.primaryContainer
    val high = MaterialTheme.colorScheme.primary
    return listOf(
        empty,
        lerp(low, high, 0f),
        lerp(low, high, 0.4f),
        lerp(low, high, 0.7f),
        high
    )
}
