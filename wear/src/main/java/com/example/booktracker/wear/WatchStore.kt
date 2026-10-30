package com.example.booktracker.wear

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

data class ActiveBook(
    val id: String,
    val title: String,
    val currentUnit: Int,
    val totalUnits: Int,
    /** Last change to the position, here or on the phone. */
    val updatedAt: Long,
    /** Where the phone last put the book, before this watch's own taps. */
    val syncedUnit: Int = currentUnit,
    val plan: FinishPlan? = null
) {
    val progress: Float get() = if (totalUnits > 0) (currentUnit.toFloat() / totalUnits).coerceIn(0f, 1f) else 0f
}

/** A book's finish-by plan from the phone; the watch keeps today's part current as pages are logged here. */
data class FinishPlan(
    val perDay: Int,
    /** Pages read in the book today, as the phone counted them. */
    val readToday: Int,
    val targetDay: Long,
    val status: String
) {
    val overdue: Boolean get() = status == "OVERDUE"
    val done: Boolean get() = status == "DONE"

    /** Pages still to read today, counting taps here the phone hasn't seen yet. */
    fun leftToday(book: ActiveBook, shownPage: Int = book.currentUnit): Int {
        val pagesLeft = if (book.totalUnits > 0) (book.totalUnits - shownPage).coerceAtLeast(0) else Int.MAX_VALUE
        return (perDay - readToday - (shownPage - book.syncedUnit)).coerceIn(0, pagesLeft)
    }
}

data class Shelf(
    val books: List<ActiveBook>,
    val dailyGoal: Int,
    val pagesToday: Int,
    val dayEndsAt: Long,
    /** Reading days in a row; 0 from a phone app too old to send it. */
    val streak: Int = 0,
    /** The phone's palette (ARGB); 0 from a phone app too old to send it. */
    val accent: Long = 0,
    val accentSecondary: Long = 0
) {
    val goalMet: Boolean get() = dailyGoal > 0 && pagesToday >= dailyGoal

    /** The book touched last, which the tile and complication show. */
    val latest: ActiveBook? get() = books.maxByOrNull { it.updatedAt }
}

/** A page tap made on the watch that the phone hasn't confirmed yet: where the book went, and when. */
data class LocalPage(val page: Int, val at: Long)

/** The phone's timer as last published; the watch counts the seconds itself from [anchor]. */
data class PhoneTimer(
    val bookId: String?,
    val running: Boolean,
    val countdown: Boolean,
    val onBreak: Boolean,
    val seconds: Int,
    val anchor: Long
) {
    fun secondsAt(now: Long): Int = when {
        !running -> seconds
        countdown -> ((anchor - now) / 1000L).toInt().coerceAtLeast(0)
        else -> ((now - anchor) / 1000L).toInt().coerceAtLeast(0)
    }
}

/**
 * The watch's view of the phone's data: what the phone last published, plus page
 * taps made here that the phone hasn't echoed back yet. The app, tile and
 * complication all read through this, so a +1 on the tile shows up in the app
 * at once and the other way round.
 */
object WatchStore {

    private const val TAG = "WatchStore"
    private const val PREFS = "watch_store"
    private const val PAGE_PREFIX = "page:"

    fun parseShelf(dataMap: DataMap): Shelf = Shelf(
        books = dataMap.getDataMapArrayList(Constants.KEY_BOOKS_LIST).orEmpty().mapNotNull { item ->
            ActiveBook(
                id = item.getString(Constants.KEY_BOOK_ID) ?: return@mapNotNull null,
                title = item.getString(Constants.KEY_TITLE).orEmpty(),
                currentUnit = item.getInt(Constants.KEY_CURRENT_UNIT),
                totalUnits = item.getInt(Constants.KEY_TOTAL_UNITS),
                updatedAt = item.getLong(Constants.KEY_UPDATED_AT),
                plan = if (item.containsKey(Constants.KEY_PLAN_PER_DAY)) {
                    FinishPlan(
                        perDay = item.getInt(Constants.KEY_PLAN_PER_DAY),
                        readToday = item.getInt(Constants.KEY_PLAN_READ_TODAY),
                        targetDay = item.getLong(Constants.KEY_PLAN_TARGET_DAY),
                        status = item.getString(Constants.KEY_PLAN_STATUS).orEmpty()
                    )
                } else null
            )
        },
        dailyGoal = dataMap.getInt(Constants.KEY_DAILY_GOAL),
        pagesToday = dataMap.getInt(Constants.KEY_PAGES_TODAY),
        dayEndsAt = dataMap.getLong(Constants.KEY_DAY_ENDS_AT),
        streak = dataMap.getInt(Constants.KEY_STREAK),
        accent = dataMap.getLong(Constants.KEY_ACCENT),
        accentSecondary = dataMap.getLong(Constants.KEY_ACCENT_SECONDARY)
    )

