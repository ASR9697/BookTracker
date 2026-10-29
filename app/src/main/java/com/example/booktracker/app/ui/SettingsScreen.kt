package com.example.booktracker.app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.R
import com.example.booktracker.app.data.AppPalette
import com.example.booktracker.app.data.AppPrefs
import com.example.booktracker.app.data.CsvImportResult
import com.example.booktracker.app.data.NoteImageStore
import com.example.booktracker.app.data.ReminderSettings
import com.example.booktracker.app.data.ThemeMode
import com.example.booktracker.app.data.bookmory.BookmoryImportSummary
import com.example.booktracker.app.data.bookmory.BookmoryImporter
import com.example.booktracker.app.notifications.Notifications
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import com.example.booktracker.app.ui.theme.AppIcon
import com.example.booktracker.shared.models.BookFormat
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val GOAL_MIN = 5
private const val GOAL_MAX = 100
private const val GOAL_STEP = 5

/** The sub-pages behind the hub's rows. Enum names are saved across rotation. */
private enum class SettingsPage(val title: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Filled.Palette),
    READING("Reading & timer", Icons.Filled.Timer),
    NOTIFICATIONS("Notifications", Icons.Filled.Notifications),
    PRIVACY("Privacy & security", Icons.Filled.Lock),
    DATA("Data & sync", Icons.Filled.Backup),
    MOTION("Motion & feel", Icons.Filled.Vibration),
    ABOUT("About", Icons.Filled.Info)
}

private val TEXT_SIZES = listOf(0.9f to "Small", 1f to "Default", 1.12f to "Large", 1.25f to "Larger")
private val LOCK_DELAYS = listOf(0 to "Immediately", 60 to "1 min", 300 to "5 min", 900 to "15 min")

