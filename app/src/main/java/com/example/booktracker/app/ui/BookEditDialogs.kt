package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.booktracker.app.camera.rememberPageScanner
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.Publication
import com.example.booktracker.shared.models.SeriesInfo
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

private val dateLabelFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * Edits a book's details after it has been added — fixing a wrong page count or a
 * missing cover is otherwise impossible. Progress is clamped to the new total by
 * the caller.
 */
@Composable
fun EditBookDialog(
    book: Book,
    onDismiss: () -> Unit,
    onSave: (Book) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(book.title) }
    var authors by remember {
        mutableStateOf(book.creators.filter { it.role == CreatorRole.Author }.joinToString(", ") { it.name })
    }
    var totalPages by remember { mutableStateOf(book.totalPages.takeIf { it > 0 }?.toString().orEmpty()) }
    var coverUrl by remember { mutableStateOf(book.coverUrl) }
    var publisher by remember { mutableStateOf(book.publication?.publisher.orEmpty()) }
    var published by remember { mutableStateOf(book.publication?.date.orEmpty()) }
    var isbn by remember { mutableStateOf(book.isbn.orEmpty()) }
    var genres by remember { mutableStateOf(book.genres.joinToString(", ")) }
    var description by remember { mutableStateOf(book.description) }
    var seriesName by remember { mutableStateOf(book.seriesInfo?.seriesName.orEmpty()) }
    var seriesPosition by remember { mutableStateOf(book.seriesInfo?.position?.takeIf { it > 0 }?.toString().orEmpty()) }
    var finishedAt by remember { mutableStateOf(book.finishedAt ?: book.completedAt.takeIf { book.isEnded() }) }
    var showFinishedPicker by remember { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val file = File(dir, "cover_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out -> it.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            coverUrl = "file://${file.absolutePath}"
        }
    }

    // A flattened, de-shadowed photo of the real cover, copied out of the scanner's cache.
    val scanCover = rememberPageScanner(
        pageLimit = 1,
        onScanned = { pages ->
            pages.firstOrNull()?.let { uri ->
                val dir = File(context.filesDir, "covers").apply { mkdirs() }
                val file = File(dir, "cover_${UUID.randomUUID()}.jpg")
                val copied = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { input.copyTo(it) }
                    }
                }.getOrNull() != null
                if (copied) coverUrl = "file://${file.absolutePath}"
            }
        },
        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )

    val pagesInt = totalPages.toIntOrNull()
    val titleValid = title.isNotBlank()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = AppShape.L,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Edit details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    isError = !titleValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = authors,
                    onValueChange = { authors = it },
                    label = { Text("Authors (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalPages,
                        onValueChange = { totalPages = it.filter(Char::isDigit).take(5) },
                        label = { Text("Total pages") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = isbn,
                        onValueChange = { isbn = it.filter { c -> c.isDigit() || c == 'X' || c == 'x' }.take(13) },
                        label = { Text("ISBN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Cover image URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { takePicture.launch() }) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = "Take a cover photo")
                    }
                    IconButton(onClick = scanCover) {
                        Icon(Icons.Rounded.Crop, contentDescription = "Scan the cover flat")
                    }
                }
                if (coverUrl.isNotBlank()) {
                    BookCover(coverUrl, Modifier.size(width = 64.dp, height = 96.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = publisher,
                        onValueChange = { publisher = it },
                        label = { Text("Publisher") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = published,
                        onValueChange = { published = it.take(10) },
                        label = { Text("Published") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seriesName,
                        onValueChange = { seriesName = it },
                        label = { Text("Series") },
                        singleLine = true,
                        modifier = Modifier.weight(1.6f)
                    )
                    OutlinedTextField(
                        value = seriesPosition,
                        onValueChange = { seriesPosition = it.filter(Char::isDigit).take(3) },
                        label = { Text("No.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = genres,
                    onValueChange = { genres = it },
                    label = { Text("Genres / tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                if (book.isEnded()) {
                    Surface(
                        shape = AppShape.S,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        onClick = { showFinishedPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (book.status == BookStatus.DNF.name) "Stopped on" else "Finished on",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                finishedAt?.let {
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateLabelFormat)
                                } ?: "Set date",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = titleValid,
                        onClick = {
                            val authorNames = authors.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val otherCreators = book.creators.filter { it.role != CreatorRole.Author }
                            onSave(
                                book.copy(
                                    title = title.trim(),
                                    creators = authorNames.map { Creator(CreatorRole.Author, it) } + otherCreators,
                                    totalPages = pagesInt ?: 0,
                                    coverUrl = coverUrl.trim(),
                                    isbn = isbn.trim().takeIf { it.isNotBlank() },
                                    publication = Publication(publisher.trim(), published.trim()),
                                    classification = book.classification.copy(
                                        tags = genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
                                    ),
                                    description = description.trim(),
                                    seriesInfo = seriesName.trim().takeIf { it.isNotBlank() }?.let {
                                        SeriesInfo(true, it, seriesPosition.toIntOrNull() ?: 0)
                                    },
                                    finishedAt = if (book.isEnded()) finishedAt else book.finishedAt
                                )
                            )
                        }
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showFinishedPicker) {
        PastDatePickerDialog(
            initialMillis = finishedAt ?: System.currentTimeMillis(),
            onDismiss = { showFinishedPicker = false },
            onConfirm = {
                finishedAt = it
                showFinishedPicker = false
            }
        )
    }
}

/**
 * Logs reading done away from the timer: a date, the pages covered and roughly
 * how long it took. The bookmark only moves forward, so back-filling an old
 * session never rewinds current progress.
 */
@Composable
fun LogSessionDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (startPage: Int, endPage: Int, minutes: Int, endTime: Long) -> Unit
) {
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startPage by remember { mutableStateOf(book.currentPage.toString()) }
    var endPage by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val start = startPage.toIntOrNull()
    val end = endPage.toIntOrNull()
    val limit = if (book.totalPages > 0) book.totalPages else Int.MAX_VALUE
    val rangeError = start != null && end != null && end < start
    val limitError = (end != null && end > limit) || (start != null && start > limit)
    val valid = start != null && end != null && !rangeError && !limitError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a reading session") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = AppShape.S,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate().format(dateLabelFormat),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startPage,
                        onValueChange = { startPage = it.filter(Char::isDigit).take(5) },
                        label = { Text("From page") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endPage,
                        onValueChange = { endPage = it.filter(Char::isDigit).take(5) },
                        label = { Text("To page") },
                        singleLine = true,
                        isError = rangeError || limitError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rangeError) {
                    Text("“To page” can't be before “From page”", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (limitError) {
                    Text("This book has ${book.totalPages} pages", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes read (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (start != null && end != null) {
                        onConfirm(start, end, minutes.toIntOrNull() ?: 0, date)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        PastDatePickerDialog(
            initialMillis = date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                // Keep "today" at the current time; earlier days log at noon.
                val picked = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                date = if (picked == LocalDate.now()) System.currentTimeMillis() else it
                showDatePicker = false
            }
        )
    }
}

/** Date picker limited to today and earlier; returns local noon of the picked day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val initialUtc = Instant.ofEpochMilli(initialMillis).atZone(zone).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val todayUtcEnd = LocalDate.now(zone).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialUtc,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis < todayUtcEnd
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { utc ->
                    val day = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(day.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli())
                } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

private fun Book.isEnded(): Boolean =
    status == BookStatus.FINISHED.name || status == BookStatus.DNF.name
