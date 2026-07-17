package com.example.booktracker.wear.auth

import android.util.Log
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class WearableAuthListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == Constants.AUTH_TOKEN_PATH) {
                val token = DataMapItem.fromDataItem(event.dataItem)
                    .dataMap.getString(Constants.TOKEN_KEY) ?: continue

                // onDataChanged is delivered on a background thread; completing the
                // sign-in before returning keeps the service alive for the duration.
                runBlocking {
                    try {
                        FirebaseAuth.getInstance().signInWithCustomToken(token).await()
                        Log.d(TAG, "Successfully signed in with token")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sign in", e)
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "WearAuth"
    }
}
