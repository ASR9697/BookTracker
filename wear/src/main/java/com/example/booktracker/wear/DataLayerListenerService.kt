package com.example.booktracker.wear

import android.content.ComponentName
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        // Request a complication update whenever data changes
        val componentName = ComponentName(this, GoalComplicationService::class.java)
        ComplicationDataSourceUpdateRequester.create(this, componentName).requestUpdateAll()
    }
}
