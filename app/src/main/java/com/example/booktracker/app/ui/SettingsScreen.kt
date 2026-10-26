package com.example.booktracker.app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.R
import com.example.booktracker.app.data.AppPalette
import com.example.booktracker.app.data.CsvImportResult
import com.example.booktracker.app.data.ReminderSettings
import com.example.booktracker.app.data.ThemeMode
import com.example.booktracker.app.notifications.Notifications
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val GOAL_MIN = 5
private const val GOAL_MAX = 100
private const val GOAL_STEP = 5

private const val YEARLY_MIN = 2
private const val YEARLY_MAX = 60
private const val YEARLY_STEP = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String,
    dailyGoal: Int,
    yearlyGoal: Int,
    dayStartsAtHour: Int,
    dndDuringSession: Boolean,
    onDndChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    palette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit,
    useDynamicColor: Boolean,
    onUserNameChange: (String) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onYearlyGoalChange: (Int) -> Unit,
    onDayStartsAtHourChange: (Int) -> Unit,
    onUseDynamicColorChange: (Boolean) -> Unit,
    useAppLock: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    reminder: ReminderSettings,
    onReminderChange: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onImportCsv: (Uri, (CsvImportResult?) -> Unit) -> Unit,
    onExportBackup: (Uri, (Boolean) -> Unit) -> Unit,
    onImportBackup: (Uri, (Int) -> Unit) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onEditHome: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onImportCsv(uri) { result ->
                val message = when {
                    result == null -> "Couldn't read that file. Export a CSV from Goodreads or StoryGraph and try again."
                    result.imported == 0 && result.skippedDuplicates == 0 -> "No books found in that file"
                    result.skippedDuplicates > 0 ->
                        "Imported ${result.imported} books · ${result.skippedDuplicates} already in your library"
                    else -> "Imported ${result.imported} books"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            onExportBackup(uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "Backup saved" else "Backup failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportBackup(uri) { count ->
                Toast.makeText(
                    context,
                    if (count >= 0) "Restored $count books" else "Restore failed — not a backup file?",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    // Enabling reminders on Android 13+ needs the notification permission first.
    var pendingReminderEnable by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingReminderEnable) {
            if (granted) {
                onReminderChange(true, reminder.hour, reminder.minute)
            } else {
                Toast.makeText(context, "Allow notifications to get reading reminders", Toast.LENGTH_LONG).show()
            }
        }
        pendingReminderEnable = false
    }

    // Local slider state so dragging is smooth; persist the snapped value on change.
    var goal by remember(dailyGoal) { mutableIntStateOf(dailyGoal) }
    var booksGoal by remember(yearlyGoal) { mutableIntStateOf(yearlyGoal) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionTitle("Profile")
            SettingsCard(
                title = "Your name",
                icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                // Typed into local state and saved after a pause: writing every
                // keystroke through DataStore and back drops characters.
                var nameText by remember { mutableStateOf(userName) }
                LaunchedEffect(nameText) {
                    delay(500)
                    if (nameText.trim() != userName && nameText.isNotBlank()) onUserNameChange(nameText)
                }
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(40) },
                    singleLine = true,
                    placeholder = { Text("Reader") },
                    supportingText = { Text("Shown on your profile and share cards") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsSectionTitle("Reading goals")
            SettingsCard(
                title = "Daily reading goal",
                icon = { Icon(Icons.Filled.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Text(
                    "$goal pages per day",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = goal.toFloat(),
                    onValueChange = { goal = snap(it) },
                    onValueChangeFinished = { onDailyGoalChange(goal) },
                    valueRange = GOAL_MIN.toFloat()..GOAL_MAX.toFloat(),
                    steps = (GOAL_MAX - GOAL_MIN) / GOAL_STEP - 1
                )
                Text(
                    "Used for your daily progress ring and reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(
                title = "Yearly reading goal",
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Text(
                    "$booksGoal books this year",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = booksGoal.toFloat(),
                    onValueChange = {
                        booksGoal = (Math.round(it / YEARLY_STEP) * YEARLY_STEP)
                            .coerceIn(YEARLY_MIN, YEARLY_MAX)
                    },
                    onValueChangeFinished = { onYearlyGoalChange(booksGoal) },
                    valueRange = YEARLY_MIN.toFloat()..YEARLY_MAX.toFloat(),
                    steps = (YEARLY_MAX - YEARLY_MIN) / YEARLY_STEP - 1
                )
                Text(
                    "Track the number of books you finish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(
                title = "New day starts at",
                icon = { Icon(Icons.Filled.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                var localHour by remember(dayStartsAtHour) { mutableIntStateOf(dayStartsAtHour) }
                Text(
                    formatTime(context, localHour, 0),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = localHour.toFloat(),
                    onValueChange = { localHour = it.toInt() },
                    onValueChangeFinished = { onDayStartsAtHourChange(localHour) },
                    valueRange = 0f..6f,
                    steps = 5
                )
                Text(
                    "Reading past midnight? Delay the day rollover so late-night sessions count for yesterday.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSectionTitle("Reminders & focus")
            SettingsCard(
                title = "Daily reminder",
                icon = { Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
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
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = { showReminderTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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

            SettingsCard(
                title = "Deep focus",
                icon = { Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                SwitchRow(
                    text = "Turn on Do Not Disturb while a reading session is running",
                    checked = dndDuringSession,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            if (!nm.isNotificationPolicyAccessGranted) {
                                Toast.makeText(
                                    context,
                                    "Allow Do Not Disturb access for this app, then turn this on again",
                                    Toast.LENGTH_LONG
                                ).show()
                                context.safeStartActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            } else {
                                onDndChange(true)
                            }
                        } else {
                            onDndChange(false)
                        }
                    }
                )
            }

            SettingsSectionTitle("Appearance")
            SettingsCard(
                title = "Theme",
                icon = { Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                val options = listOf("System", "Light", "Dark")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        val mode = ThemeMode.entries[index]
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = { onThemeModeChange(mode) },
                            selected = mode == themeMode
                        ) {
                            Text(label)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Color palette",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppPalette.entries.forEach { pal ->
                        val isSelected = pal == palette && !useDynamicColor
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPaletteChange(pal)
                                    if (useDynamicColor) onUseDynamicColorChange(false)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dual-tone palette preview swatch
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.TopStart)
                                            .background(Color(pal.primaryHex), CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.BottomEnd)
                                            .background(Color(pal.secondaryHex), CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pal.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pal.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(18.dp))
                    SwitchRow(
                        text = "Use wallpaper colors (Material You)",
                        checked = useDynamicColor,
                        onCheckedChange = onUseDynamicColorChange
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditHome)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Edit home screen", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Reorder or hide Home sections",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsSectionTitle("Privacy & security")
            SettingsCard(
                title = "App lock",
                icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                SwitchRow(
                    text = "Require fingerprint, face or screen lock to open the app",
                    checked = useAppLock,
                    onCheckedChange = { enable ->
                        if (enable) {
                            val available = BiometricManager.from(context).canAuthenticate(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            ) == BiometricManager.BIOMETRIC_SUCCESS
                            if (available) {
                                onAppLockChange(true)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Set up a screen lock or fingerprint on this device first",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            onAppLockChange(false)
                        }
                    }
                )
            }

            SettingsSectionTitle("Data")
            SettingsCard(
                title = "Import library",
                icon = { Icon(Icons.Filled.Dataset, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                FilledTonalButton(
                    onClick = {
                        csvLauncher.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Import Goodreads / StoryGraph CSV")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Adds your books, shelves, ratings and read dates. Books already in your library are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(
                title = "Backup & restore",
                icon = { Icon(Icons.Filled.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val stamp = java.text.SimpleDateFormat(
                                "yyyy-MM-dd", java.util.Locale.US
                            ).format(java.util.Date())
                            exportLauncher.launch("nocturnal-reader-backup-$stamp.zip")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export")
                    }
                    FilledTonalButton(
                        onClick = {
                            restoreLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/json",
                                    "application/octet-stream"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Restore")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Everything — books, covers, sessions, notes and goals — in one portable " +
                        ".zip you keep wherever you like. Restoring merges by id, so it's " +
                        "safe to run on an existing library. Your library is also included " +
                        "in your phone's automatic Google backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSectionTitle("About")
            SettingsCard(
                title = stringResourceSafe(context, R.string.app_name),
                icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Text(
                    "Version ${appVersionName(context)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Your reading data stays on this device. No account, no ads, no tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                LinkRow(Icons.Filled.PrivacyTip, "Privacy policy", onClick = onOpenPrivacyPolicy)
                LinkRow(Icons.Filled.StarRate, "Rate the app") {
                    val pkg = context.packageName
                    val opened = context.safeStartActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    )
                    if (!opened) {
                        context.safeStartActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                        )
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
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onReminderChange(true, state.hour, state.minute)
                    showReminderTimePicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, icon: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(12.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun snap(raw: Float): Int =
    (Math.round(raw / GOAL_STEP) * GOAL_STEP).coerceIn(GOAL_MIN, GOAL_MAX)

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

/** Clock time in the device's 12/24-hour style, e.g. "8:00 PM" or "20:00". */
private fun formatTime(context: Context, hour: Int, minute: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(pattern))
}

private fun appVersionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "—"

private fun stringResourceSafe(context: Context, id: Int): String = context.getString(id)

/** Starts [intent] if any app can handle it; returns false instead of crashing when none can. */
internal fun Context.safeStartActivity(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}
