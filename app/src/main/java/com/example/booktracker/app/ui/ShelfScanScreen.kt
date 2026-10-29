package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.booktracker.app.camera.ShelfSpineParser
import com.example.booktracker.app.data.NoteImageStore
import com.example.booktracker.app.data.remote.BookMetadata
import com.example.booktracker.app.data.remote.BookSearch
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** One spine read off the photo and what the search made of it. */
private class SpineMatch(
    val query: String,
    val options: List<BookMetadata>,
    val chosen: Int = 0,
    val selected: Boolean,
    val alreadyOwned: Boolean
) {
    val book: BookMetadata get() = options[chosen]
}

/**
 * Photograph a shelf and add every book on it in one go. The spines are read on
 * the phone; each title is then looked up like a normal search (only that text
 * is sent), and you tick the ones to keep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScanScreen(
    library: List<Book>,
    onAdd: (books: List<BookMetadata>, status: BookStatus, owned: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf(Stage.PICK) }
    var progress by remember { mutableStateOf(0 to 0) }
    var message by remember { mutableStateOf<String?>(null) }
    val matches = remember { mutableStateListOf<SpineMatch>() }
    var swapping by remember { mutableStateOf<Int?>(null) }
    var readAlready by remember { mutableStateOf(false) }
    var owned by remember { mutableStateOf(true) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }

    val known = remember(library) {
        library.flatMap { listOfNotNull(it.isbn?.takeIf(String::isNotBlank), it.title.lowercase().trim()) }.toSet()
    }

    fun process(uri: Uri) {
        stage = Stage.READING
        message = null
        matches.clear()
        scope.launch {
            val queries = readSpines(context, uri)?.let(ShelfSpineParser::queries)
            if (queries.isNullOrEmpty()) {
                stage = Stage.PICK
                message = if (queries == null) "Couldn't read that photo. Try again." else
                    "No titles found. Stand back a little so the spines are sharp and well lit."
                return@launch
            }
            stage = Stage.LOOKING_UP
            val seenTitles = mutableSetOf<String>()
            queries.forEachIndexed { i, query ->
                progress = i to queries.size
                val results = try {
                    BookSearch.search(query)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
                val top = results.take(4)
                val first = top.firstOrNull()
                if (first != null && seenTitles.add(first.title.lowercase())) {
                    val inLibrary = first.title.lowercase().trim() in known || (first.isbn.isNotBlank() && first.isbn in known)
                    matches += SpineMatch(query, top, selected = !inLibrary, alreadyOwned = inLibrary)
                }
                // Gentle on the free search services, which throttle bursts.
                delay(250)
            }
            progress = queries.size to queries.size
            stage = Stage.REVIEW
            if (matches.isEmpty()) message = "None of the spines matched a book. Try a closer photo."
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCapture?.let(::File)
        pendingCapture = null
        if (saved && file != null) process(Uri.fromFile(file)) else file?.delete()
    }
    fun launchCamera() {
        val file = NoteImageStore.newCaptureFile(context)
        pendingCapture = file.absolutePath
        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else message = "Camera permission is needed to take the photo."
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::process) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan a bookshelf") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            if (stage == Stage.REVIEW && matches.isNotEmpty()) {
                val count = matches.count { it.selected }
                Surface(tonalElevation = 3.dp) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("I own these", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = owned, onCheckedChange = { owned = it })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Already read them", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = readAlready, onCheckedChange = { readAlready = it })
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            enabled = count > 0,
                            onClick = {
                                onAdd(
                                    matches.filter { it.selected }.map { it.book },
                                    if (readAlready) BookStatus.FINISHED else BookStatus.TO_READ,
                                    owned
                                )
                            },
                            shape = AppShape.M,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) { Text(if (count == 1) "Add 1 book" else "Add $count books", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (stage) {
                Stage.PICK -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    Icon(Icons.Rounded.ViewWeek, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Add a whole shelf at once", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Take a photo with the spines facing you, sharp and well lit. A shelf of 10 to 20 books works best.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = AppShape.M
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take a photo")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = AppShape.M
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose a photo")
                    }
                    message?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                Stage.READING -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Reading the spines…", style = MaterialTheme.typography.titleMedium)
                }
                Stage.LOOKING_UP, Stage.REVIEW -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (stage == Stage.LOOKING_UP) {
                        item(key = "progress") {
                            val (done, total) = progress
                            Column {
                                Text("Finding ${done + 1} of $total…", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { if (total > 0) done.toFloat() / total else 0f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        item(key = "hint") {
                            Text(
                                message ?: "Untick anything that isn't right, or swap it for another match.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    itemsIndexed(matches, key = { _, m -> m.query }) { index, match ->
                        SpineRow(
                            match = match,
                            onToggle = { matches[index] = match.copyWith(selected = !match.selected) },
                            onSwap = if (match.options.size > 1) ({ swapping = index }) else null
                        )
                    }
                    if (stage == Stage.REVIEW) {
                        item(key = "again") {
                            TextButton(onClick = { stage = Stage.PICK; matches.clear(); message = null }) { Text("Scan another photo") }
                        }
                    }
                }
            }
        }
    }

    swapping?.let { index ->
        val match = matches.getOrNull(index)
        if (match == null) swapping = null else {
            ModalBottomSheet(onDismissRequest = { swapping = null }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                    Text("Which book is it?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Read from the spine: “${match.query}”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    match.options.forEachIndexed { option, book ->
                        Surface(
                            onClick = {
                                matches[index] = match.copyWith(chosen = option, selected = true)
                                swapping = null
                            },
                            shape = AppShape.S,
                            color = if (option == match.chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                                BookCover(book.coverUrl, Modifier.size(width = 40.dp, height = 60.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(book.authors.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class Stage { PICK, READING, LOOKING_UP, REVIEW }

private fun SpineMatch.copyWith(chosen: Int = this.chosen, selected: Boolean = this.selected) =
    SpineMatch(query, options, chosen, selected, alreadyOwned)

@Composable
private fun SpineRow(match: SpineMatch, onToggle: () -> Unit, onSwap: (() -> Unit)?) {
    val book = match.book
    Surface(
        onClick = onToggle,
        shape = AppShape.M,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
            Checkbox(checked = match.selected, onCheckedChange = { onToggle() })
            BookCover(book.coverUrl, Modifier.size(width = 44.dp, height = 66.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (book.authors.isNotEmpty()) {
                    Text(book.authors.joinToString(", "), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    if (match.alreadyOwned) "Already in your library" else "Spine: ${match.query}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (match.alreadyOwned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onSwap != null) {
                IconButton(onClick = onSwap) { Icon(Icons.Rounded.SwapHoriz, contentDescription = "Pick another match") }
            }
        }
    }
}

/**
 * Text blocks off the photo. Spines usually stand upright, so their titles run
 * sideways: the photo is read as taken and turned both ways, and every pass adds
 * what it found. Null when the photo can't be opened.
 */
private suspend fun readSpines(context: Context, uri: Uri): List<String>? = withContext(Dispatchers.Default) {
    val stored = NoteImageStore.import(context, uri) ?: return@withContext null
    val file = NoteImageStore.localFile(stored)
    try {
        val bitmap = file?.let { BitmapFactory.decodeFile(it.path) } ?: return@withContext null
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            listOf(0, 90, 270).flatMap { rotation ->
                runCatching {
                    recognizer.process(InputImage.fromBitmap(bitmap, rotation)).await().textBlocks
                        .map { block -> block.text.replace('\n', ' ') }
                }.getOrDefault(emptyList())
            }
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    } finally {
        file?.delete()
    }
}
