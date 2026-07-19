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

/** User preferences: daily page goal (streaks + analytics) and yearly books goal. */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dailyGoalKey = intPreferencesKey("daily_goal_pages")
    private val yearlyGoalKey = intPreferencesKey("yearly_goal_books")
    private val dndKey = booleanPreferencesKey("dnd_during_session")
    private val themeModeKey = intPreferencesKey("theme_mode")
    private val useDynamicColorKey = booleanPreferencesKey("use_dynamic_color")
    private val userNameKey = androidx.datastore.preferences.core.stringPreferencesKey("user_name")
    private val workMinutesKey = intPreferencesKey("work_minutes")
    private val breakMinutesKey = intPreferencesKey("break_minutes")

    val userName: Flow<String> = appContext.settingsDataStore.data.map { prefs ->
        prefs[userNameKey] ?: "Reader"
    }

    suspend fun setUserName(name: String) {
        appContext.settingsDataStore.edit { it[userNameKey] = name }
    }

    val dailyGoal: Flow<Int> = appContext.settingsDataStore.data.map { prefs ->
        prefs[dailyGoalKey] ?: StreakEngine.DEFAULT_DAILY_GOAL_PAGES
    }

    suspend fun setDailyGoal(pages: Int) {
        appContext.settingsDataStore.edit { it[dailyGoalKey] = pages }
    }

    val yearlyGoal: Flow<Int> = appContext.settingsDataStore.data.map { prefs ->
        prefs[yearlyGoalKey] ?: DEFAULT_YEARLY_GOAL_BOOKS
    }

    suspend fun setYearlyGoal(books: Int) {
        appContext.settingsDataStore.edit { it[yearlyGoalKey] = books }
    }

    val dndDuringSession: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[dndKey] ?: false
    }

    suspend fun setDndDuringSession(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[dndKey] = enabled }
    }

    val themeMode: Flow<ThemeMode> = appContext.settingsDataStore.data.map { prefs ->
        val modeInt = prefs[themeModeKey] ?: 0
        ThemeMode.entries.getOrElse(modeInt) { ThemeMode.SYSTEM }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.settingsDataStore.edit { it[themeModeKey] = mode.ordinal }
    }

    val useDynamicColor: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[useDynamicColorKey] ?: true
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[useDynamicColorKey] = enabled }
    }

    val workMinutes: Flow<Int> = appContext.settingsDataStore.data.map { prefs ->
        prefs[workMinutesKey] ?: 25
    }

    suspend fun setWorkMinutes(minutes: Int) {
        appContext.settingsDataStore.edit { it[workMinutesKey] = minutes }
    }

    val breakMinutes: Flow<Int> = appContext.settingsDataStore.data.map { prefs ->
        prefs[breakMinutesKey] ?: 5
    }

    suspend fun setBreakMinutes(minutes: Int) {
        appContext.settingsDataStore.edit { it[breakMinutesKey] = minutes }
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
