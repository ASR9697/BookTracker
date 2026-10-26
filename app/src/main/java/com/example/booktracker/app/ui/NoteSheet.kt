package com.example.booktracker.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.data.NoteDraft
import com.example.booktracker.app.data.NoteDraftStore
import com.example.booktracker.shared.models.NoteType
import kotlinx.coroutines.delay

/**
 * The one sheet for writing a note, shared by the book page, the reading timer,
 * Home and the Notes tab.
 *
 * Text goes first, with a small formatting bar and dictation; page, photos and
 * "Scan text" sit below. Unsaved work is guarded: closing asks first, rotation
 * keeps it, and for a new note ([draftKey] set) the text is kept across app
 * restarts. Photos are not kept in drafts.
 *
 * @param onConfirm saves and is expected to close the sheet.
 * @param onConfirmAndAddAnother when set (new notes only) shows "Save & add another":
 *   it saves, then the sheet clears itself and stays open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    suggestedPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onConfirm: (page: Int, text: String, type: NoteType, images: List<String>) -> Unit,
    initialText: String = "",
    initialType: NoteType = NoteType.BOOK_CONTENT,
    initialImages: List<String> = emptyList(),
    isEditing: Boolean = false,
    pageLabel: String = "Page",
    draftKey: String? = null,
    onConfirmAndAddAnother: ((page: Int, text: String, type: NoteType, images: List<String>) -> Unit)? = null
) {
    val context = LocalContext.current
    val useDraft = draftKey != null && !isEditing && initialText.isBlank()
    val draft = remember { if (useDraft) NoteDraftStore.load(context, draftKey!!) else null }

    // Saveable: rotating, or the camera app recreating the activity, must not lose a half-written note.
    var pageText by rememberSaveable { mutableStateOf(draft?.page ?: if (suggestedPage > 0) "$suggestedPage" else "") }
    var noteValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(draft?.text ?: initialText))
    }
    var noteType by rememberSaveable { mutableStateOf(draft?.type ?: initialType) }
    var images by rememberSaveable(stateSaver = listSaver<List<String>, String>(save = { it }, restore = { it })) {
        mutableStateOf(initialImages)
    }
    var showDiscard by remember { mutableStateOf(false) }
    var voiceMissing by remember { mutableStateOf(false) }
    var saveCount by remember { mutableStateOf(0) }

    val text = noteValue.text
    val changed = text != initialText || images != initialImages || noteType != initialType
    val hasChanges by rememberUpdatedState(changed)
    // A swipe-down or scrim tap with unsaved work asks first instead of throwing it away.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && hasChanges) {
                showDiscard = true
                false
            } else true
        }
    )
    fun requestClose() = if (changed) showDiscard = true else onDismiss()
    fun clearDraft() { if (useDraft) NoteDraftStore.clear(context, draftKey!!) }

    val textFocus = remember { FocusRequester() }
    LaunchedEffect(saveCount) { if (!isEditing) runCatching { textFocus.requestFocus() } }

    if (useDraft) {
        LaunchedEffect(text, pageText, noteType) {
            delay(400)
            NoteDraftStore.save(context, draftKey!!, NoteDraft(pageText, text, noteType))
        }
    }

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) {
            noteValue = NoteFormatting.insertAtCursor(noteValue, spoken)
        }
    }
    fun dictate() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your note")
        try {
            voiceMissing = false
            speech.launch(intent)
        } catch (_: ActivityNotFoundException) {
            voiceMissing = true
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(if (isEditing) "Discard your changes?" else "Discard this note?") },
            text = { Text(if (isEditing) "Your edits to this note won't be saved." else "What you've written or added won't be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    clearDraft()
                    onDismiss()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { requestClose() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // Scrolls, so Save stays reachable with the keyboard up and photos added.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (isEditing) "Edit note" else "Add a note",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (draft != null && text == draft.text) {
                Text("Draft restored", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                NoteType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = NoteType.entries.size),
                        onClick = { noteType = type },
                        selected = noteType == type,
                        icon = {}
                    ) {
                        // Short labels: the full names don't fit three segments on a phone.
                        Text(type.shortLabel(), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }

            OutlinedTextField(
                value = noteValue,
                onValueChange = { noteValue = it },
                placeholder = {
                    Text(
                        when (noteType) {
                            NoteType.BOOK_CONTENT -> "Type or paste the passage you want to keep"
                            NoteType.PERSONAL_THOUGHT -> "What did this make you think or feel?"
                            NoteType.RANDOM -> "Write anything"
                        }
                    )
                },
                minLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(textFocus),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { noteValue = NoteFormatting.toggleWrap(noteValue, NoteFormatting.BOLD) }) {
                    Icon(Icons.Filled.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = { noteValue = NoteFormatting.toggleWrap(noteValue, NoteFormatting.ITALIC) }) {
                    Icon(Icons.Filled.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = { noteValue = NoteFormatting.toggleBullets(noteValue) }) {
                    Icon(Icons.Filled.FormatListBulleted, contentDescription = "Bullet list")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = ::dictate) {
                    Icon(Icons.Filled.Mic, contentDescription = "Dictate")
                }
            }
            if (voiceMissing) {
                Text(
                    "Voice typing isn't available on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            val pageInt = pageText.toIntOrNull()
            val isPageError = pageInt != null && (pageInt < 0 || (totalPages > 0 && pageInt > totalPages))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it.filter(Char::isDigit).take(5) },
                    label = { Text(pageLabel) },
                    singleLine = true,
                    isError = isPageError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(112.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    )
                )
                if (totalPages > 0) {
                    Text(
                        "of $totalPages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isPageError) {
                Text(
                    if (pageInt < 0) "$pageLabel cannot be negative" else "Cannot exceed total pages ($totalPages)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            NotePhotoEditor(
                images = images,
                onImagesChange = { images = it },
                onExtractedText = { scanned -> noteValue = NoteFormatting.appendParagraph(noteValue, scanned) }
            )

            // A photo of the page is a note on its own; text is optional then.
            val canSave = (text.isNotBlank() || images.isNotEmpty()) && !isPageError
            Button(
                enabled = canSave,
                onClick = {
                    clearDraft()
                    onConfirm(pageInt ?: 0, text.trim(), noteType, images)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (isEditing) "Save changes" else "Save note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (!isEditing && onConfirmAndAddAnother != null) {
                OutlinedButton(
                    enabled = canSave,
                    onClick = {
                        clearDraft()
                        onConfirmAndAddAnother(pageInt ?: 0, text.trim(), noteType, images)
                        // Same chapter, same page and type; only the content starts over.
                        noteValue = TextFieldValue("")
                        images = emptyList()
                        saveCount++
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Save & add another")
                }
            }
        }
    }
}
