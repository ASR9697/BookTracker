package com.example.booktracker.wear

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/** Reading days in a row, with a flame, for a slot on the watch face. */
class StreakComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = createComplicationData(12, type)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val shelf = WatchStore.loadShelf(this) ?: return NoDataComplicationData()
        return createComplicationData(shelf.streak, request.complicationType)
    }

    private fun createComplicationData(streak: Int, type: ComplicationType): ComplicationData? {
        val tapAction = PendingIntent.getActivity(
            this, 1, Intent(this, WearActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val flame = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_flame)).build()
        val words = resources.getQuantityString(R.plurals.streak_days, streak, streak)
        val description = PlainComplicationText.Builder(words).build()
        return when (type) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(PlainComplicationText.Builder("$streak").build(), description)
                    .setMonochromaticImage(flame)
                    .setTapAction(tapAction)
                    .build()
            ComplicationType.LONG_TEXT ->
                LongTextComplicationData.Builder(description, description)
                    .setMonochromaticImage(flame)
                    .setTapAction(tapAction)
                    .build()
            else -> null
        }
    }
}
