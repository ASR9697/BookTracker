package com.example.booktracker.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory

import com.example.booktracker.app.data.ServiceLocator

/**
 * Registers Coil's OkHttp network fetcher so AsyncImage can load remote book
 * covers. Coil 3 ships no network component by default; this wires it in
 * explicitly rather than relying on service-loader auto-registration.
 */
class BookTrackerApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        // Initialize DND manager to observe sessions globally
        ServiceLocator.dndManager(this)

        scheduleReminders()
    }

    private fun scheduleReminders() {
        val workManager = androidx.work.WorkManager.getInstance(this)
        
        // Calculate delay until 8 PM
        val now = java.time.LocalDateTime.now()
        var target = now.withHour(20).withMinute(0).withSecond(0).withNano(0)
        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }
        val delay = java.time.Duration.between(now, target).toMillis()
        
        val request = androidx.work.PeriodicWorkRequestBuilder<com.example.booktracker.app.worker.ReminderWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .addTag("reading_reminders")
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            "daily_reading_reminder",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .build()
}
