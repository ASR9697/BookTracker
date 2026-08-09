package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.data.remote.BookMetadata
import com.example.booktracker.app.data.remote.BookSearch
import com.example.booktracker.app.data.remote.BookSearchException
import com.example.booktracker.app.ui.animations.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign

import com.example.booktracker.shared.models.BookStatus
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.compose.foundation.lazy.itemsIndexed
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    onDismiss: () -> Unit,
    onAddBook: (BookMetadata, BookStatus, com.example.booktracker.shared.models.BookFormat) -> Unit,
    onScanBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BookMetadata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableStateOf(0) }
    
    var previewBook by remember { mutableStateOf<BookMetadata?>(null) }
    var showManualEntry by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(query, retryTick) {
        if (query.isBlank()) {
            results = emptyList()
            isLoading = false
            error = null
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        delay(500) // Debounce
        try {
            results = BookSearch.search(query)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // Rethrow cancellation
        } catch (e: BookSearchException) {
            error = when (e.kind) {
                BookSearchException.Kind.RATE_LIMITED ->
                    "Too many searches right now. Tap Retry in a moment."
                BookSearchException.Kind.NETWORK ->
                    "No internet connection. Check your network and retry."
                else -> "Search failed. Please try again."
            }
            results = emptyList()
        } catch (e: Exception) {
            error = "Search failed. Please try again."
            results = emptyList()
        } finally {
            // Only set isLoading to false if this coroutine wasn't cancelled
            // A cancelled coroutine means a new query is starting, which will handle its own loading state
            if (kotlinx.coroutines.currentCoroutineContext().isActive) {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Book") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            
            // Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by title, author, or ISBN") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onScanBarcode,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan ISBN", style = MaterialTheme.typography.titleMedium)
                }

                // Manual Entry Button
                OutlinedButton(
                    onClick = { showManualEntry = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manual", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Results Section
            if (isLoading) {
                Text("Search Results", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().shimmer())
                        }
                    }
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { retryTick++ }) {
                        Text("Retry")
                    }
                }
            } else if (results.isNotEmpty()) {
                Text("Search Results", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(
                        items = results,
                        key = { index, book -> "${book.isbn}_$index" }
                    ) { _, book ->
                        BookSuggestionCard(
                            book = book,
                            onAdd = { previewBook = it }
                        )
                    }
                }
            }
        }
    }
    
    previewBook?.let { book ->
        BookPreviewDialog(
            book = book,
            onDismiss = { previewBook = null },
            onConfirmAdd = { status, startPage, format ->
                onAddBook(book.copy(currentPage = startPage), status, format)
                previewBook = null
            }
        )
    }

    if (showManualEntry) {
        ManualAddBookDialog(
            onDismiss = { showManualEntry = false },
            onConfirmAdd = { metadata, status, format ->
                onAddBook(metadata, status, format)
                showManualEntry = false
            }
        )
    }
}

