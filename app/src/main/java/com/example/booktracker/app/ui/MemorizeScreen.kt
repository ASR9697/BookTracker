package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.staggeredEntrance
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Style
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
    onDeleteNote: (noteId: String) -> Unit = {},
    reviewDue: Int = 0,
    wordCount: Int = 0,
    onOpenReview: () -> Unit = {},
    onOpenWords: () -> Unit = {}
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

    SectionPage(
        title = "Notes",
        line = when {
            reviewDue > 0 -> "$reviewDue ${if (reviewDue == 1) "card" else "cards"} due for review"
            notes.isEmpty() -> "Your highlights and thoughts"
            else -> "${notes.size} ${if (notes.size == 1) "note" else "notes"} · all caught up"
        },
        actions = {
            if (view == NotesView.CARDS) {
                IconButton(onClick = { shuffleSeed++ }) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle cards")
                }
            }
            IconButton(onClick = {
                searching = !searching
                if (!searching) query = ""
            }) {
                Icon(
                    if (searching) Icons.Rounded.Close else Icons.Rounded.Search,
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
                            if (option == NotesView.LIST) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.Style,
                            contentDescription = if (option == NotesView.LIST) "Show as list" else "Show as flashcards",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        // The daily review and the word list live with the notes they come from.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NotesShortcut(
                icon = Icons.Rounded.AutoAwesome,
                title = "Daily review",
                subtitle = if (reviewDue > 0) "$reviewDue due today" else "All caught up",
                highlighted = reviewDue > 0,
                onClick = onOpenReview,
                modifier = Modifier.weight(1f)
            )
            NotesShortcut(
                icon = Icons.Rounded.Spellcheck,
                title = "Words",
                subtitle = if (wordCount == 0) "Save words you meet" else "$wordCount saved",
                highlighted = false,
                onClick = onOpenWords,
                modifier = Modifier.weight(1f)
            )
        }

        if (searching) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search quotes, thoughts, books, authors") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).focusRequester(focus),
                shape = AppShape.M
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
                TickFilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = !showFavoritesOnly },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            if (showFavoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                (listOf<NoteType?>(null) + NoteType.entries).forEach { type ->
                    TickFilterChip(
                        selected = typeFilter == type,
                        onClick = { typeFilter = type },
                        label = { Text(type?.shortLabel() ?: "All") }
                    )
                }
                if (view == NotesView.LIST) {
                    Box {
                        TickFilterChip(
                            selected = false,
                            onClick = { showSortMenu = true },
                            label = { Text(sort.label) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
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
            EmptyState(
                icon = when {
                    notes.isEmpty() -> androidx.compose.material.icons.Icons.Rounded.FormatQuote
                    showFavoritesOnly && typeFilter == null -> androidx.compose.material.icons.Icons.Rounded.FavoriteBorder
                    else -> androidx.compose.material.icons.Icons.Rounded.SearchOff
                },
                title = when {
                    notes.isEmpty() -> "No notes yet"
                    showFavoritesOnly && typeFilter == null -> "No favourites yet"
                    else -> "Nothing matches"
                },
                message = when {
                    notes.isEmpty() -> "Your quotes and thoughts appear here. Add a note from any book's page."
                    query.isNotBlank() -> "No notes match \"${query.trim()}\"."
                    showFavoritesOnly && typeFilter == null -> "Tap the heart on a note to add it here."
                    else -> "No notes match these filters."
                }
            )
        } else if (view == NotesView.LIST && LocalWidthTier.current.atLeastMedium) {
            // Tablets: notes as a masonry wall, so short quotes don't leave a screen of empty card.
            val feedEntrance = rememberEntranceState()
            androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Adaptive(minSize = 320.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp + LocalNavBarClearance.current),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(listNotes, key = { _, note -> note.id }) { index, note ->
                    Box(Modifier.animateItem().staggeredEntrance(feedEntrance, index)) {
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
            }
        } else if (view == NotesView.LIST) {
            val feedEntrance = rememberEntranceState()
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp + LocalNavBarClearance.current),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(listNotes, key = { _, note -> note.id }) { index, note ->
                    Box(Modifier.animateItem().staggeredEntrance(feedEntrance, index)) {
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
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { displayNotes.size })
            // A flashcard keeps a card's proportions on a tablet; neighbours peek in at the sides.
            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = centredPadding(max = 560.dp, min = 32.dp)),
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
                    shape = AppShape.XL,
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
                                        Icons.Rounded.Share,
                                        contentDescription = "Share quote",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onToggleFavorite(note.id) }) {
                                Icon(
                                    if (note.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Mark as favorite",
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
                                        shape = AppShape.S,
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
                                    book?.title ?: "Unknown book",
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
            Spacer(Modifier.height(LocalNavBarClearance.current))
        }
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
        shape = AppShape.L,
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
                // On a tablet a note can be dragged out as text, into a doc or chat open alongside.
                val dragOut = if (LocalWidthTier.current.atLeastMedium) {
                    Modifier.draggableText("Note") { JournalingEngine.render(note.content).text }
                } else {
                    Modifier
                }
                Text(
                    JournalingEngine.render(note.content),
                    style = noteTextStyle(),
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp).then(dragOut)
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
                        if (note.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
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
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Copy note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (book != null) {
                    IconButton(onClick = { onShare(book) }) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = "Share quote",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "More note actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesShortcut(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = AppShape.M,
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
