package com.example.booktracker.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.booktracker.app.analytics.StreakEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Top-level, as the delegate requires: one DataStore per name per process.
private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "settings")

/** User preferences: daily page goal (streaks + analytics) and yearly books goal. */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dailyGoalKey = intPreferencesKey("daily_goal_pages")
    private val yearlyGoalKey = intPreferencesKey("yearly_goal_books")

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

    companion object {
        const val DEFAULT_YEARLY_GOAL_BOOKS = 12
    }
}
