package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
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
    
    val sessionsByDate = remember(sessions) {
        sessions.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
    }

    val maxPages = dailyPages.values.maxOrNull() ?: 1
    val today = LocalDate.now(zone)
    val columns = 20
    val rows = 7
    val startDate = today.minusDays((columns * rows - 1).toLong())

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text("Reading Consistency", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        val baseColor = MaterialTheme.colorScheme.primary
        val emptyColor = MaterialTheme.colorScheme.surfaceVariant

        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (col in 0 until columns) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until rows) {
                        val daysOffset = (col * rows) + row
                        val date = startDate.plusDays(daysOffset.toLong())
                        
                        val pages = dailyPages[date] ?: 0
                        
                        val color = if (pages == 0) {
                            emptyColor
                        } else {
                            val intensity = (pages.toFloat() / maxPages).coerceIn(0.2f, 1f)
                            baseColor.copy(alpha = intensity)
                        }

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                                .clickable {
                                    if (pages > 0) {
                                        selectedDate = date
                                        showBottomSheet = true
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedDate != null) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                val dateStr = selectedDate!!.toString()
                Text("Sessions on $dateStr", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                
                val daysSessions = sessionsByDate[selectedDate] ?: emptyList()
                if (daysSessions.isEmpty()) {
                    Text("No sessions recorded.")
                } else {
                    daysSessions.forEach { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Pages: ${session.pagesRead}", style = MaterialTheme.typography.titleSmall)
                                if (session.durationSeconds > 0) {
                                    Text("Duration: ${session.durationSeconds / 60} min", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
