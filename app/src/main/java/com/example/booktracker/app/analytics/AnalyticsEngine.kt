package com.example.booktracker.app.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.booktracker.shared.models.Session

// Multi-Axis Qualitative Ratings
@Composable
fun QualitativeRatingSliders() {
    var pacing by remember { mutableFloatStateOf(2.5f) }
    var focus by remember { mutableFloatStateOf(2.5f) }
    var vibe by remember { mutableFloatStateOf(2.5f) }

    Column {
        Text("Pacing: $pacing")
        Slider(value = pacing, onValueChange = { pacing = it }, valueRange = 0f..5f)

        Text("Focus: $focus")
        Slider(value = focus, onValueChange = { focus = it }, valueRange = 0f..5f)

        Text("Vibe: $vibe")
        Slider(value = vibe, onValueChange = { vibe = it }, valueRange = 0f..5f)
    }
}

// Analytics & Context Engine
class AnalyticsEngine {
    fun calculateReadingVelocity(session: Session): Float {
        val durationMinutes = (session.endTime - session.startTime) / 60_000f
        if (durationMinutes <= 0f) return 0f
        return session.unitsRead / durationMinutes
    }

    fun correlateEnvironment(sessions: List<Session>): Map<String, Float> {
        // Averages unitsRead per environmentTag
        return sessions.groupBy { it.environmentTag }
            .mapValues { entry ->
                entry.value.map { it.unitsRead }.average().toFloat()
            }
    }
}
