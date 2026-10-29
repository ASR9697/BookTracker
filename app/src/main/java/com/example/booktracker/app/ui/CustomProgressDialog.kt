package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.rememberHaptics

/** Most sessions logged by hand land around here, so the dial starts close. */
private const val DEFAULT_DELTA = 10
private const val MAX_DELTA = 2_000

@Composable
fun CustomProgressDialog(
    unitName: String = "Pages",
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var delta by remember { mutableIntStateOf(DEFAULT_DELTA) }
    val haptics = rememberHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log progress") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "How many ${unitName.lowercase()} did you read?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PageDial(
                    value = delta,
                    onValueChange = { delta = it },
                    range = 0..MAX_DELTA,
                    unit = "+ ${unitName.lowercase()}"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.confirm()
                    onConfirm(delta)
                },
                enabled = delta > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
