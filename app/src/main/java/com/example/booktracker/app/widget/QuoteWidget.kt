package com.example.booktracker.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * One of your saved passages, a new one each day, with the book it came from.
 * The arrow shows another; tapping opens the book.
 */
class QuoteWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val look = WidgetData.look(context)
        val booksById = data.books.associateBy { it.id }
        val day = data.streak.today.toEpochDay()

        provideContent {
            val offset = currentState(OFFSET) ?: 0
            val note = WidgetPicks.quoteOfTheDay(data.notes, day, offset)
            val book = note?.let { booksById[it.bookId] }
            GlanceTheme(colors = look.colors) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(24.dp)
                        .padding(16.dp)
                        .clickable(
                            actionStartActivity(DeepLink.intent(context, if (book != null) "book/${book.id}" else "memorize"))
                        )
                ) {
                    if (note == null) {
                        Text("Quote of the day", style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp))
                        Spacer(GlanceModifier.height(6.dp))
                        Text(
                            "Save a passage from a book you're reading and it will show up here.",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                        )
                    } else {
                        val height = LocalSize.current.height
                        // Roughly one line per 22dp left after the header and footer.
                        val lines = ((height.value - 84) / 22).toInt().coerceIn(2, 12)
                        Text("❝", style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 22.sp))
                        Text(
                            note.content.trim(),
                            maxLines = lines,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontStyle = FontStyle.Italic),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(GlanceModifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                listOfNotNull(book?.title, note.pageNumber.takeIf { it > 0 }?.let { "p. $it" }).joinToString(" · ")
                                    .ifBlank { "Your notes" }.let { "— $it" },
                                maxLines = 1,
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Button(
                                text = "↻",
                                onClick = actionRunCallback<NextQuoteAction>(),
                                colors = ButtonDefaults.buttonColors(GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        val OFFSET = intPreferencesKey("quote_offset")
    }
}

class NextQuoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
            prefs[QuoteWidget.OFFSET] = (prefs[QuoteWidget.OFFSET] ?: 0) + 1
        }
        QuoteWidget().update(context, glanceId)
    }
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}
