package com.example.booktracker.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.shared.models.BookFormat
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Top-level, as the delegate requires: one DataStore per name per process.
private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

// Enum names are persisted in settings, so they stay fixed even when the display title changes.
enum class AppPalette(val title: String, val subtitle: String, val primaryHex: Long, val secondaryHex: Long) {
    BOOKMORY_AMBER("Literary Amber", "Literary gold & warm terracotta", 0xFFF59E0BL, 0xFFFB923CL),
    MIDNIGHT_VELVET("Midnight Velvet", "Electric indigo & cyan twilight", 0xFF818CF8L, 0xFF38BDF8L),
    NORDIC_EMERALD("Nordic Emerald", "Botanical sage & forest pine", 0xFF34D399L, 0xFF2DD4BFL),
    SAKURA_ROSE("Sakura Rose", "Sunset coral & blush rose", 0xFFFB7185L, 0xFFF472B6L),
    ESPRESSO_CARAMEL("Espresso & Leather", "Vintage leather & roasted coffee", 0xFFF59E0BL, 0xFFEA580CL),
    MATCHA("Matcha", "Whisked green tea & botanical moss", 0xFF9CC07AL, 0xFFC9B27CL),
    NORDIC_FOG("Nordic Fog", "Cool slate & misty fjord blue", 0xFF9FB4C7L, 0xFFB8C4CEL),
    WARM_PAPYRUS("Warm Papyrus", "Parchment, sepia ink & old maps", 0xFFB7793BL, 0xFF8C6A43L),
    OLED_MIDNIGHT("OLED Midnight", "True black with moonlit silver", 0xFFE2E8F0L, 0xFF94A3B8L)
}

data class ReminderSettings(val enabled: Boolean, val hour: Int, val minute: Int)

/** True when [hour] falls in the window from [from] up to [to], which may run past midnight (22 -> 7). */
fun hourInWindow(hour: Int, from: Int, to: Int): Boolean =
    if (from <= to) hour in from until to else hour >= from || hour < to

/**
 * The finer preferences behind Settings' sub-pages, read and written as one
 * value so a new one is a field here plus a key below.
 */
