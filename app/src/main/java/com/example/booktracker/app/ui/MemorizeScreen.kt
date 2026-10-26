package com.example.booktracker.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.journal.JournalingEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class NotesView { LIST, CARDS }

private enum class FeedSort(val label: String) { NEWEST("Newest"), OLDEST("Oldest"), BOOK("By book") }

private val feedStampFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * The Notes tab: every note as a scrollable feed (default) or as shuffled
 * flashcards. Favourite and type filters apply to both views.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MemorizeScreen(
    notes: List<MarginNote>,
    books: List<Book>,
    onToggleFavorite: (String) -> Unit,
    onOpenBook: (Book) -> Unit = {},
    onUpdateNote: (noteId: String, page: Int, text: String, type: NoteType, images: List<String>) -> Unit = { _, _, _, _, _ -> },
    onDeleteNote: (noteId: String) -> Unit = {}
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var noteToEdit by remember { mutableStateOf<MarginNote?>(null) }
    var noteToDelete by remember { mutableStateOf<MarginNote?>(null) }
    var view by rememberSaveable { mutableStateOf(NotesView.LIST) }
    var showFavoritesOnly by rememberSaveable { mutableStateOf(false) }
    var typeFilter by rememberSaveable { mutableStateOf<NoteType?>(null) }
    var sort by rememberSaveable { mutableStateOf(FeedSort.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var noteToShare by remember { mutableStateOf<Pair<MarginNote, Book>?>(null) }
    val booksById = remember(books) { books.associateBy { it.id } }

    val eligibleNotes = remember(notes, showFavoritesOnly, typeFilter, query, booksById) {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        notes.filter { note ->
            (!showFavoritesOnly || note.isFavorite) && (typeFilter == null || note.type == typeFilter) &&
                // Every word must appear in the note, or in its book's title or author.
                terms.all { term ->
                    note.content.lowercase().contains(term) ||
                        booksById[note.bookId]?.let { b ->
                            b.title.lowercase().contains(term) || b.authors.any { it.lowercase().contains(term) }
                        } == true
                }
        }
    }

    // Keep shuffle order stable against toggling favorites; only re-shuffle when seed or note IDs change
    val noteIds = remember(eligibleNotes.map { it.id }.toSet(), shuffleSeed) {
        eligibleNotes.map { it.id }.shuffled()
    }
    val notesById = remember(notes) { notes.associateBy { it.id } }
    val displayNotes = remember(noteIds, notesById) {
        noteIds.mapNotNull { notesById[it] }
    }
    val listNotes = remember(eligibleNotes, sort, booksById) {
        when (sort) {
            FeedSort.NEWEST -> eligibleNotes.sortedByDescending { it.timestamp }
            FeedSort.OLDEST -> eligibleNotes.sortedBy { it.timestamp }
            // Notes whose book is gone sort last.
            FeedSort.BOOK -> eligibleNotes.sortedWith(
                compareBy<MarginNote> { booksById[it.bookId]?.title?.lowercase() ?: "￿" }.thenBy { it.pageNumber }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Notes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (notes.isNotEmpty()) {
                    Text(
                        "${notes.size} ${if (notes.size == 1) "note" else "notes"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (view == NotesView.CARDS) {
                IconButton(onClick = { shuffleSeed++ }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle cards")
                }
            }
            IconButton(onClick = {
                searching = !searching
                if (!searching) query = ""
            }) {
                Icon(
                    if (searching) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (searching) "Close search" else "Search notes"
                )
            }
            SingleChoiceSegmentedButtonRow {
                NotesView.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = view == option,
                        onClick = { view = option },
                        shape = SegmentedButtonDefaults.itemShape(index, NotesView.entries.size),
                        icon = {}
                    ) {
                        Icon(
                            if (option == NotesView.LIST) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.Style,
                            contentDescription = if (option == NotesView.LIST) "Show as list" else "Show as flashcards",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (searching) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search quotes, thoughts, books, authors") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).focusRequester(focus),
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (notes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = !showFavoritesOnly },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                (listOf<NoteType?>(null) + NoteType.entries).forEach { type ->
                    FilterChip(
                        selected = typeFilter == type,
                        onClick = { typeFilter = type },
                        label = { Text(type?.shortLabel() ?: "All") }
                    )
                }
                if (view == NotesView.LIST) {
                    Box {
                        FilterChip(
                            selected = false,
                            onClick = { showSortMenu = true },
                            label = { Text(sort.label) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            FeedSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        sort = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (eligibleNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        notes.isEmpty() -> "Your quotes and thoughts appear here. Add a note from any book's page."
                        query.isNotBlank() -> "No notes match \"${query.trim()}\"."
                        showFavoritesOnly && typeFilter == null -> "No favourite notes yet. Tap the heart on a note to add it here."
                        else -> "No notes match these filters."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else if (view == NotesView.LIST) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listNotes, key = { it.id }) { note ->
                    NoteFeedCard(
                        note = note,
                        book = booksById[note.bookId],
                        onOpenBook = onOpenBook,
                        onToggleFavorite = { onToggleFavorite(note.id) },
                        onShare = { book -> noteToShare = note to book },
                        onEdit = { noteToEdit = note },
                        onDelete = { noteToDelete = note }
                    )
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { displayNotes.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                val note = displayNotes[page]
                val book = books.find { it.id == note.bookId }

                var isFlipped by remember(note.id) { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (book != null) {
                                IconButton(onClick = { noteToShare = note to book }) {
                                    Icon(
                                        Icons.Filled.Share,
                                        contentDescription = "Share Quote",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onToggleFavorite(note.id) }) {
                                Icon(
                                    if (note.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (note.isFavorite) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!isFlipped) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (note.pageNumber > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            "Page ${note.pageNumber}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                                Text(
                                    book?.title ?: "Unknown Book",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                if (book?.authors?.isNotEmpty() == true) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "by ${book.authors.first()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(36.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "Tap to flip",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 40.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "“${note.content}”",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 26.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Text(
                "Swipe for next card",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    noteToEdit?.let { note ->
        val book = booksById[note.bookId]
        NoteSheet(
            suggestedPage = note.pageNumber,
            totalPages = book?.totalPages ?: 0,
            initialText = note.content,
            initialType = note.type,
            initialImages = note.imageUris,
            pageLabel = book?.let { FormatAdaptabilityLayer.getDisplayUnit(it).trimEnd('s') } ?: "Page",
            isEditing = true,
            onDismiss = { noteToEdit = null },
            onConfirm = { page, text, type, images ->
                onUpdateNote(note.id, page, text, type, images)
                noteToEdit = null
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete this note?") },
            text = { Text(note.content.take(140).ifBlank { "This note has only photos." } + if (note.content.length > 140) "…" else "") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNote(note.id)
                    noteToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } }
        )
    }

    noteToShare?.let { (note, book) ->
        QuoteCardDialog(
            note = note,
            book = book,
            onDismiss = { noteToShare = null }
        )
    }
}

/** One note in the feed: which book it came from, what kind it is, and the text. */
@Composable
private fun NoteFeedCard(
    note: MarginNote,
    book: Book?,
    onOpenBook: (Book) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: (Book) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Card(
        onClick = { book?.let(onOpenBook) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                BookCover(book?.coverUrl.orEmpty(), Modifier.size(width = 32.dp, height = 48.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book?.title ?: "Unknown book",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    book?.authors?.firstOrNull()?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        note.type.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (note.content.isNotBlank()) {
                Text(
                    JournalingEngine.render(note.content),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (note.imageUris.isNotEmpty()) {
                if (note.content.isNotBlank()) Spacer(Modifier.height(12.dp))
                NotePhotos(note.imageUris, modifier = Modifier.padding(end = 8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val where = if (note.pageNumber > 0 && book != null) {
                    "${FormatAdaptabilityLayer.unitAbbrev(book)} ${note.pageNumber} · "
                } else ""
                Text(
                    where + Instant.ofEpochMilli(note.timestamp).atZone(ZoneId.systemDefault()).format(feedStampFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleFavorite()
                }) {
                    Icon(
                        if (note.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Note", note.content))
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (book != null) {
                    IconButton(onClick = { onShare(book) }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share quote",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More note actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}
