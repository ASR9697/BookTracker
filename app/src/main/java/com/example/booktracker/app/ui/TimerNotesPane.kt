package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.journal.JournalingEngine
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType

/**
 * Beside the timer on a tablet: jot a line at the page you're on without
 * leaving the clock, and see this book's notes so far. The full note sheet
 * (photos, scanning, dictation) is one tap away.
 */
@Composable
fun TimerNotesPane(
    notes: List<MarginNote>,
    currentPage: Int,
    pageLabel: String,
    onQuickNote: (page: Int, text: String, type: NoteType) -> Unit,
    onOpenNoteSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    var draft by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(NoteType.PERSONAL_THOUGHT) }
    val recent = remember(notes) { notes.sortedByDescending { it.timestamp } }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenNoteSheet) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Note with photos or scanning")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("A thought or a quote, at $pageLabel $currentPage") },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TickFilterChip(
                    selected = type == NoteType.PERSONAL_THOUGHT,
                    onClick = { type = NoteType.PERSONAL_THOUGHT },
                    label = { Text("Thought") }
                )
                TickFilterChip(
                    selected = type == NoteType.BOOK_CONTENT,
                    onClick = { type = NoteType.BOOK_CONTENT },
                    label = { Text("Quote") }
                )
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        haptics.confirm()
                        onQuickNote(currentPage, draft.trim(), type)
                        draft = ""
                    }
                ) { Text("Save") }
            }
            Spacer(Modifier.height(16.dp))
            if (recent.isEmpty()) {
                Text(
                    "Notes you take while reading show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recent, key = { it.id }) { note ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "$pageLabel ${note.pageNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                JournalingEngine.render(note.content),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
