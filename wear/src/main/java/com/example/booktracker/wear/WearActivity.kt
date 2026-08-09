package com.example.booktracker.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.example.booktracker.wear.journal.WristDictaphone
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlin.random.Random
import kotlinx.coroutines.delay
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
data class TimerState(
    val isRunning: Boolean,
    val timeLeft: Int,
    val phase: Int,
    val bookId: String?,
    val mode: String
)

class WearActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private val isAmbient = mutableStateOf(false)
    private val activeBooks = mutableStateOf<List<ActiveBook>>(emptyList())
    private val timerState = mutableStateOf<TimerState?>(null)
    private val ambientOffset = mutableStateOf(IntOffset.Zero)

    // Ignore phone echoes older than our latest local tap (LWW on the watch side).
    private var lastLocalActionAt = 0L

    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                shiftAmbientPixels()
                isAmbient.value = true
            }
            override fun onExitAmbient() {
                isAmbient.value = false
                ambientOffset.value = IntOffset.Zero
            }
            override fun onUpdateAmbient() {
                shiftAmbientPixels()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)

        setContent {
            WearApp(
                isAmbient = isAmbient.value,
                ambientOffset = ambientOffset.value,
                books = activeBooks.value,
                timerState = timerState.value,
                onIncrement = ::incrementProgress,
                onVoiceNote = ::publishVoiceNote,
                onTimerAction = ::publishTimerAction
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        refreshData()
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    Constants.ACTIVE_BOOKS_PATH -> applyActiveBooks(DataMapItem.fromDataItem(event.dataItem).dataMap)
                    Constants.TIMER_STATE_PATH -> applyTimerState(DataMapItem.fromDataItem(event.dataItem).dataMap)
                }
            }
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            try {
                val buffer = Wearable.getDataClient(this@WearActivity).dataItems.await()
                try {
                    buffer.firstOrNull { it.uri.path == Constants.ACTIVE_BOOKS_PATH }
                        ?.let { applyActiveBooks(DataMapItem.fromDataItem(it).dataMap) }
                    buffer.firstOrNull { it.uri.path == Constants.TIMER_STATE_PATH }
                        ?.let { applyTimerState(DataMapItem.fromDataItem(it).dataMap) }
                } finally {
                    buffer.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load data", e)
            }
        }
    }

    private fun applyActiveBooks(dataMap: DataMap) {
        val updatedAt = dataMap.getLong(Constants.KEY_UPDATED_AT)
        if (updatedAt < lastLocalActionAt) return
        
        val listDataMap = dataMap.getDataMapArrayList(Constants.KEY_BOOKS_LIST) ?: return
        val newBooks = listDataMap.mapNotNull { itemMap ->
            val id = itemMap.getString(Constants.KEY_BOOK_ID) ?: return@mapNotNull null
            ActiveBook(
                id = id,
                title = itemMap.getString(Constants.KEY_TITLE) ?: "",
                currentUnit = itemMap.getInt(Constants.KEY_CURRENT_UNIT),
                totalUnits = itemMap.getInt(Constants.KEY_TOTAL_UNITS),
                dailyGoal = itemMap.getInt(Constants.KEY_DAILY_GOAL),
                pagesToday = itemMap.getInt(Constants.KEY_PAGES_TODAY)
            )
        }
        activeBooks.value = newBooks
        
        try {
            androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
                .create(this, android.content.ComponentName(this, GoalComplicationService::class.java))
                .requestUpdateAll()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update complication", e)
        }
    }

    private fun applyTimerState(dataMap: DataMap) {
        timerState.value = TimerState(
            isRunning = dataMap.getBoolean(Constants.KEY_TIMER_RUNNING),
            timeLeft = dataMap.getInt(Constants.KEY_TIMER_TIME_LEFT),
            phase = dataMap.getInt(Constants.KEY_TIMER_PHASE),
            bookId = dataMap.getString(Constants.KEY_TIMER_BOOK_ID),
            mode = dataMap.getString(Constants.KEY_TIMER_MODE) ?: "COUNTDOWN"
        )
    }

    // OLED burn-in protection: nudge ambient content to a fresh position on every
    // ambient tick (~once a minute) so no pixel stays lit in one place.
    private fun shiftAmbientPixels() {
        ambientOffset.value = IntOffset(Random.nextInt(-8, 9), Random.nextInt(-8, 9))
    }

    // Negative deltas come from rotary/bezel corrections; never below 0 or past the end.
    private fun incrementProgress(book: ActiveBook, delta: Int) {
        val ceiling = if (book.totalUnits > 0) book.totalUnits else Int.MAX_VALUE
        val newUnit = (book.currentUnit + delta).coerceIn(0, ceiling)
        if (newUnit == book.currentUnit) return
        val now = System.currentTimeMillis()
        lastLocalActionAt = now
        // Optimistically bump the goal dial too; the phone's echo corrects it.
        val updatedBook = book.copy(
            currentUnit = newUnit,
            pagesToday = (book.pagesToday + (newUnit - book.currentUnit)).coerceAtLeast(0)
        )
        activeBooks.value = activeBooks.value.map { if (it.id == book.id) updatedBook else it }

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

    // Publishes a dictated note to the phone over the Data Layer. The note id is
    // generated here so a Data Layer replay upserts the same row (no duplicates).
    private fun publishVoiceNote(book: ActiveBook, text: String) {
        val noteId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        lifecycleScope.launch {
            try {
                val request =
                    PutDataMapRequest.create("${Constants.NOTE_PATH_PREFIX}/$noteId").apply {
                        dataMap.putString(Constants.KEY_NOTE_ID, noteId)
                        dataMap.putString(Constants.KEY_BOOK_ID, book.id)
                        dataMap.putString(Constants.KEY_NOTE_TEXT, text)
                        dataMap.putInt(Constants.KEY_PAGE_OR_UNIT, book.currentUnit)
                        dataMap.putLong(Constants.KEY_UPDATED_AT, now)
                    }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(this@WearActivity).putDataItem(request).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync voice note", e)
            }
        }
    }

    private fun publishTimerAction(action: String) {
        val now = System.currentTimeMillis()
        lifecycleScope.launch {
            try {
                val request = PutDataMapRequest.create("${Constants.TIMER_CONTROL_PATH}/$now").apply {
                    dataMap.putString(Constants.KEY_TIMER_CONTROL_ACTION, action)
                    dataMap.putLong(Constants.KEY_UPDATED_AT, now)
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(this@WearActivity).putDataItem(request).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync timer action", e)
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
    ambientOffset: IntOffset,
    books: List<ActiveBook>,
    timerState: TimerState?,
    onIncrement: (ActiveBook, Int) -> Unit,
    onVoiceNote: (ActiveBook, String) -> Unit,
    onTimerAction: (String) -> Unit
) {
    if (isAmbient) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                books.firstOrNull()?.title ?: "Book Tracker",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset { ambientOffset }
                    .padding(horizontal = 24.dp)
            )
        }
        return
    }

    val colorScheme = dynamicColorScheme(LocalContext.current) ?: ColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        AppScaffold {
            ScreenScaffold {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (books.isEmpty()) {
                        Text(
                            "Start reading a book on your phone",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    } else {
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { books.size })
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            ActiveBookContent(
                                book = books[page],
                                timerState = if (timerState?.bookId == books[page].id) timerState else null,
                                onIncrement = { delta -> onIncrement(books[page], delta) },
                                onVoiceNote = { text -> onVoiceNote(books[page], text) },
                                onTimerAction = onTimerAction
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBookContent(
    book: ActiveBook,
    timerState: TimerState?,
    onIncrement: (Int) -> Unit,
    onVoiceNote: (String) -> Unit,
    onTimerAction: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        WristDictaphone.firstResult(result.data)?.let(onVoiceNote)
    }

    val focusRequester = remember { FocusRequester() }
    var rotaryAccum by remember { mutableFloatStateOf(0f) }
    var pendingPages by remember { mutableIntStateOf(0) }

    LaunchedEffect(pendingPages) {
        if (pendingPages != 0) {
            delay(600)
            val delta = pendingPages
            pendingPages = 0
            onIncrement(delta)
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Outer Progress (Total Book)
        if (book.totalUnits > 0) {
            val totalProgress = (book.currentUnit.toFloat() + pendingPages) / book.totalUnits
            androidx.wear.compose.material3.CircularProgressIndicator(
                progress = { totalProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 4.dp,
                colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            )
        }

        // Inner Progress (Daily Goal)
        if (book.dailyGoal > 0) {
            val dailyProgress = (book.pagesToday.toFloat() + pendingPages) / book.dailyGoal
            androidx.wear.compose.material3.CircularProgressIndicator(
                progress = { dailyProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                strokeWidth = 3.dp,
                colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.tertiary,
                    trackColor = Color.Transparent
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    rotaryAccum += event.verticalScrollPixels
                    var steps = 0
                    while (rotaryAccum >= ROTARY_PIXELS_PER_PAGE) {
                        rotaryAccum -= ROTARY_PIXELS_PER_PAGE
                        steps++
                    }
                    while (rotaryAccum <= -ROTARY_PIXELS_PER_PAGE) {
                        rotaryAccum += ROTARY_PIXELS_PER_PAGE
                        steps--
                    }
                    if (steps != 0) {
                        val ceiling = if (book.totalUnits > 0) book.totalUnits else Int.MAX_VALUE
                        val clamped = (book.currentUnit + pendingPages + steps)
                            .coerceIn(0, ceiling) - book.currentUnit
                        if (clamped != pendingPages) {
                            pendingPages = clamped
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Text(
                book.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer4()
            
            if (timerState != null) {
                // Timer is active!
                var localTimeLeft by remember(timerState) { mutableStateOf(timerState.timeLeft) }
                
                LaunchedEffect(timerState) {
                    if (timerState.isRunning) {
                        while (true) {
                            if (timerState.mode == "STOPWATCH") {
                                delay(1000)
                                localTimeLeft++
                            } else {
                                if (localTimeLeft > 0) {
                                    delay(1000)
                                    localTimeLeft--
                                } else {
                                    break
                                }
                            }
                        }
                    }
                }

                val hours = localTimeLeft / 3600
                val mins = (localTimeLeft % 3600) / 60
                val secs = localTimeLeft % 60
                val phaseName = if (timerState.mode == "STOPWATCH") "READING" else if (timerState.phase == 0) "FOCUS" else "BREAK"
                val timeString = if (hours > 0) String.format("%02d:%02d:%02d", hours, mins, secs) else String.format("%02d:%02d", mins, secs)
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.displayMedium,
                    color = if (timerState.mode == "STOPWATCH" || timerState.phase == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    androidx.wear.compose.material3.CompactButton(
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTimerAction(if (timerState.isRunning) "PAUSE" else "RESUME") 
                        }
                    ) {
                        Text(if (timerState.isRunning) "⏸" else "▶️")
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.wear.compose.material3.CompactButton(
                        onClick = {
                            onIncrement(1)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.width(48.dp)
                    ) {
                        Text("+1")
                    }
                    Text(
                        text = "${book.currentUnit + pendingPages}",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (pendingPages != 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    androidx.wear.compose.material3.CompactButton(
                        onClick = {
                            onIncrement(10)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.width(48.dp)
                    ) {
                        Text("+10")
                    }
                }
            }
            
            if (book.dailyGoal > 0 && timerState == null) {
                Spacer4()
                val goalMet = book.pagesToday >= book.dailyGoal
                Text(
                    if (goalMet) "Goal met" else "Goal: ${book.pagesToday}/${book.dailyGoal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (goalMet) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer4()
            androidx.wear.compose.material3.CompactButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        voiceLauncher.launch(WristDictaphone.getSpeechToTextIntent())
                    } catch (e: android.content.ActivityNotFoundException) {
                        Log.w("WearActivity", "No speech recognizer", e)
                    }
                }
            ) {
                Text("🎤")
            }
        }
    }
}

@Composable
private fun Spacer4() {
    Box(modifier = Modifier.height(4.dp))
}

// One "page" of bezel/crown travel; tuned so a single detent on a rotating
// bezel (~48px of scroll) logs exactly one page.
private const val ROTARY_PIXELS_PER_PAGE = 48f
