package com.example.booktracker.wear

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** Redraws the tile and complication whenever the phone publishes new books or goal numbers. */
class DataLayerListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        WatchStore.refreshSurfaces(this)
    }
}
