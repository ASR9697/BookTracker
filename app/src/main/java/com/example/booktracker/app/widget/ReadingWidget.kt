package com.example.booktracker.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.intPreferencesKey
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
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
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.FinishByPlanner
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.service.TimerService
import com.example.booktracker.app.ui.TimerMode
import com.example.booktracker.app.ui.finishByLine
import com.example.booktracker.shared.models.Book

/**
 * Now reading: cover, progress and time left, with +1 / +5 that log straight
 * from the home screen and a Read button into the timer. While the timer runs
 * it shows a live clock with pause. Several books in progress? The arrow steps
 * through them.
 */
class ReadingWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val look = WidgetData.look(context)
        val density = context.resources.displayMetrics.density
        val coverW = (COVER_WIDTH.value * density).toInt()
        val coverH = (COVER_HEIGHT.value * density).toInt()
        val ringPx = (RING.value * density).toInt()
        val books = data.reading.take(MAX_BOOKS)
        val covers = books.associate { book ->
            book.id to (WidgetArt.cover(context, book.coverUrl, coverW, coverH, 10 * density)
                ?: WidgetArt.coverPlaceholder(book.title, coverW, coverH, 10 * density, look.scheme.primaryContainer.toArgb(), look.scheme.onPrimaryContainer.toArgb()))
        }
        val rings = books.associate { book ->
            book.id to WidgetArt.ring(ringPx, fraction(book), look.scheme.surfaceVariant.toArgb(), look.scheme.primary.toArgb())
        }
        val timerColor = look.scheme.onSurface.toArgb()

        provideContent {
            val index = currentState(BOOK_INDEX) ?: 0
            // The book with the running timer always wins; otherwise the one the arrow picked.
            val book = books.find { it.id == data.timerBookId } ?: books.getOrNull(Math.floorMod(index, books.size.coerceAtLeast(1)))
            GlanceTheme(colors = look.colors) {
                Content(context, data, book, books.size, book?.let { covers[it.id] }, book?.let { rings[it.id] }, timerColor)
            }
        }
    }

    @Composable
    private fun Content(
        context: Context,
        data: WidgetSnapshot,
        book: Book?,
        bookCount: Int,
        cover: Bitmap?,
        ring: Bitmap?,
        timerColor: Int
    ) {
        val size = LocalSize.current
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
                book == null -> Empty(context)
                size.width < WIDE.width -> Small(context, book, ring)
                else -> Wide(context, data, book, bookCount, cover, timerColor, tall = size.height >= TALL.height)
            }
        }
    }

    @Composable
    private fun Empty(context: Context) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.fillMaxWidth()) {
            Text("Nothing on the go", style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp))
            Spacer(GlanceModifier.height(8.dp))
            Button(
                text = "Pick a book",
                onClick = actionStartActivity(DeepLink.intent(context, "library")),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.primary, GlanceTheme.colors.onPrimary)
            )
        }
    }

    /** 2x2: the progress ring with the percentage inside, the title, and +1. */
    @Composable
    private fun Small(context: Context, book: Book, ring: Bitmap?) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier.size(RING).clickable(actionStartActivity(DeepLink.intent(context, "book/${book.id}")))
            ) {
                ring?.let { Image(ImageProvider(it), contentDescription = null, modifier = GlanceModifier.size(RING)) }
                Text(
                    if (book.totalPages > 0) "${(fraction(book) * 100).toInt()}%" else "p. ${book.currentPage}",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(book.title, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.height(4.dp))
            Button(
                text = "+1 page",
                onClick = addPages(book, 1),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.primaryContainer, GlanceTheme.colors.onPrimaryContainer)
            )
        }
    }

    @Composable
    private fun Wide(
        context: Context,
        data: WidgetSnapshot,
        book: Book,
        bookCount: Int,
        cover: Bitmap?,
        timerColor: Int,
        tall: Boolean
    ) {
        val openBook = actionStartActivity(DeepLink.intent(context, "book/${book.id}"))
        val timerOnThis = data.timerBookId == book.id
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                cover?.let {
                    Image(
                        ImageProvider(it),
                        contentDescription = book.title,
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier.size(COVER_WIDTH, COVER_HEIGHT).clickable(openBook)
                    )
                    Spacer(GlanceModifier.width(12.dp))
                }
                Column(modifier = GlanceModifier.defaultWeight().clickable(openBook)) {
                    Text(
                        book.title,
                        maxLines = if (tall) 2 else 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                    book.authors.firstOrNull()?.let {
                        Text(it, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    if (book.totalPages > 0) {
                        LinearProgressIndicator(
                            progress = fraction(book),
                            color = GlanceTheme.colors.primary,
                            backgroundColor = GlanceTheme.colors.surfaceVariant,
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp)
                        )
                        Spacer(GlanceModifier.height(4.dp))
                    }
                    Text(progressLine(book, data), maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                    if (tall) {
                        val plan = FinishByPlanner.plan(book, data.sessions, data.streak.today, dayStartsAtHour = data.streak.dayStartsAtHour)
                        val line = finishByLine(plan)?.first
                            ?: "${data.streak.pagesToday} of ${data.streak.dailyGoal} pages today · ${data.streak.currentStreak}-day streak"
                        Spacer(GlanceModifier.height(4.dp))
                        Text(line, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            if (timerOnThis) TimerRow(context, data, book, timerColor) else ActionRow(context, book, bookCount)
        }
    }

    @Composable
    private fun ActionRow(context: Context, book: Book, bookCount: Int) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Button(
                text = "+1",
                onClick = addPages(book, 1),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
            )
            Spacer(GlanceModifier.width(6.dp))
            Button(
                text = "+5",
                onClick = addPages(book, 5),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
            )
            Spacer(GlanceModifier.width(6.dp))
            Button(
                text = "▶  Read",
                onClick = actionStartActivity(DeepLink.intent(context, "focus/${book.id}")),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.primary, GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.defaultWeight()
            )
            if (bookCount > 1) {
                Spacer(GlanceModifier.width(6.dp))
                Button(
                    text = "›",
                    onClick = actionRunCallback<NextBookAction>(),
                    colors = ButtonDefaults.buttonColors(GlanceTheme.colors.surfaceVariant, GlanceTheme.colors.onSurfaceVariant)
                )
            }
        }
    }

    /** A live clock that ticks on its own (no updates needed), with pause or resume and save. */
    @Composable
    private fun TimerRow(context: Context, data: WidgetSnapshot, book: Book, timerColor: Int) {
        val countdown = data.timerMode == TimerMode.COUNTDOWN
        val base = if (countdown) SystemClock.elapsedRealtime() + data.timeLeftSeconds * 1000L
        else SystemClock.elapsedRealtime() - data.elapsedSeconds * 1000L
        val clock = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
            setChronometer(R.id.widget_chronometer, base, null, data.timerRunning)
            setChronometerCountDown(R.id.widget_chronometer, countdown)
            setTextColor(R.id.widget_chronometer, timerColor)
        }
        val control = Intent(context, TimerService::class.java).apply {
            action = if (data.timerRunning) TimerService.ACTION_PAUSE else TimerService.ACTION_RESUME
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    if (data.timerRunning) "Reading now" else "Paused",
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                AndroidRemoteViews(clock)
            }
            Button(
                text = if (data.timerRunning) "Pause" else "Resume",
                onClick = actionStartService(control),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
            )
            Spacer(GlanceModifier.width(6.dp))
            Button(
                text = "Save",
                onClick = actionStartActivity(DeepLink.intent(context, "session_save/${book.id}")),
                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.primary, GlanceTheme.colors.onPrimary)
            )
        }
    }

    private fun addPages(book: Book, amount: Int) = actionRunCallback<AddProgressAction>(
        actionParametersOf(AddProgressAction.bookIdKey to book.id, AddProgressAction.amountKey to amount)
    )

    private fun progressLine(book: Book, data: WidgetSnapshot): String {
        val unit = FormatAdaptabilityLayer.unitAbbrev(book)
        val position = if (book.totalPages > 0) "$unit ${book.currentPage} / ${book.totalPages}" else "$unit ${book.currentPage}"
        val left = if (!data.showTimeLeft) null else AnalyticsEngine.formatEstimatedTime(
            AnalyticsEngine.bookEstimatedMinutesLeft(book, data.sessions.filter { it.bookId == book.id }, data.sessions)
        )
        return if (left != null) "$position · about $left" else position
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val TALL = DpSize(250.dp, 180.dp)
        private val COVER_WIDTH = 56.dp
        private val COVER_HEIGHT = 84.dp
        private val RING = 64.dp
        private const val MAX_BOOKS = 5
        val BOOK_INDEX = intPreferencesKey("book_index")

        private fun fraction(book: Book): Float =
            if (book.totalPages > 0) (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) else 0f
    }
}

class AddProgressAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val bookId = parameters[bookIdKey] ?: return
        val amount = parameters[amountKey] ?: return
        ServiceLocator.repository(context).addProgress(bookId, amount)
        ReadingWidget().update(context, glanceId)
    }

    companion object {
        val bookIdKey = ActionParameters.Key<String>("bookId")
        val amountKey = ActionParameters.Key<Int>("amount")
    }
}

/** Steps the widget to the next book in progress. */
class NextBookAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
            prefs[ReadingWidget.BOOK_INDEX] = (prefs[ReadingWidget.BOOK_INDEX] ?: 0) + 1
        }
        ReadingWidget().update(context, glanceId)
    }
}

class ReadingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReadingWidget()
}
