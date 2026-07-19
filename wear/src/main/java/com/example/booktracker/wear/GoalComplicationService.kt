package com.example.booktracker.wear

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.booktracker.shared.Constants
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class GoalComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(
            pagesToday = 15f,
            dailyGoal = 30f,
            type = type
        )
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        var pagesToday = 0f
        var dailyGoal = 0f
        
        try {
            val uri = android.net.Uri.parse("wear://*${Constants.ACTIVE_BOOKS_PATH}")
            val buffer = Wearable.getDataClient(this).getDataItems(uri).await()
            val dataMap = buffer.firstOrNull()?.let { DataMapItem.fromDataItem(it).dataMap }
            
            if (dataMap != null) {
                val list = dataMap.getDataMapArrayList(Constants.KEY_BOOKS_LIST)
                if (!list.isNullOrEmpty()) {
                    val firstMap = list[0]
                    pagesToday = firstMap.getInt(Constants.KEY_PAGES_TODAY, 0).toFloat()
                    dailyGoal = firstMap.getInt(Constants.KEY_DAILY_GOAL, 0).toFloat()
                }
            }
            buffer.release()
        } catch (e: Exception) {
            // Ignored, fallback to 0
        }
        
        // Prevent 0 goal division
        if (dailyGoal <= 0f) {
            dailyGoal = 20f
        }
        
        return createComplicationData(pagesToday, dailyGoal, request.complicationType)
    }

    private fun createComplicationData(
        pagesToday: Float,
        dailyGoal: Float,
        type: ComplicationType
    ): ComplicationData? {
        val intent = Intent(this, WearActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapAction = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val progressPercent = (pagesToday / dailyGoal).coerceIn(0f, 1f)
        val text = PlainComplicationText.Builder("${pagesToday.toInt()} / ${dailyGoal.toInt()}").build()
        val title = PlainComplicationText.Builder("Reading Goal").build()
        
        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = progressPercent,
                    min = 0f,
                    max = 1f,
                    contentDescription = PlainComplicationText.Builder("Daily Reading Goal").build()
                )
                .setText(PlainComplicationText.Builder("${pagesToday.toInt()}").build())
                .setTitle(title)
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = text,
                    contentDescription = PlainComplicationText.Builder("Daily Reading Goal").build()
                )
                .setTitle(title)
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = text,
                    contentDescription = PlainComplicationText.Builder("Daily Reading Goal").build()
                )
                .setTitle(title)
                .setTapAction(tapAction)
                .build()
            }
            else -> null
        }
    }
}
