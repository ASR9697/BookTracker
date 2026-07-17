package com.example.booktracker.wear.auth

import android.util.Log
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearableAuthListenerService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == Constants.AUTH_TOKEN_PATH) {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val token = dataMapItem.dataMap.getString(Constants.TOKEN_KEY)
                
                token?.let {
                    scope.launch {
                        try {
                            FirebaseAuth.getInstance().signInWithCustomToken(it).await()
                            Log.d("WearAuth", "Successfully signed in with token")
                        } catch (e: Exception) {
                            Log.e("WearAuth", "Failed to sign in", e)
                        }
                    }
                }
            }
        }
    }
}
