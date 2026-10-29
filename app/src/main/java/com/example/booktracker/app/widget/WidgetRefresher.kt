package com.example.booktracker.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.example.booktracker.app.data.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Widgets only redraw when asked, so this asks whenever anything they show
 * changes: books, sessions, notes, goals, colours or the timer. It lives as long
 * as the app process, which covers the timer service and widget taps too; the
 * widgets' own update period handles the day rolling over while the app sleeps.
 */
object WidgetRefresher {

    private fun widgets(): List<GlanceAppWidget> =
        listOf(ReadingWidget(), SessionWidget(), StreakWidget(), QuoteWidget(), PixelsWidget(), UpNextWidget())

    @OptIn(FlowPreview::class)
    fun start(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        val repository = ServiceLocator.repository(app)
        val settings = ServiceLocator.settings(app)
        val watched: List<Flow<Any?>> = listOf(
            repository.observeBooks(),
            repository.observeCompletedSessions(),
            repository.observeAllNotes(),
            settings.dailyGoal,
            settings.dayStartsAtHour,
            settings.appPalette,
            settings.useDynamicColor,
            settings.prefs.map { listOf(it.hideTitles, it.weekStart, it.showTimeLeft) },
            ServiceLocator.timerBookId,
            ServiceLocator.timerIsRunning,
            ServiceLocator.timerMode,
            ServiceLocator.sessionPageTarget,
            repository.observeOpenSession()
        )
        scope.launch {
            combine(watched) { it.toList() }
                .distinctUntilChanged()
                // Debounced so a burst of +1 taps or a backup restore redraws once.
                .debounce(1_000)
                .collect { refreshAll(app) }
        }
    }

    suspend fun refreshAll(context: Context) {
        widgets().forEach { widget -> runCatching { widget.updateAll(context) } }
    }
}