/**
 * Settings as a hub: a profile card, then one row per area with its current
 * value, each opening a page of its own. Pages slide in over the hub and back
 * returns to it, all within this one route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String,
    dndDuringSession: Boolean,
    onDndChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    palette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit,
    useDynamicColor: Boolean,
    onUserNameChange: (String) -> Unit,
    onUseDynamicColorChange: (Boolean) -> Unit,
    useAppLock: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean = true,
    onHapticsChange: (Boolean) -> Unit = {},
    reduceMotion: Boolean = false,
    onReduceMotionChange: (Boolean) -> Unit = {},
    keepScreenOn: Boolean = true,
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onImportCsv: (Uri, (CsvImportResult?) -> Unit) -> Unit,
    onImportBookmory: (Uri, (Result<BookmoryImportSummary>) -> Unit) -> Unit = { _, _ -> },
    onExportBackup: (Uri, (Boolean) -> Unit) -> Unit,
    onImportBackup: (Uri, (Int) -> Unit) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onEditHome: () -> Unit = {},
    /** The Drive sync card, supplied by the caller that owns its state. */
    driveSync: @Composable () -> Unit = {},
    prefs: AppPrefs = AppPrefs(),
    onPrefsChange: ((AppPrefs) -> AppPrefs) -> Unit = {},
    dailyGoal: Int = 0,
    yearlyGoal: Int = 0,
    reminder: ReminderSettings? = null,
    finishedCount: Int = 0,
    readingSinceYear: Int? = null,
    driveConnected: Boolean = false,
    onOpenGoals: () -> Unit = {},
    onExportCsv: (Uri, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onBackupNow: ((Boolean) -> Unit) -> Unit = {},
    onDeleteAll: ((Boolean) -> Unit) -> Unit = {},
    onBack: () -> Unit
) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    BackHandler(enabled = page != null) { page = null }
    val reduce = rememberReduceMotion()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(targetState = page?.title ?: "Settings", transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "settingsTitle") {
                        Text(it)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (page != null) page = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (reduce) {
                    fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT))
                } else {
                    // Into a page slides left; back to the hub slides right.
                    val forward = targetState != null
                    (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { w -> if (forward) w / 3 else -w / 3 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                        (slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { w -> if (forward) -w / 3 else w / 3 } + fadeOut(tween(Motion.SHORT)))
                }
            },
            label = "settingsPage",
            modifier = Modifier.padding(innerPadding)
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .readableWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (current) {
                    null -> SettingsHub(
                        userName = userName,
                        onUserNameChange = onUserNameChange,
                        finishedCount = finishedCount,
                        readingSinceYear = readingSinceYear,
                        summaries = mapOf(
                            SettingsPage.APPEARANCE to listOf(
                                if (useDynamicColor) "Wallpaper colours" else palette.title,
                                if (prefs.nocturnal) "Nocturnal" else themeMode.label
                            ).joinToString(" · "),
                            SettingsPage.READING to listOf(
                                "Week starts ${if (prefs.weekStartsSunday) "Sunday" else "Monday"}",
                                if (dndDuringSession) "Do Not Disturb on" else null
                            ).filterNotNull().joinToString(" · "),
                            SettingsPage.NOTIFICATIONS to notificationSummary(prefs, reminder),
                            SettingsPage.PRIVACY to if (useAppLock) "App lock on" else "App lock off",
                            SettingsPage.DATA to when {
                                prefs.autoBackup && prefs.lastAutoBackup > 0 -> "Backed up " + relative(prefs.lastAutoBackup)
                                prefs.autoBackup -> "Weekly backup on"
                                driveConnected -> "Syncing with Google Drive"
                                else -> "Backup, import and sync"
                            },
                            SettingsPage.MOTION to listOfNotNull(if (hapticsEnabled) "Haptics on" else "Haptics off", if (reduceMotion) "Reduced motion" else null).joinToString(" · "),
                            SettingsPage.ABOUT to "Version ${appVersionName(LocalContext.current)}"
                        ),
                        goalsSummary = listOfNotNull(
                            dailyGoal.takeIf { it > 0 }?.let { "$it pages a day" },
                            yearlyGoal.takeIf { it > 0 }?.let { "$it books a year" }
                        ).joinToString(" · ").ifEmpty { "Targets, day start and reminder" },
                        onOpen = { page = it },
                        onOpenGoals = onOpenGoals
                    )
                    SettingsPage.APPEARANCE -> AppearancePage(
                        themeMode, onThemeModeChange, palette, onPaletteChange, useDynamicColor, onUseDynamicColorChange,
                        prefs, onPrefsChange, onEditHome
                    )
                    SettingsPage.READING -> ReadingPage(dndDuringSession, onDndChange, keepScreenOn, onKeepScreenOnChange, prefs, onPrefsChange, onOpenGoals)
                    SettingsPage.NOTIFICATIONS -> NotificationsPage(prefs, onPrefsChange, reminder, onOpenGoals)
                    SettingsPage.PRIVACY -> PrivacyPage(useAppLock, onAppLockChange, prefs, onPrefsChange)
                    SettingsPage.DATA -> DataPage(
                        prefs, onPrefsChange, onImportCsv, onImportBookmory, onExportBackup, onImportBackup,
                        onExportCsv, onBackupNow, onDeleteAll, driveConnected, driveSync
                    )
                    SettingsPage.MOTION -> MotionPage(hapticsEnabled, onHapticsChange, reduceMotion, onReduceMotionChange)
                    SettingsPage.ABOUT -> AboutPage(onOpenPrivacyPolicy)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System theme"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

private fun notificationSummary(prefs: AppPrefs, reminder: ReminderSettings?): String {
    val on = listOfNotNull(
        if (reminder?.enabled == true) "goal" else null,
        if (prefs.streakNudge) "streak" else null,
        if (prefs.weeklyRecap) "recap" else null,
        if (prefs.reviewReminder) "review" else null
    )
    return when (on.size) {
        0 -> "All off"
        1 -> "${on.first().replaceFirstChar { it.uppercase() }} reminder on"
        else -> "${on.size} reminders on"
    } + if (prefs.quietHours) " · quiet hours" else ""
}

private fun relative(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().lowercase()

// ---------------------------------------------------------------- hub

@Composable
private fun SettingsHub(
    userName: String,
    onUserNameChange: (String) -> Unit,
    finishedCount: Int,
    readingSinceYear: Int?,
    summaries: Map<SettingsPage, String>,
    goalsSummary: String,
    onOpen: (SettingsPage) -> Unit,
    onOpenGoals: () -> Unit
) {
    ProfileCard(userName, onUserNameChange, finishedCount, readingSinceYear)

    @Composable
    fun row(page: SettingsPage) = SettingRow(page.icon, page.title, summaries[page], onClick = { onOpen(page) })

    SettingsGroup {
        row(SettingsPage.APPEARANCE)
        GroupDivider()
        row(SettingsPage.READING)
        GroupDivider()
        SettingRow(Icons.Filled.Flag, "Goals & reminders", goalsSummary, onClick = onOpenGoals)
    }
    SettingsGroup {
        row(SettingsPage.NOTIFICATIONS)
        GroupDivider()
        row(SettingsPage.PRIVACY)
        GroupDivider()
        row(SettingsPage.DATA)
    }
    SettingsGroup {
        row(SettingsPage.MOTION)
        GroupDivider()
        row(SettingsPage.ABOUT)
    }
}

@Composable
private fun ProfileCard(userName: String, onUserNameChange: (String) -> Unit, finishedCount: Int, readingSinceYear: Int?) {
    var editing by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClickLabel = "Edit your name") { editing = true }
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(scheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    userName.trim().firstOrNull()?.uppercase() ?: "R",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName.ifBlank { "Reader" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        finishedCount == 0 -> "Your first finished book is waiting"
                        readingSinceYear != null -> "$finishedCount ${if (finishedCount == 1) "book" else "books"} finished since $readingSinceYear"
                        else -> "$finishedCount ${if (finishedCount == 1) "book" else "books"} finished"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
    if (editing) {
        // Typed into local state and saved on Done: writing every keystroke through DataStore drops characters.
        var nameText by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Your name") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(40) },
                    singleLine = true,
                    placeholder = { Text("Reader") },
                    supportingText = { Text("Shown on your profile and share cards") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameText.isNotBlank() && nameText.trim() != userName) onUserNameChange(nameText.trim())
                    editing = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
}

// ---------------------------------------------------------------- pages

@Composable
private fun AppearancePage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    palette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    prefs: AppPrefs,
    onPrefsChange: ((AppPrefs) -> AppPrefs) -> Unit,
    onEditHome: () -> Unit
) {
    ThemePreview()

    GroupLabel("Theme")
    SettingsGroup(padded = true) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                    onClick = { onThemeModeChange(mode) },
                    selected = mode == themeMode
                ) { Text(listOf("System", "Light", "Dark")[index]) }
            }
        }
        Spacer(Modifier.height(8.dp))
        SwitchSetting(
            "Nocturnal mode",
            "Dark between ${hourLabel(prefs.nocturnalFrom)} and ${hourLabel(prefs.nocturnalTo)}, whatever the theme",
            prefs.nocturnal
        ) { on -> onPrefsChange { it.copy(nocturnal = on) } }
        AnimatedVisibility(visible = prefs.nocturnal) {
            HourRange(prefs.nocturnalFrom, prefs.nocturnalTo,
                onFrom = { h -> onPrefsChange { it.copy(nocturnalFrom = h) } },
                onTo = { h -> onPrefsChange { it.copy(nocturnalTo = h) } })
        }
    }

    GroupLabel("Colours")
    SettingsGroup(padded = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SwitchSetting("Wallpaper colours", "Material You picks colours from your wallpaper", useDynamicColor, onCheckedChange = onUseDynamicColorChange)
            Spacer(Modifier.height(8.dp))
        }
        PaletteGrid(palette, dimmed = useDynamicColor) { pal ->
            onPaletteChange(pal)
            if (useDynamicColor) onUseDynamicColorChange(false)
        }
    }

    GroupLabel("Text")
    SettingsGroup(padded = true) {
        Text("Text size", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TEXT_SIZES.forEachIndexed { i, (scale, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(i, TEXT_SIZES.size),
                    onClick = { onPrefsChange { it.copy(textScale = scale) } },
                    selected = kotlin.math.abs(prefs.textScale - scale) < 0.01f,
                    icon = {}
                ) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
            }
        }
        Spacer(Modifier.height(8.dp))
        SwitchSetting("Serif font for notes", "Notes and quotes read like a printed page", prefs.serifNotes) { on -> onPrefsChange { it.copy(serifNotes = on) } }
    }

    GroupLabel("Home")
    SettingsGroup {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SwitchSetting("Sky behind the greeting", "Sun, moon and stars that follow the clock", prefs.homeSky) { on -> onPrefsChange { it.copy(homeSky = on) } }
        }
        GroupDivider()
        SettingRow(Icons.Filled.Dashboard, "Edit home screen", "Reorder or hide Home sections", onClick = onEditHome)
    }

    GroupLabel("App icon")
    SettingsGroup(padded = true) { AppIconPicker() }
}

