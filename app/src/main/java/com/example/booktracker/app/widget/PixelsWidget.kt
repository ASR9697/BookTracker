package com.example.booktracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.booktracker.app.analytics.ReadingCalendar
import com.example.booktracker.app.analytics.StreakEngine
import java.time.ZoneId

/**
 * Your recent weeks as a heatmap, a column per week, darker on bigger days.
 * As many weeks as the widget is wide; tap for the full calendar.
 */
class PixelsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val look = WidgetData.look(context)
        // Every size's grid travels in one update, which has a bitmap memory cap; 2x is plenty sharp for cells.
        val density = context.resources.displayMetrics.density.coerceAtMost(2f)
        val streak = data.streak
        val pagesByDay = StreakEngine.pagesByDay(data.sessions, ZoneId.systemDefault(), streak.dayStartsAtHour)
        val primary = look.scheme.primary.toArgb()
        val empty = look.scheme.surfaceVariant.toArgb()
        val colors = intArrayOf(
            empty,
            ColorUtils.blendARGB(empty, primary, 0.35f),
            ColorUtils.blendARGB(empty, primary, 0.6f),
            ColorUtils.blendARGB(empty, primary, 0.82f),
            primary
        )
        // A timer-only day has 0 pages but still counts as reading, so it gets the lightest shade.
        val level = { date: java.time.LocalDate ->
            pagesByDay[date]?.let { pages -> ReadingCalendar.level(pages, streak.dailyGoal).coerceAtLeast(1) } ?: 0
        }
        val bitmaps: Map<DpSize, Bitmap> = SIZES.associateWith { size ->
            // Leave room for padding and the header, then fit square cells with a small gap.
            val gridHeight = size.height.value - 32 - 24
            val cell = ((gridHeight - 6 * GAP) / 7).coerceIn(6f, 16f)
            val weeks = ((size.width.value - 32 + GAP) / (cell + GAP)).toInt().coerceIn(4, 53)
            WidgetArt.pixels(streak.today, weeks, level, colors, (cell * density).toInt(), (GAP * density).toInt(), data.weekStart)
        }
        val year = streak.today.year
        val daysThisYear = streak.activeDays.count { it.year == year }

        provideContent {
            val size = LocalSize.current
            val grid = bitmaps[size] ?: bitmaps.values.first()
            GlanceTheme(colors = look.colors) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(24.dp)
                        .padding(16.dp)
                        .clickable(actionStartActivity(DeepLink.intent(context, "calendar")))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            "Your reading",
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            "$daysThisYear ${if (daysThisYear == 1) "day" else "days"} in $year",
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                        Image(
                            ImageProvider(grid),
                            contentDescription = "Reading days over recent weeks",
                            contentScale = ContentScale.Fit,
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val GAP = 3f
        // The wider sizes are for tablet home screens, where a widget can span half the screen.
        private val SIZES = setOf(
            DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(320.dp, 110.dp), DpSize(320.dp, 180.dp),
            DpSize(480.dp, 110.dp), DpSize(480.dp, 180.dp), DpSize(640.dp, 180.dp)
        )
    }
}

class PixelsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PixelsWidget()
}
