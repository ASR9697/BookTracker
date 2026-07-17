package com.example.booktracker.app.auth

import android.content.Context
import android.util.Log
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearableAuthSender(private val context: Context) {
    suspend fun sendAuthToken(token: String): Boolean {
        val dataClient = Wearable.getDataClient(context)
        val request = PutDataMapRequest.create(Constants.AUTH_TOKEN_PATH).apply {
            dataMap.putString(Constants.TOKEN_KEY, token)
            dataMap.putLong("timestamp", System.currentTimeMillis()) // Ensures event fires even if token is same
        }.asPutDataRequest().setUrgent()

        return try {
            dataClient.putDataItem(request).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push auth token to wearable", e)
            false
        }
    }

    private companion object {
        const val TAG = "WearableAuthSender"
    }
}
