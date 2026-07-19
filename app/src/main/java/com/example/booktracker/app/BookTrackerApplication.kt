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
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .build()
}
