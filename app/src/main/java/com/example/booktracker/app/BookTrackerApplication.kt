package com.example.booktracker.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.notifications.Notifications
import com.example.booktracker.app.widget.WidgetRefresher
import com.example.booktracker.app.worker.AutoBackupScheduler
import com.example.booktracker.app.worker.NudgeScheduler
import com.example.booktracker.app.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Registers Coil's OkHttp network fetcher so AsyncImage can load remote book
 * covers. Coil 3 ships no network component by default; this wires it in
 * explicitly rather than relying on service-loader auto-registration.
 */
class BookTrackerApplication : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        // Initialize DND manager to observe sessions globally
        ServiceLocator.dndManager(this)

        // The reminder job follows the setting: scheduled, rescheduled or cancelled
        // whenever the reader changes it, and refreshed once per process start.
        appScope.launch {
            ServiceLocator.settings(this@BookTrackerApplication).reminder
                .distinctUntilChanged()
                .collect { ReminderScheduler.apply(this@BookTrackerApplication, it) }
        }

        // The optional nudges and the weekly backup follow their switches the same way.
        appScope.launch {
            val settings = ServiceLocator.settings(this@BookTrackerApplication)
            settings.prefs
                .map { listOf(it.streakNudge, it.streakNudgeHour, it.weeklyRecap, it.reviewReminder, it.reviewReminderHour) }
                .distinctUntilChanged()
                .collect { NudgeScheduler.apply(this@BookTrackerApplication, settings.prefs.first()) }
        }
        appScope.launch {
            ServiceLocator.settings(this@BookTrackerApplication).prefs
                .map { it.autoBackup && it.autoBackupFolder.isNotBlank() }
                .distinctUntilChanged()
                .collect { AutoBackupScheduler.apply(this@BookTrackerApplication, it) }
        }

        // Home-screen widgets follow the data wherever it changes: the app, the timer or a widget tap.
        WidgetRefresher.start(this, appScope)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .build()
}