    fun parseTimer(dataMap: DataMap): PhoneTimer = PhoneTimer(
        bookId = dataMap.getString(Constants.KEY_TIMER_BOOK_ID),
        running = dataMap.getBoolean(Constants.KEY_TIMER_RUNNING),
        countdown = dataMap.getString(Constants.KEY_TIMER_MODE) == "COUNTDOWN",
        onBreak = dataMap.getString(Constants.KEY_TIMER_PHASE) == "BREAK",
        seconds = dataMap.getInt(Constants.KEY_TIMER_SECONDS),
        anchor = dataMap.getLong(Constants.KEY_TIMER_ANCHOR)
    )

    /** What the phone last published, or null before the first sync. */
    suspend fun loadSynced(context: Context): Pair<Shelf?, PhoneTimer?> = try {
        val buffer = Wearable.getDataClient(context).dataItems.await()
        try {
            val shelf = buffer.firstOrNull { it.uri.path == Constants.ACTIVE_BOOKS_PATH }
                ?.let { parseShelf(DataMapItem.fromDataItem(it).dataMap) }
            val timer = buffer.firstOrNull { it.uri.path == Constants.TIMER_STATE_PATH }
                ?.let { parseTimer(DataMapItem.fromDataItem(it).dataMap) }
            shelf to timer
        } finally {
            buffer.release()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not load synced data", e)
        null to null
    }

    suspend fun loadShelf(context: Context): Shelf? = loadSynced(context).first?.let { withLocalPages(context, it) }

    /**
     * Lays this watch's unconfirmed page taps over the phone's [synced] shelf. A tap
     * stands until the phone reports a change at least as new (Last-Write-Wins), so
     * the echo of our own tap, or a newer change on the phone, retires it.
     */
    fun withLocalPages(context: Context, synced: Shelf): Shelf {
        val prefs = prefs(context)
        val locals = prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(PAGE_PREFIX)) return@mapNotNull null
            localPage(value as? String)?.let { key.removePrefix(PAGE_PREFIX) to it }
        }.toMap()
        val (shelf, retired) = overlay(synced, locals, System.currentTimeMillis())
        if (retired.isNotEmpty()) prefs.edit { retired.forEach { remove(PAGE_PREFIX + it) } }
        return shelf
    }

    /**
     * [withLocalPages] without the storage: the shelf to show, and the books whose
     * local tap is retired (the phone caught up, or the book left the shelf).
     */
    internal fun overlay(synced: Shelf, locals: Map<String, LocalPage>, now: Long): Pair<Shelf, Set<String>> {
        val retired = locals.keys.toMutableSet()
        // Past the phone's day boundary, "today" starts again until the phone says otherwise.
        val newDay = synced.dayEndsAt in 1..now
        var extraToday = 0
        val books = synced.books.map { phone ->
            val book = if (newDay) phone.copy(plan = phone.plan?.copy(readToday = 0)) else phone
            val local = locals[book.id]
            if (local == null || local.at <= book.updatedAt) return@map book
            retired -= book.id
            extraToday += local.page - book.currentUnit
            book.copy(currentUnit = local.page, updatedAt = local.at)
        }
        val today = if (newDay) 0 else synced.pagesToday
        return synced.copy(books = books, pagesToday = (today + extraToday).coerceAtLeast(0)) to retired
    }

    /**
     * Moves [book] by [delta] pages, never below 0 or past the end, and sends it to
     * the phone. The caller redraws whichever of the tile and complication it isn't
     * already drawing.
     */
    suspend fun addPages(context: Context, book: ActiveBook, delta: Int) {
        movePage(context, book, delta)?.let { sendPage(context, it) }
    }

    /** Records the move on the watch only, so the screen can show it at once; null if nothing moved. */
    fun movePage(context: Context, book: ActiveBook, delta: Int): ActiveBook? {
        val ceiling = if (book.totalUnits > 0) book.totalUnits else Int.MAX_VALUE
        val page = (book.currentUnit + delta).coerceIn(0, ceiling)
        if (page == book.currentUnit) return null
        val now = System.currentTimeMillis()
        prefs(context).edit { putString(PAGE_PREFIX + book.id, "$page,$now") }
        return book.copy(currentUnit = page, updatedAt = now)
    }

    /**
     * Tells the phone where [book] is now, unless a session timed on the wrist owns
     * it: then the pages wait and travel with the session when it ends.
     */
    suspend fun sendPage(context: Context, book: ActiveBook) {
        if (WristSessions.current(context)?.bookId == book.id) return
        publishProgress(context, book.id, book.currentUnit, book.updatedAt)
    }

    suspend fun publishProgress(context: Context, bookId: String, page: Int, at: Long) {
        put(context, PutDataMapRequest.create("${Constants.PROGRESS_PATH_PREFIX}/$bookId").apply {
            dataMap.putString(Constants.KEY_BOOK_ID, bookId)
            dataMap.putInt(Constants.KEY_CURRENT_UNIT, page)
            dataMap.putLong(Constants.KEY_UPDATED_AT, at)
        }, "progress")
    }

    /** A dictated note; its id is made here so a Data Layer replay upserts the same row. */
    suspend fun publishNote(context: Context, book: ActiveBook, text: String) {
        val noteId = java.util.UUID.randomUUID().toString()
        put(context, PutDataMapRequest.create("${Constants.NOTE_PATH_PREFIX}/$noteId").apply {
            dataMap.putString(Constants.KEY_NOTE_ID, noteId)
            dataMap.putString(Constants.KEY_BOOK_ID, book.id)
            dataMap.putString(Constants.KEY_NOTE_TEXT, text)
            dataMap.putInt(Constants.KEY_PAGE_OR_UNIT, book.currentUnit)
            dataMap.putLong(Constants.KEY_UPDATED_AT, System.currentTimeMillis())
        }, "voice note")
    }

    suspend fun publishTimerAction(context: Context, action: String) {
        val now = System.currentTimeMillis()
        put(context, PutDataMapRequest.create("${Constants.TIMER_CONTROL_PATH}/$now").apply {
            dataMap.putString(Constants.KEY_TIMER_CONTROL_ACTION, action)
            dataMap.putLong(Constants.KEY_UPDATED_AT, now)
        }, "timer action")
    }

    suspend fun publishSession(context: Context, session: WristSession, endPage: Int, endedAt: Long, focus: Int) {
        put(context, PutDataMapRequest.create("${Constants.SESSION_PATH_PREFIX}/${session.id}").apply {
            dataMap.putString(Constants.KEY_SESSION_ID, session.id)
            dataMap.putString(Constants.KEY_BOOK_ID, session.bookId)
            dataMap.putLong(Constants.KEY_START_TIME, session.startedAt)
            dataMap.putLong(Constants.KEY_END_TIME, endedAt)
            dataMap.putInt(Constants.KEY_DURATION_SECONDS, (session.activeMillis(endedAt) / 1000L).toInt())
            dataMap.putInt(Constants.KEY_START_UNIT, session.startPage)
            dataMap.putInt(Constants.KEY_END_UNIT, endPage)
            dataMap.putString(Constants.KEY_ZONE_ID, java.time.ZoneId.systemDefault().id)
            dataMap.putInt(Constants.KEY_FOCUS, focus)
        }, "session")
    }

    /** Redraws the tile and complication, which only update when asked. */
    fun refreshSurfaces(context: Context) {
        refreshComplication(context)
        try {
            TileService.getUpdater(context).requestUpdate(ReadingTileService::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh tile", e)
        }
    }

    fun refreshComplication(context: Context) {
        for (source in listOf(GoalComplicationService::class.java, StreakComplicationService::class.java)) {
            try {
                ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, source)).requestUpdateAll()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh complication", e)
            }
        }
    }

    private suspend fun put(context: Context, request: PutDataMapRequest, what: String) {
        try {
            Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent()).await()
        } catch (e: Exception) {
            // DataClient queues items while disconnected, so failures here are rare.
            Log.w(TAG, "Failed to sync $what", e)
        }
    }

    private fun localPage(raw: String?): LocalPage? {
        val parts = raw?.split(',') ?: return null
        val page = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val at = parts.getOrNull(1)?.toLongOrNull() ?: return null
        return LocalPage(page, at)
    }

    internal fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
