package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.language.PassageTranslator
import com.example.booktracker.app.language.VocabCandidates
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.shared.models.VocabWord
import kotlinx.coroutines.launch

/**
 * Translate a passage on the phone, then keep any word of the original as a
 * vocabulary card: tapping a word translates it alone and saves it with the
 * sentence it came from, so it turns up in the daily review.
 *
 * [onUseTranslation] is offered when the passage is still being edited (the
 * OCR confirm sheet): it hands back the translation, or the original followed
 * by it when the reader keeps both.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TranslateSheet(
    passage: String,
    bookId: String?,
    page: Int,
    onSaveWord: ((VocabWord) -> Unit)?,
    onDismiss: () -> Unit,
    onUseTranslation: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var source by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf(PassageTranslator.deviceLanguage()) }
    var translation by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var identified by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(setOf<String>()) }
    var savingWord by remember { mutableStateOf<String?>(null) }
    val words = remember(passage) { VocabCandidates.words(passage) }

    LaunchedEffect(passage) {
        source = PassageTranslator.identify(passage)
        identified = true
    }
    LaunchedEffect(source, target, identified) {
        val from = source ?: return@LaunchedEffect
        translation = null
        error = null
        error = runCatching { translation = PassageTranslator.translate(passage, from, target) }
            .exceptionOrNull()
            ?.let { "Couldn't translate. The first time needs a connection to download the language." }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Translate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguagePicker(
                    label = source?.let(PassageTranslator::displayName) ?: if (identified) "Pick language" else "Detecting…",
                    onPick = { source = it }
                )
                Text("→", style = MaterialTheme.typography.titleMedium)
                LanguagePicker(label = PassageTranslator.displayName(target), onPick = { target = it })
            }

            Text(passage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when {
                identified && source == null -> Text(
                    "Couldn't tell which language this is. Pick it above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error != null -> Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                translation == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Translating on your phone…", style = MaterialTheme.typography.bodySmall)
                }
                else -> Text(translation.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            }

            val done = translation
            if (onUseTranslation != null && done != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onUseTranslation("${passage.trim()}\n\n$done") }, modifier = Modifier.weight(1f)) {
                        Text("Keep both")
                    }
                    Button(onClick = { onUseTranslation(done) }, modifier = Modifier.weight(1f)) {
                        Text("Use translation")
                    }
                }
            }

            val from = source
            if (onSaveWord != null && from != null && from != target && words.isNotEmpty()) {
                Text("Tap a word to add it to your vocabulary", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    words.forEach { word ->
                        val isSaved = word in saved
                        AssistChip(
                            enabled = !isSaved && savingWord == null,
                            onClick = {
                                savingWord = word
                                scope.launch {
                                    val meaning = runCatching { PassageTranslator.translate(word, from, target) }.getOrNull()
                                    savingWord = null
                                    if (meaning == null) {
                                        error = "Couldn't translate \"$word\"."
                                        return@launch
                                    }
                                    onSaveWord(
                                        VocabWord(
                                            word = word,
                                            definition = "$meaning (${PassageTranslator.displayName(from)})",
                                            bookId = bookId,
                                            pageNumber = page,
                                            context = VocabCandidates.sentenceWith(passage, word)
                                        )
                                    )
                                    saved = saved + word
                                    haptics.confirm()
                                }
                            },
                            label = { Text(word) },
                            leadingIcon = if (isSaved) {
                                { Icon(Icons.Filled.Check, contentDescription = "Saved", modifier = Modifier.size(16.dp)) }
                            } else if (savingWord == word) {
                                { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePicker(label: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(label) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PassageTranslator.languages.forEach { code ->
                DropdownMenuItem(
                    text = { Text(PassageTranslator.displayName(code)) },
                    onClick = {
                        open = false
                        onPick(code)
                    }
                )
            }
        }
    }
}
