package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.DnfReasons
import com.example.booktracker.shared.models.RatingAxis
import kotlin.math.roundToInt

@Composable
fun FinishDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (rating: Map<String, Float>) -> Unit
) {
    val values = remember {
        mutableStateMapOf<String, Float>().apply {
            putAll(RatingAxis.ALL.associateWith { 2.5f })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finished “${book.title}”") },
        text = {
            Column {
                Text("Rate your read (optional)", style = MaterialTheme.typography.bodyMedium)
                RatingAxis.ALL.forEach { axis ->
                    val v = values[axis] ?: 2.5f
                    Text("$axis: ${formatRating(v)}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = v,
                        onValueChange = { values[axis] = it },
                        valueRange = 0f..5f
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values.toMap()) }) { Text("Finish") }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(emptyMap()) }) { Text("Skip") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DnfDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (abandonedPercentage: Float, reason: String) -> Unit
) {
    val autoPercent = if (book.totalUnits > 0) {
        (book.currentUnit.toFloat() / book.totalUnits * 100f).coerceIn(0f, 100f)
    } else {
        0f
    }
    var percent by remember { mutableFloatStateOf(autoPercent) }
    var reason by remember { mutableStateOf(DnfReasons.ALL.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Did not finish") },
        text = {
            Column {
                Text("Abandoned at ${percent.roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = percent,
                    onValueChange = { percent = it },
                    valueRange = 0f..100f
                )
                Text("Reason", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DnfReasons.ALL.forEach { option ->
                        FilterChip(
                            selected = reason == option,
                            onClick = { reason = option },
                            label = { Text(option) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(percent, reason) }) { Text("Mark DNF") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** One decimal, snapped to the nearest half — matches the slider's practical resolution. */
private fun formatRating(v: Float): String = ((v * 2).roundToInt() / 2.0).toString()