/** A small mock of the app in the current colours, so palette and theme changes show at once. */
@Composable
private fun ThemePreview() {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(20.dp), color = scheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(scheme.primary, scheme.tertiary)))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("The book you're reading", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("p. 142 / 320", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { 0.44f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
            }
            Spacer(Modifier.width(14.dp))
            Surface(shape = CircleShape, color = scheme.primary, contentColor = scheme.onPrimary) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
            }
        }
    }
}

@Composable
private fun PaletteGrid(selected: AppPalette, dimmed: Boolean, onSelect: (AppPalette) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.alpha(if (dimmed) 0.45f else 1f)) {
        AppPalette.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { pal ->
                    val isSelected = pal == selected && !dimmed
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(onClickLabel = "Use ${pal.title}") { onSelect(pal) }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(36.dp)) {
                            Box(
                                Modifier.size(28.dp).align(Alignment.TopStart)
                                    .background(Color(pal.primaryHex), CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                            Box(
                                Modifier.size(20.dp).align(Alignment.BottomEnd)
                                    .background(Color(pal.secondaryHex), CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check, contentDescription = "Selected",
                                    tint = Color.Black.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp).align(Alignment.Center).offset(x = (-4).dp, y = (-4).dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            pal.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (dimmed) {
            Text("Using your wallpaper colours. Pick a palette to switch back.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingPage(
    dndDuringSession: Boolean,
    onDndChange: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    prefs: AppPrefs,
    onPrefsChange: ((AppPrefs) -> AppPrefs) -> Unit,
    onOpenGoals: () -> Unit
) {
    val context = LocalContext.current
    GroupLabel("Your week")
    SettingsGroup(padded = true) {
        Text("Week starts on", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(false to "Monday", true to "Sunday").forEachIndexed { i, (sunday, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    onClick = { onPrefsChange { it.copy(weekStartsSunday = sunday) } },
                    selected = prefs.weekStartsSunday == sunday
                ) { Text(label) }
            }
        }
        Text(
            "Used by the streak week, calendars and the heatmap.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }

    GroupLabel("Books")
    SettingsGroup(padded = true) {
        Text("Format for new books", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BookFormat.entries.forEach { format ->
                TickFilterChip(
                    selected = prefs.defaultFormat == format,
                    onClick = { onPrefsChange { it.copy(defaultFormat = format) } },
                    label = { Text(format.name) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SwitchSetting("Show time left", "\"About 4h left\" on Home and the reading widget", prefs.showTimeLeft) { on -> onPrefsChange { it.copy(showTimeLeft = on) } }
    }

    GroupLabel("While you read")
    SettingsGroup(padded = true) {
        SwitchSetting("Do Not Disturb", "Silence your phone while a reading session runs", dndDuringSession) { checked ->
            if (checked) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!nm.isNotificationPolicyAccessGranted) {
                    Toast.makeText(context, "Allow Do Not Disturb access for this app, then turn this on again", Toast.LENGTH_LONG).show()
                    context.safeStartActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                } else {
                    onDndChange(true)
                }
            } else {
                onDndChange(false)
            }
        }
        SwitchSetting("Keep the screen on", "While the reading timer is open", keepScreenOn, onCheckedChange = onKeepScreenOnChange)
        SwitchSetting(
            "Ask if I'm still reading",
            "After 30 minutes without a touch, the timer checks in and pauses if you've gone",
            prefs.stillReadingCheck
        ) { on -> onPrefsChange { it.copy(stillReadingCheck = on) } }
        SwitchSetting(
            "Scan the page when you finish",
            "Saving a session opens the camera on the page number",
            prefs.scanOnFinish
        ) { on -> onPrefsChange { it.copy(scanOnFinish = on) } }
        Spacer(Modifier.height(8.dp))
        Text("Shortest session to keep", style = MaterialTheme.typography.titleSmall)
        Text(
            "Anything shorter with no pages read is probably an accident, so saving it asks first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Keep all", 30 to "30 s", 60 to "1 min", 120 to "2 min", 300 to "5 min").forEach { (seconds, label) ->
                TickFilterChip(
                    selected = prefs.minSessionSeconds == seconds,
                    onClick = { onPrefsChange { it.copy(minSessionSeconds = seconds) } },
                    label = { Text(label) }
                )
            }
        }
    }

    SettingsGroup {
        SettingRow(Icons.Filled.Flag, "Goals, day start and reminder", "Daily, monthly and yearly targets", onClick = onOpenGoals)
    }
}

@Composable
private fun NotificationsPage(
    prefs: AppPrefs,
    onPrefsChange: ((AppPrefs) -> AppPrefs) -> Unit,
    reminder: ReminderSettings?,
    onOpenGoals: () -> Unit
) {
    val context = LocalContext.current
    var canPost by remember { mutableStateOf(Notifications.canPost(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canPost = it }
    if (!canPost) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Notifications are off for this app, so none of these can reach you.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Allow") }
            }
        }
    }

    GroupLabel("Reminders")
    SettingsGroup {
        SettingRow(
            Icons.Filled.Alarm, "Daily goal reminder",
            if (reminder?.enabled == true) "On at ${formatTime(context, reminder.hour, reminder.minute)} · set in Goals" else "Off · set in Goals",
            onClick = onOpenGoals
        )
        GroupDivider()
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SwitchSetting("Streak at risk", "Only on days you haven't read yet", prefs.streakNudge) { on -> onPrefsChange { it.copy(streakNudge = on) } }
            AnimatedVisibility(prefs.streakNudge) {
                HourChoice("Remind me at", prefs.streakNudgeHour) { h -> onPrefsChange { it.copy(streakNudgeHour = h) } }
            }
            SwitchSetting("Daily review", "When notes or words are due", prefs.reviewReminder) { on -> onPrefsChange { it.copy(reviewReminder = on) } }
            AnimatedVisibility(prefs.reviewReminder) {
                HourChoice("Remind me at", prefs.reviewReminderHour) { h -> onPrefsChange { it.copy(reviewReminderHour = h) } }
            }
            SwitchSetting("Weekly recap", "Pages, time and reading days, Sundays at 7 PM", prefs.weeklyRecap) { on -> onPrefsChange { it.copy(weeklyRecap = on) } }
        }
    }

    GroupLabel("Quiet hours")
    SettingsGroup(padded = true) {
        SwitchSetting(
            "Quiet hours",
            "No reminders between ${hourLabel(prefs.quietFrom)} and ${hourLabel(prefs.quietTo)}. Timer alerts still ring.",
            prefs.quietHours
        ) { on -> onPrefsChange { it.copy(quietHours = on) } }
        AnimatedVisibility(prefs.quietHours) {
            HourRange(prefs.quietFrom, prefs.quietTo,
                onFrom = { h -> onPrefsChange { it.copy(quietFrom = h) } },
                onTo = { h -> onPrefsChange { it.copy(quietTo = h) } })
        }
    }
}

@Composable
private fun PrivacyPage(
    useAppLock: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    prefs: AppPrefs,
    onPrefsChange: ((AppPrefs) -> AppPrefs) -> Unit
) {
    val context = LocalContext.current
    GroupLabel("App lock")
    SettingsGroup(padded = true) {
        SwitchSetting("Require unlock", "Fingerprint, face or screen lock to open the app", useAppLock) { enable ->
            if (enable) {
                val available = BiometricManager.from(context).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ) == BiometricManager.BIOMETRIC_SUCCESS
                if (available) onAppLockChange(true)
                else Toast.makeText(context, "Set up a screen lock or fingerprint on this device first", Toast.LENGTH_LONG).show()
            } else {
                onAppLockChange(false)
            }
        }
        AnimatedVisibility(useAppLock) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text("Lock again after leaving the app", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LOCK_DELAYS.forEachIndexed { i, (seconds, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, LOCK_DELAYS.size),
                            onClick = { onPrefsChange { it.copy(lockDelaySeconds = seconds) } },
                            selected = prefs.lockDelaySeconds == seconds,
                            icon = {}
                        ) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }

    GroupLabel("On screen")
    SettingsGroup(padded = true) {
        SwitchSetting("Hide titles outside the app", "Widgets and the timer notification show \"Your book\" and no cover", prefs.hideTitles) { on -> onPrefsChange { it.copy(hideTitles = on) } }
        SwitchSetting("Block screenshots", "Also hides the app's content in the recent apps list", prefs.secureScreen) { on -> onPrefsChange { it.copy(secureScreen = on) } }
    }
}

@Composable
private fun DataPage(
    prefs: AppPrefs,
    onPrefsChange: ((AppPrefs) -> AppPrefs) -> Unit,
    onImportCsv: (Uri, (CsvImportResult?) -> Unit) -> Unit,
    onImportBookmory: (Uri, (Result<BookmoryImportSummary>) -> Unit) -> Unit,
    onExportBackup: (Uri, (Boolean) -> Unit) -> Unit,
    onImportBackup: (Uri, (Int) -> Unit) -> Unit,
    onExportCsv: (Uri, (Boolean) -> Unit) -> Unit,
    onBackupNow: ((Boolean) -> Unit) -> Unit,
    onDeleteAll: ((Boolean) -> Unit) -> Unit,
    driveConnected: Boolean,
    driveSync: @Composable () -> Unit
) {
    val context = LocalContext.current
    val toast = { message: String -> Toast.makeText(context, message, Toast.LENGTH_LONG).show() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onExportBackup(uri) { ok -> toast(if (ok) "Backup saved" else "Backup failed") }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportBackup(uri) { count -> toast(if (count >= 0) "Restored $count books" else "Restore failed. Is that a backup file?") }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onImportCsv(uri) { result ->
                toast(
                    when {
                        result == null -> "Couldn't read that file. Export a CSV from Goodreads or StoryGraph and try again."
                        result.imported == 0 && result.skippedDuplicates == 0 -> "No books found in that file"
                        result.skippedDuplicates > 0 -> "Imported ${result.imported} books · ${result.skippedDuplicates} already in your library"
                        else -> "Imported ${result.imported} books"
                    }
                )
            }
        }
    }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) onExportCsv(uri) { ok -> toast(if (ok) "Library exported" else "Export failed") }
    }
    var bookmoryImporting by remember { mutableStateOf(false) }
    var bookmoryResult by remember { mutableStateOf<Result<BookmoryImportSummary>?>(null) }
    // .bookmory has no registered MIME type, so the picker has to allow any file.
    val bookmoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            bookmoryImporting = true
            onImportBookmory(uri) { result ->
                bookmoryImporting = false
                bookmoryResult = result
            }
        }
    }
    bookmoryResult?.let { BookmoryResultDialog(it) { bookmoryResult = null } }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onPrefsChange { it.copy(autoBackup = true, autoBackupFolder = tree.toString()) }
        }
    }
    var backingUp by remember { mutableStateOf(false) }

    GroupLabel("Backup")
    SettingsGroup(padded = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = {
                    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("nocturnal-reader-backup-$stamp.zip")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export")
            }
            // Restore merges into the library, so it looks quieter than Export.
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/json", "application/octet-stream")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restore")
            }
        }
        Spacer(Modifier.height(8.dp))
        InfoNote(
            "Everything in one .zip you keep wherever you like.",
            "Books, covers, sessions, notes and goals. Restoring merges by id, so it's safe on an existing library. " +
                "Your library is also part of your phone's automatic Google backup."
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(4.dp))
        SwitchSetting(
            "Weekly automatic backup",
            when {
                prefs.autoBackupFolder.isBlank() -> "Pick a folder, and a fresh backup lands there every week"
                prefs.lastAutoBackup > 0 -> "To ${folderName(prefs.autoBackupFolder)} · last ${relative(prefs.lastAutoBackup)}"
                else -> "To ${folderName(prefs.autoBackupFolder)} · keeps the newest 4"
            },
            prefs.autoBackup
        ) { on ->
            if (on && prefs.autoBackupFolder.isBlank()) folderLauncher.launch(null)
            else onPrefsChange { it.copy(autoBackup = on) }
        }
        AnimatedVisibility(prefs.autoBackup && prefs.autoBackupFolder.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { folderLauncher.launch(null) }) { Text("Change folder") }
                TextButton(enabled = !backingUp, onClick = {
                    backingUp = true
                    onBackupNow { ok ->
                        backingUp = false
                        toast(if (ok) "Backed up" else "Couldn't write to that folder. Pick it again?")
                    }
                }) { Text(if (backingUp) "Backing up…" else "Back up now") }
            }
        }
    }

    GroupLabel("Sync")
    driveSync()

    GroupLabel("Import & export")
    SettingsGroup {
        SettingRow(Icons.Filled.UploadFile, "Import from Goodreads or StoryGraph", "A CSV export. Books already here are skipped.", onClick = {
            csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel"))
        })
        GroupDivider()
        SettingRow(
            Icons.Filled.UploadFile, "Import a Bookmory backup",
            if (bookmoryImporting) "Importing…" else "Books, history, notes and photos. Safe to run again.",
            onClick = { if (!bookmoryImporting) bookmoryLauncher.launch(arrayOf("*/*")) }
        )
        GroupDivider()
        SettingRow(Icons.Filled.Description, "Export as CSV", "Goodreads layout, for spreadsheets and other apps", onClick = {
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            csvExportLauncher.launch("nocturnal-reader-library-$stamp.csv")
        })
    }

    GroupLabel("Storage")
    StorageCard()

    GroupLabel("Danger zone")
    DeleteAllCard(driveConnected, onDeleteAll)
}

