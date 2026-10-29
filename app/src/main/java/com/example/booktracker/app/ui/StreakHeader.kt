package com.example.booktracker.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.ui.animations.LivingFlame
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.animations.rememberHaptics
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val RUNS_SHOWN = 5
private val runDateFormat = DateTimeFormatter.ofPattern("MMM d")

/**
 * Top of the streak screen: the flame with current and best streak, whether
 * today already counts, a month calendar of reading days you can page through,
 * and your longest streaks. The session history follows below it.
 */
@Composable
fun StreakHeader(streak: StreakEngine.StreakInfo, modifier: Modifier = Modifier) {
    val runs = remember(streak.activeDays) { StreakEngine.runs(streak.activeDays) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StreakHero(streak)
        MonthCalendar(streak)
        if (runs.isNotEmpty()) StreakRuns(runs, streak)
        Text(
            "Sessions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Tap a session to fix its pages or time; swipe left to delete it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreakHero(streak: StreakEngine.StreakInfo) {
    val shown by animateCountUp(streak.currentStreak)
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            LivingFlame(streak = streak.currentStreak, baseSize = 56.dp)
            Spacer(Modifier.height(8.dp))
            Text("$shown", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
            Text(
                if (streak.currentStreak == 1) "day in a row" else "days in a row",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                HeroStat("${streak.longestStreak}", "best ever")
                HeroStat("${streak.activeDays.size}", "reading days")
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = CircleShape,
                color = if (streak.goalMetToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (streak.goalMetToday) Icons.Filled.CheckCircle else Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (streak.goalMetToday) "Today counts. ${streak.pagesToday} pages read"
                        else "Read a page today to keep it going",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A month of reading days, Monday first; arrows page back through earlier months. */
@Composable
private fun MonthCalendar(streak: StreakEngine.StreakInfo) {
    val haptics = rememberHaptics()
    val thisMonth = remember(streak.today) { YearMonth.from(streak.today) }
    var month by remember(thisMonth) { mutableStateOf(thisMonth) }
    val firstActive = remember(streak.activeDays) { streak.activeDays.minOrNull()?.let(YearMonth::from) }
    val readDays = streak.activeDays.count { YearMonth.from(it) == month }

    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { haptics.tick(); month = month.minusMonths(1) },
                    enabled = firstActive != null && month > firstActive
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month") }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "$readDays of ${if (month == thisMonth) streak.today.dayOfMonth else month.lengthOfMonth()} days read",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { haptics.tick(); month = month.plusMonths(1) },
                    enabled = month < thisMonth
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month") }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                weekDays(LocalAppPrefs.current.weekStart).forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val leading = leadingDays(month.atDay(1), LocalAppPrefs.current.weekStart)
            val cells = leading + month.lengthOfMonth()
            (0 until (cells + 6) / 7).forEach { week ->
                Row {
                    (0 until 7).forEach { col ->
                        val dayNumber = week * 7 + col - leading + 1
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNumber)
                                val read = date in streak.activeDays
                                val isToday = date == streak.today
                                val future = date.isAfter(streak.today)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(if (read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (future) 0.3f else 1f))
                                        .then(if (isToday) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape) else Modifier)
                                ) {
                                    Text(
                                        "$dayNumber",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (read || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            read -> MaterialTheme.colorScheme.onPrimary
                                            future -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Your longest runs, with the live one marked, so a broken streak still has a record. */
@Composable
private fun StreakRuns(runs: List<StreakEngine.StreakRun>, streak: StreakEngine.StreakInfo) {
    val longest = remember(runs) { runs.sortedByDescending { it.length }.take(RUNS_SHOWN) }
    val maxLength = longest.first().length
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Longest streaks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            longest.forEach { run ->
                // A run ending today or yesterday is the one still alive.
                val live = streak.currentStreak > 0 && !run.end.isBefore(streak.today.minusDays(1))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${run.length} ${if (run.length == 1) "day" else "days"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(72.dp)
                        )
                        Text(
                            if (run.length == 1) run.start.format(runDateFormat)
                            else "${run.start.format(runDateFormat)} – ${run.end.format(runDateFormat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (live) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "Current",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { run.length.toFloat() / maxLength },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = if (live) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}
