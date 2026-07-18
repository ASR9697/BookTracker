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

    suspend fun publishActiveBook(book: Book, dailyGoal: Int = 0, pagesToday: Int = 0) {
        val request = PutDataMapRequest.create(Constants.ACTIVE_BOOK_PATH).apply {
            dataMap.putString(Constants.KEY_BOOK_ID, book.id)
            dataMap.putString(Constants.KEY_TITLE, book.title)
            dataMap.putInt(Constants.KEY_CURRENT_UNIT, book.currentUnit)
            dataMap.putInt(Constants.KEY_TOTAL_UNITS, book.totalUnits)
            dataMap.putLong(Constants.KEY_UPDATED_AT, book.lastUpdated)
            dataMap.putInt(Constants.KEY_DAILY_GOAL, dailyGoal)
            dataMap.putInt(Constants.KEY_PAGES_TODAY, pagesToday)
        }.asPutDataRequest().setUrgent()

        try {
            Wearable.getDataClient(appContext).putDataItem(request).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish active book to wearable", e)
        }
    }

    private companion object {
        const val TAG = "WearBridge"
    }
}