data class AppPrefs(
    // Reading
    val weekStartsSunday: Boolean = false,
    val showTimeLeft: Boolean = true,
    val defaultFormat: BookFormat = BookFormat.Paperback,
    /** The timer asks "still reading?" after a long stretch untouched, and pauses if nobody answers. */
    val stillReadingCheck: Boolean = true,
    /** While the timer is open, the volume keys step the page forward and back. */
    val volumeKeysTurnPages: Boolean = false,
    /** Finishing a session opens the camera on the page number instead of the page wheel. */
    val scanOnFinish: Boolean = false,
    /** A session shorter than this with no pages is treated as an accident; 0 keeps everything. */
    val minSessionSeconds: Int = 60,
    /** Monthly spend on books the wishlist measures against, in the reader's currency; 0 is no budget. */
    val monthlyBookBudget: Int = 0,
    /** A notification on the day a wished-for book comes out. */
    val releaseReminders: Boolean = true,
    /** The first-run welcome has been seen (or skipped, for readers who already had books). */
    val onboarded: Boolean = false,
    // Appearance
    /** Dark theme between [nocturnalFrom] and [nocturnalTo] whatever the theme choice. */
    val nocturnal: Boolean = false,
    val nocturnalFrom: Int = 21,
    val nocturnalTo: Int = 7,
    /** Multiplies the system font scale. */
    val textScale: Float = 1f,
    val serifNotes: Boolean = false,
    val homeSky: Boolean = true,
    // Notifications
    val streakNudge: Boolean = false,
    val streakNudgeHour: Int = 21,
    val weeklyRecap: Boolean = false,
    val reviewReminder: Boolean = false,
    val reviewReminderHour: Int = 9,
    val quietHours: Boolean = false,
    val quietFrom: Int = 22,
    val quietTo: Int = 8,
    // Privacy
    /** Widgets and the timer notification show "Your book" and no cover. */
    val hideTitles: Boolean = false,
    /** How long the app may sit in the background before App lock asks again. */
    val lockDelaySeconds: Int = 60,
    /** FLAG_SECURE: no screenshots, and a blank card in Recents. */
    val secureScreen: Boolean = false,
    // Data
    val autoBackup: Boolean = false,
    /** Tree URI of the folder the weekly backup goes to; blank until one is picked. */
    val autoBackupFolder: String = "",
    val lastAutoBackup: Long = 0L
) {
    val weekStart: DayOfWeek get() = if (weekStartsSunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    fun isQuiet(hour: Int) = quietHours && hourInWindow(hour, quietFrom, quietTo)

    fun isNocturnal(hour: Int) = nocturnal && hourInWindow(hour, nocturnalFrom, nocturnalTo)
}

/** User preferences: daily page goal (streaks + analytics) and yearly books goal. */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dailyGoalKey = intPreferencesKey("daily_goal_pages")
    private val yearlyGoalKey = intPreferencesKey("yearly_goal_books")
    private val monthlyGoalPagesKey = intPreferencesKey("monthly_goal_pages")
    private val monthlyGoalBooksKey = intPreferencesKey("monthly_goal_books")
    private val dndKey = booleanPreferencesKey("dnd_during_session")
    private val themeModeKey = intPreferencesKey("theme_mode")
    private val useDynamicColorKey = booleanPreferencesKey("use_dynamic_color")
    private val appPaletteKey = androidx.datastore.preferences.core.stringPreferencesKey("app_palette")
    private val userNameKey = androidx.datastore.preferences.core.stringPreferencesKey("user_name")
    private val workMinutesKey = intPreferencesKey("work_minutes")
    private val preferredTimerModeKey = androidx.datastore.preferences.core.stringPreferencesKey("preferred_timer_mode")
    private val dayStartsAtHourKey = intPreferencesKey("day_starts_at_hour")
    private val useAppLockKey = booleanPreferencesKey("use_app_lock")
    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")
    private val homeLayoutKey = androidx.datastore.preferences.core.stringPreferencesKey("home_layout")
    private val avatarKey = androidx.datastore.preferences.core.stringPreferencesKey("avatar")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val reduceMotionKey = booleanPreferencesKey("reduce_motion")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on_timer")
    private val driveSyncKey = booleanPreferencesKey("drive_sync_enabled")
    private val driveLastSyncKey = androidx.datastore.preferences.core.longPreferencesKey("drive_last_sync")

    private val activeTimerModeKey = androidx.datastore.preferences.core.stringPreferencesKey("active_timer_mode")
    private val activeTimerBookIdKey = androidx.datastore.preferences.core.stringPreferencesKey("active_timer_book_id")
    private val activeTimerIsRunningKey = androidx.datastore.preferences.core.booleanPreferencesKey("active_timer_is_running")
    private val activeTimerTimeLeftKey = intPreferencesKey("active_timer_time_left")
    private val activeTimerElapsedKey = intPreferencesKey("active_timer_elapsed")
    private val activeTimerActiveReadingKey = intPreferencesKey("active_timer_active_reading")
    private val activeTimerLastTickTimeKey = androidx.datastore.preferences.core.longPreferencesKey("active_timer_last_tick_time")
    private val activeTimerPageTargetKey = intPreferencesKey("active_timer_page_target")

    val dailyGoal: Flow<Int> = appContext.settingsDataStore.data.map { it[dailyGoalKey] ?: 20 }
    val yearlyGoal: Flow<Int> = appContext.settingsDataStore.data.map { it[yearlyGoalKey] ?: Companion.DEFAULT_YEARLY_GOAL_BOOKS }
    val monthlyGoalPages: Flow<Int> = appContext.settingsDataStore.data.map { it[monthlyGoalPagesKey] ?: 500 }
    val monthlyGoalBooks: Flow<Int> = appContext.settingsDataStore.data.map { it[monthlyGoalBooksKey] ?: 2 }
    val dndDuringSession: Flow<Boolean> = appContext.settingsDataStore.data.map { it[dndKey] ?: false }
    val themeMode: Flow<Int> = appContext.settingsDataStore.data.map { it[themeModeKey] ?: ThemeMode.SYSTEM.ordinal }
    val useDynamicColor: Flow<Boolean> = appContext.settingsDataStore.data.map { it[useDynamicColorKey] ?: false }
    val appPalette: Flow<AppPalette> = appContext.settingsDataStore.data.map { prefs ->
        val name = prefs[appPaletteKey] ?: AppPalette.BOOKMORY_AMBER.name
        try {
            AppPalette.valueOf(name)
        } catch (_: Exception) {
            AppPalette.BOOKMORY_AMBER
        }
    }
    val userName: Flow<String> = appContext.settingsDataStore.data.map { it[userNameKey] ?: "Reader" }
    val workMinutes: Flow<Int> = appContext.settingsDataStore.data.map { it[workMinutesKey] ?: 25 }
    /**
     * The timer mode the reader last chose ("STOPWATCH" or "COUNTDOWN"), so the
     * next session opens the same way. A fresh install counts up: most readers
     * just want to track their time, not race a clock.
     */
    val preferredTimerMode: Flow<String> = appContext.settingsDataStore.data.map { it[preferredTimerModeKey] ?: "STOPWATCH" }
    val dayStartsAtHour: Flow<Int> = appContext.settingsDataStore.data.map { it[dayStartsAtHourKey] ?: 3 } // Default 3 AM
    val useAppLock: Flow<Boolean> = appContext.settingsDataStore.data.map { it[useAppLockKey] ?: false }
    val homeLayout: Flow<HomeLayout> = appContext.settingsDataStore.data.map { HomeLayout.decode(it[homeLayoutKey]) }
    /** Profile picture in [AvatarStore] form; blank shows the name's initial. */
    val avatar: Flow<String> = appContext.settingsDataStore.data.map { it[avatarKey] ?: "" }
    /** Taps, ticks and celebration buzzes; on by default. */
    val hapticsEnabled: Flow<Boolean> = appContext.settingsDataStore.data.map { it[hapticsKey] ?: true }
    /** Calmer app: no loops, particles or count-ups, on top of the system setting. */
    val reduceMotion: Flow<Boolean> = appContext.settingsDataStore.data.map { it[reduceMotionKey] ?: false }
    /** Keep the display awake while the reading timer is open, so a paper book never gets a locked phone. */
    val keepScreenOn: Flow<Boolean> = appContext.settingsDataStore.data.map { it[keepScreenOnKey] ?: true }
    /** Google Drive sync; off until the reader connects an account. */
    val driveSyncEnabled: Flow<Boolean> = appContext.settingsDataStore.data.map { it[driveSyncKey] ?: false }
    /** When the last Drive sync finished (epoch millis); 0 when it never has. */
    val driveLastSync: Flow<Long> = appContext.settingsDataStore.data.map { it[driveLastSyncKey] ?: 0L }

    /** Daily reading reminder: off until the reader opts in, 8:00 PM by default. */
    val reminder: Flow<ReminderSettings> = appContext.settingsDataStore.data.map {
        ReminderSettings(
            enabled = it[reminderEnabledKey] ?: false,
            hour = it[reminderHourKey] ?: 20,
            minute = it[reminderMinuteKey] ?: 0
        )
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        appContext.settingsDataStore.edit {
            it[reminderEnabledKey] = enabled
            it[reminderHourKey] = hour.coerceIn(0, 23)
            it[reminderMinuteKey] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setDailyGoal(pages: Int) { appContext.settingsDataStore.edit { it[dailyGoalKey] = pages } }
    suspend fun setYearlyGoal(books: Int) { appContext.settingsDataStore.edit { it[yearlyGoalKey] = books } }
    suspend fun setMonthlyGoalPages(pages: Int) { appContext.settingsDataStore.edit { it[monthlyGoalPagesKey] = pages } }
    suspend fun setMonthlyGoalBooks(books: Int) { appContext.settingsDataStore.edit { it[monthlyGoalBooksKey] = books } }
    suspend fun setDndDuringSession(enabled: Boolean) { appContext.settingsDataStore.edit { it[dndKey] = enabled } }
    suspend fun setThemeMode(mode: Int) { appContext.settingsDataStore.edit { it[themeModeKey] = mode } }
    suspend fun setUseDynamicColor(enabled: Boolean) { appContext.settingsDataStore.edit { it[useDynamicColorKey] = enabled } }
    suspend fun setAppPalette(palette: AppPalette) { appContext.settingsDataStore.edit { it[appPaletteKey] = palette.name } }
    suspend fun setUserName(name: String) { appContext.settingsDataStore.edit { it[userNameKey] = name } }
    suspend fun setWorkMinutes(mins: Int) { appContext.settingsDataStore.edit { it[workMinutesKey] = mins } }
    suspend fun setPreferredTimerMode(mode: String) { appContext.settingsDataStore.edit { it[preferredTimerModeKey] = mode } }
    suspend fun setDayStartsAtHour(hour: Int) { appContext.settingsDataStore.edit { it[dayStartsAtHourKey] = hour } }
    suspend fun setUseAppLock(enabled: Boolean) { appContext.settingsDataStore.edit { it[useAppLockKey] = enabled } }
    suspend fun setHomeLayout(layout: HomeLayout) { appContext.settingsDataStore.edit { it[homeLayoutKey] = layout.encode() } }
    suspend fun setAvatar(avatar: String) { appContext.settingsDataStore.edit { it[avatarKey] = avatar } }
    suspend fun setHapticsEnabled(enabled: Boolean) { appContext.settingsDataStore.edit { it[hapticsKey] = enabled } }
    suspend fun setReduceMotion(enabled: Boolean) { appContext.settingsDataStore.edit { it[reduceMotionKey] = enabled } }
    suspend fun setKeepScreenOn(enabled: Boolean) { appContext.settingsDataStore.edit { it[keepScreenOnKey] = enabled } }
    suspend fun setDriveSyncEnabled(enabled: Boolean) { appContext.settingsDataStore.edit { it[driveSyncKey] = enabled } }
    suspend fun setDriveLastSync(at: Long) { appContext.settingsDataStore.edit { it[driveLastSyncKey] = at } }

    val activeTimerMode: Flow<String?> = appContext.settingsDataStore.data.map { it[activeTimerModeKey] }
    val activeTimerBookId: Flow<String?> = appContext.settingsDataStore.data.map { it[activeTimerBookIdKey] }
    val activeTimerIsRunning: Flow<Boolean?> = appContext.settingsDataStore.data.map { it[activeTimerIsRunningKey] }
    val activeTimerTimeLeft: Flow<Int?> = appContext.settingsDataStore.data.map { it[activeTimerTimeLeftKey] }
    val activeTimerElapsed: Flow<Int?> = appContext.settingsDataStore.data.map { it[activeTimerElapsedKey] }
    val activeTimerActiveReading: Flow<Int?> = appContext.settingsDataStore.data.map { it[activeTimerActiveReadingKey] }
    val activeTimerLastTickTime: Flow<Long?> = appContext.settingsDataStore.data.map { it[activeTimerLastTickTimeKey] }
    val activeTimerPageTarget: Flow<Int?> = appContext.settingsDataStore.data.map { it[activeTimerPageTargetKey] }

    suspend fun saveTimerState(
        mode: String?,
        bookId: String?,
        isRunning: Boolean?,
        timeLeft: Int?,
        elapsed: Int?,
        activeReading: Int?,
        lastTickTime: Long?,
        pageTarget: Int? = null
    ) {
        appContext.settingsDataStore.edit { prefs ->
            if (mode != null) prefs[activeTimerModeKey] = mode else prefs.remove(activeTimerModeKey)
            if (bookId != null) prefs[activeTimerBookIdKey] = bookId else prefs.remove(activeTimerBookIdKey)
            if (isRunning != null) prefs[activeTimerIsRunningKey] = isRunning else prefs.remove(activeTimerIsRunningKey)
            if (timeLeft != null) prefs[activeTimerTimeLeftKey] = timeLeft else prefs.remove(activeTimerTimeLeftKey)
            if (elapsed != null) prefs[activeTimerElapsedKey] = elapsed else prefs.remove(activeTimerElapsedKey)
            if (activeReading != null) prefs[activeTimerActiveReadingKey] = activeReading else prefs.remove(activeTimerActiveReadingKey)
            if (lastTickTime != null) prefs[activeTimerLastTickTimeKey] = lastTickTime else prefs.remove(activeTimerLastTickTimeKey)
            if (pageTarget != null) prefs[activeTimerPageTargetKey] = pageTarget else prefs.remove(activeTimerPageTargetKey)
        }
    }

    suspend fun clearTimerState() {
        appContext.settingsDataStore.edit { prefs ->
            prefs.remove(activeTimerModeKey)
            prefs.remove(activeTimerBookIdKey)
            prefs.remove(activeTimerIsRunningKey)
            prefs.remove(activeTimerTimeLeftKey)
            prefs.remove(activeTimerElapsedKey)
            prefs.remove(activeTimerActiveReadingKey)
            prefs.remove(activeTimerLastTickTimeKey)
            prefs.remove(activeTimerPageTargetKey)
        }
    }

    private val seenBadgesKey = androidx.datastore.preferences.core.stringSetPreferencesKey("seen_badges")

    val seenBadges: Flow<Set<String>> = appContext.settingsDataStore.data.map { prefs ->
        prefs[seenBadgesKey] ?: emptySet()
    }

    suspend fun markBadgesSeen(badgeIds: Set<String>) {
        appContext.settingsDataStore.edit { prefs ->
            val current = prefs[seenBadgesKey] ?: emptySet()
            prefs[seenBadgesKey] = current + badgeIds
        }
    }

    private object PrefKeys {
        val weekStartsSunday = booleanPreferencesKey("pref_week_starts_sunday")
        val showTimeLeft = booleanPreferencesKey("pref_show_time_left")
        val defaultFormat = androidx.datastore.preferences.core.stringPreferencesKey("pref_default_format")
        val stillReadingCheck = booleanPreferencesKey("pref_still_reading_check")
        val volumeKeysTurnPages = booleanPreferencesKey("pref_volume_keys_turn_pages")
        val scanOnFinish = booleanPreferencesKey("pref_scan_on_finish")
        val minSessionSeconds = intPreferencesKey("pref_min_session_seconds")
        val monthlyBookBudget = intPreferencesKey("pref_monthly_book_budget")
        val releaseReminders = booleanPreferencesKey("pref_release_reminders")
        val onboarded = booleanPreferencesKey("pref_onboarded")
        val nocturnal =booleanPreferencesKey("pref_nocturnal")
        val nocturnalFrom = intPreferencesKey("pref_nocturnal_from")
        val nocturnalTo = intPreferencesKey("pref_nocturnal_to")
        val textScale = androidx.datastore.preferences.core.floatPreferencesKey("pref_text_scale")
        val serifNotes = booleanPreferencesKey("pref_serif_notes")
        val homeSky = booleanPreferencesKey("pref_home_sky")
        val streakNudge = booleanPreferencesKey("pref_streak_nudge")
        val streakNudgeHour = intPreferencesKey("pref_streak_nudge_hour")
        val weeklyRecap = booleanPreferencesKey("pref_weekly_recap")
        val reviewReminder = booleanPreferencesKey("pref_review_reminder")
        val reviewReminderHour = intPreferencesKey("pref_review_reminder_hour")
        val quietHours = booleanPreferencesKey("pref_quiet_hours")
        val quietFrom = intPreferencesKey("pref_quiet_from")
        val quietTo = intPreferencesKey("pref_quiet_to")
        val hideTitles = booleanPreferencesKey("pref_hide_titles")
        val lockDelaySeconds = intPreferencesKey("pref_lock_delay_seconds")
        val secureScreen = booleanPreferencesKey("pref_secure_screen")
        val autoBackup = booleanPreferencesKey("pref_auto_backup")
        val autoBackupFolder = androidx.datastore.preferences.core.stringPreferencesKey("pref_auto_backup_folder")
        val lastAutoBackup = androidx.datastore.preferences.core.longPreferencesKey("pref_last_auto_backup")
    }

    private fun readPrefs(p: Preferences): AppPrefs {
        val d = AppPrefs()
        return AppPrefs(
            weekStartsSunday = p[PrefKeys.weekStartsSunday] ?: d.weekStartsSunday,
            showTimeLeft = p[PrefKeys.showTimeLeft] ?: d.showTimeLeft,
            defaultFormat = p[PrefKeys.defaultFormat]?.let { name -> BookFormat.entries.firstOrNull { it.name == name } } ?: d.defaultFormat,
            stillReadingCheck = p[PrefKeys.stillReadingCheck] ?: d.stillReadingCheck,
            volumeKeysTurnPages = p[PrefKeys.volumeKeysTurnPages] ?: d.volumeKeysTurnPages,
            scanOnFinish = p[PrefKeys.scanOnFinish] ?: d.scanOnFinish,
            minSessionSeconds = p[PrefKeys.minSessionSeconds] ?: d.minSessionSeconds,
            monthlyBookBudget = p[PrefKeys.monthlyBookBudget] ?: d.monthlyBookBudget,
            releaseReminders = p[PrefKeys.releaseReminders] ?: d.releaseReminders,
            onboarded = p[PrefKeys.onboarded] ?: d.onboarded,
            nocturnal = p[PrefKeys.nocturnal] ?: d.nocturnal,
            nocturnalFrom = p[PrefKeys.nocturnalFrom] ?: d.nocturnalFrom,
            nocturnalTo = p[PrefKeys.nocturnalTo] ?: d.nocturnalTo,
            textScale = p[PrefKeys.textScale] ?: d.textScale,
            serifNotes = p[PrefKeys.serifNotes] ?: d.serifNotes,
            homeSky = p[PrefKeys.homeSky] ?: d.homeSky,
            streakNudge = p[PrefKeys.streakNudge] ?: d.streakNudge,
            streakNudgeHour = p[PrefKeys.streakNudgeHour] ?: d.streakNudgeHour,
            weeklyRecap = p[PrefKeys.weeklyRecap] ?: d.weeklyRecap,
            reviewReminder = p[PrefKeys.reviewReminder] ?: d.reviewReminder,
            reviewReminderHour = p[PrefKeys.reviewReminderHour] ?: d.reviewReminderHour,
            quietHours = p[PrefKeys.quietHours] ?: d.quietHours,
            quietFrom = p[PrefKeys.quietFrom] ?: d.quietFrom,
            quietTo = p[PrefKeys.quietTo] ?: d.quietTo,
            hideTitles = p[PrefKeys.hideTitles] ?: d.hideTitles,
            lockDelaySeconds = p[PrefKeys.lockDelaySeconds] ?: d.lockDelaySeconds,
            secureScreen = p[PrefKeys.secureScreen] ?: d.secureScreen,
            autoBackup = p[PrefKeys.autoBackup] ?: d.autoBackup,
            autoBackupFolder = p[PrefKeys.autoBackupFolder] ?: d.autoBackupFolder,
            lastAutoBackup = p[PrefKeys.lastAutoBackup] ?: d.lastAutoBackup
        )
    }

    val prefs: Flow<AppPrefs> = appContext.settingsDataStore.data.map(::readPrefs)

    /** Reads the current [AppPrefs], applies [transform] and writes the result back in one edit. */
    suspend fun updatePrefs(transform: (AppPrefs) -> AppPrefs) {
        appContext.settingsDataStore.edit { p ->
            val next = transform(readPrefs(p))
            p[PrefKeys.weekStartsSunday] = next.weekStartsSunday
            p[PrefKeys.showTimeLeft] = next.showTimeLeft
            p[PrefKeys.defaultFormat] = next.defaultFormat.name
            p[PrefKeys.stillReadingCheck] = next.stillReadingCheck
            p[PrefKeys.volumeKeysTurnPages] = next.volumeKeysTurnPages
            p[PrefKeys.scanOnFinish] = next.scanOnFinish
            p[PrefKeys.minSessionSeconds] = next.minSessionSeconds.coerceIn(0, 600)
            p[PrefKeys.monthlyBookBudget] = next.monthlyBookBudget.coerceAtLeast(0)
            p[PrefKeys.releaseReminders] = next.releaseReminders
            p[PrefKeys.onboarded] = next.onboarded
            p[PrefKeys.nocturnal] = next.nocturnal
            p[PrefKeys.nocturnalFrom] = next.nocturnalFrom.coerceIn(0, 23)
            p[PrefKeys.nocturnalTo] = next.nocturnalTo.coerceIn(0, 23)
            p[PrefKeys.textScale] = next.textScale.coerceIn(0.8f, 1.4f)
            p[PrefKeys.serifNotes] = next.serifNotes
            p[PrefKeys.homeSky] = next.homeSky
            p[PrefKeys.streakNudge] = next.streakNudge
            p[PrefKeys.streakNudgeHour] = next.streakNudgeHour.coerceIn(0, 23)
            p[PrefKeys.weeklyRecap] = next.weeklyRecap
            p[PrefKeys.reviewReminder] = next.reviewReminder
            p[PrefKeys.reviewReminderHour] = next.reviewReminderHour.coerceIn(0, 23)
            p[PrefKeys.quietHours] = next.quietHours
            p[PrefKeys.quietFrom] = next.quietFrom.coerceIn(0, 23)
            p[PrefKeys.quietTo] = next.quietTo.coerceIn(0, 23)
            p[PrefKeys.hideTitles] = next.hideTitles
            p[PrefKeys.lockDelaySeconds] = next.lockDelaySeconds.coerceAtLeast(0)
            p[PrefKeys.secureScreen] = next.secureScreen
            p[PrefKeys.autoBackup] = next.autoBackup
            p[PrefKeys.autoBackupFolder] = next.autoBackupFolder
            p[PrefKeys.lastAutoBackup] = next.lastAutoBackup
        }
    }

    /** The preferences a backup file carries, as they stand now. */
    suspend fun backupSettings(): BackupEngine.BackupSettings = BackupEngine.BackupSettings(
        dailyGoal = dailyGoal.first(),
        yearlyGoal = yearlyGoal.first(),
        monthlyGoalPages = monthlyGoalPages.first(),
        monthlyGoalBooks = monthlyGoalBooks.first(),
        dayStartsAtHour = dayStartsAtHour.first(),
        homeLayout = homeLayout.first().encode(),
        avatar = avatar.first()
    )

    companion object {
        const val DEFAULT_YEARLY_GOAL_BOOKS = 12
    }
}
