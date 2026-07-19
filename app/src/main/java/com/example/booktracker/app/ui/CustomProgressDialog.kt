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
fun CustomProgressDialog(
    unitName: String = "Pages",
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var deltaStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Progress") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "How many $unitName did you read?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = deltaStr,
                    onValueChange = { deltaStr = it.filter { char -> char.isDigit() } },
                    label = { Text("+ $unitName") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val delta = deltaStr.toIntOrNull() ?: 0
                    if (delta > 0) {
                        onConfirm(delta)
                    }
                },
                enabled = deltaStr.isNotEmpty() && (deltaStr.toIntOrNull() ?: 0) > 0
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
