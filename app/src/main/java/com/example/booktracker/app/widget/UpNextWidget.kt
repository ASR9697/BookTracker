package com.example.booktracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.booktracker.shared.models.Book

/**
 * The next books on your pile as a row of covers. Tap one to start reading it
 * (it opens the timer and moves into Reading); tap the header for the library.
 */
class UpNextWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val look = WidgetData.look(context)
        val density = context.resources.displayMetrics.density
        val books = data.upNext.take(MAX_BOOKS)
        val placeholderBg = look.scheme.primaryContainer.toArgb()
        val placeholderFg = look.scheme.onPrimaryContainer.toArgb()
        suspend fun covers(w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp): Map<String, Bitmap> {
            val wPx = (w.value * density).toInt()
            val hPx = (h.value * density).toInt()
            return books.associate { book ->
                book.id to (WidgetArt.cover(context, book.coverUrl, wPx, hPx, 8 * density)
                    ?: WidgetArt.coverPlaceholder(book.title, wPx, hPx, 8 * density, placeholderBg, placeholderFg))
            }
        }
        val small = covers(SMALL_W, SMALL_H)
        val large = covers(LARGE_W, LARGE_H)

        provideContent {
            val size = LocalSize.current
            val tall = size.height >= 180.dp
            val coverW = if (tall) LARGE_W else SMALL_W
            val coverH = if (tall) LARGE_H else SMALL_H
            val fit = ((size.width.value - 24 + GAP) / (coverW.value + GAP)).toInt().coerceIn(1, MAX_BOOKS)
            GlanceTheme(colors = look.colors) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(24.dp)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(DeepLink.intent(context, "library")))
                    ) {
                        Text(
                            "Up next",
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (data.upNext.isNotEmpty()) {
                            Text("${data.upNext.size} waiting", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp))
                        }
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    if (books.isEmpty()) {
                        Text(
                            "Nothing queued yet. Books you add to To read, Shortlist or Up next show up here.",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                        )
                    } else {
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            books.take(fit).forEachIndexed { i, book ->
                                if (i > 0) Spacer(GlanceModifier.width(GAP.dp))
                                CoverTile(context, book, (if (tall) large else small)[book.id], coverW, coverH, showTitle = tall)
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CoverTile(
        context: Context,
        book: Book,
        cover: Bitmap?,
        width: androidx.compose.ui.unit.Dp,
        height: androidx.compose.ui.unit.Dp,
        showTitle: Boolean
    ) {
        Column(modifier = GlanceModifier.width(width).clickable(actionStartActivity(DeepLink.intent(context, "focus/${book.id}")))) {
            cover?.let { Image(ImageProvider(it), contentDescription = book.title, modifier = GlanceModifier.size(width, height)) }
            if (showTitle) {
                Spacer(GlanceModifier.height(4.dp))
                Text(book.title, maxLines = 2, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp))
            }
        }
    }

    companion object {
        private const val MAX_BOOKS = 6
        private const val GAP = 8f
        private val SMALL_W = 44.dp
        private val SMALL_H = 64.dp
        private val LARGE_W = 56.dp
        private val LARGE_H = 84.dp
        private val SIZES = setOf(
            DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(320.dp, 110.dp),
            DpSize(250.dp, 180.dp), DpSize(320.dp, 180.dp)
        )
    }
}

class UpNextWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpNextWidget()
}
