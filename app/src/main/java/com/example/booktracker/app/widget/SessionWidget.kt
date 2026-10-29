package com.example.booktracker.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import com.example.booktracker.app.R
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.service.TimerService
import com.example.booktracker.app.ui.TimerMode
import com.example.booktracker.shared.models.Book

/**
 * The session in progress, live: a clock that ticks on its own, pages this
 * session, the page goal, and pause, +1 and finish. With no session running it
 * offers to start one on the book read most recently.
 */
class SessionWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val look = WidgetData.look(context)
        val density = context.resources.displayMetrics.density
        val active = data.timerBookId?.let { timerId -> data.books.find { it.id == timerId } }
        val book = active ?: data.reading.firstOrNull()
        val coverW = (COVER_WIDTH.value * density).toInt()
        val coverH = (COVER_HEIGHT.value * density).toInt()
        val cover = book?.let {
            WidgetArt.cover(context, it.coverUrl, coverW, coverH, 8 * density)
                ?: WidgetArt.coverPlaceholder(it.title, coverW, coverH, 8 * density, look.scheme.primaryContainer.toArgb(), look.scheme.onPrimaryContainer.toArgb())
        }
        val clockColor = look.scheme.onSurface.toArgb()

        provideContent {
            GlanceTheme(colors = look.colors) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(24.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        active != null -> Live(context, data, active, cover, clockColor)
                        book != null -> Ready(context, book, cover)
                        else -> Nothing(context)
                    }
                }
            }
        }
    }

    @Composable
    private fun Live(context: Context, data: WidgetSnapshot, book: Book, cover: Bitmap?, clockColor: Int) {
        val size = LocalSize.current
        val small = size.width < WIDE.width
        val tall = size.height >= TALL.height
        val unit = FormatAdaptabilityLayer.unitAbbrev(book)
        val start = data.sessionStartPage ?: book.currentPage
        val delta = (book.currentPage - start).coerceAtLeast(0)
        val goalPages = data.pageTarget?.let { (it - start).coerceAtLeast(1) }
        val expired = data.timerMode == TimerMode.COUNTDOWN && data.timeLeftSeconds <= 0
        val openTimer = actionStartActivity(DeepLink.intent(context, "focus/${book.id}"))

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight().clickable(openTimer)
            ) {
                if (!small) {
                    cover?.let {
                        Image(ImageProvider(it), contentDescription = book.title, contentScale = ContentScale.Fit, modifier = GlanceModifier.size(COVER_WIDTH, COVER_HEIGHT))
                        Spacer(GlanceModifier.width(12.dp))
                    }
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        when {
                            expired -> "Time's up"
                            data.timerRunning -> "Reading now"
                            else -> "Paused"
                        },
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    AndroidRemoteViews(clock(context, data, clockColor, if (small) 26f else 34f))
                    if (tall) {
                        Text(book.title, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                    }
                    Text(
                        buildString {
                            append("$unit ${book.currentPage}")
                            if (book.totalPages > 0 && !small) append(" / ${book.totalPages}")
                            if (delta > 0) append(" · +$delta")
                        },
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    )
                    if (goalPages != null && !small) {
                        Spacer(GlanceModifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (delta.toFloat() / goalPages).coerceIn(0f, 1f),
                            color = GlanceTheme.colors.primary,
                            backgroundColor = GlanceTheme.colors.surfaceVariant,
                            modifier = GlanceModifier.fillMaxWidth().height(5.dp)
                        )
                        Text(
                            if (delta >= goalPages) "Page goal reached" else "Goal: $delta of $goalPages",
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                if (!expired) {
                    val control = Intent(context, TimerService::class.java).apply {
                        action = if (data.timerRunning) TimerService.ACTION_PAUSE else TimerService.ACTION_RESUME
                    }
                    Button(
                        text = if (data.timerRunning) "Pause" else "Resume",
                        onClick = actionStartService(control),
                        colors = ButtonDefaults.buttonColors(GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Button(
                        text = "+1",
                        onClick = actionRunCallback<SessionPageAction>(actionParametersOf(SessionPageAction.bookIdKey to book.id)),
                        colors = ButtonDefaults.buttonColors(GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
                    )
                }
                if (!small || expired) {
                    Spacer(GlanceModifier.width(6.dp))
                    Button(
                        text = "Finish",
                        onClick = actionStartActivity(DeepLink.intent(context, "session_save/${book.id}")),
                        colors = ButtonDefaults.buttonColors(GlanceTheme.colors.primary, GlanceTheme.colors.onPrimary),
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun Ready(context: Context, book: Book, cover: Bitmap?) {
        val small = LocalSize.current.width < WIDE.width
        val unit = FormatAdaptabilityLayer.unitAbbrev(book)
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = if (small) Alignment.CenterHorizontally else Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                if (!small) {
                    cover?.let {
                        Image(ImageProvider(it), contentDescription = book.title, contentScale = ContentScale.Fit, modifier = GlanceModifier.size(COVER_WIDTH, COVER_HEIGHT))
                        Spacer(GlanceModifier.width(12.dp))
                    }
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("Ready to read", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Text(book.title, maxLines = 2, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Text(
                        if (book.totalPages > 0) "$unit ${book.currentPage} / ${book.totalPages}" else "$unit ${book.currentPage}",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    )
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Button(
                text = "▶  Start reading",
                onClick = actionStartActivity(DeepLink.intent(context, "focus/${book.id}")),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.primary, GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun Nothing(context: Context) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.fillMaxWidth()) {
            Text("No book on the go", style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp))
            Spacer(GlanceModifier.height(8.dp))
            Button(
                text = "Pick a book",
                onClick = actionStartActivity(DeepLink.intent(context, "library")),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.primary, GlanceTheme.colors.onPrimary)
            )
        }
    }

    /** A Chronometer the launcher ticks itself, so the widget needn't redraw every second. */
    private fun clock(context: Context, data: WidgetSnapshot, color: Int, textSp: Float): RemoteViews {
        val countdown = data.timerMode == TimerMode.COUNTDOWN
        val base = if (countdown) SystemClock.elapsedRealtime() + data.timeLeftSeconds * 1000L
        else SystemClock.elapsedRealtime() - data.elapsedSeconds * 1000L
        return RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
            setChronometer(R.id.widget_chronometer, base, null, data.timerRunning)
            setChronometerCountDown(R.id.widget_chronometer, countdown)
            setTextColor(R.id.widget_chronometer, color)
            setTextViewTextSize(R.id.widget_chronometer, TypedValue.COMPLEX_UNIT_SP, textSp)
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val TALL = DpSize(250.dp, 180.dp)
        private val COVER_WIDTH = 48.dp
        private val COVER_HEIGHT = 72.dp
    }
}

/** +1 page on the session's book, logged straight to it. */
class SessionPageAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val bookId = parameters[bookIdKey] ?: return
        ServiceLocator.repository(context).addProgress(bookId, 1)
        SessionWidget().update(context, glanceId)
    }

    companion object {
        val bookIdKey = ActionParameters.Key<String>("bookId")
    }
}

class SessionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SessionWidget()
}
