package com.example.booktracker.app.sync

import android.content.Context
import android.util.Log
import com.example.booktracker.shared.Constants
import com.example.booktracker.shared.models.Book
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Publishes the currently-reading book to the watch. DataClient persists the
 * item locally and delivers it whenever the watch is reachable, so this is
 * safe to call with no watch paired and no network.
 */
class WearBridge(context: Context) {

    private val appContext = context.applicationContext

    suspend fun publishActiveBooks(books: List<Book>, dailyGoal: Int = 0, pagesToday: Int = 0) {
        val request = PutDataMapRequest.create(Constants.ACTIVE_BOOKS_PATH).apply {
            val listDataMap = ArrayList<com.google.android.gms.wearable.DataMap>()
            for (book in books) {
                val map = com.google.android.gms.wearable.DataMap()
                map.putString(Constants.KEY_BOOK_ID, book.id)
                map.putString(Constants.KEY_TITLE, book.title)
                map.putInt(Constants.KEY_CURRENT_UNIT, book.currentUnit)
                map.putInt(Constants.KEY_TOTAL_UNITS, book.totalUnits)
                map.putLong(Constants.KEY_UPDATED_AT, book.lastUpdated)
                map.putInt(Constants.KEY_DAILY_GOAL, dailyGoal)
                map.putInt(Constants.KEY_PAGES_TODAY, pagesToday)
                listDataMap.add(map)
            }
            dataMap.putDataMapArrayList(Constants.KEY_BOOKS_LIST, listDataMap)
            dataMap.putLong(Constants.KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            Wearable.getDataClient(appContext).putDataItem(request).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish active books to wearable", e)
        }
    }

    suspend fun publishTimerState(isRunning: Boolean, timeLeft: Int, phase: Int, bookId: String?) {
        val request = PutDataMapRequest.create(Constants.TIMER_STATE_PATH).apply {
            dataMap.putBoolean(Constants.KEY_TIMER_RUNNING, isRunning)
            dataMap.putInt(Constants.KEY_TIMER_TIME_LEFT, timeLeft)
            dataMap.putInt(Constants.KEY_TIMER_PHASE, phase)
            if (bookId != null) {
                dataMap.putString(Constants.KEY_TIMER_BOOK_ID, bookId)
            } else {
                dataMap.remove(Constants.KEY_TIMER_BOOK_ID)
            }
            dataMap.putLong(Constants.KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            Wearable.getDataClient(appContext).putDataItem(request).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish timer state to wearable", e)
        }
    }

    private companion object {
        const val TAG = "WearBridge"
    }
}
