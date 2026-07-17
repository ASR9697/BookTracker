package com.example.booktracker.app.sync

import android.util.Log
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/**
 * Receives page-progress data items from the watch and applies them to the
 * local database with Last-Write-Wins. Runs even when the app UI is closed.
 */
class WearSyncService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith(Constants.PROGRESS_PATH_PREFIX)) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val bookId = dataMap.getString(Constants.KEY_BOOK_ID) ?: continue
            val currentUnit = dataMap.getInt(Constants.KEY_CURRENT_UNIT)
            val updatedAt = dataMap.getLong(Constants.KEY_UPDATED_AT)

            // onDataChanged runs on a background thread; finishing the write
            // before returning keeps the service alive for the duration.
            runBlocking {
                ServiceLocator.repository(applicationContext)
                    .applyRemoteProgress(bookId, currentUnit, updatedAt)
            }
            Log.d(TAG, "Applied watch progress for $bookId -> $currentUnit")
        }
    }

    private companion object {
        const val TAG = "WearSyncService"
    }
}
