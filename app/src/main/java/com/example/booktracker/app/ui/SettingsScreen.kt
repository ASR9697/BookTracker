package com.example.booktracker.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import android.app.NotificationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

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
    themeMode: com.example.booktracker.app.data.ThemeMode,
    onThemeModeChange: (com.example.booktracker.app.data.ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUserNameChange: (String) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onYearlyGoalChange: (Int) -> Unit,
    onDayStartsAtHourChange: (Int) -> Unit,
    onUseDynamicColorChange: (Boolean) -> Unit,
    useAppLock: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    onImportCsv: (Uri, (Int) -> Unit) -> Unit,
    onExportBackup: (Uri, (Boolean) -> Unit) -> Unit,
    onImportBackup: (Uri, (Int) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onImportCsv(uri) { count ->
                Toast.makeText(context, "Imported $count books", Toast.LENGTH_SHORT).show()
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
    // Local slider state so dragging is smooth; persist the snapped value on change.
    var goal by remember(dailyGoal) { mutableIntStateOf(dailyGoal) }
    var booksGoal by remember(yearlyGoal) { mutableIntStateOf(yearlyGoal) }

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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionTitle("Profile")
            SettingsCard(title = "User Details") {
                SettingsTextField(
                    label = "Display Name",
                    value = userName,
                    onValueChange = onUserNameChange
                )
            }

            SettingsSectionTitle("Reading Goals")
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
                    "Meet this each day to keep your streak alive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(
                title = "Yearly reading goal",
                icon = { Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
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
                var localHour by remember { mutableIntStateOf(dayStartsAtHour) }
                val amPm = if (localHour < 12) "AM" else "PM"
                val displayHour = if (localHour == 0) 12 else if (localHour > 12) localHour - 12 else localHour
                
                Text(
                    "$displayHour:00 $amPm",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = localHour.toFloat(),
                    onValueChange = { localHour = it.toInt() },
                    onValueChangeFinished = { onDayStartsAtHourChange(localHour) },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Text(
                    "Reading past midnight? Delay the day rollover so late-night sessions count for yesterday.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSectionTitle("Theme & Display")
            SettingsCard(
                title = "Deep Focus",
                icon = { Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        "Silence notifications during reading sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = dndDuringSession,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                if (!nm.isNotificationPolicyAccessGranted) {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                } else {
                                    onDndChange(true)
                                }
                            } else {
                                onDndChange(false)
                            }
                        }
                    )
                }
            }

            SettingsCard(
                title = "Visuals",
                icon = { Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Text(
                    "App Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val options = listOf("System", "Light", "Dark")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        val mode = com.example.booktracker.app.data.ThemeMode.entries[index]
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = { onThemeModeChange(mode) },
                            selected = mode == themeMode
                        ) {
                            Text(label)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        "Use Dynamic Color (Android 12+)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = useDynamicColor,
                        onCheckedChange = { onUseDynamicColorChange(it) },
                        enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                    )
                }
            }

            SettingsSectionTitle("Security")
            SettingsCard(
                title = "App Lock",
                icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        "Require biometric authentication to open app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = useAppLock,
                        onCheckedChange = { onAppLockChange(it) }
                    )
                }
            }

            SettingsCard(
                title = "Data",
                icon = { Icon(Icons.Filled.Dataset, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                FilledTonalButton(
                    onClick = { launcher.launch("text/csv") },
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
                    "Importing will add books to your library based on your history.",
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
                            exportLauncher.launch("booktracker-backup-$stamp.zip")
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
                    "Everything — books, sessions, notes and goals — in one portable " +
                        ".zip you keep wherever you like. Restoring merges by id, so it's " +
                        "safe to run on an existing library.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, icon: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Increased radius for premium look
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(12.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
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

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.width(160.dp)
        )
    }
}
