package com.example.booktracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalTime

/**
 * The streak at a glance: the number, today's pages as a ring around it, and
 * (when wide) this week's reading days. In the evening, with nothing read yet,
 * it turns amber so the streak doesn't slip by unnoticed.
 */
class StreakWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val look = WidgetData.look(context)
        val streak = data.streak
        val density = context.resources.displayMetrics.density
        val atRisk = !streak.goalMetToday && streak.currentStreak > 0 && LocalTime.now().hour >= AT_RISK_HOUR
        val accent = if (atRisk) AMBER else look.scheme.primary.toArgb()
        val fraction = if (streak.dailyGoal > 0) streak.pagesToday.toFloat() / streak.dailyGoal else if (streak.goalMetToday) 1f else 0f
        val ring = WidgetArt.ring((RING.value * density).toInt(), fraction, look.scheme.surfaceVariant.toArgb(), accent)
        val week = WidgetArt.weekDots(
            today = streak.today,
            readDays = streak.activeDays,
            dotPx = (DOT.value * density).toInt(),
            filled = accent,
            empty = look.scheme.surfaceVariant.toArgb(),
            label = look.scheme.onSurfaceVariant.toArgb(),
            onFilled = look.scheme.onPrimary.toArgb()
        )
        val status = when {
            streak.goalMetToday && streak.pagesToday >= streak.dailyGoal -> "Goal met today"
            streak.goalMetToday -> "Today counts · ${streak.pagesToday}/${streak.dailyGoal}"
            atRisk -> "Read a page to keep it"
            streak.currentStreak == 0 -> "Read today to start one"
            else -> "${streak.pagesToday}/${streak.dailyGoal} pages today"
        }

        provideContent {
            GlanceTheme(colors = look.colors) {
                Content(context, data, ring, week, status, atRisk)
            }
        }
    }

    @Composable
    private fun Content(context: Context, data: WidgetSnapshot, ring: Bitmap, week: Bitmap, status: String, atRisk: Boolean) {
        val streak = data.streak
        val wide = LocalSize.current.width >= WIDE.width
        val accentText = if (atRisk) ColorProvider(androidx.compose.ui.graphics.Color(AMBER)) else GlanceTheme.colors.primary
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(DeepLink.intent(context, "streak"))),
            contentAlignment = Alignment.Center
        ) {
            if (!wide) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RingWithNumber(ring, streak.currentStreak)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(status, maxLines = 1, style = TextStyle(color = accentText, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingWithNumber(ring, streak.currentStreak)
                    Spacer(GlanceModifier.width(14.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            when (streak.currentStreak) {
                                0 -> "No streak yet"
                                1 -> "1-day streak"
                                else -> "${streak.currentStreak}-day streak"
                            },
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                        Text(
                            "Best ${streak.longestStreak} · $status",
                            maxLines = 1,
                            style = TextStyle(color = accentText, fontSize = 12.sp)
                        )
                        Spacer(GlanceModifier.height(8.dp))
                        // Seven dots with a third of a dot between them, as WidgetArt.weekDots draws them.
                        Image(
                            ImageProvider(week),
                            contentDescription = "This week's reading days",
                            modifier = GlanceModifier.size(DOT * 9, DOT)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RingWithNumber(ring: Bitmap, streak: Int) {
        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(RING)) {
            Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(RING))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", style = TextStyle(fontSize = 14.sp))
                Text("$streak", style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp))
            }
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val RING = 76.dp
        private val DOT = 16.dp
        /** From this hour an unread day shows the streak as at risk. */
        private const val AT_RISK_HOUR = 20
        private const val AMBER = 0xFFFFA000.toInt()
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
