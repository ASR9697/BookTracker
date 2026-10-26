package com.example.booktracker.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.booktracker.app.analytics.StreakEngine
import kotlinx.coroutines.flow.Flow
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
    ESPRESSO_CARAMEL("Espresso Caramel", "Vintage leather & roasted coffee", 0xFFF59E0BL, 0xFFEA580CL)
}

data class ReminderSettings(val enabled: Boolean, val hour: Int, val minute: Int)

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
    private val breakMinutesKey = intPreferencesKey("break_minutes")
    private val dayStartsAtHourKey = intPreferencesKey("day_starts_at_hour")
    private val useAppLockKey = booleanPreferencesKey("use_app_lock")
    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")
    private val homeLayoutKey = androidx.datastore.preferences.core.stringPreferencesKey("home_layout")
    
    private val activeTimerModeKey = androidx.datastore.preferences.core.stringPreferencesKey("active_timer_mode")
    private val activeTimerBookIdKey = androidx.datastore.preferences.core.stringPreferencesKey("active_timer_book_id")
    private val activeTimerPhaseKey = androidx.datastore.preferences.core.stringPreferencesKey("active_timer_phase")
    private val activeTimerIsRunningKey = androidx.datastore.preferences.core.booleanPreferencesKey("active_timer_is_running")
    private val activeTimerTimeLeftKey = intPreferencesKey("active_timer_time_left")
    private val activeTimerElapsedKey = intPreferencesKey("active_timer_elapsed")
    private val activeTimerActiveReadingKey = intPreferencesKey("active_timer_active_reading")
    private val activeTimerLastTickTimeKey = androidx.datastore.preferences.core.longPreferencesKey("active_timer_last_tick_time")

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
    val breakMinutes: Flow<Int> = appContext.settingsDataStore.data.map { it[breakMinutesKey] ?: 5 }
    val dayStartsAtHour: Flow<Int> = appContext.settingsDataStore.data.map { it[dayStartsAtHourKey] ?: 3 } // Default 3 AM
    val useAppLock: Flow<Boolean> = appContext.settingsDataStore.data.map { it[useAppLockKey] ?: false }
    val homeLayout: Flow<HomeLayout> = appContext.settingsDataStore.data.map { HomeLayout.decode(it[homeLayoutKey]) }

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
    suspend fun setBreakMinutes(mins: Int) { appContext.settingsDataStore.edit { it[breakMinutesKey] = mins } }
    suspend fun setDayStartsAtHour(hour: Int) { appContext.settingsDataStore.edit { it[dayStartsAtHourKey] = hour } }
    suspend fun setUseAppLock(enabled: Boolean) { appContext.settingsDataStore.edit { it[useAppLockKey] = enabled } }
    suspend fun setHomeLayout(layout: HomeLayout) { appContext.settingsDataStore.edit { it[homeLayoutKey] = layout.encode() } }

    val activeTimerMode: Flow<String?> = appContext.settingsDataStore.data.map { it[activeTimerModeKey] }
    val activeTimerBookId: Flow<String?> = appContext.settingsDataStore.data.map { it[activeTimerBookIdKey] }
    val activeTimerPhase: Flow<String?> = appContext.settingsDataStore.data.map { it[activeTimerPhaseKey] }
    val activeTimerIsRunning: Flow<Boolean?> = appContext.settingsDataStore.data.map { it[activeTimerIsRunningKey] }
    val activeTimerTimeLeft: Flow<Int?> = appContext.settingsDataStore.data.map { it[activeTimerTimeLeftKey] }
    val activeTimerElapsed: Flow<Int?> = appContext.settingsDataStore.data.map { it[activeTimerElapsedKey] }
    val activeTimerActiveReading: Flow<Int?> = appContext.settingsDataStore.data.map { it[activeTimerActiveReadingKey] }
    val activeTimerLastTickTime: Flow<Long?> = appContext.settingsDataStore.data.map { it[activeTimerLastTickTimeKey] }

    suspend fun saveTimerState(
        mode: String?,
        bookId: String?,
        phase: String?,
        isRunning: Boolean?,
        timeLeft: Int?,
        elapsed: Int?,
        activeReading: Int?,
        lastTickTime: Long?
    ) {
        appContext.settingsDataStore.edit { prefs ->
            if (mode != null) prefs[activeTimerModeKey] = mode else prefs.remove(activeTimerModeKey)
            if (bookId != null) prefs[activeTimerBookIdKey] = bookId else prefs.remove(activeTimerBookIdKey)
            if (phase != null) prefs[activeTimerPhaseKey] = phase else prefs.remove(activeTimerPhaseKey)
            if (isRunning != null) prefs[activeTimerIsRunningKey] = isRunning else prefs.remove(activeTimerIsRunningKey)
            if (timeLeft != null) prefs[activeTimerTimeLeftKey] = timeLeft else prefs.remove(activeTimerTimeLeftKey)
            if (elapsed != null) prefs[activeTimerElapsedKey] = elapsed else prefs.remove(activeTimerElapsedKey)
            if (activeReading != null) prefs[activeTimerActiveReadingKey] = activeReading else prefs.remove(activeTimerActiveReadingKey)
            if (lastTickTime != null) prefs[activeTimerLastTickTimeKey] = lastTickTime else prefs.remove(activeTimerLastTickTimeKey)
        }
    }

    suspend fun clearTimerState() {
        appContext.settingsDataStore.edit { prefs ->
            prefs.remove(activeTimerModeKey)
            prefs.remove(activeTimerBookIdKey)
            prefs.remove(activeTimerPhaseKey)
            prefs.remove(activeTimerIsRunningKey)
            prefs.remove(activeTimerTimeLeftKey)
            prefs.remove(activeTimerElapsedKey)
            prefs.remove(activeTimerActiveReadingKey)
            prefs.remove(activeTimerLastTickTimeKey)
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

    companion object {
        const val DEFAULT_YEARLY_GOAL_BOOKS = 12
    }
}
