package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.booktracker.app.camera.OcrTextCleaner
import com.example.booktracker.app.camera.rememberPageScanner
import com.example.booktracker.app.data.NoteImageStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

/** Photos a single note can carry; enough for a spread and a close-up. */
const val MAX_NOTE_PHOTOS = 4

private val PhotoShape = AppShape.S

/**
 * Photos on a note card: one photo fills the width, several share a row.
 * Tapping a photo opens it full screen.
 */
@Composable
fun NotePhotos(images: List<String>, modifier: Modifier = Modifier) {
    if (images.isEmpty()) return
    var viewing by remember { mutableStateOf<String?>(null) }
    if (images.size == 1) {
        AsyncImage(
            model = images.first(),
            contentDescription = "Note photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(PhotoShape)
                .clickable { viewing = images.first() }
        )
    } else {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            images.forEachIndexed { index, uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Note photo ${index + 1} of ${images.size}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(3f / 4f)
                        .clip(PhotoShape)
                        .clickable { viewing = uri }
                )
            }
        }
    }
    viewing?.let { PhotoViewer(it) { viewing = null } }
}

@Composable
private fun PhotoViewer(uri: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Note photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

/**
 * Photo row for the add/edit note sheet: take or pick photos, remove them, or
 * pull a photo's text into the note with on-device OCR.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotePhotoEditor(
    images: List<String>,
    onImagesChange: (List<String>) -> Unit,
    onExtractedText: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    // Path of the file the camera is writing; saveable because the camera app can recreate this activity.
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    // Saveable like pendingCapture: the camera app can recreate this activity mid-capture.
    var scanMode by rememberSaveable { mutableStateOf(false) }
    val room = MAX_NOTE_PHOTOS - images.size

    var choosing by remember { mutableStateOf<List<String>?>(null) }
    var highlighting by remember { mutableStateOf<String?>(null) }

    /** One block goes straight in; several let the reader pick, since a page also has headers and page numbers. */
    fun deliver(blocks: List<String>?, emptyMessage: String) {
        when {
            blocks.isNullOrEmpty() -> message = emptyMessage
            blocks.size == 1 -> {
                message = null
                onExtractedText(blocks.first())
            }
            else -> {
                message = null
                choosing = blocks
            }
        }
    }

    /** Photo -> text for the note, then the photo is thrown away. */
    fun scan(source: Uri) {
        busy = true
        scope.launch {
            val stored = NoteImageStore.import(context, source)
            val blocks = stored?.let { extractBlocks(context, it) }
            stored?.let { NoteImageStore.localFile(it)?.delete() }
            busy = false
            if (stored == null) message = "Couldn't read that photo. Try taking it again."
            else deliver(blocks, "No text found. Try again with the page flat and well lit.")
        }
    }

    fun add(sources: List<Uri>) {
        if (sources.isEmpty()) return
        busy = true
        scope.launch {
            val stored = sources.take(room).mapNotNull { NoteImageStore.import(context, it) }
            busy = false
            message = if (stored.size < sources.take(room).size) "Some photos couldn't be read." else null
            onImagesChange(images + stored)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCapture?.let(::File)
        val scanning = scanMode
        pendingCapture = null
        scanMode = false
        // A file:// source lets the store recognise and delete its own capture file.
        when {
            saved && file != null && scanning -> scan(Uri.fromFile(file))
            saved && file != null -> add(listOf(Uri.fromFile(file)))
            else -> file?.delete()
        }
    }
    fun launchCamera() {
        val file = NoteImageStore.newCaptureFile(context)
        pendingCapture = file.absolutePath
        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }

    // The manifest declares CAMERA for the scanner, which makes the system camera
    // intent require the grant too; launching without it throws.
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else message = "Camera permission is needed to take a photo."
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_NOTE_PHOTOS)
    ) { picked -> add(picked) }

    val scanPage = rememberPageScanner(
        pageLimit = room,
        onScanned = { pages -> add(pages) },
        onError = { message = it }
    )

    // In split-screen, photos can be dragged in straight from Photos or Files.
    Column(modifier = Modifier.imageDropTarget { dropped -> if (room > 0 && !busy) add(dropped) }) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    scanMode = true
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
                }
            ) {
                Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan text")
            }
            OutlinedButton(
                enabled = room > 0 && !busy,
                onClick = {
                    scanMode = false
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
                }
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Camera")
            }
            OutlinedButton(
                enabled = room > 0 && !busy,
                onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gallery")
            }
            OutlinedButton(enabled = room > 0 && !busy, onClick = scanPage) {
                Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clean page")
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }

        if (images.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(images, key = { _, uri -> uri }) { index, uri ->
                    Box(modifier = Modifier.size(width = 84.dp, height = 112.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Photo ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(PhotoShape)
                        )
                        PhotoAction(
                            icon = Icons.Rounded.Close,
                            label = "Remove photo ${index + 1}",
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) { onImagesChange(images - uri) }
                        PhotoAction(
                            icon = Icons.Rounded.DocumentScanner,
                            label = "Extract text from photo ${index + 1}",
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            busy = true
                            scope.launch {
                                val blocks = extractBlocks(context, uri)
                                busy = false
                                deliver(blocks, "No text found in that photo.")
                            }
                        }
                        PhotoAction(
                            icon = Icons.Rounded.Brush,
                            label = "Highlight photo ${index + 1}",
                            modifier = Modifier.align(Alignment.BottomStart)
                        ) { highlighting = uri }
                        if (index > 0) {
                            PhotoAction(
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                label = "Move photo ${index + 1} earlier",
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                onImagesChange(images.toMutableList().apply { add(index - 1, removeAt(index)) })
                            }
                        }
                    }
                }
            }
        }

        message?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    choosing?.let { blocks ->
        ChooseTextDialog(
            blocks = blocks,
            onDismiss = { choosing = null },
            onConfirm = { picked ->
                choosing = null
                onExtractedText(picked.joinToString("\n\n"))
            }
        )
    }

    highlighting?.let { uri ->
        PhotoHighlightEditor(
            uri = uri,
            onDismiss = { highlighting = null },
            onSaved = { newUri ->
                highlighting = null
                onImagesChange(images.map { if (it == uri) newUri else it })
            }
        )
    }
}

/** Recognised paragraphs with checkboxes; all start selected, so a clean page is one tap. */
@Composable
private fun ChooseTextDialog(blocks: List<String>, onDismiss: () -> Unit, onConfirm: (List<String>) -> Unit) {
    var selected by remember { mutableStateOf(blocks.indices.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose the text to add") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = { selected = if (selected.size == blocks.size) emptySet() else blocks.indices.toSet() }) {
                    Text(if (selected.size == blocks.size) "Clear all" else "Select all")
                }
                blocks.forEachIndexed { index, block ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = if (index in selected) selected - index else selected + index }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = index in selected, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(block, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = { onConfirm(blocks.filterIndexed { i, _ -> i in selected }) }
            ) { Text("Add ${selected.size}") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PhotoAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** On-device OCR of a stored photo as one string per paragraph; null when recognition fails. */
private suspend fun extractBlocks(context: Context, uri: String): List<String>? = runCatching {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        recognizer.process(InputImage.fromFilePath(context, Uri.parse(uri))).await().textBlocks
            .map { OcrTextCleaner.clean(it.text) }
            .filter { it.isNotBlank() }
    } finally {
        recognizer.close()
    }
}.getOrNull()
