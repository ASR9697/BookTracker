package com.example.booktracker.app.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.glance.material3.ColorProviders
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.ui.TimerMode
import com.example.booktracker.app.ui.theme.buildDarkColorScheme
import com.example.booktracker.app.ui.theme.buildLightColorScheme
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.flow.first

/** Everything the widgets show, read once per update. */
data class WidgetSnapshot(
    val books: List<Book>,
    val sessions: List<Session>,
    val notes: List<MarginNote>,
    val streak: StreakEngine.StreakInfo,
    val timerBookId: String?,
    val timerRunning: Boolean,
    val timerMode: TimerMode,
    val elapsedSeconds: Int,
    val timeLeftSeconds: Int
) {
    val reading: List<Book> get() = WidgetPicks.reading(books)
    val upNext: List<Book> get() = WidgetPicks.upNext(books)
}

/** The app's colours for the widgets: the reader's palette, or Material You when they chose it. */
class WidgetLook(val scheme: ColorScheme, val colors: androidx.glance.color.ColorProviders)

object WidgetData {

    suspend fun load(context: Context): WidgetSnapshot {
        val repository = ServiceLocator.repository(context)
        val settings = ServiceLocator.settings(context)
        val sessions = repository.observeCompletedSessions().first()
        return WidgetSnapshot(
            books = repository.observeBooks().first(),
            sessions = sessions,
            notes = repository.observeAllNotes().first(),
            streak = StreakEngine.compute(
                sessions = sessions,
                dailyGoal = settings.dailyGoal.first(),
                dayStartsAtHour = settings.dayStartsAtHour.first()
            ),
            timerBookId = ServiceLocator.timerBookId.value,
            timerRunning = ServiceLocator.timerIsRunning.value,
            timerMode = ServiceLocator.timerMode.value,
            elapsedSeconds = ServiceLocator.elapsedSeconds.value,
            timeLeftSeconds = ServiceLocator.timeLeftSeconds.value
        )
    }

    suspend fun look(context: Context): WidgetLook {
        val settings = ServiceLocator.settings(context)
        val palette = settings.appPalette.first()
        val dynamic = settings.useDynamicColor.first() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val light = if (dynamic) dynamicLightColorScheme(context) else buildLightColorScheme(palette)
        val dark = if (dynamic) dynamicDarkColorScheme(context) else buildDarkColorScheme(palette)
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return WidgetLook(if (night) dark else light, ColorProviders(light = light, dark = dark))
    }
}

/** The pure choices behind the widgets, kept apart so they can be tested. */
object WidgetPicks {

    private val UP_NEXT_ORDER = listOf(BookStatus.UP_NEXT.name, BookStatus.SHORTLIST.name, BookStatus.TO_READ.name, "BACKLOG")

    /** Longer passages don't fit a widget, so they sit out the daily pick. */
    const val QUOTE_MAX_CHARS = 320

    fun reading(books: List<Book>): List<Book> =
        books.filter { it.status == BookStatus.READING.name }.sortedByDescending { it.lastUpdated }

    /** Same order as Home: Up next first, then the shortlist, then the rest of the pile. */
    fun upNext(books: List<Book>): List<Book> =
        books.filter { b -> UP_NEXT_ORDER.any { it.equals(b.status, ignoreCase = true) } }
            .sortedWith(
                compareBy<Book> { b -> UP_NEXT_ORDER.indexOfFirst { it.equals(b.status, ignoreCase = true) } }
                    .thenByDescending { it.lastUpdated }
            )

    /**
     * One saved passage per day, the same all day. Starred ones get the stage once
     * there are a few of them; [offset] steps to another for the shuffle button.
     */
    fun quoteOfTheDay(notes: List<MarginNote>, day: Long, offset: Int = 0): MarginNote? {
        val fits = notes.filter { it.content.isNotBlank() && it.content.length <= QUOTE_MAX_CHARS }
        val passages = fits.filter { it.type == NoteType.BOOK_CONTENT }.ifEmpty { fits }
        val starred = passages.filter { it.isFavorite }
        val pool = (if (starred.size >= 3) starred else passages).sortedBy { it.id }
        if (pool.isEmpty()) return null
        return pool[Math.floorMod(day * 7 + offset, pool.size.toLong()).toInt()]
    }
}
