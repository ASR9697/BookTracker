package com.example.booktracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun ReadingHeatmap(
    sessions: List<Session>,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    
    // Aggregate pages read by LocalDate
    val dailyPages = remember(sessions) {
        val map = mutableMapOf<LocalDate, Int>()
        for (session in sessions) {
            val date = Instant.ofEpochMilli(session.startTime).atZone(zone).toLocalDate()
            map[date] = (map[date] ?: 0) + session.pagesRead
        }
        map
    }

    val maxPages = dailyPages.values.maxOrNull() ?: 1
    val today = LocalDate.now(zone)
    val columns = 20
    val rows = 7
    val startDate = today.minusDays((columns * rows - 1).toLong())

    Column(modifier = modifier) {
        Text("Reading Consistency", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        val baseColor = MaterialTheme.colorScheme.primary
        val emptyColor = MaterialTheme.colorScheme.surfaceVariant

        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val boxSize = 12.dp.toPx()
            val padding = 4.dp.toPx()
            
            // Draw grid
            for (col in 0 until columns) {
                for (row in 0 until rows) {
                    val daysOffset = (col * rows) + row
                    val date = startDate.plusDays(daysOffset.toLong())
                    
                    val pages = dailyPages[date] ?: 0
                    
                    val color = if (pages == 0) {
                        emptyColor
                    } else {
                        // Intensity based on pages read relative to max
                        val intensity = (pages.toFloat() / maxPages).coerceIn(0.2f, 1f)
                        baseColor.copy(alpha = intensity)
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(col * (boxSize + padding), row * (boxSize + padding)),
                        size = Size(boxSize, boxSize),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }
    }
}