@Composable
private fun StorageCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val sizes by produceState<Triple<Long, Long, Long>?>(null, refresh) {
        value = withContext(Dispatchers.IO) {
            val db = listOf("booktracker.db", "booktracker.db-wal", "booktracker.db-shm").sumOf { context.getDatabasePath(it).length() }
            val images = listOf("covers", NoteImageStore.DIR, "avatar").sumOf { dirSize(File(context.filesDir, it)) }
            Triple(db, images, dirSize(context.cacheDir))
        }
    }
    SettingsGroup(padded = true) {
        val s = sizes
        if (s == null) {
            Text("Measuring…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            StorageLine("Library", s.first)
            StorageLine("Covers and photos", s.second)
            StorageLine("Image cache", s.third)
            Spacer(Modifier.height(4.dp))
            TextButton(
                enabled = s.third > 0,
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val loader = coil3.SingletonImageLoader.get(context)
                            loader.memoryCache?.clear()
                            loader.diskCache?.clear()
                        }
                        refresh++
                    }
                },
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) { Text("Clear image cache") }
        }
    }
}

@Composable
private fun StorageLine(label: String, bytes: Long) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(Formatter.formatShortFileSize(context, bytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun dirSize(dir: File): Long = if (!dir.exists()) 0L else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

@Composable
private fun DeleteAllCard(driveConnected: Boolean, onDeleteAll: ((Boolean) -> Unit) -> Unit) {
    val context = LocalContext.current
    var confirming by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delete all data", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            Text(
                if (driveConnected) "Stop Google Drive sync first, or the next sync brings everything back."
                else "Every book, session, note, word, cover and photo. Your settings stay. This can't be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                enabled = !driveConnected,
                onClick = { confirming = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (driveConnected) 0.3f else 1f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete everything…")
            }
        }
    }
    if (confirming) {
        var typed by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { confirming = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete your whole library?") },
            text = {
                Column {
                    Text("Export a backup first if you might want it back. Type DELETE to confirm.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = typed, onValueChange = { typed = it.take(10) }, singleLine = true, placeholder = { Text("DELETE") })
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.trim() == "DELETE",
                    onClick = {
                        confirming = false
                        onDeleteAll { ok ->
                            Toast.makeText(context, if (ok) "Library deleted" else "Couldn't delete. Is Drive sync still on?", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BookmoryResultDialog(result: Result<BookmoryImportSummary>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text(if (result.isSuccess) "Bookmory import done" else "Couldn't import") },
        text = {
            val summary = result.getOrNull()
            Text(
                when {
                    summary == null ->
                        if (result.exceptionOrNull() is BookmoryImporter.NotABookmoryBackup) {
                            "That file isn't a Bookmory backup. In Bookmory, go to My page → Backup and Restore and save a backup file, then pick that file here."
                        } else {
                            "Something went wrong reading that backup. Nothing was changed."
                        }
                    summary.booksAdded + summary.sessions + summary.notes == 0 ->
                        "Everything in this backup is already in your library."
                    else -> buildString {
                        append("${summary.booksAdded} ${if (summary.booksAdded == 1) "book" else "books"} added")
                        if (summary.booksMatched > 0) {
                            append("\n${summary.booksMatched} already in your library; their history and notes were added to your copy")
                        }
                        append("\n${summary.sessions} reading ${if (summary.sessions == 1) "session" else "sessions"}")
                        append("\n${summary.notes} ${if (summary.notes == 1) "note" else "notes"}")
                        if (summary.photos > 0) append(" with ${summary.photos} ${if (summary.photos == 1) "photo" else "photos"}")
                        if (summary.notesSkipped > 0) append("\n${summary.notesSkipped} notes skipped: their book wasn't in the backup")
                        summary.dayStartHour?.let { hour ->
                            append("\nDay start set to ${if (hour == 0) "midnight" else "$hour:00"} to match Bookmory")
                        }
                    }
                }
            )
        }
    )
}

@Composable
private fun MotionPage(hapticsEnabled: Boolean, onHapticsChange: (Boolean) -> Unit, reduceMotion: Boolean, onReduceMotionChange: (Boolean) -> Unit) {
    SettingsGroup(padded = true) {
        SwitchSetting("Haptic feedback", "Taps, goals and unlocks", hapticsEnabled, onCheckedChange = onHapticsChange)
        SwitchSetting("Reduce motion", "No confetti, flames, stars or count-ups", reduceMotion, onCheckedChange = onReduceMotionChange)
        Text(
            "Android's own \"Remove animations\" setting is respected too.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AboutPage(onOpenPrivacyPolicy: () -> Unit) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Icon(Icons.Filled.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text(context.getString(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Version ${appVersionName(context)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(
            "Your library lives on this device. It only leaves when you export a backup or turn on Google Drive sync. No ads, no tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
    SettingsGroup {
        LinkRow(Icons.Filled.PrivacyTip, "Privacy policy", onClick = onOpenPrivacyPolicy)
        GroupDivider()
        LinkRow(Icons.Filled.StarRate, "Rate the app") {
            val pkg = context.packageName
            val opened = context.safeStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
            if (!opened) context.safeStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
        }
    }
}

// ---------------------------------------------------------------- building blocks

/** A rounded group of rows. [padded] for free-form content; rows bring their own padding. */
@Composable
private fun SettingsGroup(padded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = if (padded) Modifier.padding(16.dp) else Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 6.dp)
    )
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, summary: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!summary.isNullOrBlank()) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A switch with a title and a one-line explanation; the whole row toggles. */
@Composable
private fun SwitchSetting(title: String, summary: String?, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!summary.isNullOrBlank()) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** A one-line explanation with an ⓘ that opens the long version. */
@Composable
internal fun InfoNote(summary: String, details: String) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = { open = !open }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = if (open) "Hide details" else "More details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        AnimatedVisibility(open) {
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, end = 32.dp))
        }
    }
}

private fun hourLabel(hour: Int): String =
    LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("h a")).lowercase().replace(" ", " ")

@Composable
private fun HourChoice(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        HourDropdown(hour, onChange)
    }
}

