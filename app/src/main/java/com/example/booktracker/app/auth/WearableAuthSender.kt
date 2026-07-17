package com.example.booktracker.app.auth

import android.content.Context
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearableAuthSender(private val context: Context) {
    suspend fun sendAuthToken(token: String) {
        val dataClient = Wearable.getDataClient(context)
        val putDataReq = PutDataMapRequest.create(Constants.AUTH_TOKEN_PATH).apply {
            dataMap.putString(Constants.TOKEN_KEY, token)
            dataMap.putLong("timestamp", System.currentTimeMillis()) // Ensures event fires even if token is same
        }.asPutDataRequest()
        
        try {
            dataClient.putDataItem(putDataReq).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
