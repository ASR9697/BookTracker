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

/** User preferences. Currently just the daily page goal used by streaks + analytics. */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dailyGoalKey = intPreferencesKey("daily_goal_pages")

    val dailyGoal: Flow<Int> = appContext.settingsDataStore.data.map { prefs ->
        prefs[dailyGoalKey] ?: StreakEngine.DEFAULT_DAILY_GOAL_PAGES
    }

    suspend fun setDailyGoal(pages: Int) {
        appContext.settingsDataStore.edit { it[dailyGoalKey] = pages }
    }
}
