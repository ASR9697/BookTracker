package com.example.booktracker.wear

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/**
 * Today's pages against the daily goal. With no goal set it doesn't invent one:
 * the ring follows the book touched last instead, and the text counts today's pages.
 */
class GoalComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        createComplicationData(Shelf(emptyList(), dailyGoal = 30, pagesToday = 15, dayEndsAt = 0), type)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val shelf = WatchStore.loadShelf(this) ?: return NoDataComplicationData()
        return createComplicationData(shelf, request.complicationType)
    }

    private fun createComplicationData(shelf: Shelf, type: ComplicationType): ComplicationData? {
        val tapAction = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WearActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hasGoal = shelf.dailyGoal > 0
        val description = plain(getString(R.string.complication_label))
        val title = plain(getString(if (hasGoal) R.string.complication_label else R.string.complication_book))
        val text = plain(
            if (hasGoal) "${shelf.pagesToday} / ${shelf.dailyGoal}" else resources.getQuantityString(R.plurals.pages_today, shelf.pagesToday, shelf.pagesToday)
        )

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                val value = if (hasGoal) (shelf.pagesToday.toFloat() / shelf.dailyGoal).coerceIn(0f, 1f)
                else shelf.latest?.progress ?: return NoDataComplicationData()
                RangedValueComplicationData.Builder(value = value, min = 0f, max = 1f, contentDescription = description)
                    .setText(plain(if (hasGoal) "${shelf.pagesToday}" else "${(value * 100).toInt()}%"))
                    .setTitle(title)
                    .setTapAction(tapAction)
                    .build()
            }
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(text = plain(if (hasGoal) "${shelf.pagesToday}/${shelf.dailyGoal}" else "${shelf.pagesToday}"), contentDescription = description)
                    .setTitle(title)
                    .setTapAction(tapAction)
                    .build()
            ComplicationType.LONG_TEXT ->
                LongTextComplicationData.Builder(text = text, contentDescription = description)
                    .setTitle(plain(shelf.latest?.title ?: getString(R.string.complication_label)))
                    .setTapAction(tapAction)
                    .build()
            else -> null
        }
    }

    private fun plain(text: String) = PlainComplicationText.Builder(text).build()
}
