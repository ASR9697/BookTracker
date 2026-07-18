package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val GOAL_MIN = 5
private const val GOAL_MAX = 100
private const val GOAL_STEP = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dailyGoal: Int,
    onDailyGoalChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    // Local slider state so dragging is smooth; persist the snapped value on change.
    var goal by remember(dailyGoal) { mutableIntStateOf(dailyGoal) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Daily reading goal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "$goal pages per day",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = goal.toFloat(),
                onValueChange = { goal = snap(it) },
                onValueChangeFinished = { onDailyGoalChange(goal) },
                valueRange = GOAL_MIN.toFloat()..GOAL_MAX.toFloat(),
                steps = (GOAL_MAX - GOAL_MIN) / GOAL_STEP - 1
            )
            Text(
                "Meet this each day to keep your streak alive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun snap(raw: Float): Int =
    (Math.round(raw / GOAL_STEP) * GOAL_STEP).coerceIn(GOAL_MIN, GOAL_MAX)
