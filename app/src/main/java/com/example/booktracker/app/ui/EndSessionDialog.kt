package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine

@Composable
fun EndSessionDialog(
    pagesRead: Int,
    durationMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var customTag by remember { mutableStateOf("") }
    val presets = listOf("Bed", "Coffee Shop", "Commute", "Couch", "Desk", "Tea")
    var selectedPreset by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Session") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SessionRecap(pagesRead, durationMillis)
                Text(
                    "Where or how were you reading?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Render preset chips
                val chunkedPresets = presets.chunked(2)
                chunkedPresets.forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset,
                                onClick = {
                                    if (selectedPreset == preset) {
                                        selectedPreset = null
                                    } else {
                                        selectedPreset = preset
                                        customTag = "" // Clear custom if picking a preset
                                    }
                                },
                                label = { Text(preset) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowPresets.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customTag,
                    onValueChange = { 
                        customTag = it
                        if (it.isNotEmpty()) {
                            selectedPreset = null // Clear preset if typing custom
                        }
                    },
                    label = { Text("Custom tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalTag = customTag.takeIf { it.isNotBlank() } ?: selectedPreset ?: ""
                    onConfirm(finalTag)
                }
            ) {
                Text("End Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SessionRecap(pagesRead: Int, durationMillis: Long) {
    val duration = AnalyticsEngine.formatMinutes(durationMillis / 60_000L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                if (pagesRead > 0) "$pagesRead pages in $duration"
                else "Session lasted $duration",
                style = MaterialTheme.typography.titleMedium
            )
            if (pagesRead > 0 && durationMillis >= AnalyticsEngine.MIN_TIMED_SESSION_MILLIS) {
                val perHour = pagesRead / (durationMillis / 3_600_000f)
                Text(
                    "≈ ${perHour.toInt()} pages/hour",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
