package com.example.booktracker.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory

/**
 * Registers Coil's OkHttp network fetcher so AsyncImage can load remote book
 * covers. Coil 3 ships no network component by default; this wires it in
 * explicitly rather than relying on service-loader auto-registration.
 */
class BookTrackerApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .build()
}
