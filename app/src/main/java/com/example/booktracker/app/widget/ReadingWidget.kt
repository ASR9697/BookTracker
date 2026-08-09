package com.example.booktracker.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class ReadingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = ServiceLocator.settings(context)
        val dailyGoal = settings.dailyGoal.firstOrNull() ?: 20
        val repository = ServiceLocator.repository(context)
        val streak = com.example.booktracker.app.analytics.StreakEngine.compute(
            repository.observeCompletedSessions().firstOrNull() ?: emptyList(),
            settings.dayStartsAtHour.firstOrNull() ?: 3
        ).currentStreak
        val books = repository.observeBooks().firstOrNull() ?: emptyList()
        val readingBook = books
            .filter { it.status == BookStatus.READING.name }
            .maxByOrNull { it.lastUpdated }

        provideContent {
            GlanceTheme {
                WidgetContent(readingBook, dailyGoal, streak)
            }
        }
    }

    @Composable
    private fun WidgetContent(book: Book?, dailyGoal: Int, streak: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (book != null) {
                Text(
                    text = book.title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(4.dp))
                
                val progressStr = if (book.totalPages > 0) {
                    "${book.currentPage} / ${book.totalPages} ${book.format.name.lowercase()}"
                } else {
                    "${book.currentPage} ${book.format.name.lowercase()}"
                }
                
                Text(
                    text = progressStr,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
                Spacer(GlanceModifier.height(16.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val context = androidx.glance.LocalContext.current
                    WidgetButton("+1", actionRunCallback<AddProgressAction>(
                        actionParametersOf(AddProgressAction.bookIdKey to book.id, AddProgressAction.amountKey to 1)
                    ))
                    Spacer(GlanceModifier.width(8.dp))
                    WidgetButton("Timer", actionStartActivity(
                        android.content.Intent(context, com.example.booktracker.app.MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    ))
                }
            } else {
                Text(
                    text = "No active reading",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
            
            Spacer(GlanceModifier.height(16.dp))
            Text(
                text = "🔥 Streak: $streak days  •  🎯 Goal: $dailyGoal pages",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    @Composable
    private fun WidgetButton(text: String, action: androidx.glance.action.Action) {
        Text(
            text = text,
            modifier = GlanceModifier
                .background(GlanceTheme.colors.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(action),
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

class AddProgressAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val bookId = parameters[bookIdKey] ?: return
        val amount = parameters[amountKey] ?: return
        
        val repository = ServiceLocator.repository(context)
        val books = repository.observeBooks().firstOrNull() ?: emptyList()
        val book = books.find { it.id == bookId }
        
        if (book != null) {
            repository.addProgress(book.id, amount)
            ReadingWidget().update(context, glanceId)
        }
    }

    companion object {
        val bookIdKey = ActionParameters.Key<String>("bookId")
        val amountKey = ActionParameters.Key<Int>("amount")
    }
}

class ReadingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReadingWidget()
}
