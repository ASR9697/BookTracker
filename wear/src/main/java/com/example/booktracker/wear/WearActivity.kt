package com.example.booktracker.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ActiveBook(
    val id: String,
    val title: String,
    val currentUnit: Int,
    val totalUnits: Int,
    val dailyGoal: Int = 0,
    val pagesToday: Int = 0
)

class WearActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private val isAmbient = mutableStateOf(false)
    private val activeBook = mutableStateOf<ActiveBook?>(null)

    // Ignore phone echoes older than our latest local tap (LWW on the watch side).
    private var lastLocalActionAt = 0L

    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                isAmbient.value = true
            }
            override fun onExitAmbient() {
                isAmbient.value = false
            }
            override fun onUpdateAmbient() {}
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)

        setContent {
            WearApp(
                isAmbient = isAmbient.value,
                book = activeBook.value,
                onIncrement = ::incrementProgress
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        refreshActiveBook()
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == Constants.ACTIVE_BOOK_PATH
            ) {
                applyActiveBook(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
    }

    private fun refreshActiveBook() {
        lifecycleScope.launch {
            try {
                val buffer = Wearable.getDataClient(this@WearActivity).dataItems.await()
                try {
                    buffer.firstOrNull { it.uri.path == Constants.ACTIVE_BOOK_PATH }
                        ?.let { applyActiveBook(DataMapItem.fromDataItem(it).dataMap) }
                } finally {
                    buffer.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load active book", e)
            }
        }
    }

    private fun applyActiveBook(dataMap: DataMap) {
        val updatedAt = dataMap.getLong(Constants.KEY_UPDATED_AT)
        if (updatedAt < lastLocalActionAt) return
        val id = dataMap.getString(Constants.KEY_BOOK_ID) ?: return
        activeBook.value = ActiveBook(
            id = id,
            title = dataMap.getString(Constants.KEY_TITLE) ?: "",
            currentUnit = dataMap.getInt(Constants.KEY_CURRENT_UNIT),
            totalUnits = dataMap.getInt(Constants.KEY_TOTAL_UNITS),
            dailyGoal = dataMap.getInt(Constants.KEY_DAILY_GOAL),
            pagesToday = dataMap.getInt(Constants.KEY_PAGES_TODAY)
        )
    }

    private fun incrementProgress(delta: Int) {
        val book = activeBook.value ?: return
        val ceiling = if (book.totalUnits > 0) book.totalUnits else Int.MAX_VALUE
        val newUnit = (book.currentUnit + delta).coerceAtMost(ceiling)
        val now = System.currentTimeMillis()
        lastLocalActionAt = now
        // Optimistically bump the goal dial too; the phone's echo corrects it.
        activeBook.value = book.copy(
            currentUnit = newUnit,
            pagesToday = book.pagesToday + (newUnit - book.currentUnit)
        )

        lifecycleScope.launch {
            try {
                val request =
                    PutDataMapRequest.create("${Constants.PROGRESS_PATH_PREFIX}/${book.id}").apply {
                        dataMap.putString(Constants.KEY_BOOK_ID, book.id)
                        dataMap.putInt(Constants.KEY_CURRENT_UNIT, newUnit)
                        dataMap.putLong(Constants.KEY_UPDATED_AT, now)
                    }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(this@WearActivity).putDataItem(request).await()
            } catch (e: Exception) {
                // DataClient queues items while disconnected, so failures here are rare;
                // the position also remains on-screen and re-sends on the next tap.
                Log.w(TAG, "Failed to sync progress", e)
            }
        }
    }

    private companion object {
        const val TAG = "WearActivity"
    }
}

@Composable
fun WearApp(
    isAmbient: Boolean,
    book: ActiveBook?,
    onIncrement: (Int) -> Unit
) {
    // Ambient: keep it mostly black and static (burn-in + power), no scaffold/clock.
    if (isAmbient) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                book?.title ?: "Book Tracker",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        return
    }

    // Material You on Wear: tint from the active watch face when the platform
    // supports it, otherwise fall back to the M3 baseline scheme.
    val colorScheme = dynamicColorScheme(LocalContext.current) ?: ColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        AppScaffold {
            ScreenScaffold {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (book == null) {
                        Text(
                            "Start reading a book on your phone",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    } else {
                        ActiveBookContent(book = book, onIncrement = onIncrement)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBookContent(
    book: ActiveBook,
    onIncrement: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            book.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer4()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    onIncrement(1)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier.width(64.dp)
            ) {
                Text("+1")
            }
            Text(
                text = "${book.currentUnit}",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Button(
                onClick = {
                    onIncrement(10)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier.width(64.dp)
            ) {
                Text("+10")
            }
        }
        if (book.totalUnits > 0) {
            Spacer4()
            Text(
                "of ${book.totalUnits}",
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (book.dailyGoal > 0) {
            Spacer4()
            val goalMet = book.pagesToday >= book.dailyGoal
            Text(
                if (goalMet) "Goal met · ${book.pagesToday} today"
                else "Today ${book.pagesToday}/${book.dailyGoal}",
                style = MaterialTheme.typography.labelSmall,
                color = if (goalMet) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Spacer4() {
    Box(modifier = Modifier.height(4.dp))
}
