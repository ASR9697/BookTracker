package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.booktracker.app.analytics.AnalyticsEngine

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun EndSessionDialog(
    startPage: Int, // Represents the current reading unit/page
    totalUnits: Int,
    durationMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var customTag by remember { mutableStateOf("") }
    var endingPage by remember { mutableStateOf(startPage.toString()) }
    val presets = listOf("Bed", "Coffee Shop", "Commute", "Couch", "Desk", "Tea")
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Session") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentEndPage = endingPage.toIntOrNull() ?: startPage
                val sessionPagesRead = (currentEndPage - startPage).coerceAtLeast(0)
                SessionRecap(sessionPagesRead, durationMillis)
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
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    value = endingPage,
                    onValueChange = { endingPage = it },
                    label = { Text("Ending page") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = endingPage.toIntOrNull()?.let { it > totalUnits && totalUnits > 0 } == true
                )
                if (endingPage.toIntOrNull()?.let { it > totalUnits && totalUnits > 0 } == true) {
                    Text(
                        "Cannot exceed total units ($totalUnits)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val finalTag = customTag.takeIf { it.isNotBlank() } ?: selectedPreset ?: ""
                    val finalPage = endingPage.toIntOrNull() ?: startPage
                    onConfirm(finalTag, finalPage)
                },
                enabled = endingPage.toIntOrNull()?.let { it <= totalUnits || totalUnits == 0 } != false
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
            if (pagesRead > 0 && durationMillis > 0) {
                val perHour = pagesRead / (durationMillis / 3_600_000f)
                val timePerPage = durationMillis.toFloat() / pagesRead.toFloat()
                val minutesPerPage = timePerPage / 60000f
                val secondsPerPage = (timePerPage / 1000f).toInt() % 60
                val perPageText = if (minutesPerPage >= 1) {
                    "${minutesPerPage.toInt()}m ${secondsPerPage}s / page"
                } else {
                    "${secondsPerPage}s / page"
                }
                
                Text(
                    "≈ ${perHour.toInt()} pages/hour • $perPageText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
