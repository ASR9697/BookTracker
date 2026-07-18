package com.example.booktracker.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val GOAL_MIN = 5
private const val GOAL_MAX = 100
private const val GOAL_STEP = 5

private const val YEARLY_MIN = 2
private const val YEARLY_MAX = 60
private const val YEARLY_STEP = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dailyGoal: Int,
    yearlyGoal: Int,
    onDailyGoalChange: (Int) -> Unit,
    onYearlyGoalChange: (Int) -> Unit,
    onImportCsv: (Uri, (Int) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onImportCsv(uri) { count ->
                Toast.makeText(context, "Imported $count books", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Local slider state so dragging is smooth; persist the snapped value on change.
    var goal by remember(dailyGoal) { mutableIntStateOf(dailyGoal) }
    var booksGoal by remember(yearlyGoal) { mutableIntStateOf(yearlyGoal) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(title = "Daily reading goal") {
                Text(
                    "$goal pages per day",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
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

            SettingsCard(title = "Yearly reading goal") {
                Text(
                    "$booksGoal books this year",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = booksGoal.toFloat(),
                    onValueChange = {
                        booksGoal = (Math.round(it / YEARLY_STEP) * YEARLY_STEP)
                            .coerceIn(YEARLY_MIN, YEARLY_MAX)
                    },
                    onValueChangeFinished = { onYearlyGoalChange(booksGoal) },
                    valueRange = YEARLY_MIN.toFloat()..YEARLY_MAX.toFloat(),
                    steps = (YEARLY_MAX - YEARLY_MIN) / YEARLY_STEP - 1
                )
                Text(
                    "Tracked on your profile as books finished this year.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(title = "Data") {
                FilledTonalButton(
                    onClick = { launcher.launch("text/csv") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Import Goodreads / StoryGraph CSV")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Importing will add books to your library based on your history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private fun snap(raw: Float): Int =
    (Math.round(raw / GOAL_STEP) * GOAL_STEP).coerceIn(GOAL_MIN, GOAL_MAX)
