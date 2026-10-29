package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.data.ReminderSettings
import com.example.booktracker.app.notifications.Notifications
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.animations.staggeredEntrance

/** What you've done so far against each goal, so every slider shows where you stand. */
data class GoalProgress(
    val pagesToday: Int = 0,
    val booksThisYear: Int = 0,
    val pagesThisMonth: Int = 0,
    val booksThisMonth: Int = 0
)

private val DAILY_RANGE = 5..100
private const val DAILY_STEP = 5
private val YEARLY_RANGE = 2..60
private const val YEARLY_STEP = 2
private val MONTHLY_PAGES_RANGE = 100..3000
private const val MONTHLY_PAGES_STEP = 100
private val MONTHLY_BOOKS_RANGE = 1..10

/**
 * Every reading target in one place, each with a live bar of how far along you
 * are: today, this month, this year. Also the rules that decide what "today"
 * means and when the app nudges you.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    dailyGoal: Int,
    yearlyGoal: Int,
    monthlyGoalPages: Int,
    monthlyGoalBooks: Int,
    dayStartsAtHour: Int,
    reminder: ReminderSettings,
    progress: GoalProgress,
    onDailyGoalChange: (Int) -> Unit,
    onYearlyGoalChange: (Int) -> Unit,
    onMonthlyGoalPagesChange: (Int) -> Unit,
    onMonthlyGoalBooksChange: (Int) -> Unit,
    onDayStartsAtHourChange: (Int) -> Unit,
    onReminderChange: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entrance = rememberEntranceState()
    var showReminderTimePicker by remember { mutableStateOf(false) }

    // Turning reminders on needs the notification permission on Android 13+.
    var pendingReminderEnable by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (pendingReminderEnable) {
            if (granted) onReminderChange(true, reminder.hour, reminder.minute)
            else Toast.makeText(context, "Allow notifications to get reading reminders", Toast.LENGTH_LONG).show()
        }
        pendingReminderEnable = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading goals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoalCard(
                icon = Icons.Rounded.Flag,
                title = "Every day",
                unit = "pages a day",
                value = dailyGoal,
                range = DAILY_RANGE,
                step = DAILY_STEP,
                done = progress.pagesToday,
                doneLabel = "today",
                hint = "Drives your daily ring, your streak flame and the reminder.",
                onChange = onDailyGoalChange,
                modifier = Modifier.staggeredEntrance(entrance, 0)
            )
            GoalCard(
                icon = Icons.Rounded.CalendarMonth,
                title = "This month",
                unit = "pages a month",
                value = monthlyGoalPages,
                range = MONTHLY_PAGES_RANGE,
                step = MONTHLY_PAGES_STEP,
                done = progress.pagesThisMonth,
                doneLabel = "so far",
                onChange = onMonthlyGoalPagesChange,
                modifier = Modifier.staggeredEntrance(entrance, 1)
            )
            GoalCard(
                icon = Icons.Rounded.LibraryBooks,
                title = "Books this month",
                unit = "books a month",
                value = monthlyGoalBooks,
                range = MONTHLY_BOOKS_RANGE,
                step = 1,
                done = progress.booksThisMonth,
                doneLabel = "finished",
                onChange = onMonthlyGoalBooksChange,
                modifier = Modifier.staggeredEntrance(entrance, 2)
            )
            GoalCard(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                title = "This year",
                unit = "books a year",
                value = yearlyGoal,
                range = YEARLY_RANGE,
                step = YEARLY_STEP,
                done = progress.booksThisYear,
                doneLabel = "finished",
                hint = "Shown as the ring around your profile picture.",
                onChange = onYearlyGoalChange,
                modifier = Modifier.staggeredEntrance(entrance, 3)
            )

            SettingsSectionTitle("Rules")
            SettingsCard(
                title = "New day starts at",
                icon = { Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                var localHour by remember(dayStartsAtHour) { mutableIntStateOf(dayStartsAtHour) }
                Text(formatTime(context, localHour, 0), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = localHour.toFloat(),
                    onValueChange = { localHour = it.toInt() },
                    onValueChangeFinished = { onDayStartsAtHourChange(localHour) },
                    valueRange = 0f..6f,
                    steps = 5
                )
                Text(
                    "Reading past midnight? Delay the rollover so late sessions count for the day you started.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(
                title = "Daily reminder",
                icon = { Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                SwitchRow(
                    text = "Remind me to read if I haven't hit my daily goal",
                    checked = reminder.enabled,
                    onCheckedChange = { enable ->
                        if (enable && !Notifications.canPost(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pendingReminderEnable = true
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onReminderChange(enable, reminder.hour, reminder.minute)
                        }
                    }
                )
                if (reminder.enabled) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = AppShape.S,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = { showReminderTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder time", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                formatTime(context, reminder.hour, reminder.minute),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReminderTimePicker) {
        val state = rememberTimePickerState(
            initialHour = reminder.hour,
            initialMinute = reminder.minute,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            title = { Text("Reminder time") },
            text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } },
            confirmButton = {
                TextButton(onClick = {
                    onReminderChange(true, state.hour, state.minute)
                    showReminderTimePicker = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") } }
        )
    }
}

/**
 * One target: the number, a slider to change it, and a bar of how much of it
 * is already done. The slider moves a local value and saves when you let go.
 */
@Composable
private fun GoalCard(
    icon: ImageVector,
    title: String,
    unit: String,
    value: Int,
    range: IntRange,
    step: Int,
    done: Int,
    doneLabel: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    val haptics = rememberHaptics()
    var local by remember(value) { mutableIntStateOf(value.coerceIn(range)) }
    val fraction = if (local > 0) (done.toFloat() / local).coerceIn(0f, 1f) else 0f
    val bar by animateFloatAsStateCompat(fraction)
    val shownDone by animateCountUp(done)
    val reached = done >= local

    Surface(
        shape = AppShape.L,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (reached) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "Reached",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$local", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            }
            Slider(
                value = local.toFloat(),
                onValueChange = { raw ->
                    val snapped = (Math.round(raw / step) * step).coerceIn(range)
                    if (snapped != local) haptics.tick()
                    local = snapped
                },
                onValueChangeFinished = { onChange(local) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = ((range.last - range.first) / step - 1).coerceAtLeast(0)
            )
            LinearProgressIndicator(
                progress = { bar },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$shownDone of $local $doneLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hint != null) {
                Spacer(Modifier.height(6.dp))
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun animateFloatAsStateCompat(target: Float): State<Float> =
    androidx.compose.animation.core.animateFloatAsState(target, Motion.settle(), label = "goalBar")
