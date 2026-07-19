package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun StartSessionDialog(
    currentUnit: Int,
    totalUnits: Int,
    unitName: String = "Page",
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var startPage by remember { mutableStateOf(currentUnit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Reading Session") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Confirm the $unitName you are starting on:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = startPage,
                    onValueChange = { startPage = it.filter { char -> char.isDigit() } },
                    label = { Text("Start $unitName") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = startPage.toIntOrNull()?.let { it > totalUnits && totalUnits > 0 } == true
                )
                if (startPage.toIntOrNull()?.let { it > totalUnits && totalUnits > 0 } == true) {
                    Text(
                        "Cannot exceed total ${unitName}s ($totalUnits)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalPage = startPage.toIntOrNull() ?: currentUnit
                    onConfirm(finalPage)
                },
                enabled = startPage.toIntOrNull()?.let { it <= totalUnits || totalUnits == 0 } != false
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