@Composable
fun BookSuggestionCard(book: BookMetadata, onAdd: (BookMetadata) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAdd(book) 
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(url = book.coverUrl, modifier = Modifier.size(width = 48.dp, height = 72.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.authors.joinToString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun BookPreviewDialog(
    book: BookMetadata,
    onDismiss: () -> Unit,
    onConfirmAdd: (BookStatus, Int, com.example.booktracker.shared.models.BookFormat) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(BookStatus.SHORTLIST) }
    var selectedFormat by remember { mutableStateOf(com.example.booktracker.shared.models.BookFormat.Paperback) }
    var startPage by remember { mutableStateOf("") }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BookCover(url = book.coverUrl, modifier = Modifier.size(width = 120.dp, height = 180.dp))
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = book.authors.joinToString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                if (book.pageCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${book.pageCount} pages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (book.description.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                androidx.compose.material3.OutlinedTextField(
                    value = startPage,
                    onValueChange = { startPage = it },
                    label = { Text("Start Page (optional)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.booktracker.shared.models.BookFormat.values().forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.name) }
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Add to...",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                
                val options = listOf(
                    BookStatus.SHORTLIST,
                    BookStatus.UP_NEXT,
                    BookStatus.READING
                ).map { it to it.label }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (status, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedStatus = status }
                                .background(
                                    if (selectedStatus == status) MaterialTheme.colorScheme.primaryContainer 
                                    else Color.Transparent
                                )
                                .padding(12.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedStatus == status) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirmAdd(selectedStatus, startPage.toIntOrNull() ?: 0, selectedFormat) }) {
                        Text("Save Book")
                    }
                }
            }
        }
    }
}

@Composable
fun ManualAddBookDialog(
    onDismiss: () -> Unit,
    onConfirmAdd: (BookMetadata, BookStatus, com.example.booktracker.shared.models.BookFormat) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var creators by remember { mutableStateOf(listOf(com.example.booktracker.shared.models.Creator(com.example.booktracker.shared.models.CreatorRole.Author, ""))) }
    var pages by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var publishedDate by remember { mutableStateOf("") }
    var genres by remember { mutableStateOf("") }
    var startPage by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(BookStatus.SHORTLIST) }
    var selectedFormat by remember { mutableStateOf(com.example.booktracker.shared.models.BookFormat.Paperback) }
    
    var purchaseLogs by remember { mutableStateOf(emptyList<com.example.booktracker.shared.models.PurchaseLog>()) }
    var showPurchaseLogForm by remember { mutableStateOf(false) }
    
    var loanRecords by remember { mutableStateOf(emptyList<com.example.booktracker.shared.models.LoanRecord>()) }
    var showLoanRecordForm by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val file = File(context.filesDir, "cover_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            coverUrl = "file://${file.absolutePath}"
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Manual Entry",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Creators",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                creators.forEachIndexed { index, creator ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = creator.name,
                            onValueChange = { newName ->
                                val updated = creators.toMutableList()
                                updated[index] = creator.copy(name = newName)
                                creators = updated
                            },
                            label = { Text("Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }) {
                                Text(creator.role.name)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                com.example.booktracker.shared.models.CreatorRole.values().forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role.name) },
                                        onClick = {
                                            val updated = creators.toMutableList()
                                            updated[index] = creator.copy(role = role)
                                            creators = updated
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            if (creators.size > 1) {
                                val updated = creators.toMutableList()
                                updated.removeAt(index)
                                creators = updated
                            }
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
                TextButton(onClick = {
                    creators = creators + com.example.booktracker.shared.models.Creator(com.example.booktracker.shared.models.CreatorRole.Author, "")
                }) {
                    Text("+ Add Creator")
                }
                Spacer(Modifier.height(8.dp))

                val pagesInt = pages.toIntOrNull()
                val isPagesError = pagesInt != null && pagesInt < 0
                OutlinedTextField(
                    value = pages,
                    onValueChange = { pages = it.filter { char -> char.isDigit() } },
                    label = { Text("Total Pages") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = isPagesError
                )
                if (isPagesError) {
                    Text(
                        "Total pages cannot be negative",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp).align(Alignment.Start)
                    )
                }
                Spacer(Modifier.height(8.dp))

                val startPageInt = startPage.toIntOrNull()
                val isStartPageError = startPageInt != null && (startPageInt < 0 || (pagesInt != null && pagesInt > 0 && startPageInt > pagesInt))
                OutlinedTextField(
                    value = startPage,
                    onValueChange = { startPage = it.filter { char -> char.isDigit() } },
                    label = { Text("Start Page (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = isStartPageError
                )
                if (isStartPageError) {
                    Text(
                        if (startPageInt != null && startPageInt < 0) "Start page cannot be negative"
                        else "Cannot exceed total pages ($pagesInt)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp).align(Alignment.Start)
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Cover Image URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { takePictureLauncher.launch() }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = "Take Photo")
                    }
                }
                if (coverUrl.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    BookCover(url = coverUrl, modifier = Modifier.size(80.dp, 120.dp))
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = publishedDate,
                    onValueChange = { publishedDate = it },
                    label = { Text("Published Date (e.g. 2023)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = genres,
                    onValueChange = { genres = it },
                    label = { Text("Genres (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                
                // Purchase Logs Accordion
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showPurchaseLogForm = !showPurchaseLogForm }) {
                            Text("Purchase Logs (${purchaseLogs.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(if (showPurchaseLogForm) "Hide" else "Show", color = MaterialTheme.colorScheme.primary)
                        }
                        if (showPurchaseLogForm) {
                            Spacer(Modifier.height(8.dp))
                            purchaseLogs.forEachIndexed { idx, log ->
                                Text("• ${log.vendor} - ${log.price} ${log.currency}")
                            }
                            var vendor by remember { mutableStateOf("") }
                            var price by remember { mutableStateOf("") }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = vendor, onValueChange = { vendor = it }, label = { Text("Vendor") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                            Button(onClick = {
                                if (vendor.isNotBlank() && price.toDoubleOrNull() != null) {
                                    purchaseLogs = purchaseLogs + com.example.booktracker.shared.models.PurchaseLog(System.currentTimeMillis(), vendor, price.toDouble(), "USD", "")
                                    vendor = ""
                                    price = ""
                                }
                            }, modifier = Modifier.padding(top = 8.dp)) { Text("Add Log") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                
                // Loan Records Accordion
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showLoanRecordForm = !showLoanRecordForm }) {
                            Text("Loan Records (${loanRecords.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(if (showLoanRecordForm) "Hide" else "Show", color = MaterialTheme.colorScheme.primary)
                        }
                        if (showLoanRecordForm) {
                            Spacer(Modifier.height(8.dp))
                            loanRecords.forEachIndexed { idx, log ->
                                Text("• Lent to ${log.lender}")
                            }
                            var lender by remember { mutableStateOf("") }
                            OutlinedTextField(value = lender, onValueChange = { lender = it }, label = { Text("Lender Name") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
                            Button(onClick = {
                                if (lender.isNotBlank()) {
                                    loanRecords = loanRecords + com.example.booktracker.shared.models.LoanRecord(System.currentTimeMillis(), 0L, lender, "")
                                    lender = ""
                                }
                            }, modifier = Modifier.padding(top = 8.dp)) { Text("Add Record") }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Format",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.booktracker.shared.models.BookFormat.values().forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.name) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Add to...",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))

                val options = listOf(
                    BookStatus.SHORTLIST,
                    BookStatus.UP_NEXT,
                    BookStatus.READING
                ).map { it to it.label }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (status, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedStatus = status }
                                .background(
                                    if (selectedStatus == status) MaterialTheme.colorScheme.primaryContainer 
                                    else Color.Transparent
                                )
                                .padding(12.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedStatus == status) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalCreators = creators.filter { it.name.isNotBlank() }.ifEmpty { listOf(com.example.booktracker.shared.models.Creator(com.example.booktracker.shared.models.CreatorRole.Author, "Unknown Author")) }
                            val metadata = BookMetadata(
                                title = title.takeIf { it.isNotBlank() } ?: "Unknown Title",
                                authors = finalCreators.map { it.name },
                                creators = finalCreators,
                                isbn = "manual_${System.currentTimeMillis()}", // Fake ISBN
                                coverUrl = coverUrl.trim(),
                                pageCount = pages.toIntOrNull() ?: 0,
                                publishedDate = publishedDate.trim(),
                                genres = genres.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                description = description.trim(),
                                currentPage = startPage.toIntOrNull() ?: 0,
                                purchaseLog = purchaseLogs,
                                loanRecord = loanRecords
                            )
                            onConfirmAdd(metadata, selectedStatus, selectedFormat)
                        },
                        enabled = title.isNotBlank() && !isPagesError && !isStartPageError
                    ) {
                        Text("Save Book")
                    }
                }
            }
        }
    }
}
