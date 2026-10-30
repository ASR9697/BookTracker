package com.example.booktracker.app.sync

import android.util.Log
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.shared.Constants
import com.example.booktracker.shared.models.Session
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/**
 * Receives data items from the watch (page progress, dictated margin notes,
 * sessions timed on the wrist and timer taps) and applies them. Runs even when
 * the app UI is closed. Progress uses Last-Write-Wins; notes and sessions are
 * keyed by the watch's own id (upsert), so replays are idempotent.
 */
class WearSyncService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

            when {
                path.startsWith(Constants.NOTE_PATH_PREFIX) -> {
                    val noteId = dataMap.getString(Constants.KEY_NOTE_ID) ?: continue
                    val bookId = dataMap.getString(Constants.KEY_BOOK_ID) ?: continue
                    val text = dataMap.getString(Constants.KEY_NOTE_TEXT) ?: continue
                    val page = dataMap.getInt(Constants.KEY_PAGE_OR_UNIT)
                    val timestamp = dataMap.getLong(Constants.KEY_UPDATED_AT)
                    runBlocking {
                        ServiceLocator.repository(applicationContext)
                            .addRemoteNote(noteId, bookId, page, text, timestamp)
                    }
                    // The note is now persisted locally; drop the transport item so
                    // dictated notes don't accumulate in the Data Layer indefinitely.
                    try {
                        Wearable.getDataClient(applicationContext).deleteDataItems(event.dataItem.uri)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to clear synced note item", e)
                    }
                    Log.d(TAG, "Applied watch note $noteId for $bookId")
                }

                path.startsWith(Constants.PROGRESS_PATH_PREFIX) -> {
                    val bookId = dataMap.getString(Constants.KEY_BOOK_ID) ?: continue
                    val currentPage = dataMap.getInt(Constants.KEY_CURRENT_UNIT)
                    val updatedAt = dataMap.getLong(Constants.KEY_UPDATED_AT)
                    // onDataChanged runs on a background thread; finishing the write
                    // before returning keeps the service alive for the duration.
                    runBlocking {
                        ServiceLocator.repository(applicationContext)
                            .applyRemoteProgress(bookId, currentPage, updatedAt)
                    }
                    Log.d(TAG, "Applied watch progress for $bookId -> $currentPage")
                }

                path.startsWith(Constants.SESSION_PATH_PREFIX) -> {
                    val sessionId = dataMap.getString(Constants.KEY_SESSION_ID) ?: continue
                    val bookId = dataMap.getString(Constants.KEY_BOOK_ID) ?: continue
                    val startPage = dataMap.getInt(Constants.KEY_START_UNIT)
                    val endPage = dataMap.getInt(Constants.KEY_END_UNIT)
                    val session = Session(
                        id = sessionId,
                        bookId = bookId,
                        startTime = dataMap.getLong(Constants.KEY_START_TIME),
                        endTime = dataMap.getLong(Constants.KEY_END_TIME),
                        durationSeconds = dataMap.getInt(Constants.KEY_DURATION_SECONDS),
                        startPage = startPage,
                        endPage = endPage,
                        pagesRead = (endPage - startPage).coerceAtLeast(0),
                        deviceSource = "watch",
                        zoneId = dataMap.getString(Constants.KEY_ZONE_ID).orEmpty(),
                        focus = dataMap.getInt(Constants.KEY_FOCUS).coerceIn(0, 5)
                    )
                    runBlocking { ServiceLocator.repository(applicationContext).addRemoteSession(session) }
                    try {
                        Wearable.getDataClient(applicationContext).deleteDataItems(event.dataItem.uri)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to clear synced session item", e)
                    }
                    Log.d(TAG, "Applied watch session $sessionId for $bookId")
                }

                path.startsWith(Constants.TIMER_CONTROL_PATH) -> {
                    val action = dataMap.getString(Constants.KEY_TIMER_CONTROL_ACTION) ?: continue
                    runBlocking { WearBridge.applyTimerControl(applicationContext, action) }
                    try {
                        Wearable.getDataClient(applicationContext).deleteDataItems(event.dataItem.uri)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to clear timer control item", e)
                    }
                    Log.d(TAG, "Applied watch timer control: $action")
                }
            }
        }
    }

    private companion object {
        const val TAG = "WearSyncService"
    }
}
