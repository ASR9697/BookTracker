package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.TandemEngine
import com.example.booktracker.shared.models.Book

/**
 * Print and audiobook on one bar. The bar is the page progress; the audio
 * position underneath is the same point in the narration, so switching from
 * the paperback to headphones (or back) never loses the place.
 */
@Composable
fun TandemCard(
    book: Book,
    onSetAudiobook: (minutes: Int, speed: Float) -> Unit,
    onLogListening: (minutes: Int) -> Unit,
    onSetAudioPosition: (position: Int) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<TandemDialog?>(null) }
    val tandem = TandemEngine.isTandem(book)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = AppShape.L,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook,contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Tandem reading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (tandem) {
                    IconButton(onClick = { editing = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit audiobook", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            if (!tandem) {
                Text(
                    "Listening to the audiobook too? Add its length and your place follows whichever edition you pick up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { editing = true }) { Text("Add the audiobook") }
            } else {
                val position = TandemEngine.audioPositionFor(book)
                LinearProgressIndicator(
                    progress = { (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) },
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Page ${book.currentPage} of ${book.totalPages}", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${TandemEngine.formatClock(position)} of ${TandemEngine.formatClock(book.audioMinutes)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    "${TandemEngine.formatClock(TandemEngine.listeningMinutesLeft(book))} of listening left at ${TandemEngine.formatSpeed(book.audioSpeed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { dialog = TandemDialog.LISTENED }, modifier = Modifier.weight(1f)) {
                        Text("Log listening")
                    }
                    OutlinedButton(onClick = { dialog = TandemDialog.POSITION }, modifier = Modifier.weight(1f)) {
                        Text("Set position")
                    }
                }
            }
        }
    }

    if (editing) {
        AudiobookSetupDialog(
            book = book,
            onDismiss = { editing = false },
            onSave = { minutes, speed ->
                editing = false
                onSetAudiobook(minutes, speed)
            }
        )
    }
    dialog?.let { mode ->
        ListeningDialog(
            book = book,
            mode = mode,
            onDismiss = { dialog = null },
            onConfirm = { minutes ->
                dialog = null
                if (mode == TandemDialog.LISTENED) onLogListening(minutes) else onSetAudioPosition(minutes)
            }
        )
    }
}

private enum class TandemDialog { LISTENED, POSITION }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudiobookSetupDialog(book: Book, onDismiss: () -> Unit, onSave: (Int, Float) -> Unit) {
    val tandem = TandemEngine.isTandem(book)
    var length by remember {
        mutableStateOf(if (tandem) "${book.audioMinutes / 60}:${"%02d".format(book.audioMinutes % 60)}" else "")
    }
    var speed by remember { mutableStateOf(if (tandem) book.audioSpeed else 1f) }
    val minutes = TandemEngine.parseLength(length)?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audiobook edition") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it.take(10) },
                    label = { Text("Length, e.g. 9:40") },
                    supportingText = { Text("The full running time at normal speed, as your player shows it.") },
                    isError = length.isNotBlank() && minutes == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("You listen at", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TandemEngine.SPEEDS.forEach { option ->
                        TickFilterChip(
                            selected = speed == option,
                            onClick = { speed = option },
                            label = { Text(TandemEngine.formatSpeed(option)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = minutes != null, onClick = { minutes?.let { onSave(it, speed) } }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (tandem) TextButton(onClick = { onSave(0, 1f) }) { Text("Stop tandem") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun ListeningDialog(book: Book, mode: TandemDialog, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var input by remember { mutableStateOf("") }
    val minutes = TandemEngine.parseLength(input)
    val page = minutes?.let {
        if (mode == TandemDialog.LISTENED) TandemEngine.pageAfterListening(book, it) else TandemEngine.pageForAudio(book, it)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == TandemDialog.LISTENED) "How long did you listen?" else "Where is your player?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(10) },
                    label = { Text(if (mode == TandemDialog.LISTENED) "Minutes, e.g. 25" else "Elapsed, e.g. 3:12") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (mode == TandemDialog.LISTENED) KeyboardType.Number else KeyboardType.Text
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    when {
                        page == null -> if (mode == TandemDialog.LISTENED) {
                            "Real minutes; your ${TandemEngine.formatSpeed(book.audioSpeed)} speed is applied for you."
                        } else {
                            "The elapsed time your audiobook player shows."
                        }
                        else -> "That takes you to page $page of ${book.totalPages}."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = minutes != null && minutes >= 0, onClick = { minutes?.let(onConfirm) }) {
                Text(if (mode == TandemDialog.LISTENED) "Log" else "Move there")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