@Composable
private fun HourRange(from: Int, to: Int, onFrom: (Int) -> Unit, onTo: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text("From", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        HourDropdown(from, onFrom)
        Spacer(Modifier.width(12.dp))
        Text("to", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        HourDropdown(to, onTo)
    }
}

@Composable
private fun HourDropdown(hour: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { open = true }, label = { Text(formatTime(context, hour, 0)) }, trailingIcon = {
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            (0..23).forEach { h ->
                DropdownMenuItem(text = { Text(formatTime(context, h, 0)) }, onClick = {
                    onChange(h)
                    open = false
                })
            }
        }
    }
}

private fun folderName(tree: String): String = runCatching {
    DocumentsContract.getTreeDocumentId(Uri.parse(tree)).substringAfter(':').ifBlank { "your folder" }
}.getOrDefault("your folder")

/** Launcher icon choices, drawn from the same layers the launcher uses. */
@Composable
private fun AppIconPicker() {
    val context = LocalContext.current
    val haptics = com.example.booktracker.app.ui.animations.rememberHaptics()
    var current by remember { mutableStateOf(AppIcon.current(context)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppIcon.entries.forEach { icon ->
            val selected = icon == current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable(onClickLabel = "Use the ${icon.title} icon") {
                        if (icon != current) {
                            AppIcon.apply(context, icon)
                            current = icon
                            haptics.confirm()
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .border(
                            width = if (selected) 2.5.dp else 0.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(4.dp)
                        .clip(RoundedCornerShape(15.dp))
                ) {
                    // Adaptive layers are 108dp with a 72dp visible window; scale so the window fills the tile.
                    Image(
                        painterResource(icon.background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                    )
                    Image(
                        painterResource(icon.foreground),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    icon.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "Your launcher may take a moment to show the new icon.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun SettingsCard(title: String, icon: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(12.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
internal fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
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

internal fun snap(raw: Float): Int =
    (Math.round(raw / GOAL_STEP) * GOAL_STEP).coerceIn(GOAL_MIN, GOAL_MAX)

@Composable
internal fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

/** Clock time in the device's 12/24-hour style, e.g. "8:00 PM" or "20:00". */
internal fun formatTime(context: Context, hour: Int, minute: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(pattern))
}

private fun appVersionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "—"

/** Starts [intent] if any app can handle it; returns false instead of crashing when none can. */
internal fun Context.safeStartActivity(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}
