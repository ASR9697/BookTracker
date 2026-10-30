package com.example.booktracker.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlin.random.Random
import kotlinx.coroutines.launch

/** What the screens can ask for; [WearActivity] carries each one out. */
interface WearActions {
    fun addPages(book: ActiveBook, delta: Int)
    fun voiceNote(book: ActiveBook, text: String)
    fun phoneTimer(action: String)
    /** [minutes] is a countdown's length, or 0 to count up. */
    fun startSession(book: ActiveBook, minutes: Int)
    fun setSessionRunning(running: Boolean)
    fun expireSession()
    fun finishSession(book: ActiveBook, focus: Int)
    fun discardSession(book: ActiveBook)
    fun allowOnTimeBuzz()
}

class WearActivity : ComponentActivity(), DataClient.OnDataChangedListener, WearActions {

    private val isAmbient = mutableStateOf(false)
    private val ambientOffset = mutableStateOf(IntOffset.Zero)
    private val shelf = mutableStateOf<Shelf?>(null)
    private val phoneTimer = mutableStateOf<PhoneTimer?>(null)
    private val session = mutableStateOf<WristSession?>(null)
    private val loaded = mutableStateOf(false)

    /** The phone's last published shelf, before this watch's own taps are laid over it. */
    private var synced: Shelf? = null

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

    // The session starts either way; without the permission it just has no chip on the watch face.
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) WristSessions.current(this)?.let { SessionNotifier.show(this, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)

        setContent {
            WearApp(
                isAmbient = isAmbient.value,
                ambientOffset = ambientOffset.value,
                loaded = loaded.value,
                shelf = shelf.value,
                phoneTimer = phoneTimer.value,
                session = session.value,
                actions = this
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        // A reboot clears the chip but not the session; put it back. Rescheduling also
        // turns the countdown's alarm exact if the reader just allowed that in Settings.
        session.value = WristSessions.current(this)?.also {
            SessionNotifier.show(this, it)
            SessionAlarm.schedule(this, it)
        }
        lifecycleScope.launch {
            val (books, timer) = WatchStore.loadSynced(this@WearActivity)
            books?.let { synced = it }
            timer?.let { phoneTimer.value = it }
            remerge()
            loaded.value = true
        }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            when (event.dataItem.uri.path) {
                Constants.ACTIVE_BOOKS_PATH -> {
                    synced = WatchStore.parseShelf(DataMapItem.fromDataItem(event.dataItem).dataMap)
                    remerge()
                }
                Constants.TIMER_STATE_PATH ->
                    phoneTimer.value = WatchStore.parseTimer(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
    }

    private fun remerge() {
        shelf.value = synced?.let { WatchStore.withLocalPages(this, it) }
    }

    // OLED burn-in protection: nudge ambient content to a fresh position on every
    // ambient tick (~once a minute) so no pixel stays lit in one place.
    private fun shiftAmbientPixels() {
        ambientOffset.value = IntOffset(Random.nextInt(-8, 9), Random.nextInt(-8, 9))
    }

    override fun addPages(book: ActiveBook, delta: Int) {
        val moved = WatchStore.movePage(this, book, delta) ?: return
        remerge()
        lifecycleScope.launch {
            WatchStore.sendPage(this@WearActivity, moved)
            WatchStore.refreshSurfaces(this@WearActivity)
        }
    }

    override fun voiceNote(book: ActiveBook, text: String) {
        lifecycleScope.launch { WatchStore.publishNote(this@WearActivity, book, text) }
    }

    override fun phoneTimer(action: String) {
        lifecycleScope.launch { WatchStore.publishTimerAction(this@WearActivity, action) }
    }

    override fun startSession(book: ActiveBook, minutes: Int) {
        if (WristSessions.current(this) != null) return
        session.value = WristSessions.start(this, book, minutes)
        WatchStore.refreshSurfaces(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (minutes > 0 && !SessionAlarm.canBeOnTime(this) && SessionAlarm.firstAsk(this)) {
            // Without exact alarms the buzz can land a minute or so late while the watch
            // sleeps, so the first countdown offers the setting once.
            allowOnTimeBuzz()
        }
    }

    override fun setSessionRunning(running: Boolean) {
        session.value = WristSessions.setRunning(this, running)
    }

    override fun expireSession() {
        session.value = WristSessions.expire(this)
    }

    override fun finishSession(book: ActiveBook, focus: Int) {
        session.value = null
        lifecycleScope.launch { WristSessions.finish(this@WearActivity, book, focus) }
    }

    override fun allowOnTimeBuzz() {
        val intent = SessionAlarm.permissionIntent(this) ?: return
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.w("WearActivity", "No exact-alarm settings page", e)
        }
    }

    override fun discardSession(book: ActiveBook) {
        session.value = null
        lifecycleScope.launch { WristSessions.discard(this@WearActivity, book) }
    }
}
