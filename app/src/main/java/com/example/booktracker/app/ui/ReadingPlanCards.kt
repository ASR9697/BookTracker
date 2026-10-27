package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.FinishByPlanner
import com.example.booktracker.app.analytics.RereadMemories
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val planDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
private val shortDateFormat = DateTimeFormatter.ofPattern("MMM d")
private val memoryDateFormat = DateTimeFormatter.ofPattern("MMM yyyy")

/**
 * "Finish by" on the book page: pick a day, see the pages a day it takes, how
 * today is going against that, and whether your real pace gets you there.
 */
@Composable
fun FinishByCard(
    book: Book,
    sessions: List<Session>,
    streak: StreakEngine.StreakInfo,
    onSetFinishBy: (Long?) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    var picking by remember { mutableStateOf(false) }
    val plan = remember(book, sessions, streak.today, streak.dayStartsAtHour) {
        FinishByPlanner.plan(book, sessions, streak.today, zone, streak.dayStartsAtHour)
    }
    val unit = FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()

    PlanCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                if (plan == null) "Finish by a date" else "Finish by ${plan.target.format(planDateFormat)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (plan != null) {
                IconButton(onClick = { picking = true }) { Icon(Icons.Filled.Edit, contentDescription = "Change date") }
                IconButton(onClick = { onSetFinishBy(null) }) { Icon(Icons.Filled.Close, contentDescription = "Remove deadline") }
            }
        }
        if (plan == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick a day and see how many $unit a day get you there. It adjusts every day to how you actually read.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = { picking = true }) { Text("Set a date") }
        } else {
            Spacer(Modifier.height(12.dp))
            if (plan.status == FinishByPlanner.Status.DONE) {
                Text("All read. Nicely done.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${plan.pagesPerDay}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$unit a day",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                val todayFraction = if (plan.pagesPerDay > 0) (plan.readToday.toFloat() / plan.pagesPerDay).coerceIn(0f, 1f) else 1f
                LinearProgressIndicator(
                    progress = { todayFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (plan.leftToday == 0) "Today's ${plan.pagesPerDay} done. Anything more puts you ahead."
                    else "${plan.readToday} read today · ${plan.leftToday} to go",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${plan.pagesLeft} $unit left · ${plan.daysLeft} ${if (plan.daysLeft == 1) "day" else "days"} including today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            StatusPill(plan, unit)
        }
    }

    if (picking) {
        FinishByDatePicker(
            initial = plan?.target,
            today = streak.today,
            onDismiss = { picking = false },
            onPicked = { date ->
                picking = false
                onSetFinishBy(date.atStartOfDay(zone).toInstant().toEpochMilli())
            }
        )
    }
}

@Composable
private fun StatusPill(plan: FinishByPlanner.Plan, unit: String) {
    val pace = plan.recentPace?.let { if (it >= 10) "${it.roundToInt()}" else "%.1f".format(it) }
    val projected = plan.projectedFinish?.format(shortDateFormat)
    val (text, good) = when (plan.status) {
        FinishByPlanner.Status.DONE -> "Finished" to true
        FinishByPlanner.Status.ON_TRACK -> "On track: at $pace $unit a day you finish $projected" to true
        FinishByPlanner.Status.AHEAD -> "Ahead: at $pace $unit a day you finish $projected" to true
        FinishByPlanner.Status.BEHIND -> {
            val late = plan.projectedFinish?.let { it.toEpochDay() - plan.target.toEpochDay() } ?: 0
            "At $pace $unit a day you'd finish $projected, $late ${if (late == 1L) "day" else "days"} late" to false
        }
        FinishByPlanner.Status.OVERDUE -> "That date has passed. Pick a new one to keep a plan." to false
        FinishByPlanner.Status.NO_PACE -> "Read a little and this will check your pace against the plan." to true
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (good) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (good) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** Only today and later can be picked; the picker speaks UTC midnight, converted back to a local date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishByDatePicker(initial: LocalDate?, today: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: today.plusWeeks(2)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            override fun isSelectableYear(year: Int) = year >= today.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                }
            ) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state, title = { Text("Finish by", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

/**
 * On a re-read: what you wrote last time on the pages you've passed, and how far
 * the next one is. The next note's text stays hidden so it's still a surprise.
 */
@Composable
fun RereadMemoriesCard(book: Book, readStart: Long, earlierNotes: List<MarginNote>) {
    val passed = remember(earlierNotes, book.currentPage) { RereadMemories.passed(earlierNotes, readStart, book.currentPage).take(3) }
    val next = remember(earlierNotes, book.currentPage) { RereadMemories.ahead(earlierNotes, readStart, book.currentPage).firstOrNull() }
    PlanCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("From your last read", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${earlierNotes.size} ${if (earlierNotes.size == 1) "note" else "notes"} from before. They come back as you reach their pages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (passed.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                passed.forEach { MemoryNote(it) }
            }
        }
        next?.let {
            Spacer(Modifier.height(12.dp))
            val away = it.pageNumber - book.currentPage
            Text(
                "Next one on p. ${it.pageNumber}, $away ${if (away == 1) "page" else "pages"} away",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** The session recap's version: only the earlier notes on the pages just read. */
@Composable
fun MemoriesReachedCard(notes: List<MarginNote>, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (notes.size == 1) "You passed a note from your last read" else "You passed ${notes.size} notes from your last read",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notes.take(5).forEach { MemoryNote(it) }
            }
        }
    }
}

@Composable
private fun MemoryNote(note: MarginNote) {
    val zone = remember { ZoneId.systemDefault() }
    Row {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                "p. ${note.pageNumber}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                note.content,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "You wrote this in ${Instant.ofEpochMilli(note.timestamp).atZone(zone).format(memoryDateFormat)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Matches the book page's other cards. */
@Composable
private fun PlanCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

/** One line for the Home card: today's share of the plan, or null without a deadline. */
fun finishByLine(plan: FinishByPlanner.Plan?): Pair<String, Boolean>? {
    plan ?: return null
    val date = plan.target.format(shortDateFormat)
    return when (plan.status) {
        FinishByPlanner.Status.DONE -> null
        FinishByPlanner.Status.OVERDUE -> "Finish-by date passed" to false
        FinishByPlanner.Status.BEHIND -> "${plan.leftToday} to go today · behind for $date" to false
        else -> if (plan.leftToday == 0) "Today done · on plan for $date" to true
        else "${plan.leftToday} to go today for $date" to true
    }
}
