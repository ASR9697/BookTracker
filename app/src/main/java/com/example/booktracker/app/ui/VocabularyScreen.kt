package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape
import com.example.booktracker.app.ui.animations.staggeredItems

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.booktracker.app.camera.TextRecognitionAnalyzer
import com.example.booktracker.app.data.remote.DictionaryClient
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.VocabWord
import kotlinx.coroutines.launch

/**
 * Words met while reading: what they mean, which book and page they came from,
 * and the sentence they were in. They join the daily review so they stick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    words: List<VocabWord>,
    books: List<Book>,
    dueCount: Int,
    onSave: (VocabWord) -> Unit,
    onDelete: (VocabWord) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<VocabWord?>(null) }
    var query by remember { mutableStateOf("") }
    val listDetail = LocalWidthTier.current.atLeastExpanded
    val booksById = remember(books) { books.associateBy { it.id } }
    val shown = remember(words, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) words.sortedBy { it.word.lowercase() }
        else words.filter { it.word.lowercase().contains(q) || it.definition.lowercase().contains(q) }.sortedBy { it.word.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Words") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = VocabWord() },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add word") }
            )
        }
    ) { padding ->
        if (words.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Rounded.Spellcheck,
                    title = "Your words",
                    message = "Met a word you didn't know? Save it with its meaning and the sentence it was in. It comes back in your daily review until it sticks."
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            val side = if (listDetail) 0.dp else windowCentredPadding()
            val entrance = com.example.booktracker.app.ui.animations.rememberEntranceState()
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp + side, end = 16.dp + side, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(if (listDetail) 0.45f else 1f).fillMaxHeight()
            ) {
                item(key = "summary") {
                    Surface(shape = AppShape.L, color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${words.size} ${if (words.size == 1) "word" else "words"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    if (dueCount > 0) "$dueCount in today's review" else "Nothing due today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (dueCount > 0) FilledTonalButton(onClick = onReview) { Text("Review") }
                        }
                    }
                }
                if (words.size > 6) {
                    item(key = "search") {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search words and meanings") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = AppShape.M,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                staggeredItems(entrance, shown, key = { it.id }, startIndex = 1) { word ->
                    val book = word.bookId?.let { booksById[it] }
                    Surface(
                        onClick = { editing = word },
                        shape = AppShape.M,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(word.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (word.definition.isNotBlank()) {
                                Text(word.definition, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (book != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    listOfNotNull(book.title, word.pageNumber.takeIf { it > 0 }?.let { "p. $it" }).joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            if (listDetail) {
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Box(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                    val word = editing
                    if (word == null) {
                        EmptyState(
                            icon = Icons.Rounded.Spellcheck,
                            title = "Pick a word",
                            message = "Its meaning, the sentence and the book it came from open here to edit."
                        )
                    } else {
                        // Keyed so picking another word starts its editor fresh.
                        key(word.id.ifBlank { "new" }) {
                            WordEditorSheet(
                                initial = word,
                                books = books,
                                onDismiss = { editing = null },
                                onSave = { onSave(it); editing = null },
                                onDelete = if (word.id.isNotBlank()) ({ onDelete(word); editing = null }) else null,
                                asPane = true
                            )
                        }
                    }
                }
            }
            }
        }
    }

    // On a tablet the editor lives in the right pane once there is a list to sit beside.
    val inPane = listDetail && words.isNotEmpty()
    editing?.takeIf { !inPane }?.let { word ->
        WordEditorSheet(
            initial = word,
            books = books,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = if (word.id.isNotBlank()) ({ onDelete(word); editing = null }) else null
        )
    }
}

/** Add or edit one word, with an optional dictionary look-up and a camera to pick it off the page. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordEditorSheet(
    initial: VocabWord,
    books: List<Book>,
    onDismiss: () -> Unit,
    onSave: (VocabWord) -> Unit,
    onDelete: (() -> Unit)?,
    /** Draw inline (the right pane on a tablet) instead of as a bottom sheet. */
    asPane: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val reading = remember(books) {
        books.filter { it.status == BookStatus.READING.name }.sortedByDescending { it.lastUpdated }.take(6)
    }
    val defaultBook = remember(initial, reading) { initial.bookId ?: if (initial.id.isBlank()) reading.firstOrNull()?.id else null }
    var word by remember { mutableStateOf(initial.word) }
    var definition by remember { mutableStateOf(initial.definition) }
    var context by remember { mutableStateOf(initial.context) }
    var bookId by remember { mutableStateOf(defaultBook) }
    var page by remember {
        mutableStateOf(
            initial.pageNumber.takeIf { it > 0 }?.toString()
                ?: books.find { it.id == defaultBook }?.currentPage?.takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var lookingUp by remember { mutableStateOf(false) }
    var lookupMessage by remember { mutableStateOf<String?>(null) }
    var senses by remember { mutableStateOf<List<DictionaryClient.Definition>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    val chipBooks = remember(reading, bookId, books) {
        (reading + listOfNotNull(books.find { it.id == bookId })).distinctBy { it.id }
    }

    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = if (asPane) 16.dp else 0.dp, bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (initial.id.isBlank()) "New word" else "Edit word", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = word,
                onValueChange = { word = it; senses = emptyList(); lookupMessage = null },
                label = { Text("Word") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                trailingIcon = {
                    IconButton(onClick = { scanning = true }) { Icon(Icons.Rounded.DocumentScanner, contentDescription = "Pick it off the page") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = word.isNotBlank() && !lookingUp,
                    onClick = {
                        lookingUp = true
                        lookupMessage = null
                        scope.launch {
                            DictionaryClient.lookUp(word).fold(
                                onSuccess = { found ->
                                    senses = found
                                    lookupMessage = if (found.isEmpty()) "Not in the dictionary. Type your own meaning." else null
                                    if (found.size == 1 && definition.isBlank()) definition = found.first().meaning
                                },
                                onFailure = { lookupMessage = "Couldn't reach the dictionary. Check your connection." }
                            )
                            lookingUp = false
                        }
                    }
                ) {
                    if (lookingUp) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Look up meaning")
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Sends only the word",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            lookupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (senses.isNotEmpty()) {
                Text("Tap the meaning that fits", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                senses.forEach { sense ->
                    val chosen = definition == sense.meaning
                    Surface(
                        onClick = { haptics.tick(); definition = sense.meaning },
                        shape = AppShape.S,
                        color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(sense.partOfSpeech, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(sense.meaning, style = MaterialTheme.typography.bodyMedium)
                            sense.example?.let { Text("“$it”", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic) }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = definition,
                onValueChange = { definition = it },
                label = { Text("Meaning") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = context,
                onValueChange = { context = it },
                label = { Text("The sentence it was in (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            if (chipBooks.isNotEmpty()) {
                Text("From", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TickFilterChip(selected = bookId == null, onClick = { bookId = null }, label = { Text("No book") })
                    chipBooks.forEach { book ->
                        TickFilterChip(
                            selected = bookId == book.id,
                            onClick = {
                                bookId = book.id
                                if (page.isBlank() && book.currentPage > 0) page = book.currentPage.toString()
                            },
                            label = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp)) }
                        )
                    }
                }
            }
            if (bookId != null) {
                OutlinedTextField(
                    value = page,
                    onValueChange = { page = it.filter(Char::isDigit).take(5) },
                    label = { Text("Page") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(140.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = word.isNotBlank(),
                    onClick = {
                        haptics.confirm()
                        onSave(
                            initial.copy(
                                word = word.trim(),
                                definition = definition.trim(),
                                context = context.trim(),
                                bookId = bookId,
                                pageNumber = if (bookId != null) page.toIntOrNull() ?: 0 else 0
                            )
                        )
                    }
                ) { Text("Save") }
            }
        }
    }

    if (asPane) {
        Box(modifier = Modifier.fillMaxSize()) { body() }
    } else {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            body()
        }
    }

    if (scanning) {
        WordScanDialog(
            onDismiss = { scanning = false },
            onPicked = { picked, sentence ->
                scanning = false
                word = picked
                senses = emptyList()
                lookupMessage = null
                if (context.isBlank()) context = sentence
            }
        )
    }
}

/**
 * Hold the camera over the page, freeze it, then tap the word you meant. The line
 * it sat in comes along as the example sentence. Recognition stays on the phone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordScanDialog(onDismiss: () -> Unit, onPicked: (word: String, sentence: String) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }
    var liveText by remember { mutableStateOf("") }
    var frozen by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                val text = frozen
                if (text == null) {
                    if (hasPermission) {
                        CameraPreview(
                            analyzer = remember { TextRecognitionAnalyzer { liveText = it } },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("The camera is needed to read the page.", color = Color.White, textAlign = TextAlign.Center)
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                        }
                    }
                    Button(
                        enabled = liveText.isNotBlank(),
                        onClick = { frozen = liveText },
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp).height(56.dp)
                    ) { Text("Freeze the page") }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .statusBarsPadding()
                            .padding(top = 56.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Tap the word you meant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        text.lines().filter { it.isNotBlank() }.forEach { line ->
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                line.split(Regex("\\s+")).forEach { token ->
                                    val clean = token.trim { !it.isLetter() && it != '\'' && it != '-' }
                                    if (clean.length >= 2) {
                                        AssistChip(onClick = { onPicked(clean.lowercase(), line.trim()) }, label = { Text(token) })
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { frozen = null }) { Text("Scan again") }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = if (text == null) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
